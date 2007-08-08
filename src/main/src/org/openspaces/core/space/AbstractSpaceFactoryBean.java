/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openspaces.core.space;

import com.gigaspaces.cluster.activeelection.ISpaceModeListener;
import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.j_spaces.core.IJSpace;
import com.j_spaces.core.SecurityContext;
import com.j_spaces.core.admin.IInternalRemoteJSpaceAdmin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspaces.core.space.mode.AfterSpaceModeChangeEvent;
import org.openspaces.core.space.mode.BeforeSpaceModeChangeEvent;
import org.openspaces.core.util.SpaceUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.dao.DataAccessException;

import java.rmi.RemoteException;

/**
 * Base class for most space factory beans responsible for creating/finding {@link IJSpace}
 * implementation.
 *
 * <p>Provides support for raising Spring application events: {@link BeforeSpaceModeChangeEvent} and
 * {@link AfterSpaceModeChangeEvent} alerting other beans of the current space mode
 * (primary/backup). Beans that wish to be notified of it should implement Spring
 * {@link org.springframework.context.ApplicationListener}. Note that this space mode events might
 * be raised more than once for the same space mode, and beans that listen to it should take it into
 * account.
 *
 * <p>The space mode event will be raised regardless of the space "type" that is used. For embedded
 * spaces, an actual space mode event listener will be registered with the actual cluster member (if
 * not in cluster mode, the actual space). For remote space lookups (jini/rmi), no listener will be
 * registered and Space mode events will still be raised during context refresh with a
 * <code>PRIMARY</code> mode in order to allow beans to be written regardless of how the space is
 * looked up.
 *
 * <p>Derived classes should implement the {@link #doCreateSpace()} to obtain the {@link IJSpace}.
 *
 * @author kimchy
 */
public abstract class AbstractSpaceFactoryBean implements InitializingBean, DisposableBean, FactoryBean,
        ApplicationContextAware, ApplicationListener {

    protected Log logger = LogFactory.getLog(getClass());

    private IJSpace space;

    private ApplicationContext applicationContext;

    private SpaceMode currentSpaceMode;

    private PrimaryBackupListener primaryBackupListener;

    private Boolean registerForSpaceMode;

    private SecurityConfig securityConfig;

    /**
     * Sets if the space should register for primary backup (mode) notifications. Default behaviour (if the flag was not set)
     * will register to primary backup notification if the space was found using an embedded
     * protocol, and will not register for notification if the space was found using <code>rmi</code>
     * or <code>jini</code> protocols.
     */
    public void setRegisterForSpaceModeNotifications(boolean registerForSpaceMode) {
        this.registerForSpaceMode = registerForSpaceMode;
    }

    /**
     * Sets security confiugration for the Space. If not set, no security will be used.
     */
    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    /**
     * Injected by Spring thanks to {@link ApplicationContextAware}.
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Initializes the space by calling the {@link #doCreateSpace()}.
     *
     * <p>Registers with the Space an internal space mode listener in order to be able to send Spring
     * level {@link BeforeSpaceModeChangeEvent} and {@link AfterSpaceModeChangeEvent} for primary
     * and backup handling of different beans within the context. The registration is based on
     * {@link #isRegisterForSpaceModeNotifications()}.
     */
    public void afterPropertiesSet() throws DataAccessException {
        this.space = doCreateSpace();
        // apply security configuration if set
        if (securityConfig != null) {
            SecurityContext securityContext = new SecurityContext(securityConfig.getUsername(), securityConfig.getPassword());
            securityContext.setEncrypted(securityConfig.isEncrypted());
            securityContext.setPermissions(securityConfig.getPermissions());
        }

        // register the space mode listener with the space
        if (isRegisterForSpaceModeNotifications()) {
            primaryBackupListener = new PrimaryBackupListener();
            try {
                IJSpace clusterMemberSpace = SpaceUtils.getClusterMemberSpace(space);
                ISpaceModeListener remoteListener = (ISpaceModeListener) clusterMemberSpace.getStubHandler()
                        .exportObject(primaryBackupListener);
                currentSpaceMode = ((IInternalRemoteJSpaceAdmin) clusterMemberSpace.getAdmin()).addSpaceModeListener(remoteListener);
                if (logger.isDebugEnabled()) {
                    logger.debug("Space [" + clusterMemberSpace + "] mode is [" + currentSpaceMode + "]");
                }
            } catch (RemoteException e) {
                throw new CannotCreateSpaceException("Failed to register space mode listener with space [" + space
                        + "]", e);
            }
        } else {
            currentSpaceMode = SpaceMode.PRIMARY;
        }
    }

    /**
     * Destroys the space and unregisters the internal space mode listener (if registered).
     */
    public void destroy() throws Exception {
        if (isRegisterForSpaceModeNotifications()) {
            // unregister the space mode listener
            IJSpace clusterMemberSpace = SpaceUtils.getClusterMemberSpace(space);
            try {
                ISpaceModeListener remoteListener = (ISpaceModeListener) clusterMemberSpace.getStubHandler()
                        .exportObject(primaryBackupListener);
                ((IInternalRemoteJSpaceAdmin) clusterMemberSpace.getAdmin()).removeSpaceModeListener(remoteListener);
            } catch (RemoteException e) {
                logger.warn("Failed to unregister space mode listener with space [" + space + "]", e);
            }
        }
        try {
            if (!SpaceUtils.isRemoteProtocol(space)) {
                // shutdown the space if we are in embedded mode
                space.getContainer().shutdown();
            }
        } finally {
            space = null;
        }
    }

    /**
     * If {@link ContextRefreshedEvent} is raised will send two extra events:
     * {@link BeforeSpaceModeChangeEvent} and {@link AfterSpaceModeChangeEvent} with the current
     * space mode. This is done since other beans that use this events might not catch them while
     * the context is constructed.
     *
     * <p>Note, this will mean that events with the same Space mode might be raised, one after the
     * other, and Spring beans that listens for them should take it into account.
     */
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextRefreshedEvent) {
            if (applicationContext != null) {
                applicationContext.publishEvent(new BeforeSpaceModeChangeEvent(space, currentSpaceMode));
                applicationContext.publishEvent(new AfterSpaceModeChangeEvent(space, currentSpaceMode));
            }
        }
    }

    /**
     * Spring factory bean returning the {@link IJSpace} created during the bean initialization
     * ({@link #afterPropertiesSet()}).
     *
     * @return The {@link IJSpace} implementation
     * @throws Exception
     */
    public Object getObject() throws Exception {
        return this.space;
    }

    /**
     * Returns the object type of the factory bean. Defaults to IJSpace class or the actual
     * {@link IJSpace} implementation class.
     */
    public Class<? extends IJSpace> getObjectType() {
        return (space == null ? IJSpace.class : space.getClass());
    }

    /**
     * Returns <code>true</code> since this factory is a singleton.
     */
    public boolean isSingleton() {
        return true;
    }

    /**
     * Responsible for creating/finding the actual {@link IJSpace} implementation.
     *
     * @return The IJSpace implementation used for the factory bean
     * @throws DataAccessException
     */
    protected abstract IJSpace doCreateSpace() throws DataAccessException;

    /**
     * Returns if the space should register for primary backup notifications. If {@link #setRegisterForSpaceModeNotifications(boolean)}
     * was set, will return this flag. If not, will register to primary backup notification if the space was
     * found using an embedded protocol, and will not register for notification if the space was found using <code>rmi</code>
     * or <code>jini</code> protocols.
     */
    protected boolean isRegisterForSpaceModeNotifications() {
        if (registerForSpaceMode != null) {
            return registerForSpaceMode;
        }
        return !SpaceUtils.isRemoteProtocol(space);
    }

    private class PrimaryBackupListener implements ISpaceModeListener {

        public void beforeSpaceModeChange(SpaceMode spaceMode) throws RemoteException {
            currentSpaceMode = spaceMode;
            if (logger.isDebugEnabled()) {
                logger.debug("Space [" + space + "] BEFORE mode is [" + currentSpaceMode + "]");
            }
            if (applicationContext != null) {
                applicationContext.publishEvent(new BeforeSpaceModeChangeEvent(space, spaceMode));
            }
        }

        public void afterSpaceModeChange(SpaceMode spaceMode) throws RemoteException {
            currentSpaceMode = spaceMode;
            if (logger.isDebugEnabled()) {
                logger.debug("Space [" + space + "] AFTER mode is [" + currentSpaceMode + "]");
            }
            if (applicationContext != null) {
                applicationContext.publishEvent(new AfterSpaceModeChangeEvent(space, spaceMode));
            }
        }
    }
}

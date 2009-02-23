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

package org.openspaces.admin.pu;

import org.openspaces.admin.AdminAware;
import org.openspaces.admin.StatisticsMonitor;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.events.BackupGridServiceManagerChangedEventManager;
import org.openspaces.admin.pu.events.ManagingGridServiceManagerChangedEventManager;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventManager;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceLifecycleEventListener;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventManager;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceStatisticsChangedEventManager;
import org.openspaces.admin.pu.events.ProcessingUnitSpaceCorrelatedEventManager;
import org.openspaces.admin.pu.events.ProcessingUnitStatusChangedEventManager;
import org.openspaces.admin.space.Space;

import java.util.concurrent.TimeUnit;

/**
 * A processing unit holds one or more {@link org.openspaces.admin.pu.ProcessingUnitInstance}s.
 *
 * @author kimchy
 */
public interface ProcessingUnit extends Iterable<ProcessingUnitInstance>, AdminAware, StatisticsMonitor {

    /**
     * Returns the handle to all the different processing units.
     */
    ProcessingUnits getProcessingUnits();

    /**
     * Returns the name of the processing unit.
     */
    String getName();

    /**
     * Retruns the number of instances of the processing unit.
     */
    int getNumberOfInstances();

    /**
     * Returns the number of backups (if the topology is a backup one) per instance.
     */
    int getNumberOfBackups();

    /**
     * Returns the deployment status of the processing unit.
     */
    DeploymentStatus getStatus();

    /**
     * Waits till at least the provided number of Processing Unit Instances are up.
     */
    boolean waitFor(int numberOfProcessingUnitInstances);

    /**
     * Waits till at least the provided number of Processing Unit Instances are up for the specified timeout.
     */
    boolean waitFor(int numberOfProcessingUnitInstances, long timeout, TimeUnit timeUnit);

    /**
     * Waits till an embedded Space is correlated with the processing unit.
     */
    Space waitForSpace();

    /**
     * Waits till an embedded Space is correlated with the processing unit for the specified timeout.
     */
    Space waitForSpace(long timeout, TimeUnit timeUnit);

    /**
     * Returns <code>true</code> if this processing unit allows to increment instances on it.
     */
    boolean canIncrementInstance();

    /**
     * Returns <code>true</code> if this processing unit allows to decrement instances on it.
     */
    boolean canDecrementInstance();

    /**
     * Will increment a processing unit instance.
     */
    void incrementInstance();

    /**
     * Will randomly decrement an instance from the processing units. For more fine
     * grained control see {@link ProcessingUnitInstance#decrement()}.
     */
    void decrementInstance();

    /**
     * Returns <code>true</code> if there is a managing GSM for it.
     */
    boolean isManaged();

    /**
     * Returns the managing (primary) GSM for the processing unit.
     */
    GridServiceManager getManagingGridServiceManager();

    /**
     * Returns the backup GSMs for the processing unit.
     */
    GridServiceManager[] getBackupGridServiceManagers();

    /**
     * Returns the backup GSM matching the provided UID.
     */
    GridServiceManager getBackupGridServiceManager(String gridServiceManagerUID);

    /**
     * Undeploys the processing unit.
     */
    void undeploy();

    /**
     * Returns the (first) embedded space within a processing unit. Returns <code>null</code> if
     * no embedded space is defined within the processing unit or if no processing unit instance
     * has been added to the processing unit.
     */
    Space getSpace();

    /**
     * Returns all the embedded spaces within a processing unit. Returns an empty array if there
     * are no embedded spaces defined within the processing unit, or none has been associated with
     * the processing unit yet.
     */
    Space[] getSpaces();

    /**
     * Returns the processing unit instances currently discovered.
     */
    ProcessingUnitInstance[] getInstances();

    /**
     * Returns the processing unit paritions of this processing unit.
     */
    ProcessingUnitPartition[] getPartitions();

    /**
     * Retruns a processign unit parititon based on the specified partition id.
     */
    ProcessingUnitPartition getPartition(int partitionId);

    /**
     * Retruns an event manager allowing to register {@link org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener}s.
     */
    ProcessingUnitInstanceAddedEventManager getProcessingUnitInstanceAdded();

    /**
     * Retruns an event manager allowing to register {@link org.openspaces.admin.pu.events.ProcessingUnitInstanceRemovedEventListener}s.
     */
    ProcessingUnitInstanceRemovedEventManager getProcessingUnitInstanceRemoved();

    /**
     * Adds a {@link ProcessingUnitInstanceLifecycleEventListener}.
     */
    void addLifecycleListener(ProcessingUnitInstanceLifecycleEventListener eventListener);

    /**
     * Removes a {@link ProcessingUnitInstanceLifecycleEventListener}.
     */
    void removeLifecycleListener(ProcessingUnitInstanceLifecycleEventListener eventListener);

    /**
     * Returns an event manger allowing to listen for {@link org.openspaces.admin.pu.events.ManagingGridServiceManagerChangedEvent}s.
     */
    ManagingGridServiceManagerChangedEventManager getManagingGridServiceManagerChanged();

    /**
     * Returns an event manager allowing to listen for {@link org.openspaces.admin.pu.events.BackupGridServiceManagerChangedEvent}s.
     */
    BackupGridServiceManagerChangedEventManager getBackupGridServiceManagerChanged();

    /**
     * Retruns an event manager allowing to listen for {@link org.openspaces.admin.pu.events.ProcessingUnitStatusChangedEvent}s.
     */
    ProcessingUnitStatusChangedEventManager getProcessingUnitStatusChanged();

    /**
     * Returns an event manager allowing to listen for {@link org.openspaces.admin.pu.events.ProcessingUnitSpaceCorrelatedEvent}s.
     */
    ProcessingUnitSpaceCorrelatedEventManager getSpaceCorrelated();

    /**
     * Returns a processing unit instance statistics change event manger allowing to register for
     * events of {@link org.openspaces.admin.pu.events.ProcessingUnitInstanceStatisticsChangedEvent}.
     *
     * <p>Note, in order to receive events, the virtual machines need to be in a "statistics" monitored
     * state.
     */
    ProcessingUnitInstanceStatisticsChangedEventManager getProcessingUnitInstanceStatisticsChange();
}

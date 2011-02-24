package org.openspaces.grid.gsm.containers;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.grid.gsm.sla.ServiceLevelAgreementEnforcementEndpoint;
import org.openspaces.grid.gsm.sla.ServiceLevelAgreementEnforcementEndpointDestroyedException;

/**
 * A service that on demand enforces the specified number of containers. 
 *  
 * @author itaif
 *
 * @see ContainersSlaPolicy
 */
public interface ContainersSlaEnforcementEndpoint 
    extends ServiceLevelAgreementEnforcementEndpoint<ContainersSlaPolicy> {
    
    /**
     * @return the list of containers managed by this service.
     * @throws ServiceLevelAgreementEnforcementEndpointDestroyedException 
     */
    GridServiceContainer[] getContainers() throws ServiceLevelAgreementEnforcementEndpointDestroyedException;

    /**
     * @return true if there are one or more containers managed that need to be cleared of processing units. 
     * 
     * Unless all PU instances are relocated the sla would not be reached.
     * @throws ServiceLevelAgreementEnforcementEndpointDestroyedException 
     * 
     */
    boolean isContainersPendingDeallocation() throws ServiceLevelAgreementEnforcementEndpointDestroyedException;
}

package org.openspaces.admin.pu.elastic.topology;

import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.core.util.MemoryUnit;

import com.gigaspaces.security.directory.UserDetails;

public interface ElasticDeploymentTopology {

    /**
     * Sets the processing unit name that will be deployed. By default it will be based on the
     * parameter passed in the constructor.
     */
    ElasticDeploymentTopology name(String name);

   /**
    * Defines a context deploy time property overriding any <code>${...}</code> defined within a processing
    * unit configuration.
    */
    ElasticDeploymentTopology addContextProperty(String key, String value);

    /**
     * Will deploy a secured processing unit. Note, by setting user details the processing unit will be secured automatically.
     */
    ElasticDeploymentTopology secured(boolean secured);

    /**
     * Advanced: Sets the security user details for authentication and authorization of the
     * processing unit.
     */
    ElasticDeploymentTopology userDetails(UserDetails userDetails);

    /**
     * Advanced: Sets the security user details for authentication and authorization of the
     * processing unit.
     */ 
    ElasticDeploymentTopology userDetails(String userName, String password);

    /**
     * Will cause the {@link org.openspaces.admin.gsc.GridServiceContainer} to be started using a script
     * and not a pure Java process.
     */
    ElasticDeploymentTopology useScriptToStartContainer();

    /**
     * Will cause JVM options added using {@link #commandLineArgument(String)} to override all the vm arguments
     * that the JVM will start by default with.
     */
    ElasticDeploymentTopology overrideCommandLineArguments();

    /**
     * Will add a JVM level argument when the process is executed using pure JVM. For example, the memory
     * can be controlled using <code>-Xmx512m</code>.
     */
    ElasticDeploymentTopology commandLineArgument(String vmInputArgument);

    /**
     * Sets an environment variable that will be passed to forked process.
     */
    ElasticDeploymentTopology environmentVariable(String name, String value);
    
    /**
     * Enables the server side bean that starts and stops machines automatically.
     * For example, the bean could delegate the request to a cloud provider.  
     */
    ElasticDeploymentTopology machineProvisioning(ElasticMachineProvisioningConfig config);
    
    /**
     * Specifies the the heap size per container (operating system process)
     * For example: 
     * memoryCapacityPerContainer(256,MemoryUnit.MEGABYTES) is equivalent to
     * commandLineArgument("-Xmx256m").commandLineArgument("-Xms256m")
     */
    ElasticDeploymentTopology memoryCapacityPerContainer(int memoryCapacityPerContainer, MemoryUnit unit);
    
    /**
     * Specifies the the heap size per container (operating system process)
     * For example: 
     * memoryCapacityPerContainer("256m") is equivalent to
     * commandLineArgument("-Xmx256m").commandLineArgument("-Xms256m")
     */
    ElasticDeploymentTopology memoryCapacityPerContainer(String memoryCapacityPerContainer);


}
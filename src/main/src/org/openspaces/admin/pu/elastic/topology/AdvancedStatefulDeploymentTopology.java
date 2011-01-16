package org.openspaces.admin.pu.elastic.topology;




public interface AdvancedStatefulDeploymentTopology extends ElasticStatefulDeploymentTopology {

    /**
     * Overrides the number of backup processing unit instances per partition.
     */
    public AdvancedStatefulDeploymentTopology numberOfBackupsPerPartition(int numberOfBackupsPerPartition);
    
    /**
     * Overrides the number of processing unit partitions.
     */
    public AdvancedStatefulDeploymentTopology numberOfPartitions(int numberOfPartitions);

    /**
     * If specified, allows deployment of the processing unit on a single machine, by lifting the limitation
     * for primary and backup processing unit instances from the same partition to be deployed on different machines.
     * Default value is false (by default primary instances and backup instances need separate machines).
     */
    public AdvancedStatefulDeploymentTopology allowDeploymentOnSingleMachine(boolean allowDeploymentOnSingleMachine);

    /**
     * If specified, allows deployment of the processing unit on the same machine as the Grid Service Managers and Lookup Services.
     * Default value is true (by default processing unit can deploy on same machine as Grid Service Manager)
     */
    public AdvancedStatefulDeploymentTopology allowDeploymentOnManagementMachine(boolean allowDeploymentOnManagementMachine);
    
    /**
     * Overrides the minimum number of CPU cores per machine assumption.
     */
    public AdvancedStatefulDeploymentTopology minNumberOfCpuCoresPerMachine(double minNumberOfCpuCoresPerMachine);
}

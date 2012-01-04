package org.openspaces.admin.internal.esm;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.jini.core.lookup.ServiceID;

import org.jini.rio.monitor.event.Events;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.dump.DumpResult;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.internal.dump.InternalDumpResult;
import org.openspaces.admin.internal.support.AbstractAgentGridComponent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.elastic.config.ScaleStrategyConfig;
import org.openspaces.admin.pu.elastic.events.ElasticProcessingUnitEvent;
import org.openspaces.admin.pu.elastic.events.ElasticProcessingUnitScaleProgressChangedEvent;
import org.openspaces.grid.esm.ESM;

import com.gigaspaces.internal.jvm.JVMDetails;
import com.gigaspaces.internal.jvm.JVMStatistics;
import com.gigaspaces.internal.os.OSDetails;
import com.gigaspaces.internal.os.OSStatistics;
import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntryMatcher;
import com.gigaspaces.log.LogProcessType;
import com.gigaspaces.lrmi.nio.info.NIODetails;
import com.gigaspaces.lrmi.nio.info.NIOStatistics;

/**
 * @author Moran Avigdor
 * @author itaif
 */
public class DefaultElasticServiceManager extends AbstractAgentGridComponent implements InternalElasticServiceManager {

    private final ServiceID serviceID;

    private final ESM esm;

    private final Map<String, Boolean> esmScaleIsInProgressPerProcessingUnit;

    public DefaultElasticServiceManager(ServiceID serviceID, ESM esm, InternalAdmin admin, int agentId, String agentUid)
    throws RemoteException {
        super(admin, agentId, agentUid);
        this.serviceID = serviceID;
        this.esm = esm;
        this.esmScaleIsInProgressPerProcessingUnit = new ConcurrentHashMap<String, Boolean>();
    }

    public String getUid() {
        return serviceID.toString();
    }

    public ServiceID getServiceID() {
        return this.serviceID;
    }
    
    
    public LogEntries logEntries(LogEntryMatcher matcher) throws AdminException {
        if (getGridServiceAgent() != null) {
            return getGridServiceAgent().logEntries(LogProcessType.ESM, getVirtualMachine().getDetails().getPid(), matcher);
        }
        return logEntriesDirect(matcher);
    }

    public LogEntries logEntriesDirect(LogEntryMatcher matcher) throws AdminException {
        try {
            return esm.logEntriesDirect(matcher);
        } catch (IOException e) {
            throw new AdminException("Failed to get log", e);
        }
    }

    public DumpResult generateDump(String cause, Map<String, Object> context) throws AdminException {
        try {
            return new InternalDumpResult(this, esm, esm.generateDump(cause, context));
        } catch (Exception e) {
            throw new AdminException("Failed to generate dump", e);
        }
    }

    public DumpResult generateDump(String cause, Map<String, Object> context, String... processors) throws AdminException {
        try {
            return new InternalDumpResult(this, esm, esm.generateDump(cause, context, processors));
        } catch (Exception e) {
            throw new AdminException("Failed to generate dump", e);
        }
    }

    // NIO, OS, and JVM stats

    public NIODetails getNIODetails() throws RemoteException {
        return esm.getNIODetails();
    }

    public NIOStatistics getNIOStatistics() throws RemoteException {
        return esm.getNIOStatistics();
    }

    public long getCurrentTimeInMillis() throws RemoteException {
        return esm.getCurrentTimestamp();
    }

    public OSDetails getOSDetails() throws RemoteException {
        return esm.getOSDetails();
    }

    public OSStatistics getOSStatistics() throws RemoteException {
        return esm.getOSStatistics();
    }

    public JVMDetails getJVMDetails() throws RemoteException {
        return esm.getJVMDetails();
    }

    public JVMStatistics getJVMStatistics() throws RemoteException {
        return esm.getJVMStatistics();
    }

    public void runGc() throws RemoteException {
        esm.runGc();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultElasticServiceManager that = (DefaultElasticServiceManager) o;
        return serviceID.equals(that.serviceID);
    }

    @Override
    public int hashCode() {
        return serviceID.hashCode();
    }

    public void setProcessingUnitElasticProperties(ProcessingUnit pu, Map<String,String> properties) {
        try {
            esm.setProcessingUnitElasticProperties(pu.getName(),properties);
        }
        catch (RemoteException e) {
            throw new AdminException("Failed to set processing unit dynamic properties",e);
        }
    }
    
    public void setProcessingUnitScaleStrategyConfig(ProcessingUnit pu, ScaleStrategyConfig scaleStrategyConfig) {
        try {
            esm.setProcessingUnitScaleStrategy(pu.getName(), scaleStrategyConfig);
        }
        catch (RemoteException e) {
            throw new AdminException("Failed to set processing unit dynamic properties",e);
        }
    }

    public ScaleStrategyConfig getProcessingUnitScaleStrategyConfig(ProcessingUnit pu) {
        try {
            return esm.getProcessingUnitScaleStrategyConfig(pu.getName());
        }
        catch (RemoteException e) {
            throw new AdminException("Failed to set processing unit dynamic properties",e);
        }
    }

    @Override
    public boolean isManagingProcessingUnit(ProcessingUnit pu) {
        return esmScaleIsInProgressPerProcessingUnit.containsKey(pu.getName());
    }

    @Override
    public boolean isManagingProcessingUnitAndScaleNotInProgress(ProcessingUnit pu) {
        Boolean esmScaleIsInProgress = esmScaleIsInProgressPerProcessingUnit.get(pu.getName());
        return esmScaleIsInProgress != null && !esmScaleIsInProgress; 
    }

    @Override
    public boolean isManagingProcessingUnitAndScaleInProgress(ProcessingUnit pu) {
        Boolean esmScaleIsInProgress = esmScaleIsInProgressPerProcessingUnit.get(pu.getName());
        return esmScaleIsInProgress != null && esmScaleIsInProgress; 
    }
    
    @Override
    public Events getScaleStrategyEvents(long cursor, int maxNumberOfEvents) {
        try {
            return esm.getScaleStrategyEvents(cursor, maxNumberOfEvents);
        }
        catch (RemoteException e) {
            throw new AdminException("Failed to determine if scale strategy is enforced",e);
        }
    }

    @Override
    public void processElasticScaleStrategyEvent(ElasticProcessingUnitEvent event) {
        
        if (event instanceof ElasticProcessingUnitScaleProgressChangedEvent) {
            ElasticProcessingUnitScaleProgressChangedEvent progressEvent = (ElasticProcessingUnitScaleProgressChangedEvent)event;
            String puName = progressEvent.getProcessingUnitName();
            if (progressEvent.isComplete()) {
                if (progressEvent.isUndeploying()) {
                    esmScaleIsInProgressPerProcessingUnit.remove(puName);
                }
                else {
                    esmScaleIsInProgressPerProcessingUnit.put(puName,false);
                }
            }
            else {
                esmScaleIsInProgressPerProcessingUnit.put(puName,true);
            }
        }
    }
}

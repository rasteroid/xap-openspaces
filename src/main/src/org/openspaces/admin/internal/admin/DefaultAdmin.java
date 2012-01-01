package org.openspaces.admin.internal.admin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.jini.core.discovery.LookupLocator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jini.rio.core.OperationalString;
import org.openspaces.admin.AdminEventListener;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.GridComponent;
import org.openspaces.admin.alert.AlertManager;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.Applications;
import org.openspaces.admin.dump.CompoundDumpResult;
import org.openspaces.admin.dump.DumpGeneratedListener;
import org.openspaces.admin.dump.DumpProvider;
import org.openspaces.admin.dump.DumpResult;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.esm.ElasticServiceManagers;
import org.openspaces.admin.gateway.Gateways;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceAgents;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.internal.alert.DefaultAlertManager;
import org.openspaces.admin.internal.application.DefaultApplication;
import org.openspaces.admin.internal.application.DefaultApplications;
import org.openspaces.admin.internal.application.InternalApplication;
import org.openspaces.admin.internal.application.InternalApplications;
import org.openspaces.admin.internal.discovery.DiscoveryService;
import org.openspaces.admin.internal.esm.DefaultElasticServiceManagers;
import org.openspaces.admin.internal.esm.InternalElasticServiceManager;
import org.openspaces.admin.internal.esm.InternalElasticServiceManagers;
import org.openspaces.admin.internal.gateway.DefaultGateways;
import org.openspaces.admin.internal.gsa.DefaultGridServiceAgents;
import org.openspaces.admin.internal.gsa.InternalGridServiceAgent;
import org.openspaces.admin.internal.gsa.InternalGridServiceAgents;
import org.openspaces.admin.internal.gsc.DefaultGridServiceContainers;
import org.openspaces.admin.internal.gsc.InternalGridServiceContainer;
import org.openspaces.admin.internal.gsc.InternalGridServiceContainers;
import org.openspaces.admin.internal.gsm.DefaultGridServiceManagers;
import org.openspaces.admin.internal.gsm.InternalGridServiceManager;
import org.openspaces.admin.internal.gsm.InternalGridServiceManagers;
import org.openspaces.admin.internal.lus.DefaultLookupServices;
import org.openspaces.admin.internal.lus.InternalLookupService;
import org.openspaces.admin.internal.lus.InternalLookupServices;
import org.openspaces.admin.internal.machine.DefaultMachine;
import org.openspaces.admin.internal.machine.DefaultMachines;
import org.openspaces.admin.internal.machine.InternalMachine;
import org.openspaces.admin.internal.machine.InternalMachineAware;
import org.openspaces.admin.internal.machine.InternalMachines;
import org.openspaces.admin.internal.os.DefaultOperatingSystem;
import org.openspaces.admin.internal.os.DefaultOperatingSystems;
import org.openspaces.admin.internal.os.InternalOperatingSystem;
import org.openspaces.admin.internal.os.InternalOperatingSystemInfoProvider;
import org.openspaces.admin.internal.os.InternalOperatingSystems;
import org.openspaces.admin.internal.pu.DefaultProcessingUnit;
import org.openspaces.admin.internal.pu.DefaultProcessingUnitInstances;
import org.openspaces.admin.internal.pu.DefaultProcessingUnits;
import org.openspaces.admin.internal.pu.InternalProcessingUnit;
import org.openspaces.admin.internal.pu.InternalProcessingUnitInstance;
import org.openspaces.admin.internal.pu.InternalProcessingUnitInstances;
import org.openspaces.admin.internal.pu.InternalProcessingUnits;
import org.openspaces.admin.internal.pu.events.InternalProcessingUnitInstanceAddedEventManager;
import org.openspaces.admin.internal.pu.events.InternalProcessingUnitInstanceRemovedEventManager;
import org.openspaces.admin.internal.space.DefaultSpace;
import org.openspaces.admin.internal.space.DefaultSpaces;
import org.openspaces.admin.internal.space.InternalSpace;
import org.openspaces.admin.internal.space.InternalSpaceInstance;
import org.openspaces.admin.internal.space.InternalSpaces;
import org.openspaces.admin.internal.support.EventRegistrationHelper;
import org.openspaces.admin.internal.support.InternalAgentGridComponent;
import org.openspaces.admin.internal.support.NetworkExceptionHelper;
import org.openspaces.admin.internal.transport.DefaultTransport;
import org.openspaces.admin.internal.transport.DefaultTransports;
import org.openspaces.admin.internal.transport.InternalTransport;
import org.openspaces.admin.internal.transport.InternalTransportInfoProvider;
import org.openspaces.admin.internal.transport.InternalTransports;
import org.openspaces.admin.internal.vm.DefaultVirtualMachine;
import org.openspaces.admin.internal.vm.DefaultVirtualMachines;
import org.openspaces.admin.internal.vm.InternalVirtualMachine;
import org.openspaces.admin.internal.vm.InternalVirtualMachineInfoProvider;
import org.openspaces.admin.internal.vm.InternalVirtualMachines;
import org.openspaces.admin.internal.zone.DefaultZone;
import org.openspaces.admin.internal.zone.DefaultZones;
import org.openspaces.admin.internal.zone.InternalZone;
import org.openspaces.admin.internal.zone.InternalZoneAware;
import org.openspaces.admin.internal.zone.InternalZones;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.lus.LookupServices;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.machine.Machines;
import org.openspaces.admin.os.OperatingSystem;
import org.openspaces.admin.os.OperatingSystems;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitType;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.pu.elastic.events.ElasticProcessingUnitEvent;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.space.SpaceInstance;
import org.openspaces.admin.space.Spaces;
import org.openspaces.admin.transport.TransportDetails;
import org.openspaces.admin.transport.Transports;
import org.openspaces.admin.vm.VirtualMachine;
import org.openspaces.admin.vm.VirtualMachines;
import org.openspaces.admin.zone.Zone;
import org.openspaces.admin.zone.ZoneAware;
import org.openspaces.admin.zone.Zones;
import org.openspaces.core.space.SpaceServiceDetails;
import org.openspaces.grid.gsm.strategy.ElasticScaleStrategyEvents;
import org.openspaces.pu.service.ServiceMonitors;

import com.gigaspaces.grid.gsa.AgentProcessesDetails;
import com.gigaspaces.grid.gsa.GSA;
import com.gigaspaces.grid.gsm.PUDetails;
import com.gigaspaces.grid.gsm.PUsDetails;
import com.gigaspaces.internal.jvm.JVMDetails;
import com.gigaspaces.internal.os.OSDetails;
import com.gigaspaces.lrmi.nio.info.NIODetails;
import com.gigaspaces.security.directory.UserDetails;
import com.j_spaces.kernel.GSThreadFactory;

/**
 * @author kimchy
 * @author itaif - added Applications and ESM event processing
 */
public class DefaultAdmin implements InternalAdmin {

    private static final Log logger = LogFactory.getLog(DefaultAdmin.class);
    
    private static final int DEFAULT_EVENT_LISTENER_THREADS = 10;
    private static final DiscardPolicy DEFAULT_EVENT_LISTENER_REJECTED_POLICY = new ThreadPoolExecutor.DiscardPolicy();
    
    private static final int DEFAULT_STATE_CHANGE_THREADS = 10;

    private ScheduledThreadPoolExecutor scheduledExecutorService;

    private final DiscoveryService discoveryService;

    private final InternalLookupServices lookupServices = new DefaultLookupServices(this);

    private final InternalZones zones = new DefaultZones(this);
    
    private final InternalApplications applications = new DefaultApplications(this);

    private final InternalMachines machines = new DefaultMachines(this);

    private final InternalGridServiceAgents gridServiceAgents = new DefaultGridServiceAgents(this);

    private final InternalGridServiceManagers gridServiceManagers = new DefaultGridServiceManagers(this);
    
    private final InternalElasticServiceManagers elasticServiceManagers = new DefaultElasticServiceManagers(this);

    private final InternalGridServiceContainers gridServiceContainers = new DefaultGridServiceContainers(this);

    private final Gateways gateways = new DefaultGateways(this);
    
    private final InternalTransports transports = new DefaultTransports(this);

    private final InternalOperatingSystems operatingSystems = new DefaultOperatingSystems(this);

    private final InternalVirtualMachines virtualMachines = new DefaultVirtualMachines(this);

    private final InternalProcessingUnits processingUnits = new DefaultProcessingUnits(this);

    private final InternalProcessingUnitInstances processingUnitInstances = new DefaultProcessingUnitInstances(this);

    private final Map<String, InternalAgentGridComponent> orphanedAgentGridComponents = new ConcurrentHashMap<String, InternalAgentGridComponent>();

    private final InternalSpaces spaces = new DefaultSpaces(this);

    private ExecutorService[] eventsExecutorServices;

    private ExecutorService longRunningExecutorService;
    
    private LinkedList<Runnable>[] eventsQueue;

    private volatile long scheduledProcessingUnitMonitorInterval = 1000; // default to one second

    private volatile long scheduledAgentProcessessMonitorInterval = 5000; // defaults to 5 seconds

    private volatile long scheduledSpaceMonitorInterval = 1000; // default to one second

    private Future scheduledAgentProcessessMonitorFuture;

    private Future scheduledProcessingUnitMonitorFuture;

    private boolean scheduledStatisticsMonitor = false;

    private volatile boolean closed = false;

    private volatile UserDetails userDetails;

    private long defaultTimeout = Long.MAX_VALUE;

    private TimeUnit defaultTimeoutTimeUnit = TimeUnit.MILLISECONDS;

    private boolean singleThreadedEventListeners = false;

    private long executorSingleThreadId;

    private final AlertManager alertManager;

    private boolean useDaemonThreads;
    
    private final AtomicInteger eventListenersCount = new AtomicInteger();

    public DefaultAdmin() {
        this.discoveryService = new DiscoveryService(this);
        this.alertManager = new DefaultAlertManager(this);
    }

    @Override
    public String[] getGroups() {
        return discoveryService.getGroups();
    }

    @Override
    public LookupLocator[] getLocators() {
        return discoveryService.getLocators();
    }

    @Override
    public UserDetails getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    public void addGroup(String group) {
        discoveryService.addGroup(group);
    }

    public void addLocator(String locator) {
        discoveryService.addLocator(locator);
    }
    
    @Override
    public void setStatisticsInterval(long interval, TimeUnit timeUnit) {
        this.spaces.setStatisticsInterval(interval, timeUnit);
        this.virtualMachines.setStatisticsInterval(interval, timeUnit);
        this.transports.setStatisticsInterval(interval, timeUnit);
        this.operatingSystems.setStatisticsInterval(interval, timeUnit);
        this.processingUnits.setStatisticsInterval(interval, timeUnit);
    }

    @Override
    public void setStatisticsHistorySize(int historySize) {
        this.spaces.setStatisticsHistorySize(historySize);
        this.virtualMachines.setStatisticsHistorySize(historySize);
        this.transports.setStatisticsHistorySize(historySize);
        this.operatingSystems.setStatisticsHistorySize(historySize);
        this.processingUnits.setStatisticsHistorySize(historySize);
    }

    public void setUseDaemonThreads(boolean useDaemonThreads) {
        this.useDaemonThreads = useDaemonThreads;
    }
    
    @Override
    public void singleThreadedEventListeners() {
      this.singleThreadedEventListeners = true;
    }

    @Override
    public synchronized void startStatisticsMonitor() {
        scheduledStatisticsMonitor = true;
        this.spaces.startStatisticsMonitor();
        this.virtualMachines.startStatisticsMonitor();
        this.transports.startStatisticsMonitor();
        this.operatingSystems.startStatisticsMonitor();
        this.processingUnits.startStatisticsMonitor();
    }

    @Override
    public synchronized void stopStatisticsMonitor() {
        scheduledStatisticsMonitor = false;
        this.spaces.stopStatisticsMonitor();
        this.virtualMachines.stopStatisticsMonitor();
        this.transports.stopStatisticsMonitor();
        this.operatingSystems.stopStatisticsMonitor();
        this.processingUnits.stopStatisticsMonitor();
    }

    @Override
    public boolean isMonitoring() {
        return scheduledStatisticsMonitor;
    }
    
    public void begin() {
        
        this.longRunningExecutorService = createThreadPoolExecutor("admin-state-change-thread",DEFAULT_STATE_CHANGE_THREADS);
        
        int numberOfThreads = singleThreadedEventListeners ? 1 :  DEFAULT_EVENT_LISTENER_THREADS;
        this.eventsExecutorServices = new ExecutorService[numberOfThreads];
        eventsQueue = new LinkedList[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            eventsExecutorServices[i] = createThreadPoolExecutor("admin-event-executor-tread", 1, singleThreadedEventListeners);
            eventsQueue[i] = new LinkedList<Runnable>();
        }
    
        this.scheduledExecutorService = createScheduledThreadPoolExecutor("admin-scheduled-executor-thread",5);

        discoveryService.start();
        scheduledProcessingUnitMonitorFuture = scheduledExecutorService.scheduleWithFixedDelay(
                new ScheduledProcessingUnitMonitor(), scheduledProcessingUnitMonitorInterval, scheduledProcessingUnitMonitorInterval, TimeUnit.MILLISECONDS);
        scheduledAgentProcessessMonitorFuture = scheduledExecutorService.scheduleWithFixedDelay(new ScheduledAgentProcessessMonitor(),
                scheduledAgentProcessessMonitorInterval, scheduledAgentProcessessMonitorInterval, TimeUnit.MILLISECONDS);
    }

    private ScheduledThreadPoolExecutor createScheduledThreadPoolExecutor(String threadName, int numberOfThreads) {
        final ClassLoader correctClassLoader = Thread.currentThread().getContextClassLoader();
        ScheduledThreadPoolExecutor executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(numberOfThreads, 
                   new GSThreadFactory(threadName,useDaemonThreads) {
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread thread = super.newThread(r);
                            thread.setContextClassLoader(correctClassLoader);
                            return thread;
                    }});
        return executorService;
    }
    
    private ThreadPoolExecutor createThreadPoolExecutor(String threadName, int numberOfThreads) {
        return createThreadPoolExecutor(threadName,numberOfThreads,false);
    }
    
    private ThreadPoolExecutor createThreadPoolExecutor(String threadName, int numberOfThreads, final boolean updateSingleThreadId) {
        final ClassLoader correctClassLoader = Thread.currentThread().getContextClassLoader();
        ThreadPoolExecutor executorService = 
            (ThreadPoolExecutor) Executors.newFixedThreadPool(numberOfThreads, 
                    new GSThreadFactory(threadName,useDaemonThreads) {
           
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = super.newThread(r);
                        thread.setContextClassLoader(correctClassLoader);
                        if (updateSingleThreadId) {
                            DefaultAdmin.this.executorSingleThreadId = thread.getId();
                        }
                        return thread;
                    }});
                executorService.setRejectedExecutionHandler(DEFAULT_EVENT_LISTENER_REJECTED_POLICY);
        return executorService;
    }

    @Override
    public void setProcessingUnitMonitorInterval(long interval, TimeUnit timeUnit) {
        if (closed) {
            throw new IllegalStateException("Admin already closed");
        }
        this.scheduledProcessingUnitMonitorInterval = timeUnit.toMillis(interval);
        if (scheduledProcessingUnitMonitorFuture != null) { // during initialization
            scheduledProcessingUnitMonitorFuture.cancel(false);
            scheduledProcessingUnitMonitorFuture = this.scheduleWithFixedDelay(new ScheduledProcessingUnitMonitor(), interval, interval, timeUnit);
        }
    }

    @Override
    public void setAgentProcessessMonitorInterval(long interval, TimeUnit timeUnit) {
        if (closed) {
            throw new IllegalStateException("Admin already closed");
        }
        this.scheduledAgentProcessessMonitorInterval = timeUnit.toMillis(interval);
        if (scheduledAgentProcessessMonitorFuture != null) { // during initialization
            scheduledAgentProcessessMonitorFuture.cancel(false);
            scheduledAgentProcessessMonitorFuture = this.scheduleWithFixedDelay(new ScheduledAgentProcessessMonitor(), interval, interval, timeUnit);
        }
    }

    @Override
    public long getScheduledSpaceMonitorInterval() {
        return scheduledSpaceMonitorInterval;
    }
    
    @Override
    public long getDefaultTimeout() {
        return defaultTimeout;
    }

    @Override
    public TimeUnit getDefaultTimeoutTimeUnit() {
        return defaultTimeoutTimeUnit;
    }

    public DiscoveryService getDiscoveryService() {
        return this.discoveryService;
    }

    @Override
    public synchronized void setSpaceMonitorInterval(long interval, TimeUnit timeUnit) {
        this.scheduledSpaceMonitorInterval = timeUnit.toMillis(interval);
        this.spaces.refreshScheduledSpaceMonitors();
    }

    @Override
    public ScheduledThreadPoolExecutor getScheduler() {
        return this.scheduledExecutorService;
    }
    
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return getScheduler().scheduleWithFixedDelay(new LoggerRunnable(command), initialDelay, delay, unit);
    }

    @Override
    public void setSchedulerCorePoolSize(int coreThreads) {
        scheduledExecutorService.setCorePoolSize(coreThreads);
    }
    
    @Override
    public void setDefaultTimeout(long timeout, TimeUnit timeUnit) {
        this.defaultTimeout = timeout;
        this.defaultTimeoutTimeUnit = timeUnit;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        discoveryService.stop();
        scheduledExecutorService.shutdownNow();
        for (ExecutorService executorService : eventsExecutorServices) {
            executorService.shutdownNow();
        }
    }

    @Override
    public LookupServices getLookupServices() {
        return this.lookupServices;
    }

    @Override
    public GridServiceAgents getGridServiceAgents() {
        return this.gridServiceAgents;
    }

    @Override
    public GridServiceManagers getGridServiceManagers() {
        return this.gridServiceManagers;
    }
    

    @Override
    public ElasticServiceManagers getElasticServiceManagers() {
        return this.elasticServiceManagers;
    }

    @Override
    public GridServiceContainers getGridServiceContainers() {
        return this.gridServiceContainers;
    }
    
    @Override
    public Gateways getGateways() {
        return this.gateways;
    }
    
    @Override
    public GridComponent getGridComponentByUID(String uid) {
        GridComponent component = getGridServiceAgents().getAgentByUID(uid);
        if (component == null) {
            component = getElasticServiceManagers().getManagerByUID(uid);
            if (component == null) {
                component = getGridServiceManagers().getManagerByUID(uid);
                if (component == null) {
                    component = getGridServiceContainers().getContainerByUID(uid);
                    if (component == null) {
                        component = getLookupServices().getLookupServiceByUID(uid);
                    }
                }
            }
        }
        return component;
    }

    @Override
    public Machines getMachines() {
        return this.machines;
    }

    @Override
    public Zones getZones() {
        return this.zones;
    }
    
    @Override
    public Applications getApplications() {
        return this.applications;
    }

    @Override
    public Transports getTransports() {
        return this.transports;
    }

    @Override
    public VirtualMachines getVirtualMachines() {
        return this.virtualMachines;
    }

    @Override
    public OperatingSystems getOperatingSystems() {
        return operatingSystems;
    }

    @Override
    public ProcessingUnits getProcessingUnits() {
        return this.processingUnits;
    }

    @Override
    public Spaces getSpaces() {
        return this.spaces;
    }
    
    @Override
    public AlertManager getAlertManager() {
        return alertManager;
    }

    @Override
    public void addEventListener(AdminEventListener eventListener) {
        EventRegistrationHelper.addEventListener(this, eventListener);
        eventListenersCount.incrementAndGet();
    }

    @Override
    public void removeEventListener(AdminEventListener eventListener) {
        EventRegistrationHelper.removeEventListener(this, eventListener);
        eventListenersCount.decrementAndGet();
    }
    
    @Override
    public int getEventListenersCount(){
        return eventListenersCount.get();
    }

    @Override
    public synchronized void pushEvent(Object listener, Runnable notifier) {
        eventsQueue[Math.abs(listener.hashCode() % eventsExecutorServices.length)].add(new LoggerRunnable(notifier));
    }

    @Override
    public synchronized void pushEventAsFirst(Object listener, Runnable notifier) {
        eventsQueue[Math.abs(listener.hashCode() % eventsExecutorServices.length)].addFirst(new LoggerRunnable(notifier));
    }

    public synchronized void flushEvents() {
        if (closed) {
            //clear all pending events in queue that may have arrived just before closing of the admin.
            for (LinkedList<Runnable> l : eventsQueue) {
                l.clear();
            }
            return;
        }
        
        for (int i = 0; i < eventsExecutorServices.length; i++) {
            for (Runnable notifier : eventsQueue[i]) {
                eventsExecutorServices[i].submit(notifier);
            }
            eventsQueue[i].clear();
        }
    }

    @Override
    public synchronized void raiseEvent(Object listener, Runnable notifier) {
        eventsExecutorServices[Math.abs(listener.hashCode() % eventsExecutorServices.length)].submit(new LoggerRunnable(notifier));
    }

    @Override
    public void scheduleNonBlockingStateChange(Runnable command) {
        if (singleThreadedEventListeners) {
            raiseEvent(this,command);
        }
        else {
            command.run();
        }
    }

   
    @Override
    public void scheduleAdminOperation(Runnable command) {
        longRunningExecutorService.submit(command);
    }
    
    @Override
    public void assertStateChangesPermitted() {
        
        if (singleThreadedEventListeners &&
            Thread.currentThread().getId() != executorSingleThreadId) {
            
            throw new IllegalStateException("Assertion Failure. Cannot change admin state from this thread. Call scheduleNonBlockingStateChange(runnable) instead.");
        }
    }
    
    @Override
    public synchronized void addGridServiceAgent(InternalGridServiceAgent gridServiceAgent, NIODetails nioDetails, OSDetails osDetails, JVMDetails jvmDetails, String jmxUrl, String[] zones) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding GSA uid=" + gridServiceAgent.getUid());
        }
        OperatingSystem operatingSystem = processOperatingSystemOnServiceAddition(gridServiceAgent, osDetails);
        VirtualMachine virtualMachine = processVirtualMachineOnServiceAddition(gridServiceAgent, jvmDetails, jmxUrl);
        InternalTransport transport = processTransportOnServiceAddition(gridServiceAgent, nioDetails, virtualMachine);

        InternalMachine machine = processMachineOnServiceAddition(transport.getDetails(),
                transport, operatingSystem, virtualMachine,
                (InternalMachineAware) virtualMachine, gridServiceAgent);

        processZonesOnServiceAddition(zones, gridServiceAgent.getUid(), transport, virtualMachine, machine, gridServiceAgent);

        ((InternalGridServiceAgents) machine.getGridServiceAgents()).addGridServiceAgent(gridServiceAgent);
        ((InternalGridServiceAgents) ((InternalVirtualMachine) virtualMachine).getGridServiceAgents()).addGridServiceAgent(gridServiceAgent);
        for (Zone zone : gridServiceAgent.getZones().values()) {
            ((InternalGridServiceAgents) zone.getGridServiceAgents()).addGridServiceAgent(gridServiceAgent);
        }

        gridServiceAgents.addGridServiceAgent(gridServiceAgent);


        for (Iterator<Map.Entry<String, InternalAgentGridComponent>> it = orphanedAgentGridComponents.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, InternalAgentGridComponent> entry = it.next();
            if (entry.getValue().getAgentUid().equals(gridServiceAgent.getUid())) {
                entry.getValue().setGridServiceAgent(gridServiceAgent);
                it.remove();
            }
        }

        flushEvents();
    }

    @Override
    public synchronized void removeGridServiceAgent(String uid) {
        if (logger.isDebugEnabled()) {
            logger.debug("Removing GSA uid=" + uid);
        }
        InternalGridServiceAgent gridServiceAgent = gridServiceAgents.removeGridServiceAgent(uid);
        if (gridServiceAgent != null) {
            gridServiceAgent.setDiscovered(false);
            processTransportOnServiceRemoval(gridServiceAgent, gridServiceAgent, gridServiceAgent);
            processOperatingSystemOnServiceRemoval(gridServiceAgent, gridServiceAgent);

            processVirtualMachineOnServiceRemoval(gridServiceAgent, gridServiceAgent, gridServiceAgent);
            ((InternalGridServiceAgents) ((InternalVirtualMachine) gridServiceAgent.getVirtualMachine()).getGridServiceAgents()).removeGridServiceAgent(uid);

            processMachineOnServiceRemoval(gridServiceAgent, gridServiceAgent);
            ((InternalGridServiceAgents) ((InternalMachine) gridServiceAgent.getMachine()).getGridServiceAgents()).removeGridServiceAgent(uid);

            processZonesOnServiceRemoval(uid, gridServiceAgent);
            for (Zone zone : gridServiceAgent.getZones().values()) {
                ((InternalGridServiceAgents) zone.getGridServiceAgents()).removeGridServiceAgent(uid);
            }

            for (Iterator<Map.Entry<String, InternalAgentGridComponent>> it = orphanedAgentGridComponents.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, InternalAgentGridComponent> entry = it.next();
                if (entry.getValue().getAgentUid().equals(gridServiceAgent.getUid())) {
                    it.remove();
                }
            }
        }

        flushEvents();
    }

    @Override
    public synchronized void addLookupService(InternalLookupService lookupService,
            NIODetails nioDetails, OSDetails osDetails, JVMDetails jvmDetails, String jmxUrl, String[] zones) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding LUS uid=" + lookupService.getUid());
        }
        OperatingSystem operatingSystem = processOperatingSystemOnServiceAddition(lookupService, osDetails);
        VirtualMachine virtualMachine = processVirtualMachineOnServiceAddition(lookupService, jvmDetails, jmxUrl );
        InternalTransport transport = processTransportOnServiceAddition(lookupService, nioDetails, virtualMachine);

        InternalMachine machine = processMachineOnServiceAddition(transport.getDetails(),
                transport, operatingSystem, virtualMachine,
                (InternalMachineAware) virtualMachine, lookupService);

        processZonesOnServiceAddition(zones, lookupService.getUid(), transport, virtualMachine, machine, lookupService);
        processAgentOnServiceAddition(lookupService);

        ((InternalLookupServices) machine.getLookupServices()).addLookupService(lookupService);
        ((InternalLookupServices) ((InternalVirtualMachine) virtualMachine).getLookupServices()).addLookupService(lookupService);

        for (Zone zone : lookupService.getZones().values()) {
            ((InternalLookupServices) zone.getLookupServices()).addLookupService(lookupService);
        }

        lookupServices.addLookupService(lookupService);

        flushEvents();
    }

    @Override
    public synchronized void removeLookupService(String uid) {
        if (logger.isDebugEnabled()) {
            logger.debug("Removing LUS uid=" + uid);
        }
        InternalLookupService lookupService = lookupServices.removeLookupService(uid);
        if (lookupService != null) {
            lookupService.setDiscovered(false);
            processTransportOnServiceRemoval(lookupService, lookupService, lookupService);
            processOperatingSystemOnServiceRemoval(lookupService, lookupService);
            processVirtualMachineOnServiceRemoval(lookupService, lookupService, lookupService);

            processMachineOnServiceRemoval(lookupService, lookupService);
            ((InternalLookupServices) ((InternalMachine) lookupService.getMachine()).getLookupServices()).removeLookupService(uid);

            processZonesOnServiceRemoval(uid, lookupService);
            for (Zone zone : lookupService.getZones().values()) {
                ((InternalLookupServices) zone.getLookupServices()).removeLookupService(uid);
            }
        }

        flushEvents();
    }

    @Override
    public synchronized void addGridServiceManager(InternalGridServiceManager gridServiceManager,
            NIODetails nioDetails, OSDetails osDetails, JVMDetails jvmDetails, String jmxUrl, String[] zones) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding GSM uid=" + gridServiceManager.getUid());
        }
        OperatingSystem operatingSystem = processOperatingSystemOnServiceAddition(gridServiceManager, osDetails);
        VirtualMachine virtualMachine = processVirtualMachineOnServiceAddition(gridServiceManager, jvmDetails, jmxUrl);
        InternalTransport transport = processTransportOnServiceAddition(gridServiceManager, nioDetails, virtualMachine);

        InternalMachine machine = processMachineOnServiceAddition(transport.getDetails(),
                transport, operatingSystem, virtualMachine,
                (InternalMachineAware) virtualMachine, gridServiceManager);

        processAgentOnServiceAddition(gridServiceManager);
        processZonesOnServiceAddition(zones, gridServiceManager.getUid(), transport, virtualMachine, machine, gridServiceManager);

        ((InternalGridServiceManagers) machine.getGridServiceManagers()).addGridServiceManager(gridServiceManager);
        ((InternalGridServiceManagers) ((InternalVirtualMachine) virtualMachine).getGridServiceManagers()).addGridServiceManager(gridServiceManager);
        for (Zone zone : gridServiceManager.getZones().values()) {
            ((InternalGridServiceManagers) zone.getGridServiceManagers()).addGridServiceManager(gridServiceManager);
        }

        gridServiceManagers.addGridServiceManager(gridServiceManager);

        flushEvents();
    }

    @Override
    public synchronized void removeGridServiceManager(String uid) {
        if (logger.isDebugEnabled()) {
            logger.debug("Removing GSM uid=" + uid);
        }
        InternalGridServiceManager gridServiceManager = gridServiceManagers.removeGridServiceManager(uid);
        if (gridServiceManager != null) {
            gridServiceManager.setDiscovered(false);
            processTransportOnServiceRemoval(gridServiceManager, gridServiceManager, gridServiceManager);
            processOperatingSystemOnServiceRemoval(gridServiceManager, gridServiceManager);

            processVirtualMachineOnServiceRemoval(gridServiceManager, gridServiceManager, gridServiceManager);
            ((InternalGridServiceManagers) ((InternalVirtualMachine) gridServiceManager.getVirtualMachine()).getGridServiceManagers()).removeGridServiceManager(uid);

            processMachineOnServiceRemoval(gridServiceManager, gridServiceManager);
            ((InternalGridServiceManagers) ((InternalMachine) gridServiceManager.getMachine()).getGridServiceManagers()).removeGridServiceManager(uid);

            processZonesOnServiceRemoval(uid, gridServiceManager);
            for (Zone zone : gridServiceManager.getZones().values()) {
                ((InternalGridServiceManagers) zone.getGridServiceManagers()).removeGridServiceManager(uid);
            }
        }

        flushEvents();
    }
    
    @Override
    public synchronized void addElasticServiceManager(InternalElasticServiceManager elasticServiceManager,
            NIODetails nioDetails, OSDetails osDetails, JVMDetails jvmDetails, String jmxUrl, String[] zones) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding ESM uid=" + elasticServiceManager.getUid());
        }
        OperatingSystem operatingSystem = processOperatingSystemOnServiceAddition(elasticServiceManager, osDetails);
        VirtualMachine virtualMachine = processVirtualMachineOnServiceAddition(elasticServiceManager, jvmDetails, jmxUrl );
        InternalTransport transport = processTransportOnServiceAddition(elasticServiceManager, nioDetails, virtualMachine);

        InternalMachine machine = processMachineOnServiceAddition(transport.getDetails(),
                transport, operatingSystem, virtualMachine,
                (InternalMachineAware) virtualMachine, elasticServiceManager);

        processAgentOnServiceAddition(elasticServiceManager);
        processZonesOnServiceAddition(zones, elasticServiceManager.getUid(), transport, virtualMachine, machine, elasticServiceManager);

        ((InternalElasticServiceManagers) machine.getElasticServiceManagers()).addElasticServiceManager(elasticServiceManager);
        ((InternalElasticServiceManagers) ((InternalVirtualMachine) virtualMachine).getElasticServiceManagers()).addElasticServiceManager(elasticServiceManager);
        for (Zone zone : elasticServiceManager.getZones().values()) {
            ((InternalElasticServiceManagers) zone.getElasticServiceManagers()).addElasticServiceManager(elasticServiceManager);
        }

        elasticServiceManagers.addElasticServiceManager(elasticServiceManager);

        flushEvents();
    }

    @Override
    public synchronized void removeElasticServiceManager(String uid) {
        if (logger.isDebugEnabled()) {
            logger.debug("Removing ESM uid=" + uid);
        }
        InternalElasticServiceManager elasticServiceManager = elasticServiceManagers.removeElasticServiceManager(uid);
        if (elasticServiceManager != null) {
            elasticServiceManager.setDiscovered(false);
            processTransportOnServiceRemoval(elasticServiceManager, elasticServiceManager, elasticServiceManager);
            processOperatingSystemOnServiceRemoval(elasticServiceManager, elasticServiceManager);

            processVirtualMachineOnServiceRemoval(elasticServiceManager, elasticServiceManager, elasticServiceManager);
            ((InternalElasticServiceManagers) ((InternalVirtualMachine) elasticServiceManager.getVirtualMachine()).getElasticServiceManagers()).removeElasticServiceManager(uid);

            processMachineOnServiceRemoval(elasticServiceManager, elasticServiceManager);
            ((InternalElasticServiceManagers) ((InternalMachine) elasticServiceManager.getMachine()).getElasticServiceManagers()).removeElasticServiceManager(uid);

            processZonesOnServiceRemoval(uid, elasticServiceManager);
            for (Zone zone : elasticServiceManager.getZones().values()) {
                ((InternalElasticServiceManagers) zone.getElasticServiceManagers()).removeElasticServiceManager(uid);
            }
        }

        flushEvents();
    }

    @Override
    public synchronized void addGridServiceContainer(InternalGridServiceContainer gridServiceContainer,
            NIODetails nioDetails, OSDetails osDetails, JVMDetails jvmDetails, String jmxUrl, String[] zones) {
    	 if (logger.isDebugEnabled()) {
             logger.debug("Adding GSC uid=" + gridServiceContainer.getUid());
         }
        OperatingSystem operatingSystem = processOperatingSystemOnServiceAddition(gridServiceContainer, osDetails);
        VirtualMachine virtualMachine = processVirtualMachineOnServiceAddition(gridServiceContainer, jvmDetails, jmxUrl);
        InternalTransport transport = processTransportOnServiceAddition(gridServiceContainer, nioDetails, virtualMachine);

        InternalMachine machine = processMachineOnServiceAddition(transport.getDetails(),
                transport, operatingSystem, virtualMachine,
                (InternalMachineAware) virtualMachine, gridServiceContainer);

        processAgentOnServiceAddition(gridServiceContainer);

        processZonesOnServiceAddition(zones, gridServiceContainer.getUid(), transport, virtualMachine, machine, gridServiceContainer);

        ((InternalGridServiceContainers) machine.getGridServiceContainers()).addGridServiceContainer(gridServiceContainer);
        ((InternalGridServiceContainers) ((InternalVirtualMachine) virtualMachine).getGridServiceContainers()).addGridServiceContainer(gridServiceContainer);
        for (Zone zone : gridServiceContainer.getZones().values()) {
            ((InternalGridServiceContainers) zone.getGridServiceContainers()).addGridServiceContainer(gridServiceContainer);
        }

        gridServiceContainers.addGridServiceContainer(gridServiceContainer);

        flushEvents();
    }

    @Override
    public synchronized void removeGridServiceContainer(String uid) {
        if (logger.isDebugEnabled()) {
            logger.debug("Removing GSC uid=" + uid);
        }
        InternalGridServiceContainer gridServiceContainer = gridServiceContainers.removeGridServiceContainer(uid);
        if (gridServiceContainer != null) {
            gridServiceContainer.setDiscovered(false);
            processTransportOnServiceRemoval(gridServiceContainer, gridServiceContainer, gridServiceContainer);
            processOperatingSystemOnServiceRemoval(gridServiceContainer, gridServiceContainer);

            processVirtualMachineOnServiceRemoval(gridServiceContainer, gridServiceContainer, gridServiceContainer);
            ((InternalGridServiceContainers) ((InternalVirtualMachine) gridServiceContainer.getVirtualMachine()).getGridServiceContainers()).removeGridServiceContainer(uid);

            processMachineOnServiceRemoval(gridServiceContainer, gridServiceContainer);
            ((InternalGridServiceContainers) ((InternalMachine) gridServiceContainer.getMachine()).getGridServiceContainers()).removeGridServiceContainer(uid);

            processZonesOnServiceRemoval(uid, gridServiceContainer);
            for (Zone zone : gridServiceContainer.getZones().values()) {
                ((InternalGridServiceContainers) zone.getGridServiceContainers()).removeGridServiceContainer(uid);
            }
        }

        flushEvents();
    }

    @Override
    public synchronized void addProcessingUnitInstance(InternalProcessingUnitInstance processingUnitInstance, NIODetails nioDetails, OSDetails osDetails, JVMDetails jvmDetails, String jmxUrl, String[] zones) {
        if (logger.isDebugEnabled()) {
            logger.debug("Service Added [Processing Unit Instance] " + processingUnitInstance.getProcessingUnitInstanceName() + " with uid [" + processingUnitInstance.getUid() + "]");
        }
        OperatingSystem operatingSystem = processOperatingSystemOnServiceAddition(processingUnitInstance, osDetails);
        VirtualMachine virtualMachine = processVirtualMachineOnServiceAddition(processingUnitInstance, jvmDetails, jmxUrl);
        InternalTransport transport = processTransportOnServiceAddition(processingUnitInstance, nioDetails, virtualMachine);

        InternalMachine machine = processMachineOnServiceAddition(transport.getDetails(),
                transport, operatingSystem, virtualMachine,
                (InternalMachineAware) virtualMachine, processingUnitInstance);

        processZonesOnServiceAddition(zones, processingUnitInstance.getUid(), transport, virtualMachine, machine, processingUnitInstance);

        InternalProcessingUnit processingUnit = (InternalProcessingUnit) processingUnits.getProcessingUnit(processingUnitInstance.getClusterInfo().getName());
        InternalGridServiceContainer gridServiceContainer = (InternalGridServiceContainer) gridServiceContainers.getContainerByUID(processingUnitInstance.getGridServiceContainerServiceID().toString());

        if (processingUnit == null || gridServiceContainer == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(processingUnitInstance.getProcessingUnitInstanceName() + " is orphaned until it's hosting container is discovered");
            }
            processingUnitInstances.addOrphaned(processingUnitInstance);
        } else {
            processProcessingUnitInstanceAddition(processingUnit, processingUnitInstance);
        }

        flushEvents();
    }

    @Override
    public synchronized void removeProcessingUnitInstance(String uid, boolean removeEmbeddedSpaces) {
        if (logger.isDebugEnabled()) {
            logger.debug("Removing Processing Unit Instance uid=" + uid);
        }
        processingUnitInstances.removeOrphaned(uid);
        InternalProcessingUnitInstance processingUnitInstance = (InternalProcessingUnitInstance) processingUnitInstances.removeInstance(uid);
        if (processingUnitInstance != null) {
            processingUnitInstance.setDiscovered(false);
            ((InternalProcessingUnit) processingUnitInstance.getProcessingUnit()).removeProcessingUnitInstance(uid);
            Application application = processingUnitInstance.getProcessingUnit().getApplication();
            if (application != null) {
                ((InternalProcessingUnitInstanceRemovedEventManager) application.getProcessingUnits().getProcessingUnitInstanceRemoved()).processingUnitInstanceRemoved(processingUnitInstance);
            }
            ((InternalGridServiceContainer) processingUnitInstance.getGridServiceContainer()).removeProcessingUnitInstance(uid);
            ((InternalVirtualMachine) processingUnitInstance.getVirtualMachine()).removeProcessingUnitInstance(processingUnitInstance.getUid());
            ((InternalMachine) processingUnitInstance.getMachine()).removeProcessingUnitInstance(processingUnitInstance.getUid());
            for (Zone zone : processingUnitInstance.getZones().values()) {
                ((InternalZone) zone).removeProcessingUnitInstance(processingUnitInstance.getUid());
            }

            processTransportOnServiceRemoval(processingUnitInstance, processingUnitInstance, processingUnitInstance);
            processOperatingSystemOnServiceRemoval(processingUnitInstance, processingUnitInstance);
            processVirtualMachineOnServiceRemoval(processingUnitInstance, processingUnitInstance, processingUnitInstance);
            processMachineOnServiceRemoval(processingUnitInstance, processingUnitInstance);
            processZonesOnServiceRemoval(processingUnitInstance.getUid(), processingUnitInstance);
            if (logger.isDebugEnabled()) {
                logger.debug("removed processing unit instance " + processingUnitInstance.getProcessingUnitInstanceName() +" uid:"+uid);
            }
            if (removeEmbeddedSpaces) {
                for (SpaceServiceDetails serviceDetails : processingUnitInstance.getEmbeddedSpacesDetails()) {
                    removeSpaceInstance(serviceDetails.getServiceID().toString());
                    if (logger.isDebugEnabled()) {
                        logger.debug("removed space instance " + serviceDetails.getName() +" id:"+serviceDetails.getServiceID());
                    }
                }
            }
        }

        flushEvents();
    }

    @Override
    public synchronized void addSpaceInstance(InternalSpaceInstance spaceInstance, NIODetails nioDetails, OSDetails osDetails, JVMDetails jvmDetails, String jmxUrl, String[] zones) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding Space Instance uid=" + spaceInstance.getUid());
        }
        OperatingSystem operatingSystem = processOperatingSystemOnServiceAddition(spaceInstance, osDetails);
        VirtualMachine virtualMachine = processVirtualMachineOnServiceAddition(spaceInstance, jvmDetails, jmxUrl);
        InternalTransport transport = processTransportOnServiceAddition(spaceInstance, nioDetails, virtualMachine);

        InternalMachine machine = processMachineOnServiceAddition(transport.getDetails(),
                transport, operatingSystem, virtualMachine,
                (InternalMachineAware) virtualMachine, spaceInstance);

        processZonesOnServiceAddition(zones, spaceInstance.getUid(), transport, virtualMachine, machine, spaceInstance);

        InternalSpace space = (InternalSpace) spaces.getSpaceByName(spaceInstance.getSpaceName());
        if (space == null) {
            space = new DefaultSpace(spaces, spaceInstance.getSpaceName(), spaceInstance.getSpaceName());
            spaces.addSpace(space);
        }
        spaceInstance.setSpace(space);
        space.addInstance(spaceInstance);
        spaces.addSpaceInstance(spaceInstance);

        // go over all the processing unit instances and add the space if matching
        for (ProcessingUnit processingUnit : processingUnits) {
            for (ProcessingUnitInstance processingUnitInstance : processingUnit) {
                ((InternalProcessingUnitInstance) processingUnitInstance).addSpaceInstanceIfMatching(spaceInstance);
            }
        }

        machine.addSpaceInstance(spaceInstance);
        ((InternalVirtualMachine) virtualMachine).addSpaceInstance(spaceInstance);
        for (Zone zone : spaceInstance.getZones().values()) {
            ((InternalZone) zone).addSpaceInstance(spaceInstance);
        }

        flushEvents();
    }

    @Override
    public synchronized void removeSpaceInstance(String uid) {
        if (logger.isDebugEnabled()) {
            logger.debug("Removing Space Instance uid=" + uid);
        }
        InternalSpaceInstance spaceInstance = (InternalSpaceInstance) spaces.removeSpaceInstance(uid);
        if (spaceInstance != null) {
            spaceInstance.setDiscovered(false);
            InternalSpace space = (InternalSpace) spaces.getSpaceByName(spaceInstance.getSpaceName());
            space.removeInstance(uid);
            if (space.getSize() == 0) {
                // no more instances, remove it completely
                spaces.removeSpace(space.getUid());
            }
            ((InternalVirtualMachine) spaceInstance.getVirtualMachine()).removeSpaceInstance(spaceInstance.getUid());
            ((InternalMachine) spaceInstance.getMachine()).removeSpaceInstance(spaceInstance.getUid());
            for (Zone zone : spaceInstance.getZones().values()) {
                ((InternalZone) zone).removeSpaceInstance(uid);
            }

            processTransportOnServiceRemoval(spaceInstance, spaceInstance, spaceInstance);
            processOperatingSystemOnServiceRemoval(spaceInstance, spaceInstance);
            processVirtualMachineOnServiceRemoval(spaceInstance, spaceInstance, spaceInstance);
            processMachineOnServiceRemoval(spaceInstance, spaceInstance);
            processZonesOnServiceRemoval(uid, spaceInstance);
        }

        flushEvents();
    }

    private void processAgentOnServiceAddition(InternalAgentGridComponent agentGridComponent) {
        if (agentGridComponent.getAgentUid() == null) {
            // did not start by an agent, disard
            return;
        }
        GridServiceAgent gridServiceAgent = gridServiceAgents.getAgentByUID(agentGridComponent.getAgentUid());
        if (gridServiceAgent == null) {
            orphanedAgentGridComponents.put(agentGridComponent.getUid(), agentGridComponent);
        } else {
            agentGridComponent.setGridServiceAgent(gridServiceAgent);
        }
    }

    private synchronized void processProcessingUnitInstanceAddition(InternalProcessingUnit processingUnit, InternalProcessingUnitInstance processingUnitInstance) {
        if (logger.isDebugEnabled()) {
            logger.debug(processingUnitInstance.getProcessingUnitInstanceName() + " is being added");
        }
        processingUnitInstances.removeOrphaned(processingUnitInstance.getUid());

        processingUnitInstance.setProcessingUnit(processingUnit);
        processingUnit.addProcessingUnitInstance(processingUnitInstance);
        Application application = processingUnit.getApplication();
        if (application != null) {
            ((InternalProcessingUnitInstanceAddedEventManager) application.getProcessingUnits().getProcessingUnitInstanceAdded()).processingUnitInstanceAdded(processingUnitInstance);
        }
        
        InternalGridServiceContainer gridServiceContainer = (InternalGridServiceContainer) gridServiceContainers.getContainerByUID(processingUnitInstance.getGridServiceContainerServiceID().toString());
        if (gridServiceContainer == null) {
            throw new IllegalStateException("Internal error in admin, should not happen");
        }
        processingUnitInstance.setGridServiceContainer(gridServiceContainer);
        gridServiceContainer.addProcessingUnitInstance(processingUnitInstance);

        ((InternalMachine) processingUnitInstance.getMachine()).addProcessingUnitInstance(processingUnitInstance);
        ((InternalVirtualMachine) processingUnitInstance.getVirtualMachine()).addProcessingUnitInstance(processingUnitInstance);
        for (Zone zone : processingUnitInstance.getZones().values()) {
            ((InternalZone) zone).addProcessingUnitInstance(processingUnitInstance);
        }

        // go over all the space instances, and add the matched one to the processing unit
        for (Space space : spaces) {
            for (SpaceInstance spaceInstance : space) {
                processingUnitInstance.addSpaceInstanceIfMatching(spaceInstance);
            }
        }

        processingUnitInstances.addInstance(processingUnitInstance);
        
        if (logger.isDebugEnabled()) {
            logger.debug(processingUnitInstance.getProcessingUnitInstanceName() + " has been added");
        }
    }

    private void processZonesOnServiceAddition(String[] zonesNames, String zoneUidProvider,
            InternalTransport transport, VirtualMachine virtualMachine, Machine machine,
            InternalZoneAware... zoneAwares) {
        if (zonesNames == null) {
            return;
        }
        for (String zoneName : zonesNames) {
            InternalZone zone = (InternalZone) zones.getByName(zoneName);
            if (zone == null) {
                zone = new DefaultZone(this, zoneName);
            }
            zones.addZone(zone, zoneUidProvider);
            ((InternalTransports) zone.getTransports()).addTransport(transport);
            ((InternalVirtualMachines) zone.getVirtualMachines()).addVirtualMachine(virtualMachine);
            ((InternalMachines) zone.getMachines()).addMachine((InternalMachine) machine);
            for (InternalZoneAware zoneAware : zoneAwares) {
                zoneAware.addZone(zone);
            }
        }
    }
    
    private void processApplicationsOnProcessingUnitAddition(ProcessingUnit processingUnit) {
        
        //TODO: [itaif] Discover application through Lookup Service and add orphan PU detection (PU that was discovered before its Application)
        String applicationName = ((InternalProcessingUnit)processingUnit).getApplicationName();
        if (applicationName == null || applicationName.length() == 0) {
            return;
        }
        
        InternalApplication application = (InternalApplication) applications.getApplication(applicationName);
        if (application == null) {
            application = new DefaultApplication(this, applicationName);
        }
        applications.addApplication(application, processingUnit);
    }
    
    private void processZonesOnServiceRemoval(String zoneUidProvider, ZoneAware zoneAware) {
        for (Zone zone : zoneAware.getZones().values()) {
            zones.removeProvider(zone, zoneUidProvider);
        }
    }

    private void processApplicationsOnProcessingUnitRemoval(ProcessingUnit processingUnit) {
        applications.removeProcessingUnit(processingUnit);
    }
    
    private InternalMachine processMachineOnServiceAddition(TransportDetails transportDetails,
            InternalTransport transport, OperatingSystem operatingSystem,
            VirtualMachine virtualMachine, InternalMachineAware... machineAwares) {
        InternalMachine machine = (InternalMachine) machines.getMachineByHostAddress(transportDetails.getHostAddress());
        if (machine == null) {
            machine = new DefaultMachine(this, transportDetails.getHostAddress(), transportDetails.getHostAddress());
            machine.setOperatingSystem(operatingSystem);
            machines.addMachine(machine);
        }
        ((InternalTransports) machine.getTransports()).addTransport(transport);
        ((InternalVirtualMachines) machine.getVirtualMachines()).addVirtualMachine(virtualMachine);
        for (InternalMachineAware machineAware : machineAwares) {
            machineAware.setMachine(machine);
        }
        return machine;
    }

    private void processMachineOnServiceRemoval(InternalMachineAware machineAware, ZoneAware zoneAware) {
        Machine machine = machineAware.getMachine();
        machine = machines.getMachineByUID(machine.getUid());
        if (machine != null) {
            machines.removeMachine(machine);
            for (Zone zone : zoneAware.getZones().values()) {
                ((InternalMachines) zone.getMachines()).removeMachine(machine);
            }
        }
    }

    private InternalVirtualMachine processVirtualMachineOnServiceAddition(InternalVirtualMachineInfoProvider vmProvider, JVMDetails jvmDetails, String jmxUrl) {
        InternalVirtualMachine virtualMachine = (InternalVirtualMachine) virtualMachines.getVirtualMachineByUID(jvmDetails.getUid());
        if (virtualMachine == null) {
            virtualMachine = new DefaultVirtualMachine(virtualMachines, jvmDetails,jmxUrl);
            virtualMachines.addVirtualMachine(virtualMachine);
        }
        virtualMachine.addVirtualMachineInfoProvider(vmProvider);
        vmProvider.setVirtualMachine(virtualMachine);
        return virtualMachine;
    }

    private void processVirtualMachineOnServiceRemoval(InternalVirtualMachineInfoProvider vmProvider, InternalMachineAware machineAware, ZoneAware zoneAware) {
        InternalVirtualMachine virtualMachine = (InternalVirtualMachine) vmProvider.getVirtualMachine();
        virtualMachine.removeVirtualMachineInfoProvider(vmProvider);
        if (!virtualMachine.hasVirtualMachineInfoProviders()) {
            virtualMachines.removeVirtualMachine(virtualMachine.getUid());
            ((InternalVirtualMachines) machineAware.getMachine().getVirtualMachines()).removeVirtualMachine(virtualMachine.getUid());
            for (Zone zone : zoneAware.getZones().values()) {
                ((InternalVirtualMachines) zone.getVirtualMachines()).removeVirtualMachine(virtualMachine.getUid());
            }
        }
    }

    private InternalTransport processTransportOnServiceAddition(InternalTransportInfoProvider txProvider, NIODetails nioDetails, VirtualMachine virtualMachine) {
        InternalTransport transport = (InternalTransport) transports.getTransportByHostAndPort(nioDetails.getBindHost(), nioDetails.getPort());
        if (transport == null) {
            transport = new DefaultTransport(nioDetails, transports);
            transport.setVirtualMachine(virtualMachine);
            transports.addTransport(transport);
        }
        transport.addTransportInfoProvider(txProvider);
        txProvider.setTransport(transport);
        return transport;
    }

    private void processTransportOnServiceRemoval(InternalTransportInfoProvider txProvider, InternalMachineAware machineAware, ZoneAware zoneAware) {
        InternalTransport transport = ((InternalTransport) txProvider.getTransport());
        transport.removeTransportInfoProvider(txProvider);
        if (!transport.hasTransportInfoProviders()) {
            transports.removeTransport(transport.getUid());
            ((InternalTransports) machineAware.getMachine().getTransports()).removeTransport(transport.getUid());
            for (Zone zone : zoneAware.getZones().values()) {
                ((InternalTransports) zone.getTransports()).removeTransport(transport.getUid());
            }
        }
    }

    private InternalOperatingSystem processOperatingSystemOnServiceAddition(InternalOperatingSystemInfoProvider osProvider, OSDetails osDetails) {
        InternalOperatingSystem os = (InternalOperatingSystem) operatingSystems.getByUID(osDetails.getUID());
        if (os == null) {
            os = new DefaultOperatingSystem(osDetails, operatingSystems);
            operatingSystems.addOperatingSystem(os);
        }
        os.addOperatingSystemInfoProvider(osProvider);
        osProvider.setOperatingSystem(os);
        return os;
    }

    private void processOperatingSystemOnServiceRemoval(InternalOperatingSystemInfoProvider osProvider, InternalMachineAware machineAware) {
        InternalOperatingSystem os = (InternalOperatingSystem) osProvider.getOperatingSystem();
        os.removeOperatingSystemInfoProvider(osProvider);
        if (!os.hasOperatingSystemInfoProviders()) {
            operatingSystems.removeOperatingSystem(os.getUid());
        }
    }

    @Override
    public DumpResult generateDump(final Set<DumpProvider> dumpProviders, final DumpGeneratedListener listener, final String cause, final Map<String, Object> context, final String... processor) throws AdminException {
        CompoundDumpResult dumpResult = new CompoundDumpResult();

        ExecutorService es = Executors.newFixedThreadPool(dumpProviders.size());
        CompletionService<DumpResult> cs = new ExecutorCompletionService<DumpResult>(es);

        final AtomicInteger counter = new AtomicInteger();
        for (final DumpProvider dumpProvider : dumpProviders) {
            cs.submit(new Callable<DumpResult>() {
                @Override
                public DumpResult call() throws Exception {
                    DumpResult result = dumpProvider.generateDump(cause, context, processor);
                    synchronized (listener) {
                        listener.onGenerated(dumpProvider, result, counter.incrementAndGet(), dumpProviders.size());
                    }
                    return result;
                }
            });
        }

        for (int i = 0; i < dumpProviders.size(); i++) {
            try {
                dumpResult.add(cs.take().get());
            } catch (Exception e) {
                // ignore it for now
            }
        }

        es.shutdown();

        return dumpResult;
    }

    @Override
    public DumpResult generateDump(String cause, Map<String, Object> context) throws AdminException {
        return generateDump(cause, context, (String[]) null);
    }

    @Override
    public DumpResult generateDump(String cause, Map<String, Object> context, String... processor) throws AdminException {
        CompoundDumpResult dumpResult = new CompoundDumpResult();
        for (ElasticServiceManager esm : elasticServiceManagers) {
            dumpResult.add(esm.generateDump(cause, context, processor));
        }
        for (GridServiceManager gsm : gridServiceManagers) {
            dumpResult.add(gsm.generateDump(cause, context, processor));
        }
        for (GridServiceContainer gsc : gridServiceContainers) {
            dumpResult.add(gsc.generateDump(cause, context, processor));
        }
        for (GridServiceAgent gsa : gridServiceAgents) {
            dumpResult.add(gsa.generateDump(cause, context, processor));
        }
        for (LookupService lus : lookupServices) {
            dumpResult.add(lus.generateDump(cause, context, processor));
        }
        return dumpResult;
    }

    private class ScheduledAgentProcessessMonitor implements Runnable {
        @Override
        public void run() {
            final Map<InternalGridServiceAgent,AgentProcessesDetails> newdetails = new HashMap<InternalGridServiceAgent,AgentProcessesDetails>();
            for (GridServiceAgent gridServiceAgent : gridServiceAgents) {
                GSA gsa = ((InternalGridServiceAgent) gridServiceAgent).getGSA();
                try {
                    newdetails.put((InternalGridServiceAgent) gridServiceAgent, gsa.getDetails());
                } catch (Exception e) {
                    // failed to get the info, do nothing
                }
            }
            
            DefaultAdmin.this.scheduleNonBlockingStateChange(new Runnable() {

                @Override
                public void run() {
                    updateState(newdetails);
                }
            });
        }
        
        private void updateState(Map<InternalGridServiceAgent, AgentProcessesDetails> newdetails) {
            for (Entry<InternalGridServiceAgent, AgentProcessesDetails> entry : newdetails.entrySet()) {
                entry.getKey().setProcessesDetails(entry.getValue());
            }
        }
    }

    private class ScheduledProcessingUnitMonitor implements Runnable {

        private static final int maxNumberOfEvents = 1000;
        private Long eventsCursor;
        private String eventsCursorEsmUid;

        @Override
        public void run() {
            final Map<String, Holder> holders = new HashMap<String, Holder>();
            for (GridServiceManager gsm : gridServiceManagers) {
                try {
                    PUsDetails pusDetails = ((InternalGridServiceManager) gsm).getGSM().getPUsDetails();
                    for (PUDetails detail : pusDetails.getDetails()) {
                        Holder holder = holders.get(detail.getName());
                        if (holder == null) {
                            holder = new Holder();
                            holder.name = detail.getName();
                            holders.put(holder.name, holder);
                        }
                        if (detail.isManaging()) {
                            holder.detail = detail;
                            holder.managingGSM = gsm;
                        } else {
                            holder.backupDetail = detail;
                            holder.backupGSMs.put(gsm.getUid(), gsm);
                        }
                    }
                } catch (Exception e) {
                    if (NetworkExceptionHelper.isConnectOrCloseException(e)) {
                        // GSM is down, continue
                        continue;
                    }
                    logger.warn("Failed to get GSM details", e);
                }
            }
            
            ElasticScaleStrategyEvents scaleStrategyEvents1 = null;
            InternalElasticServiceManager esm1 = null;
            try {
                
                if (elasticServiceManagers.getSize() > 0) {
                    esm1 = ((InternalElasticServiceManager)elasticServiceManagers.getManagers()[0]);
                    if (eventsCursorEsmUid == null || !esm1.getUid().equals(eventsCursorEsmUid)) {
                        eventsCursor = 0L;
                        eventsCursorEsmUid = esm1.getUid();
                    }
                    scaleStrategyEvents1 = esm1.getScaleStrategyEvents(eventsCursor, maxNumberOfEvents);
                    eventsCursor = scaleStrategyEvents1.getNextCursor();
                }
            } catch (AdminException e) {
                if (e.getCause() != null && NetworkExceptionHelper.isConnectOrCloseException(e.getCause())) {
                    // ESM is down, continue
                }
                logger.warn("Failed to get ESM details", e);
            }
            
            final ElasticScaleStrategyEvents scaleStrategyEvents = scaleStrategyEvents1;
            final InternalElasticServiceManager esm = esm1;
            
            DefaultAdmin.this.scheduleNonBlockingStateChange(new Runnable(){
                @Override
                public void run() {
                    updateState(holders, scaleStrategyEvents, esm);
                }});
        }

        private void updateState(Map<String, Holder> holders, ElasticScaleStrategyEvents scaleStrategyEvents, InternalElasticServiceManager esm) {

            //TODO: Move after pu added event below
            // make sure that admin API events and internal state is updated based on elastic PU scale strategy events
            if (scaleStrategyEvents !=null && esm != null) {
                for (ElasticProcessingUnitEvent event : scaleStrategyEvents.getEvents()) {
                    machines.processElasticScaleStrategyEvent(event);
                    gridServiceAgents.processElasticScaleStrategyEvent(event);
                    gridServiceContainers.processElasticScaleStrategyEvent(event);
                    esm.processElasticScaleStrategyEvent(event);
                }
            }
            
            // first go over all of them and remove the ones needed
            for (ProcessingUnit processingUnit : processingUnits) {
                if (!holders.containsKey(processingUnit.getName())) {
                    processingUnits.removeProcessingUnit(processingUnit.getName());
                    processApplicationsOnProcessingUnitRemoval(processingUnit);
                }
            }
            // now, go over and update what needed to be updated
            for (Holder holder : holders.values()) {
                PUDetails details = holder.detail;
                if (details == null) {
                    details = holder.backupDetail;
                }
                boolean newProcessingUnit = false;
                InternalProcessingUnit processingUnit = (InternalProcessingUnit) processingUnits.getProcessingUnit(holder.name);
                if (processingUnit == null) {
                    processingUnit = new DefaultProcessingUnit(DefaultAdmin.this, processingUnits, details);
                    newProcessingUnit = true;
                }
                // we always update the number of instances and backups since they might increate/decrease
                processingUnit.setNumberOfInstances(details.getNumberOfInstances());
                processingUnit.setNumberOfBackups(details.getNumberOfBackups());
                if (!newProcessingUnit) {
                    // handle managing GSM
                    if (holder.managingGSM == null) {
                        if (processingUnit.isManaged()) {
                            // event since we no longer have a managing GSM
                            processingUnit.addManagingGridServiceManager(null);
                        }
                    } else {
                        if (!processingUnit.isManaged() || !processingUnit.getManagingGridServiceManager().getUid().equals(holder.managingGSM.getUid())) {
                            // we changed managing GSM
                            processingUnit.addManagingGridServiceManager(holder.managingGSM);
                            // if it was in the backups, remove it from it
                            if (processingUnit.getBackupGridServiceManager(holder.managingGSM.getUid()) != null) {
                                processingUnit.removeBackupGridServiceManager(holder.managingGSM.getUid());
                            }
                        }
                    }
                    // handle backup GSM removal
                    for (GridServiceManager backupGSM : processingUnit.getBackupGridServiceManagers()) {
                        if (!holder.backupGSMs.containsKey(backupGSM.getUid())) {
                            processingUnit.removeBackupGridServiceManager(backupGSM.getUid());
                        }
                    }
                    // handle new backup GSMs
                    for (GridServiceManager backupGSM : holder.backupGSMs.values()) {
                        if (processingUnit.getBackupGridServiceManager(backupGSM.getUid()) == null) {
                            processingUnit.addBackupGridServiceManager(backupGSM);
                        }
                    }
                } else { // we have a new processing unit
                    if (holder.managingGSM != null) {
                        processingUnit.setManagingGridServiceManager(holder.managingGSM);
                        processingUnits.addProcessingUnit(processingUnit);
                        processingUnit.addManagingGridServiceManager(holder.managingGSM);
                        for (GridServiceManager backupGSM : holder.backupGSMs.values()) {
                            processingUnit.addBackupGridServiceManager(backupGSM);
                        }

                        processApplicationsOnProcessingUnitAddition(processingUnit);
                    }
                }
                processingUnit.setStatus( getPuStatusForUSM(processingUnit, details.getStatus()));
                processingUnit.processProvisionEvents(details.getProvisionLifeCycleEvents());
            }

            // Now, process any orphaned processing unit instances
            synchronized (DefaultAdmin.this) {
                for (ProcessingUnitInstance orphanedX : processingUnitInstances.getOrphaned()) {
                    InternalProcessingUnitInstance orphaned = (InternalProcessingUnitInstance) orphanedX;

                    InternalProcessingUnit processingUnit = (InternalProcessingUnit) processingUnits.getProcessingUnit(orphaned.getName());
                    InternalGridServiceContainer gridServiceContainer = (InternalGridServiceContainer) gridServiceContainers.getContainerByUID(orphaned.getGridServiceContainerServiceID().toString());

                    if (processingUnit != null && gridServiceContainer != null) {
                        processProcessingUnitInstanceAddition(processingUnit, orphaned);
                    }
                }

                flushEvents();
            }
        }

        
        /*
         * Extract USM service state if processing unit is a USM. Returns SCHEDULED if underlying USM process is not in a running state.
         * Otherwise, returns the original status as returned by the GSM.
         */
        private int getPuStatusForUSM(InternalProcessingUnit processingUnit, int status) {

            final String CloudifyConstants_USM_MONITORS_SERVICE_ID = "USM"; //CloudifyConstants.USM_MONITORS_SERVICE_ID
            final String CloudifyConstants_USM_MONITORS_STATE_ID = "USM_State"; //CloudifyConstants.USM_MONITORS_STATE_ID
            final Integer USMState_RUNNING = 2; //USMState.RUNNING ordinal
            
            //All USM services have a USM State defined by the USMState enum: (com.gigaspaces.cloudify.dsl.internal.CloudifyConstants.USMState)
            //show "SCHEDULED" state if USM is not in a running state (USMState.RUNNING)
            if (OperationalString.INTACT == status && ProcessingUnitType.UNIVERSAL.equals(processingUnit.getType())) {
                for (ProcessingUnitInstance instance : processingUnit.getProcessingUnitInstances()) {
                    Map<String, ServiceMonitors> monitors = instance.getStatistics().getMonitors();
                    ServiceMonitors serviceMonitors = monitors.get(CloudifyConstants_USM_MONITORS_SERVICE_ID); 
                    if (serviceMonitors != null) {
                        Integer state = (Integer)serviceMonitors.getMonitors().get(CloudifyConstants_USM_MONITORS_STATE_ID); 
                        if (!USMState_RUNNING.equals(state)) {  
                            status = OperationalString.SCHEDULED;
                        }
                    }

                }
            }
            return status;
        }

        private class Holder {

            String name;

            PUDetails detail;

            PUDetails backupDetail;

            GridServiceManager managingGSM;

            Map<String, GridServiceManager> backupGSMs = new HashMap<String, GridServiceManager>();
        }
    }

    private static class LoggerRunnable implements Runnable {
        private final Runnable runnable;

        private LoggerRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } catch (Exception e) {
                logger.warn("Failed to execute: " + runnable, e);
            } catch (Error e) {
                logger.error("Failed to execute: " + runnable, e);
                throw e;
            }
        }
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelayNonBlockingStateChange(final Runnable command, long initialDelay,
            long delay, TimeUnit unit) {
        return this.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                scheduleNonBlockingStateChange(command);
            }}, 
            
            initialDelay, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleOneTimeWithDelayNonBlockingStateChange(
            final Runnable command, 
            long delay, TimeUnit unit) {
        return this.getScheduler().schedule(new Runnable() {

            @Override
            public void run() {
                scheduleNonBlockingStateChange(command);
            }}, 
            
            delay, unit);
    }
}

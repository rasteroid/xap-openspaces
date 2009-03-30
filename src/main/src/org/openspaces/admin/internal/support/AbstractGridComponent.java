package org.openspaces.admin.internal.support;

import org.openspaces.admin.Admin;
import org.openspaces.admin.zone.Zone;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.os.OperatingSystem;
import org.openspaces.admin.transport.Transport;
import org.openspaces.admin.vm.VirtualMachine;

import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author kimchy
 */
public abstract class AbstractGridComponent implements InternalGridComponent {

    protected final InternalAdmin admin;

    private volatile Machine machine;

    private volatile Transport transport;

    private volatile OperatingSystem operatingSystem;

    private volatile VirtualMachine virtualMachine;

    private final Map<String, Zone> zones = new ConcurrentHashMap<String, Zone>();

    protected AbstractGridComponent(InternalAdmin admin) {
        this.admin = admin;
    }

    public Admin getAdmin() {
        return this.admin;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public Machine getMachine() {
        return this.machine;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public Transport getTransport() {
        return this.transport;
    }

    public void setOperatingSystem(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public OperatingSystem getOperatingSystem() {
        return this.operatingSystem;
    }

    public void setVirtualMachine(VirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;
    }

    public VirtualMachine getVirtualMachine() {
        return this.virtualMachine;
    }

    public Map<String, Zone> getZones() {
        return Collections.unmodifiableMap(zones);
    }

    public void addZone(Zone zone) {
        zones.put(zone.getName(), zone);
    }
}

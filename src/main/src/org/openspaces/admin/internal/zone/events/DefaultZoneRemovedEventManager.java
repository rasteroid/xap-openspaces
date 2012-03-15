/*******************************************************************************
 * 
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 ******************************************************************************/
package org.openspaces.admin.internal.zone.events;

import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.internal.support.GroovyHelper;
import org.openspaces.admin.internal.zone.InternalZones;
import org.openspaces.admin.zone.Zone;
import org.openspaces.admin.zone.events.ZoneRemovedEventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author kimchy
 */
public class DefaultZoneRemovedEventManager implements InternalZoneRemovedEventManager {

    private final InternalZones zones;

    private final InternalAdmin admin;

    private final List<ZoneRemovedEventListener> zoneRemovedEventListeners = new CopyOnWriteArrayList<ZoneRemovedEventListener>();

    public DefaultZoneRemovedEventManager(InternalZones zones) {
        this.zones = zones;
        this.admin = (InternalAdmin) zones.getAdmin();
    }

    public void zoneRemoved(final Zone zone) {
        for (final ZoneRemovedEventListener listener : zoneRemovedEventListeners) {
            admin.pushEvent(listener, new Runnable() {
                public void run() {
                    listener.zoneRemoved(zone);
                }
            });
        }
    }

    public void add(ZoneRemovedEventListener eventListener) {
        zoneRemovedEventListeners.add(eventListener);
    }

    public void remove(ZoneRemovedEventListener eventListener) {
        zoneRemovedEventListeners.remove(eventListener);
    }

    public void plus(Object eventListener) {
        if (GroovyHelper.isClosure(eventListener)) {
            add(new ClosureZoneRemovedEventListener(eventListener));
        } else {
            add((ZoneRemovedEventListener) eventListener);
        }
    }

    public void leftShift(Object eventListener) {
        plus(eventListener);
    }

    public void minus(Object eventListener) {
        if (GroovyHelper.isClosure(eventListener)) {
            remove(new ClosureZoneRemovedEventListener(eventListener));
        } else {
            remove((ZoneRemovedEventListener) eventListener);
        }
    }

    public void rightShift(Object eventListener) {
        minus(eventListener);
    }
}

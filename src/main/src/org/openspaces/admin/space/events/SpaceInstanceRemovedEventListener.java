package org.openspaces.admin.space.events;

import org.openspaces.admin.space.SpaceInstance;

/**
 * @author kimchy
 */
public interface SpaceInstanceRemovedEventListener {

    void spaceInstanceRemoved(SpaceInstance spaceInstance);
}
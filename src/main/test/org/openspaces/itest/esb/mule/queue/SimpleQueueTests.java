package org.openspaces.itest.esb.mule.queue;

import org.mule.umo.UMOMessage;
import org.openspaces.itest.esb.mule.AbstractMuleTests;

/**
 * @author kimchy
 */
public class SimpleQueueTests extends AbstractMuleTests {

    protected String[] getConfigLocations() {
        return new String[]{"/org/openspaces/itest/esb/mule/queue/simple.xml"};
    }

    public void testSimpleQueueHandling() throws Exception {
        muleClient.dispatch("os-queue://test1", "testme", null);

        UMOMessage message = muleClient.request("os-queue://test3", 5000);
        assertEquals("testmeAppender1Appender2", message.getPayload());
    }
}

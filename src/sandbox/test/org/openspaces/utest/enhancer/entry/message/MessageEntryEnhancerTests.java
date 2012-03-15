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
package org.openspaces.utest.enhancer.entry.message;

import junit.framework.TestCase;
import org.openspaces.enhancer.support.ExternalizableHelper;

import java.io.Externalizable;

/**
 * @author kimchy
 */
public class MessageEntryEnhancerTests extends TestCase {

    public void testSimpleMessage() throws Exception {
        Message origMessage = new Message();
        origMessage.setValue(1);
        origMessage.setContent(new byte[]{(byte) 1, (byte) 2});

        Message newMessage = new Message();
        ExternalizableHelper.externalize((Externalizable) origMessage, (Externalizable) newMessage);

        assertEquals(1, origMessage.getValue());
        assertEquals(2, newMessage.getContent().length);
        assertEquals(1, newMessage.getContent()[0]);
        assertEquals(2, newMessage.getContent()[1]);
    }

    public void testZeroSimpleMessage() throws Exception {
        Message origMessage = new Message();
        origMessage.setValue(1);
        origMessage.setContent(new byte[0]);

        Message newMessage = new Message();
        ExternalizableHelper.externalize((Externalizable) origMessage, (Externalizable) newMessage);

        assertEquals(1, origMessage.getValue());
        assertEquals(0, newMessage.getContent().length);
    }
}

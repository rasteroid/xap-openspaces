/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openspaces.itest.esb.mule.queue;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

/**
 * A simple message object that is written to the space. Note, this
 * message uses GigaSpaces support for POJO entries. With GigaSpaces
 * support for POJO entries there is no need even to mark the class
 * using annotations or xml though further customization is allowed
 * when using it.
 *
 * @author yitzhaki
 */
public class EchoeComponentAppender implements Callable {

    public Object onCall(UMOEventContext eventContext) throws Exception {
        return eventContext.getMessage().getPayload() + eventContext.getComponent().getName();
    }
}
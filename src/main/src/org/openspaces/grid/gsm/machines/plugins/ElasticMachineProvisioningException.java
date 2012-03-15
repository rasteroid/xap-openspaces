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
package org.openspaces.grid.gsm.machines.plugins;


/**
 * An abstraction for any runtime exception that could be raised by an elastic machine provisioning implementation.
 * @author itaif
 *
 */
public class ElasticMachineProvisioningException extends Exception {

    private static final long serialVersionUID = 1L;

    public ElasticMachineProvisioningException(String message) {
        super(message);
    }

    public ElasticMachineProvisioningException(String message, Throwable cause) {
        super(message,cause);
    }

}

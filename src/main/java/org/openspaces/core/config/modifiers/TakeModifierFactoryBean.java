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
package org.openspaces.core.config.modifiers;

import org.springframework.core.Constants;

import com.gigaspaces.client.TakeModifiers;

/**
 * A {@link TakeModifiers} factory bean.
 * @author Dan Kilman
 * @since 9.5
 */
public class TakeModifierFactoryBean extends AbstractSpaceProxyOperationModifierFactoryBean<TakeModifiers> {

    private static final Constants constants = new Constants(TakeModifiers.class);
    
    public TakeModifierFactoryBean() {
        super(TakeModifiers.class);
    }

    @Override
    protected Constants getConstants() {
        return constants;
    }
    
}

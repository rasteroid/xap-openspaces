/*
* Copyright 2006-2007 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.openspaces.esb.servicemix.pu;

import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Extends <@link> ClassPathXmlApplicationContext</link> in order to inject beanPostProcessor.
 *
 * @author yitzhaki
 */
public class ServiceMixApplicationContext extends ClassPathXmlApplicationContext {

    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

    public ServiceMixApplicationContext(String s) throws BeansException {
        super(s);
    }

    /**
     * Adds Spring bean post processor. Note, this method should be called before the
     * {@link #refresh()} is called on this application context for the bean post processor to take
     * affect.
     *
     * @param beanPostProcessor The bean post processor to add
     */
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.add(beanPostProcessor);
    }

    /**
     * Creates a new bean factory by delegating to the super bean factory creation and then adding
     * all the registered {@link BeanPostProcessor}s.
     */
    protected DefaultListableBeanFactory createBeanFactory() {
        DefaultListableBeanFactory beanFactory = super.createBeanFactory();
        if (beanPostProcessors != null) {
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                beanFactory.addBeanPostProcessor(beanPostProcessor);
            }
        }
        return beanFactory;
    }

}

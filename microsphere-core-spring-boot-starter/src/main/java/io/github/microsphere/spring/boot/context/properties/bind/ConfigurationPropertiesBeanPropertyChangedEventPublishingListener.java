/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.microsphere.spring.boot.context.properties.bind;

import io.github.microsphere.spring.context.event.BeanPropertyChangedEvent;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static io.github.microsphere.spring.boot.context.properties.bind.util.BindUtils.isConfigurationPropertiesBean;
import static io.github.microsphere.spring.boot.context.properties.source.util.ConfigurationPropertyUtils.getPrefix;
import static io.github.microsphere.spring.boot.context.properties.util.ConfigurationPropertiesUtils.CONFIGURATION_PROPERTIES_CLASS;
import static io.github.microsphere.spring.boot.context.properties.util.ConfigurationPropertiesUtils.findConfigurationProperties;

/**
 * A {@link BindListener} implementation of {@link ConfigurationProperties @ConfigurationProperties} Bean to publish
 * the {@link BeanPropertyChangedEvent}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ConfigurationProperties
 * @see BeanPropertyChangedEvent
 * @since 1.0.0
 */
public class ConfigurationPropertiesBeanPropertyChangedEventPublishingListener implements BindListener, BeanFactoryPostProcessor, ApplicationContextAware, SmartInitializingSingleton {

    private static final Class<ConfigurableApplicationContext> CONFIGURABLE_APPLICATION_CONTEXT_CLASS = ConfigurableApplicationContext.class;

    private Map<String, ConfigurationPropertiesBeanContext> beanContexts;

    private ConfigurableApplicationContext context;

    private boolean bound = false;

    @Override
    public <T> void onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
        if (isBound()) {

        } else {
            initConfigurationPropertiesBeanContext(name, target, context);
        }
    }

    @Override
    public void onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        if (isBound()) {
            updateConfigurationPropertiesBeanContext(name, target, context, result);
        } else {
            initConfigurationPropertiesBeanContext(name, target, context);
        }

    }

    private void initConfigurationPropertiesBeanContext(ConfigurationPropertyName name, Bindable<?> target, BindContext context) {
        if (isConfigurationPropertiesBean(context)) {
            ConfigurationPropertiesBeanContext configurationPropertiesBeanContext = getConfigurationPropertiesBeanContext(name, target, context);
            Supplier<?> value = target.getValue();
            Object bean = value.get();
            if (bean != null) {
                configurationPropertiesBeanContext.initialize(bean, this.context);
            }
        }
    }

    private ConfigurationPropertiesBeanContext getConfigurationPropertiesBeanContext(ConfigurationPropertyName name, Bindable<?> target, BindContext context) {
        String prefix = getPrefix(name, context);
        return beanContexts.computeIfAbsent(prefix, p -> {
            Class<?> beanClass = target.getType().getRawClass();
            ConfigurationProperties annotation = findConfigurationProperties(target);
            return new ConfigurationPropertiesBeanContext(beanClass, annotation, p);
        });
    }

    private void updateConfigurationPropertiesBeanContext(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        ConfigurationPropertiesBeanContext configurationPropertiesBeanContext = getConfigurationPropertiesBeanContext(name, target, context);
        if (isConfigurationPropertiesBean(context)) {
        } else {
            configurationPropertiesBeanContext.setProperty(name, result);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        initConfigurationPropertiesBeanContexts(beanFactory);
    }

    private void initConfigurationPropertiesBeanContexts(ConfigurableListableBeanFactory beanFactory) {
        String[] beanNames = beanFactory.getBeanNamesForAnnotation(CONFIGURATION_PROPERTIES_CLASS);
        int beanCount = beanNames.length;
        this.beanContexts = new HashMap<>(beanCount);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        Class<ConfigurableApplicationContext> expectedType = CONFIGURABLE_APPLICATION_CONTEXT_CLASS;
        Assert.isInstanceOf(expectedType, context, "The 'context' argument is not an instance of " + expectedType.getName());
        this.context = expectedType.cast(context);
    }

    @Override
    public void afterSingletonsInstantiated() {
        bound = true;
    }

    public boolean isBound() {
        return bound;
    }
}

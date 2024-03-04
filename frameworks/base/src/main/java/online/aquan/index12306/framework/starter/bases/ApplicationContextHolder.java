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

package online.aquan.index12306.framework.starter.bases;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Application context holder.
 */
public class ApplicationContextHolder implements ApplicationContextAware {

  
    private static ApplicationContext CONTEXT;

    /*
    ApplicationContextAware 是 Spring 框架中的一个接口，用于获取 Spring 应用上下文（ApplicationContext）。
    当一个类实现了 ApplicationContextAware 接口，并且注册为 Spring Bean，Spring 在实例化这个 Bean 时会调用 setApplicationContext 方法，
    并将应用上下文作为参数传入，从而允许这个 Bean 访问应用上下文中的各种组件
    */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.CONTEXT = applicationContext;
    }

    /**
     * Get ioc container bean by type.
     */
    public static <T> T getBean(Class<T> clazz) {
        return CONTEXT.getBean(clazz);
    }

    /**
     * Get ioc container bean by name.
     */
    public static Object getBean(String name) {
        return CONTEXT.getBean(name);
    }

    /**
     * Get ioc container bean by name and type.
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return CONTEXT.getBean(name, clazz);
    }

    /**
     * Get a set of ioc container beans by type.
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return CONTEXT.getBeansOfType(clazz);
    }

    /**
     * Find whether the bean has annotations.
     */
    public static <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
        return CONTEXT.findAnnotationOnBean(beanName, annotationType);
    }

    /**
     * Get application context.
     */
    public static ApplicationContext getInstance() {
        return CONTEXT;
    }
}

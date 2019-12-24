/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactpro.sf;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CollisionsUtilityFunctions {

    private final static String BASE_CLASSPATH = "com.exactpro.sf.";
    private final static String UTILITY_METHOD = BASE_CLASSPATH + "scriptrunner.utilitymanager.UtilityMethod";

    private final SetMultimap<String, Class<?>> methodNameToClasses;
    private final Class<Annotation> methodClass;

    public CollisionsUtilityFunctions(ClassLoader classLoader) throws ClassNotFoundException {
        methodNameToClasses = HashMultimap.create();
        methodClass = (Class<Annotation>) classLoader.loadClass(UTILITY_METHOD);
    }

    public Multimap<String, Class<?>> getCollision() {
        SetMultimap<String, Class<?>> collisions = HashMultimap.create();
        for (Map.Entry<String, Collection<Class<?>>> methodClasses: methodNameToClasses.asMap().entrySet()) {
            if(methodClasses.getValue().size()>1){
                collisions.putAll(methodClasses.getKey(),methodClasses.getValue());
            }
        }
        return collisions;
    }


    public void put(Class<?> utilityClass) {
        for (Method utilityMethod : utilityClass.getMethods()) {
            if (utilityMethod.isAnnotationPresent(methodClass)) {
                methodNameToClasses.put(utilityMethod.getName(),utilityClass);
            }
        }
    }

    public void putAll(List<Class<?>> utilityClasses){
        for (Class<?> utilityClass : utilityClasses) {
            put(utilityClass);
        }
    }

}



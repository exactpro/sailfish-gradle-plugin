/******************************************************************************
 * Copyright 2009-2016 Exactpro (Exactpro Systems Limited)
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
package com.exactprosystems.testtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import com.google.common.io.Files;

public class CompatibilityChecker extends DefaultTask{
    
    private final static String BASE_CLASSPATH = "com.exactprosystems.testtools.";
    private final static String PLUGIN_LOADER = BASE_CLASSPATH + "center.impl.PluginLoader";
    private final static String I_SERVICE = BASE_CLASSPATH + "services.IService";
    private final static String I_UTILITY_CALLER = BASE_CLASSPATH + "scriptrunner.utilitymanager.IUtilityCaller";
    private final static String I_ACTION_CALLER = BASE_CLASSPATH + "scriptrunner.actionmanager.IActionCaller";
    private final static String PLUGIN_VERSION = BASE_CLASSPATH + "center.impl.PluginVersion";
    private final static String ACTIONS = BASE_CLASSPATH + "aml.legacy.Actions";
    private final static String ACTION_DEFINITION = BASE_CLASSPATH + "aml.legacy.ActionDefinition";
    private final static String CLASS_NAME = BASE_CLASSPATH + "aml.legacy.ClassName";
    private final static String DICTIONARIES = BASE_CLASSPATH + "aml.Dictionaries";
    private final static String DICTIONARY = BASE_CLASSPATH + "aml.Dictionary";
    private final static String SERVICE_DEFINITION = BASE_CLASSPATH + "configuration.services.ServiceDefinition";
    private final static String SERVICES = BASE_CLASSPATH + "configuration.services.Services";
    
    private final static String ACTIONS_XML_FILE_NAME = "ACTIONS_XML_FILE_NAME";
    private final static String DICTIONARIES_XML_FILE_NAME = "DICTIONARIES_XML_FILE_NAME";
    private final static String SERVICES_XML_FILE_NAME = "SERVICES_XML_FILE_NAME";
    private final static String CORE_VERSION_PROPERTY = "CORE_VERSION_PROPERTY";
    private final static String CORE_VERSION = "CORE_VERSION";
    
    private ClassLoader classLoader;
    @InputDirectory
    private File cfgDir;
    @InputFile
    private File versionFile;
    
    @TaskAction
    public void checkCompatibility() throws Exception {
        
        Project p = getProject();        
        List<URL> urls = new ArrayList<>();
        for (File f:p.getConfigurations().getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME).getFiles()) {
            urls.add(f.toURI().toURL());
        }
        classLoader = URLClassLoader.newInstance(urls.toArray(new URL[0]), this.getClass().getClassLoader());
        
        boolean success = true; 
        if (getCfgDir().exists()) {
            File cfgFile = new File(getCfgDir(), loadStaticStringField(PLUGIN_LOADER, ACTIONS_XML_FILE_NAME));
            if(cfgFile.exists()) {
                success &= checkActions(classLoader, cfgFile);
            }

            cfgFile = new File(getCfgDir(), loadStaticStringField(PLUGIN_LOADER, DICTIONARIES_XML_FILE_NAME));
            if(cfgFile.exists()) {
                success &= checkDictionaries(classLoader, cfgFile);
            }
            
            cfgFile = new File(getCfgDir(), loadStaticStringField(PLUGIN_LOADER, SERVICES_XML_FILE_NAME));
            if(cfgFile.exists()) {
                success &= checkServices(classLoader, cfgFile);
            }
        }

        if(success) {
            Files.append(String.format("%s: %s", loadStaticStringField(PLUGIN_VERSION, CORE_VERSION_PROPERTY), loadStaticStringField(PLUGIN_LOADER, CORE_VERSION)), getVersionFile(), Charset.defaultCharset());
        } else {
            throw new RuntimeException("Some classes of this plugin are incompatible with core");
        }
    }

    // checks action classes and utility classes attached to them
    private static boolean checkActions(ClassLoader classLoader, File actionsFile) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        String fileName = actionsFile.getAbsolutePath();
        System.out.println("Reading actions file: " + fileName);

        try {
            Object root = unmarshal(actionsFile, classLoader.loadClass(ACTIONS)).getValue();
            Set<String> utilityClasses = new HashSet<String>();
            boolean valid = true;

            for(Object action : (Iterable<?>)root.getClass().getMethod("getAction").invoke(root)) {
                Object classNameInstance = action.getClass().getMethod("getActionClassName").invoke(action);
                String className = (String) classNameInstance.getClass().getMethod("getName").invoke(classNameInstance);
                System.out.println("Checking action class: " + className);

                try {
                    Class<?> actionClass = classLoader.loadClass(className);
                    actionClass.asSubclass(classLoader.loadClass(I_ACTION_CALLER)).newInstance();
                } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
                    System.out.println("Failed to load action class '" + className + "': " + e);
                    valid = false;
                }

                for(Object utility : (Iterable<?>)action.getClass().getMethod("getUtilityClassName").invoke(action)) {
                    utilityClasses.add((String) utility.getClass().getMethod("getName").invoke(utility));
                }
            }

            return valid && checkUtilites(classLoader, utilityClasses);
        } catch(JAXBException | FileNotFoundException e) {
            System.out.println("Failed to read actions file '" + fileName + "': " + e.getMessage());
            return false;
        }
    }

    // checks utility classes attached to dictionaries
    private static boolean checkDictionaries(ClassLoader classLoader, File dictionariesFile) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        String fileName = dictionariesFile.getAbsolutePath();
        System.out.println("Reading dictionaries file: " + fileName);

        try {
            Object root = unmarshal(dictionariesFile, classLoader.loadClass(DICTIONARIES)).getValue();
            Set<String> utilityClasses = new HashSet<String>();

            for(Object dictionary : (Iterable<?>)root.getClass().getMethod("getDictionary").invoke(root)) {
                for(Object className : (Iterable<?>)dictionary.getClass().getMethod("getUtilityClassName").invoke(dictionary)) {
                    utilityClasses.add((String) className);
                }
            }

            return checkUtilites(classLoader, utilityClasses);
        } catch(JAXBException | FileNotFoundException e) {
            System.out.println("Failed to read dictionaries file '" + fileName + "': " + e.getMessage());
            return false;
        }
    }

    private static boolean checkServices(ClassLoader classLoader, File servicesFile) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        String fileName = servicesFile.getAbsolutePath();
        System.out.println("Reading services file: " + fileName);

        try {
            Object root = unmarshal(servicesFile, classLoader.loadClass(SERVICES)).getValue();
            boolean valid = true;

            for(Object service : (Iterable<?>)root.getClass().getMethod("getService").invoke(root)) {
                String className = (String) service.getClass().getMethod("getClassName").invoke(service);
                System.out.println("Checking service class: " + className);

                try {
                    Class<?> serviceClass = classLoader.loadClass(className);
                    serviceClass.asSubclass(classLoader.loadClass(I_SERVICE)).newInstance();
                } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
                    System.out.println("Failed to load service class '" + className + "': " + e.getMessage());
                    valid = false;
                }
            }

            return valid;
        } catch(JAXBException | FileNotFoundException e) {
            System.out.println("Failed to read services file '" + fileName + "': " + e.getMessage());
            return false;
        }
    }

    private static boolean checkUtilites(ClassLoader classLoader, Set<String> utilityClasses) {
        boolean valid = true;

        for(String className : utilityClasses) {
            System.out.println("Checking utility class: " + className);

            try {
                Class<?> utilityClass = classLoader.loadClass(className);
                utilityClass.asSubclass(classLoader.loadClass(I_UTILITY_CALLER)).newInstance();
            } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
                System.out.println("Failed to load utility class '" + className + "': " + e.getMessage());
                valid = false;
            }
        }

        return valid;
    }

    private static <T> JAXBElement<T> unmarshal(File file, Class<T> targetClass) throws FileNotFoundException, JAXBException {
        InputStream fileStream = new FileInputStream(file);
        Unmarshaller unmarshaller = JAXBContext.newInstance(targetClass).createUnmarshaller();

        return unmarshaller.unmarshal(new StreamSource(fileStream), targetClass);
    }
    
    private String loadStaticStringField(String className, String fieldName) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Class<?> clazz = classLoader.loadClass(className);
        Field f = clazz.getDeclaredField(fieldName);
        if (Modifier.isStatic(f.getModifiers())) {
            f.setAccessible(true);
            return (String) f.get(null);
        } else {
            throw new RuntimeException("Accesing to non static field");
        }
    }
    
    @InputDirectory
    public File getCfgDir() {
        return cfgDir;
    }
    @InputDirectory
    public void setCfgDir(File cfgDir) {
        this.cfgDir = cfgDir;
    }
    @InputFile
    public File getVersionFile() {
        return versionFile;
    }
    @InputFile
    public void setVersionFile(File versionFile) {
        this.versionFile = versionFile;
    }
}

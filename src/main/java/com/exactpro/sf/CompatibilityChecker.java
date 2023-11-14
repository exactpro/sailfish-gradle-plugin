/******************************************************************************
 * Copyright 2009-2023 Exactpro (Exactpro Systems Limited)
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import com.google.common.collect.Multimap;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

import com.google.common.io.Files;

public class CompatibilityChecker extends DefaultTask{
    
    private final static String BASE_CLASSPATH = "com.exactpro.sf.";
    private final static String PLUGIN_LOADER = BASE_CLASSPATH + "center.impl.PluginLoader";
    private final static String I_SERVICE = BASE_CLASSPATH + "services.IService";
    private final static String I_UTILITY_CALLER = BASE_CLASSPATH + "scriptrunner.utilitymanager.IUtilityCaller";
    private final static String I_ACTION_CALLER = BASE_CLASSPATH + "scriptrunner.actionmanager.IActionCaller";
    private final static String VERSION_CLASS = BASE_CLASSPATH + "center.IVersion";
    private final static String CORE_VERSION_CLASS = BASE_CLASSPATH + "center.impl.CoreVersion";
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
    private final static String LIBS_SUBDIRECTORY = "libs";

    private ClassLoader classLoader;
    private File versionFile;
    private List<File> cfgDirs;
    
    private boolean silent;
    
    private int minCoreRevision = 0;

    @TaskAction
    public void checkCompatibility() throws Exception {
        Project p = getProject();
        List<URL> urls = new ArrayList<>();
        Configuration configuration = p.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
        for (File f: configuration.getFiles()) {
            urls.add(f.toURI().toURL());
        }
        for (File f: p.fileTree(p.getBuildDir().toPath().resolve(LIBS_SUBDIRECTORY))) {
            urls.add(f.toURI().toURL());
        }
        for (PublishArtifact f: configuration.getArtifacts()) {
            urls.add(f.getFile().toURI().toURL());
        }
        classLoader = URLClassLoader.newInstance(urls.toArray(new URL[0]), this.getClass().getClassLoader());

        boolean success = true; 
        for (File cfgDir : cfgDirs) {
            System.out.println("Checking configDir:" + cfgDir);
            if (cfgDir.exists()) {
                File cfgFile = new File(cfgDir, loadStaticStringField(PLUGIN_LOADER, ACTIONS_XML_FILE_NAME));
                if (cfgFile.exists()) {
                    success &= checkActions(classLoader, cfgFile);
                }

                cfgFile = new File(cfgDir, loadStaticStringField(PLUGIN_LOADER, DICTIONARIES_XML_FILE_NAME));
                if (cfgFile.exists()) {
                    success &= checkDictionaries(classLoader, cfgFile);
                }

                cfgFile = new File(cfgDir, loadStaticStringField(PLUGIN_LOADER, SERVICES_XML_FILE_NAME));
                if (cfgFile.exists()) {
                    success &= checkServices(classLoader, cfgFile);
                }
            }
        }
        
        Files.append(String.format("%s: %s.%s.%s", loadStaticStringField(VERSION_CLASS, CORE_VERSION_PROPERTY),
                loadStringGetter(CORE_VERSION_CLASS, "getMajor"), loadStringGetter(CORE_VERSION_CLASS, "getMinor"),
                this.minCoreRevision), getVersionFile(), Charset.defaultCharset());
        if(!success && !silent) {
            throw new RuntimeException("Some classes of this plugin are incompatible with core");
        }
    }

    // checks action classes and utility classes attached to them
    private boolean checkActions(ClassLoader classLoader, File actionsFile) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        String fileName = actionsFile.getAbsolutePath();
        System.out.println("Reading actions file: " + fileName);

        try {
            Object root = unmarshal(actionsFile, classLoader.loadClass(ACTIONS)).getValue();

            boolean valid = true;

            Set<Class<?>> listUtilityClass = new HashSet<>();

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

                CollisionsUtilityFunctions collisionsUtilityFunctions = new CollisionsUtilityFunctions(classLoader);

                for(Object utility : (Iterable<?>)action.getClass().getMethod("getUtilityClassName").invoke(action)) {
                    String utilityClassName = (String) utility.getClass().getMethod("getName").invoke(utility);

                    Class<?> utilityClass = classLoader.loadClass(utilityClassName);
                    listUtilityClass.add(utilityClass);
                    collisionsUtilityFunctions.put(utilityClass);
                }

                printCollisions(className, collisionsUtilityFunctions, "action ", fileName);

            }

            return valid && checkUtilites(classLoader, listUtilityClass);
        } catch(JAXBException | FileNotFoundException e) {
            System.out.println("Failed to read actions file '" + fileName + "': " + e.getMessage());
            return false;
        }
    }

    private void printCollisions(String className, CollisionsUtilityFunctions collisionsUtilityFunctions, String type, String fileName) throws ClassNotFoundException {
        Multimap<String, Class<?>> collisions = collisionsUtilityFunctions.getCollision();
        if (!collisions.isEmpty()) {
            System.out.println("WARNING Collisions in " + type + className + " in file " + fileName);
            for (Map.Entry<String, Collection<Class<?>>> collision : collisions.asMap().entrySet()) {
                System.out.println("    Method \"" + collision.getKey() + "\" have collision in classes:");
                for (Class<?> collisionClassName : collision.getValue()) {
                    System.out.println("        - " + collisionClassName.getName());
                }
            }
        }
    }

    // checks utility classes attached to dictionaries
    private boolean checkDictionaries(ClassLoader classLoader, File dictionariesFile) throws ClassNotFoundException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        String fileName = dictionariesFile.getAbsolutePath();
        System.out.println("Reading dictionaries file: " + fileName);
        try {
            Object root = unmarshal(dictionariesFile, classLoader.loadClass(DICTIONARIES)).getValue();
            Set<Class<?>> listUtilityClass = new HashSet<>();

            boolean dictionariesOk = true;
            for (Object dictionary : (Iterable<?>) root.getClass().getMethod("getDictionary").invoke(root)) {
                String resourcePath = (String) dictionary.getClass().getMethod("getResource").invoke(dictionary);

                boolean resourseFound = false;
                for (File cfgDir : cfgDirs) {
                    File resource = new File(cfgDir.getPath() + File.separator + "dictionaries" + File.separator + resourcePath);
                    if (resource.exists()) {
                        resourseFound = true;
                        break;
                    }
                }
                if (!resourseFound) {
                    System.err.println(resourcePath + "doesn't exist in the plugin");
                    dictionariesOk = false;
                }

                CollisionsUtilityFunctions collisionsUtilityFunctions = new CollisionsUtilityFunctions(classLoader);

                for (Object className : (Iterable<?>) dictionary.getClass().getMethod("getUtilityClassName").invoke(dictionary)) {

                    Class<?> utilityClass = classLoader.loadClass((String) className);
                    listUtilityClass.add(utilityClass);
                    collisionsUtilityFunctions.put(utilityClass);
                }

                printCollisions(resourcePath, collisionsUtilityFunctions, "dictionary ", fileName);

            }

            return checkUtilites(classLoader, listUtilityClass) & dictionariesOk;
        } catch (JAXBException | IOException e) {
            System.out.println("Failed to read dictionaries file '" + fileName + "': " + e.getMessage());
            return false;
        }

    }

    private boolean checkServices(ClassLoader classLoader, File servicesFile) throws ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
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

    private boolean checkUtilites(ClassLoader classLoader, Set<Class<?>> utilityClasses) {
        boolean valid = true;

        for(Class<?> utilityClass : utilityClasses) {
            System.out.println("Checking utility class: " + utilityClass.getName());

            try {
                utilityClass.asSubclass(classLoader.loadClass(I_UTILITY_CALLER)).newInstance();
            } catch(ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
                System.out.println("Failed to load utility class '" + utilityClass.getName() + "': " + e.getMessage());
                valid = false;
            }
        }

        return valid;
    }

    private <T> JAXBElement<T> unmarshal(File file, Class<T> targetClass) throws FileNotFoundException, JAXBException {
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

    private String loadStringGetter(String className, String getterName) throws ReflectiveOperationException {
        Class<?> clazz = classLoader.loadClass(className);
        Constructor<?> constructor = clazz.getConstructor();
        Object instance =  constructor.newInstance();
        Method method = clazz.getDeclaredMethod(getterName);
        return String.valueOf(Objects.requireNonNull(method.invoke(instance), "Empty result for " + getterName));
    }
    
    @InputFile
    public File getVersionFile() {
        return versionFile;
    }

    public void setVersionFile(File versionFile) {
        this.versionFile = versionFile;
    }
    
    @InputFiles
    public List<File> getCfgDirs() {
        return cfgDirs;
    }

    public void setCfgDirs(List<File> cfgDirs) {
        this.cfgDirs = cfgDirs;
    }

    @Input
    public boolean getSilent() {
        return silent;
    }
    
    public void setSilent(boolean value) {
        silent = value;
    }

    @Input
    public int getMinCoreRevision() {
        return minCoreRevision;
    }

    public void setMinCoreRevision(int minCoreRevision) {
        this.minCoreRevision = minCoreRevision;
    }
}

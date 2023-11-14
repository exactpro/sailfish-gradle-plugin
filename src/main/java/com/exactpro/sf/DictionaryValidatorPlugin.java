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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaExecSpec;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import groovy.lang.Closure;

public class DictionaryValidatorPlugin extends DefaultTask {

    protected static final String XML_EXTENSION = "xml";
    private final List<File> sourceDirs = new ArrayList<>();
    private final SetMultimap<String, String> pendingRequests = HashMultimap.create();
    private final SetMultimap<String, String> excluded = HashMultimap.create();
    private String defaultValidator = "";
    private FileCollection classpath;
    private File outputDir = new File(getProject().getBuildDir(), "validated");

    @TaskAction
    public void validateDictionary(IncrementalTaskInputs inputs) throws Exception {

        SetMultimap<String, File> toValidate = init(inputs);

        boolean buildOK = true;
        for (String validator : toValidate.keySet()) {
            System.out.println("Validate by: " + validator);
            buildOK &= execCoreValidator(validator, toValidate.get(validator), classpath);
        }

        if (!buildOK) {
            outputDir.delete();
            throw new RuntimeException("Dictionary validation failed");
        }
    }

    @Input
    public String getDefault() {
        return defaultValidator;
    }

    public void setDefault(String name) {
        this.defaultValidator = name;
    }

    public void validate(String name) {
        validate(defaultValidator, name);
    }

    public void validate(String validator, String name) {
        pendingRequests.put(validator, name);
    }

    public void remove(String validator, String name) {
        excluded.put(validator, name);
    }

    public void sourceDir(List<File> inputs) {
        sourceDirs.addAll(inputs);
    }

    @InputFiles
    public List<File> getSourceDirs() {

        return sourceDirs;
    }
    
    
    //without incrementaltaskInputs returns only directories
    @InputFiles
    public List<File> getFiles() {
        List<File> result = new ArrayList<>();
        
        for (File sourceDir:sourceDirs) {
            if (sourceDir.exists()) {
                Collections.addAll(result, sourceDir.listFiles(new XMLFileNameFilter()));
            }
        }
        
        return result;
    }

    @OutputDirectory
    public File getOutputDir() {
        return outputDir;
    }

    private Collection<File> resolveFiles(String pattern, Collection<File> from) {
        List<File> ret = new ArrayList<>();
        Pattern p = Pattern.compile(pattern);
        for (File dict : from) {
            if (p.matcher(dict.getName()).matches()) {
                ret.add(dict);
            }
        }
        return ret;
    }

    private boolean execCoreValidator(final String validator, final Collection<File> dictionary, final FileCollection classpath) {
        if (dictionary.size() == 0) {
            return true;
        }
        @SuppressWarnings("serial")
        ExecResult result = getProject().javaexec(new Closure<JavaExecSpec>(this, this) {
            @SuppressWarnings({ "unused" })
            void doCall() {
                setProperty("classpath", classpath);
                setProperty("main", "com.exactpro.sf.util.DictionaryValidator");
                List<Object> args = new ArrayList<>(dictionary.size() + 1);
                args.add(validator);
                args.addAll(dictionary);
                setProperty("args", args);
                setProperty("ignoreExitValue", true);
            }
        });
        return result.getExitValue() == 0;
    }

    private SetMultimap<String, File> init(IncrementalTaskInputs inputs) throws IOException {

        SetMultimap<String, File> result = matchFiles(inputs, pendingRequests, excluded, sourceDirs);

        classpath = getProject().getConvention().getPlugin(JavaPluginConvention.class)
                .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getRuntimeClasspath();

        return result;
    }

    private void intersectRequestsToOutdated(IncrementalTaskInputs inputs, Collection<File> files) {

        final List<File> outdated = new ArrayList<>();
        
        inputs.outOfDate(new Action<InputFileDetails>() {
            public void execute(InputFileDetails arg0) {
                try {
                    outdated.add(arg0.getFile().getCanonicalFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        });
        
        files.retainAll(outdated);
    }

    private SetMultimap<String, File> matchFiles(IncrementalTaskInputs inputs, SetMultimap<String, String> requests,
            SetMultimap<String, String> excludedRequests, Collection<File> sourceDirs) throws IOException {

        SetMultimap<String, File> result = HashMultimap.create();
        Collection<File> allFiles = result.get(defaultValidator);
        
        collectXmlFiles(sourceDirs, allFiles);
        intersectRequestsToOutdated(inputs, allFiles);
        
        for (String validator : requests.keySet()) {
            
            if (validator.equals(defaultValidator)) {
                continue;
            }
            
            Collection<File> from = allFiles;
            Collection<File> to = result.get(validator);

            for (String regexp : requests.get(validator)) {
                // resolve files for specified validator
                Collection<File> mappedFiles = resolveFiles(regexp, from);
                // exclude files from computed collection
                for (String excludedregexp : excludedRequests.get(validator)) {
                    mappedFiles.removeAll(resolveFiles(excludedregexp, mappedFiles));
                }
                to.addAll(mappedFiles);
                from.removeAll(to);
            }
        }
        return result;
    }

    private void collectXmlFiles(Collection<File> sourceDirs, Collection<File> forFiles) throws IOException {

        for (File sourceDir : sourceDirs) {
            if (sourceDir.exists()) {
                for (File canonicalFile : sourceDir.listFiles(new XMLFileNameFilter())) {
                    forFiles.add(canonicalFile.getCanonicalFile());
                }
            }
        }
    }

    private class XMLFileNameFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return pathname.getName().toLowerCase().endsWith(XML_EXTENSION) && pathname.isFile();
        }
    }
}
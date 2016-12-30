package com.exactprosystems.testtools;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
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
    private final List<File> allFiles = new ArrayList<>();
    private final List<File> genDirs = new ArrayList<>();
    private final Set<File> pendingGenDirs = new HashSet<>();
    private final SetMultimap<String, String> pendingRequests = HashMultimap.create();
    private final SetMultimap<String, String> excluded = HashMultimap.create();
    private final SetMultimap<String, File> toValidate = HashMultimap.create(); 
    private String defaultValidator = "";
    private FileCollection classpath;
    private File outputDir = new File(getProject().getBuildDir(), "validated");
    
    @TaskAction
    public void validateDictionary(IncrementalTaskInputs inputs) throws Exception {
        init(inputs);
        
        boolean buildOK = true;
        for (String validator:toValidate.keySet()) {
                buildOK &= validateFiles(validator, toValidate.get(validator));
        }
        System.out.println("validate by default validator");
        //Check not validated dictionaries
        buildOK &= validateFiles(defaultValidator, allFiles);
        
        if (!buildOK) {
            outputDir.delete();
            throw new RuntimeException("Dictionary validation failed");
        }
    }
    
    @Input
    public void setDefault(String name) {
        this.defaultValidator = name;
    }
    
    public void validate(String name) {
        validate(defaultValidator, name);
    }
    
    public void validate(String validator, String name) {
        pendingRequests.put(validator, name);
    }
    
    public void remove(String name) {
        remove(defaultValidator, name);
    }
    
    public void remove(String validator, String name) {
        excluded.put(validator, name);
    }
    
    public void sourceDir(List<File> inputs, boolean useDefaultValidator) {
        for (File input:inputs) {
            if (input == null || input.listFiles() == null) {
                continue;
            }
            if (!useDefaultValidator) {
                pendingGenDirs.add(input);
            } else {
                Collections.addAll(allFiles, input.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(XML_EXTENSION);
                    }
                }));
            }
        }
    }
    
    public void sourceDir(List<File> inputs) {
        sourceDir(inputs,true);
    }
    
    @InputFiles
    public List<File> getGenDirs() {
        return genDirs;
    }
    @InputFiles
    public List<File> getAllFiles() {
        return allFiles;
    }
    @OutputDirectory
    public File getOutputDir() {
        return outputDir;
    }
    
    private List<File> resolveFiles(String pattern) {
        List<File> ret = new ArrayList<>();
        Pattern p = Pattern.compile(pattern);
        for (File dict:allFiles) {
            if (p.matcher(dict.getName()).matches()) {
                ret.add(dict); 
            }
        }
        return ret;
    }
    
    private boolean validateFiles(String validator, Collection<File> dictionary) {
        try {
            return execCoreValidator(validator, dictionary);
        } finally {
            allFiles.removeAll(dictionary);
        }
    }
    
    
    private boolean execCoreValidator(final String validator, final Collection<File> dictionary) {
        if (dictionary.size() == 0) {
            return true;
        }
        @SuppressWarnings("serial")
        ExecResult result =  getProject().javaexec(new Closure<JavaExecSpec>(this, this) {
            @SuppressWarnings({"unused" })
            void doCall() {
                setProperty("classpath", classpath);
                setProperty("main", "com.exactprosystems.testtools.util.DictionaryValidator");
                List<Object> args = new ArrayList<>(dictionary.size()+1);
                args.add(validator);
                args.addAll(dictionary);
                setProperty("args", args);
                setProperty("ignoreExitValue", true);
            }
        });
        return result.getExitValue() == 0;
    }
    
    private void init(IncrementalTaskInputs inputs) {
        //include all files from CopyFromDataTask
        for (File directory:pendingGenDirs) {
            Collections.addAll(allFiles, directory.listFiles());
            genDirs.add(directory);
        }
        
        final List<File> outdated = new ArrayList<>();
        
        inputs.outOfDate(new Action<InputFileDetails>() {
            public void execute(InputFileDetails arg0) {
                outdated.add(arg0.getFile());
            };
        });
        
        allFiles.retainAll(outdated);
        
        //search files in full file list
        matchFiles();
        matchExcluded();
        
        //exclude files not used defaultValidator
        for (File gen:genDirs) {
            if (gen.listFiles() != null) {
                allFiles.removeAll(Arrays.asList(gen.listFiles()));
            }
        }
        classpath = getProject().getConfigurations().getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME);
    }
    
    private void matchFiles() {
        for (String validator:pendingRequests.keySet()) {
            for (String name:pendingRequests.get(validator)) {
                toValidate.putAll(validator, resolveFiles(name));
            }
        }
        pendingRequests.clear();
    }
    
    private void matchExcluded() {
        for (String validator:excluded.keySet()) {
            for (String name:excluded.get(validator)) {
                for (File toRemove:resolveFiles(name)) {
                    toValidate.remove(validator, toRemove);
                }
            }
        }
        excluded.clear();
    }
}

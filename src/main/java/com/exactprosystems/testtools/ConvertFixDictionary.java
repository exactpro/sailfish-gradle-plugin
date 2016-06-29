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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.AntBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import groovy.lang.Closure;

public class ConvertFixDictionary extends DefaultTask {
    @Input
    private String fileSuffix;
    @InputDirectory
    private File inputDirectory;
    @OutputDirectory
    private File outputDirectory;

    @SuppressWarnings("serial")
    @TaskAction
    public void generateXmlFix() throws TaskExecutionException, IOException {
        Map<String, Object> fileTreeArgs = new HashMap<String, Object>() {{
            put("dir", inputDirectory);
            put("include", "*.xml");
        }};

        Project project = getProject();
        final AntBuilder antBuilder = project.getAnt();
        FileTree dictionaries = project.fileTree(fileTreeArgs);
        final String configurationPath = project.getConfigurations().getByName("compile").getAsPath();

        Map<String, Object> xsltArgs = new HashMap<String, Object>() {{
            put("extension", ".xml");
            put("classpath", configurationPath);
            put("basedir", inputDirectory);
            put("destdir", outputDirectory);
        }};

        ClassLoader classLoader = ConvertFixDictionary.class.getClassLoader();

        try(InputStream xslResource = classLoader.getResourceAsStream("fix/qfj2dict.xsl");
            InputStream typesResource = classLoader.getResourceAsStream("fix/types.xml")) {

            File tmpDir = getTemporaryDir();

            File xslFile = new File(tmpDir, "qfj2dict.xsl");
            File typesFile = new File(tmpDir, "types.xml");

            FileUtils.writeLines(xslFile, IOUtils.readLines(xslResource));
            FileUtils.writeLines(typesFile, IOUtils.readLines(typesResource));

            xsltArgs.put("style", xslFile);

            Map<String, Object> mapperArgs = new HashMap<String, Object>() {{
                put("type", "glob");
                put("from", "*.xml");
                put("to", "*." + fileSuffix + ".xml");
            }};

            Map<String, Object> prefixParamArgs = new HashMap<String, Object>() {{
               put("name", "nsprefix");
               put("expression", fileSuffix + "_");
            }};

            Map<String, Object> sessionDictionaryParamArgs = new HashMap<String, Object>() {{
               put("name", "sessionDictionary");
               put("expression", new File(inputDirectory, "FIXT11.xml").getAbsolutePath());
            }};

            for(File dictionary : dictionaries) {
                String dictionaryName = dictionary.getName();

                if(dictionaryName.startsWith("FIXT")) {
                    continue;
                }

                xsltArgs.put("includes", dictionaryName);

                antBuilder.invokeMethod("xslt", new Object[] { xsltArgs, new Closure<Object>(this, this) {
                    @SuppressWarnings("unused")
                    public void doCall(Object args) {
                        antBuilder.invokeMethod("mapper", mapperArgs);
                        antBuilder.invokeMethod("param", prefixParamArgs);

                        if(dictionaryName.startsWith("FIX5")) {
                            antBuilder.invokeMethod("param", sessionDictionaryParamArgs);
                        }
                    }
                }});
            }
        }
    }

    @Input
    public String getFileSuffix() {
        return fileSuffix;
    }

    @Input
    public void setFileSuffix(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    @InputDirectory
    public File getInputDirectory() {
        return inputDirectory;
    }

    @InputDirectory
    public void setInputDirectory(File inputDirectory) {
        this.inputDirectory = inputDirectory;
    }

    @OutputDirectory
    public File getOutputDirectory() {
        return outputDirectory;
    }

    @OutputDirectory
    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
}

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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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

public class ConvertFASTTemplate extends DefaultTask {

    private String xslPath = "fast/fast2dict.xsl";
    private String inputFolderPath = "";
    private String outputFolderPath = "";

    @TaskAction
    public void generateXmlFAST() throws TaskExecutionException, IOException {

        Project project = getProject();

        Map<String, String> params = new HashMap<>();
        params.put("dir", inputFolderPath);
        params.put("include", "*.xml");

        try(InputStream fileInputXslt = ConvertFASTTemplate.class.getClassLoader().getResourceAsStream(xslPath)) {

            byte[] content = new byte[fileInputXslt.available()];

            fileInputXslt.read(content);

            final String xsltString = new String(content, StandardCharsets.UTF_8);

            FileTree dictionaries = project.fileTree(params);

            final AntBuilder ant = getProject().getAnt();

            for (final File file : dictionaries) {
                Map<String, Object> xsltParams = new HashMap<>();

                xsltParams.put("extension", ".xml");
                xsltParams.put("basedir", inputFolderPath);
                xsltParams.put("includes", file.getName());
                xsltParams.put("destdir", outputFolderPath);
                xsltParams.put("force", true);

                ant.invokeMethod("xslt", new Object[]{xsltParams, new Closure<Object>(this, this) {

                    public void doCall(Object ignore) {
                        ant.invokeMethod("style", new Object[]{new Closure<Object>(this, this) {

                            public void doCall(Object ignore) {
                                Map<String, String> xsltContentParam = new HashMap<>();

                                xsltContentParam.put("value", xsltString);

                                ant.invokeMethod("string", xsltContentParam);
                            }
                        }});
                    }
                }});
            }
        }
    }

    @Input
    public String getXslPath() {
        return xslPath;
    }

    public void setXslPath(String xslPath) {
        this.xslPath = xslPath;
    }

    public String getInputFolderPath() {
        return inputFolderPath;
    }

    public void setInputFolderPath(String inputFolderPath) {
        this.inputFolderPath = inputFolderPath;
    }

    public String getOutputFolderPath() {
        return outputFolderPath;
    }

    public void setOutputFolderPath(String outputFolderPath) {
        this.outputFolderPath = outputFolderPath;
    }

    @OutputDirectory
    public File getOutputFolder() {
        return new File(this.outputFolderPath);
    }
    
    @InputDirectory
    public File getInputFolder() {
        return new File(this.inputFolderPath);
    }
}
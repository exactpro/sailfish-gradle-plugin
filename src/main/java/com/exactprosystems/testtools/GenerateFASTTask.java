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

import org.gradle.api.AntBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class GenerateFASTTask extends DefaultTask {

    @TaskAction
    public void generateXmlFAST() throws TaskExecutionException {

        Project project = getProject();
        GradleFASTPluginExtension extension = getProject().getExtensions()
                .findByType(GradleFASTPluginExtension.class);

        String xslPath = extension.getXslPath();
        String inputFolderPath = extension.getInputFolderPath();
        String outputFolderPath = extension.getOutputFolderPath();

        Map<String, String> params = new HashMap<>();
        params.put("dir", inputFolderPath);
        params.put("include", "*.xml");

        String xsltTempFilePath = outputFolderPath +
                (xslPath.startsWith(File.separator) ? "" : File.separator) +
                xslPath;
        File xsltTempFile = new File(xsltTempFilePath);

        try {
            xsltTempFile.getParentFile().mkdirs();
            xsltTempFile.createNewFile();

            try(InputStream fileInputXslt = GenerateFASTTask.class.getClassLoader().getResourceAsStream(xslPath);
                OutputStream fileOutputXslt = new FileOutputStream(xsltTempFile)) {

                byte[] content = new byte[fileInputXslt.available()];

                fileInputXslt.read(content);
                fileOutputXslt.write(content);

                FileTree dictionaries = project.fileTree(params);

                AntBuilder ant = getProject().getAnt();

                for (final File file : dictionaries) {
                    Map<String, Object> xsltParams = new HashMap<>();

                    xsltParams.put("extension", ".xml");
                    xsltParams.put("style", xsltTempFile.getCanonicalPath());
                    xsltParams.put("basedir", inputFolderPath);
                    xsltParams.put("includes", file.getName());
                    xsltParams.put("destdir", outputFolderPath);

                    ant.invokeMethod("xslt", xsltParams);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(xsltTempFile.exists()) {
                xsltTempFile.delete();
            }
        }
    }
}
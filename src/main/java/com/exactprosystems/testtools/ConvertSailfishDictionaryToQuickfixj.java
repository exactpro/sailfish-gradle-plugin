/******************************************************************************
 * Copyright 2009-2017 Exactpro (Exactpro Systems Limited)
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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import groovy.lang.Closure;

/**
 * @author oleg.smirnov
 *
 */
public class ConvertSailfishDictionaryToQuickfixj extends DefaultTask {

    @InputDirectory
    private File inputDirectory;

    @OutputDirectory
    private File outputDirectory;

    @Input
    private String filterExpression;

    @SuppressWarnings("serial")
    @TaskAction
    public void generateXmlQuicfixj()
            throws TaskExecutionException, IOException, ParserConfigurationException, SAXException {

        Map<String, Object> fileTreeArgs = new HashMap<String, Object>() {
            {
                put("dir", inputDirectory);
                put("include", "*.xml");
            }
        };

        Project project = getProject();
        final AntBuilder antBuilder = project.getAnt();
        FileTree dictionaries = project.fileTree(fileTreeArgs);
        final String configurationPath = project.getConfigurations().getByName("compile").getAsPath();

        Map<String, Object> xsltArgs = new HashMap<String, Object>() {
            {
                put("extension", ".xml");
                put("classpath", configurationPath);
                put("basedir", inputDirectory);
                put("destdir", outputDirectory);
            }
        };

        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db = dbf.newDocumentBuilder();

        ClassLoader classLoader = ConvertFixDictionary.class.getClassLoader();

        try (InputStream xslResource = classLoader.getResourceAsStream("fix/slf2qfjDict.xsl");
                InputStream typesResource = classLoader.getResourceAsStream("fix/types.xml")) {

            File tmpDir = getTemporaryDir();

            File xslFile = new File(tmpDir, "slf2qfjDict.xsl");
            File typesFile = new File(tmpDir, "types.xml");

            try (FileOutputStream xslOut = new FileOutputStream(xslFile);
                    FileOutputStream typesOut = new FileOutputStream(typesFile)) {
                IOUtils.copy(xslResource, xslOut);
                IOUtils.copy(typesResource, typesOut);
            }

            xsltArgs.put("style", xslFile);

            final Pattern pattern = Pattern.compile(filterExpression);

            for (File dictionary : dictionaries) {
                final String dictionaryName = dictionary.getName();

                if (!pattern.matcher(dictionaryName).matches()) {
                    continue;
                }

                xsltArgs.put("includes", dictionaryName);

                Node node = db.parse(dictionary).getElementsByTagName("dictionary").item(0);

                String[] version = node.getAttributes().getNamedItem("name").getNodeValue().split("_");
                final String minor = version[version.length - 1], major = version[version.length - 2];

                antBuilder.invokeMethod("xslt", new Object[] { xsltArgs, new Closure<Object>(this, this) {
                    @SuppressWarnings("unused")
                    public void doCall(Object args) {
                        antBuilder.invokeMethod("mapper", new HashMap<String, Object>() {
                            {
                                put("type", "glob");
                                put("from", "*.xml");
                                put("to", "FIX" + major + minor + ".xml");
                            }
                        });
                        antBuilder.invokeMethod("param", getParamMap("minor", Integer.parseInt(minor)));
                        antBuilder.invokeMethod("param", getParamMap("major", Integer.parseInt(major)));
                        antBuilder.invokeMethod("param",
                                getParamMap("mode", Integer.parseInt(major) >= 5 ? "app" : "all"));
                    }
                } });

                if (Integer.parseInt(major) >= 5)
                    antBuilder.invokeMethod("xslt", new Object[] { xsltArgs, new Closure<Object>(this, this) {
                        @SuppressWarnings("unused")
                        public void doCall(Object args) {
                            antBuilder.invokeMethod("mapper", new HashMap<String, Object>() {
                                {
                                    put("type", "glob");
                                    put("from", "*.xml");
                                    put("to", "FIXT11.xml");
                                }
                            });
                            antBuilder.invokeMethod("param", getParamMap("minor", 1));
                            antBuilder.invokeMethod("param", getParamMap("major", 1));
                            antBuilder.invokeMethod("param", getParamMap("mode", "admin"));
                        }
                    } });
            }
        }
    }

    @SuppressWarnings("serial")
    private Map<String, Object> getParamMap(final String paramName, final Object expression) {
        return new HashMap<String, Object>() {
            {
                put("name", paramName);
                put("expression", expression);
            }
        };
    }

    @InputDirectory
    public File getInputDirectory() {
        return inputDirectory;
    }

    @InputDirectory
    public void setInputDirectory(File inputDirectory) {
        this.inputDirectory = inputDirectory;
    }

    @Input
    public void setFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    @Input
    public String getFilterExpression() {
        return this.filterExpression;
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

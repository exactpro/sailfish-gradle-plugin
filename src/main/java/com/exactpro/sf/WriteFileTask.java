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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleScriptException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * @author nikita.smirnov
 *
 */
public class WriteFileTask extends DefaultTask {

    private final List<String> lines;
    
    private File outputDir;
    private String fileName;
    
    public WriteFileTask() {
        this.lines = new ArrayList<>();
        this.outputDir = new File("build");
        this.fileName = "outPutFile.log";
    }
    
    @TaskAction
    public void write() throws FileNotFoundException, IOException {
        if (this.outputDir != null && this.fileName != null && this.lines != null && !this.lines.isEmpty()) {
            if (!this.outputDir.exists()) {
                this.outputDir.mkdirs();
            }

            if (this.outputDir.exists()) {
                File targetFile = getOutputFile();
                try (OutputStream stream = new FileOutputStream(targetFile)) {
                    for (String line : this.lines) {
                        stream.write(line.getBytes());
                        stream.write(System.lineSeparator().getBytes());
                    }
                } catch (Exception e) {
                    throw new GradleScriptException("Problem with file " + targetFile, e);
                }
            } else {
                throw new GradleScriptException("outputDir: " + this.outputDir + " can't be created", null);
            }
        } else {
            throw new GradleScriptException("write can't be executed with outputDir: "
                        + this.outputDir + " outputFile: " + this.fileName + " lines: " + this.lines, null);
        }
    }

    public void line(String line) {
        this.lines.add(line);
    }
    
    public void lines(Collection<String> lines) {
        if (lines != null) {
            for (String line : lines) {
                line(line);
            }
        }
    }

    @OutputDirectory
    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    @OutputFile
    public File getOutputFile() {
        return new File(this.outputDir, this.fileName);
    }

    @Input
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
}

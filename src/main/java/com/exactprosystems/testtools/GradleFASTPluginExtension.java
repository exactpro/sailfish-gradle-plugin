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

public class GradleFASTPluginExtension {

    private String xslPath = "fast/fast2dict.xsl";
    private String inputFolderPath = "";
    private String outputFolderPath = "";

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

    public void setSampleFilePath(String xslPath){
        this.xslPath = xslPath;
    }
    public String getSampleFilePath(){
        return xslPath;
    }

}
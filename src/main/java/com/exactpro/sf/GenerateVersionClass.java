/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.lang.model.SourceVersion;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleScriptException;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.ParseException;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;

public class GenerateVersionClass extends DefaultTask {
    
    private static final Logger logger = LoggerFactory.getLogger(GenerateVersionClass.class);
    
    private static final String SERVICE_LOADER_FILE = "com.exactpro.sf.center.IVersion";
    
    private final Template template;

    private String packageName = "com.exactpro.sf.center.impl";
    private File outputJavaDir = new File("src/gen/java");
    private File outputResourceDir = new File("src/gen/resources");
    private boolean isPlugin = false;
    private int major = 0;
    private int minor = 0;
    private int maintenance = 0;
    private int minCoreRevision = 0;
    private int build = 0;
    private String alias = "impl";
    private String branch = "master";
    private String revision = "std";
    private String artifactName = "none";

    public GenerateVersionClass() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_24);

        configuration.setTemplateLoader(new ClassTemplateLoader(getClass(), "/template"));
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setAPIBuiltinEnabled(true);
        configuration.setObjectWrapper(new BeansWrapperBuilder(Configuration.VERSION_2_3_24).build());
        configuration.setRecognizeStandardFileExtensions(true);

        this.template = configuration.getTemplate("version_class.ftl");
    }

    @TaskAction
    public void write() throws FileNotFoundException, IOException {
        writeClassFile();
        if (this.isPlugin) {
            writeServiceFile();
        }
    }

    /**
     * @return the isPlugin
     */
    public boolean isPlugin() {
        return isPlugin;
    }

    /**
     * @param isPlugin the isPlugin to set
     */
    public void setPlugin(boolean isPlugin) {
        this.isPlugin = isPlugin;
    }

    /**
     * @return the packageName
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @param packageName
     *            the packageName to set
     */
    public void setPackageDir(String packageName) {
        this.packageName = Objects.requireNonNull(packageName, "'Package name' can't be null");
    }
    
    /**
     * @return the outputDir
     */
    public File getOutputJavaDir() {
        return outputJavaDir;
    }

    /**
     * @param outputJavaDir
     *            the outputDir to set
     */
    public void setOutputJavaDir(File outputJavaDir) {
        this.outputJavaDir = Objects.requireNonNull(outputJavaDir, "'Output java directory' can't be null");
    }
    
    /**
     * @return the outputResourceDir
     */
    public File getOutputResourceDir() {
        return outputResourceDir;
    }

    /**
     * @param outputResourceDir the outputResourceDir to set
     */
    public void setOutputResourceDir(File outputResourceDir) {
        this.outputResourceDir = Objects.requireNonNull(outputResourceDir, "'Output resource directory' can't be null");
    }

    /**
     * @return the major
     */
    public int getMajor() {
        return major;
    }

    /**
     * @param major
     *            the major to set
     */
    public void setMajor(int major) {
        this.major = requireNonNegative(major, "Major");
    }

    /**
     * @return the minor
     */
    public int getMinor() {
        return minor;
    }

    /**
     * @param minor
     *            the minor to set
     */
    public void setMinor(int minor) {
        this.minor = requireNonNegative(minor, "Minor");
    }

    /**
     * @return the maintenance
     */
    public int getMaintenance() {
        return maintenance;
    }

    /**
     * @param maintenance
     *            the maintenance to set
     */
    public void setMaintenance(int maintenance) {
        this.maintenance = requireNonNegative(maintenance, "Maintenance");
    }

    /**
     * @return the minCoreRevision
     */
    public int getMinCoreRevision() {
        return minCoreRevision;
    }

    /**
     * @param minCoreRevision the minCoreRevision to set
     */
    public void setMinCoreRevision(int minCoreRevision) {
        this.minCoreRevision = requireNonNegative(minCoreRevision, "Min core revision");
    }

    /**
     * @return the build
     */
    public int getBuild() {
        return build;
    }

    /**
     * @param build
     *            the build to set
     */
    public void setBuild(int build) {
        this.build = requireNonNegative(build, "Build");
    }

    /**
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * @param revision the revision to set
     */
    public void setRevision(String revision) {
        this.revision = requireNonBlank(revision, "Revision");
    }

    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param alias
     *            the alias to set
     */
    public void setAlias(String alias) {
        requireNonBlank(alias, "Alias");
        if (!SourceVersion.isIdentifier(alias)) {
            throw new GradleScriptException("'Alias' invalid format", null);
        }
        this.alias = alias;
    }

    /**
     * @return the branch
     */
    public String getBranch() {
        return branch;
    }

    /**
     * @param branch
     *            the branch to set
     */
    public void setBranch(String branch) {
        this.branch = requireNonBlank(branch, "Branch");
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = requireNonBlank(artifactName, "Artifact name");
    }

    @OutputFiles
    public List<File> getOutputFiles() {
        List<File> result = new ArrayList<>();
        result.add(getClassFile());
        if (this.isPlugin) {
            result.add(getServiceFile());
        }
        logger.info("OutputFiles {}", result);
        return result;
    }
    
    public File getClassFile() {
        return Paths.get(getProject().getProjectDir().toString(), this.outputJavaDir.toString(), packageToPath(this.packageName), getClassName() + ".java").toFile();
    }
    
    public File getServiceFile() {
        return Paths.get(getProject().getProjectDir().toString(), this.outputResourceDir.toString(), "META-INF", "services", SERVICE_LOADER_FILE).toFile();
    }

    private void writeServiceFile() {
        File outputFile = getServiceFile();
        File parentDir = outputFile.getParentFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new GradleScriptException("Directory '" + parentDir + "' can't be created", null);
            }
        }
    
        try (OutputStream stream = new FileOutputStream(outputFile)) {
            stream.write((packageName + '.' + getClassName()).getBytes());
        } catch (IOException e) {
            throw new GradleScriptException("Problem with file " + outputFile, e);
        }
    }

    private void writeClassFile() {
        File outputFile = getClassFile();
        File parentDir = outputFile.getParentFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new GradleScriptException("Directory '" + parentDir + "' can't be created", null);
            }
        }
    
        Map<String, Object> data = new HashMap<>();
        data.put("className", getClassName());
        data.put("isPlugin", this.isPlugin);
        data.put("revision", this.revision);
        data.put("major", this.major);
        data.put("minor", this.minor);
        data.put("maintenance", this.maintenance);
        data.put("minCoreRevision", this.minCoreRevision);
        data.put("build", this.build);
        data.put("alias", this.alias);
        data.put("branch", this.branch);
        data.put("artifactName", this.artifactName);
        
        try (OutputStream stream = new FileOutputStream(outputFile);
                Writer writer = new OutputStreamWriter(stream)) {
            this.template.process(data, writer);
        } catch (IOException | TemplateException e) {
            throw new GradleScriptException("Problem with file " + outputFile, e);
        }
    }

    private String getClassName() {
        return WordUtils.capitalizeFully(this.alias).replaceAll("\\W", "") + "Version";
    }
    
    private String packageToPath(String packageName) {
        return packageName.replace('.', File.separatorChar);
    }
    
    private String requireNonBlank(String value, String field) {
        if (StringUtils.isBlank(value)) {
            throw new GradleScriptException("'" + field + "' can't be blank", null);
        }
        return value;
    }

    private int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new GradleScriptException("'" + field + "' can't be negative", null);
        }
        return value;
    }
}

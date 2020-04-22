<#--
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  ~ Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
-->
<#function getValue staticValue staticVariable>
	<#return isPlugin?then(staticValue, staticVariable)>
</#function>
<#assign formattedAlias = isPlugin?then('"${alias}"', "GENERAL")>
<#assign formattedBranch = '"${branch!"master"}"'>
<#assign formattedRevision = '"${revision!"std"}"'>
<#assign formattedArtifactName = '"${artifactName!"none"}"'>
/******************************************************************************
 * Copyright 2009-${.now?string('yyyy')} Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.center.impl;

public class ${className} extends AbstractVersion {
	<#if !isPlugin>
	
    public static final int MAJOR = ${major?c};
    public static final int MINOR = ${minor?c};
    public static final int MAINTENANCE = ${maintenance?c};
    public static final int BUILD = ${build?c};
    public static final String ALIAS = ${formattedAlias};
    public static final String BRANCH = ${formattedBranch};
    public static final String ARTIFACT_NAME = ${formattedArtifactName};
	</#if>
	

    @Override
    public int getMajor() {
        return ${getValue(major?c, "MAJOR")};
    }

    @Override
    public int getMinor() {
        return ${getValue(minor?c, "MINOR")};
    }

    @Override
    public int getMaintenance() {
        return ${getValue(maintenance?c, "MAINTENANCE")};
    }

    @Override
    public int getBuild() {
        return ${getValue(build?c, "BUILD")};
    }

    @Override
    public String getAlias() {
        return ${getValue(formattedAlias, "ALIAS")};
    }

    @Override
    public String getBranch() {
        return ${getValue(formattedBranch, "BRANCH")};
    }

    @Override
    public String getArtifactName() {
        return ${getValue(formattedArtifactName, "ARTIFACT_NAME")};
    }
    <#if isPlugin>
    
    @Override
    public String getRevision() {
        return ${formattedRevision};
    }
    
    @Override
    public int getMinCoreRevision() {
        return ${minCoreRevision};
    }
    </#if>
}
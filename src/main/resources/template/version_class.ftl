<#function getValue staticValue staticVariable>
	<#return isPlugin?then(staticValue, staticVariable)>
</#function>
<#assign formattedAlias = isPlugin?then('"${alias}"', "GENERAL")>
<#assign formattedBranch = '"${branch!"master"}"'>
<#assign formattedRevision = '"${revision!"std"}"'>
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
package com.exactprosystems.testtools.center.impl;

public class ${className} extends AbstractVersion {
	<#if !isPlugin>
	
    public static final int MAJOR = ${major};
    public static final int MINOR = ${minor};
    public static final int MAINTENANCE = ${maintenance};
    public static final int BUILD = ${build};
    public static final String ALIAS = ${formattedAlias};
    public static final String BRANCH = ${formattedBranch};
	</#if>
	

    @Override
    public int getMajor() {
        return ${getValue(major, "MAJOR")?c};
    }

    @Override
    public int getMinor() {
        return ${getValue(minor, "MINOR")?c};
    }

    @Override
    public int getMaintenance() {
        return ${getValue(maintenance, "MAINTENANCE")?c};
    }

    @Override
    public int getBuild() {
        return ${getValue(build, "BUILD")?c};
    }

    @Override
    public String getAlias() {
        return ${getValue(formattedAlias, "ALIAS")};
    }

    @Override
    public String getBranch() {
        return ${getValue(formattedBranch, "BRANCH")};
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
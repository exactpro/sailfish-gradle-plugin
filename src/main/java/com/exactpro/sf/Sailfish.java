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

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class Sailfish implements Plugin<Project> {

    @Override
    public void apply(Project target) {
        List<DefaultTask> list = new ArrayList<>();
        list.add(target.getTasks().create("generateXmlFAST", ConvertFASTTemplate.class));
        list.add(target.getTasks().create("writeBuildInfo", BuildInfoWriter.class));
        list.add(target.getTasks().create("writeFile", WriteFileTask.class));
        list.add(target.getTasks().create("generateXmlFix", ConvertFixDictionary.class));
        list.add(target.getTasks().create("checkCompatibility", CompatibilityChecker.class));
        list.add(target.getTasks().create("validateDictionary", DictionaryValidatorPlugin.class));
        list.add(target.getTasks().create("generateVersionClass", GenerateVersionClass.class));
        list.add(target.getTasks().create("generateXmlQuicfixj", ConvertSailfishDictionaryToQuickfixj.class));
        list.add(target.getTasks().create("collectDependencies", DependencyCollector.class));
        list.add(target.getTasks().create("convertFixOrchestraToSailfishDictionary", OrchestraToSailfishConverter.class));
        
        for (DefaultTask defaultTask : list) {
            defaultTask.setGroup("Sailfish");
        }
    }
}
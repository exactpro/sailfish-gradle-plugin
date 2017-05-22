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
package com.exactprosystems.testtools

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.util.jar.JarFile

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class DependencyCollector extends DefaultTask {
    @Input
    def repository
    @Input
    def buildArguments = [:]
    @Input
    def outputPath
    @Input
    def incrementalBuild
    @Input
    def type = ''

    @TaskAction
    def collectDependencies() {
        if(!incrementalBuild) {
            throw new Exception('This task can only be executed in incremental build mode')
        }

        if(!outputPath?.trim()) {
            throw new Exception('Dependency file path cannot be empty')
        }

        def outputFile = new File(outputPath)
        def dependencyMap = outputFile.length() ? new JsonSlurper().parse(outputFile) : [:]
        def jarFiles = [] as Set

        project.configurations.each { configuration ->
            configuration.allDependencies.grep {
                it.group == 'com.exactprosystems.testtools'
            }.each { dependency ->
                jarFiles.addAll(configuration.files(dependency).grep {
                    (it.name.endsWith('jar') || it.name.endsWith('zip')) && it.name.contains(dependency.name)
                })
            }
        }

        def dependencies = [:]

        jarFiles.each { jarFile ->
            new JarFile(jarFile).withCloseable {
                if(!it.manifest) {
                    return
                }

                def attributes = it.manifest.mainAttributes
                def projectName = attributes.getValue('Project_Name')

                if(projectName) {
                    def dependencyProperties = [:]
                    def release = attributes.getValue('Release')
                    def dictionaryVersion = attributes.getValue('Dictionary_Version')

                    if(release && !buildArguments.release) {
                        dependencyProperties.release = release
                    }

                    if(dictionaryVersion && !buildArguments.dictVer) {
                        dependencyProperties.dictVer = dictionaryVersion
                    }

                    dependencies[projectName] = dependencyProperties
                }
            }
        }

        def rootPath = project.rootDir.parentFile.toPath()

        dependencyMap["${repository}-${project.name}"] = [
            dependencies: dependencies,
            subprojects: project.subprojects.collect { "${repository}-${it.name}" },
            type: type,
            publishResource: "${rootPath.relativize(project.buildDir.toPath())}/release",
            path: "${rootPath.relativize(project.projectDir.toPath())}",
            buildArguments: buildArguments,
            isNode: project.subprojects.asBoolean(),
            repository: repository
        ]

        def json = JsonOutput.prettyPrint(JsonOutput.toJson(dependencyMap))
        outputFile.text = json
    }
}

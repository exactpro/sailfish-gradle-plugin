/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf

import io.fixprotocol.orchestra.quickfix.DataDictionaryGenerator
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class OrchestraToSailfishConverter extends DefaultTask {

    @InputFile
    URI orchestraXml
    @OutputDirectory
    File outputDirectory
    @Input
    String fileSuffix


    @TaskAction
    def convert() {

        def generator = new DataDictionaryGenerator()

        def orchestraTarget = new File(project.buildDir, 'orchestrafiles')

        orchestraXml.toURL().withInputStream { stream ->
            try {
                generator.generate(stream, orchestraTarget)
            } catch (e) {
                e.printStackTrace()
            }
        }

        def fixDictionary = new FileNameFinder().getFileNames(orchestraTarget.absolutePath, 'FIX*.xml').first()
        println fixDictionary

        convert(orchestraTarget, outputDirectory, fileSuffix)

    }

    def convert(File inputDirectory, File outputDirectory, String fileSuffix) {

        def fileTreeArgs = ["dir": inputDirectory, "include": "*.xml"]
        def dictionaries = project.fileTree(fileTreeArgs)
        def configurationPath = project.getConfigurations().getByName("compile").getAsPath()

        def xsltArgs = ["extension": ".xml", "classpath": configurationPath, "basedir": inputDirectory, "destdir": outputDirectory]

        ClassLoader classLoader = ConvertFixDictionary.class.getClassLoader()

        InputStream xslResource = classLoader.getResourceAsStream("fix/qfj2dict.xsl")
        InputStream typesResource = classLoader.getResourceAsStream("fix/types.xml")

        File tmpDir = getTemporaryDir()

        File xslFile = new File(tmpDir, "qfj2dict.xsl")
        File typesFile = new File(tmpDir, "types.xml")

        xslResource.withCloseable {
            FileUtils.writeLines(xslFile, IOUtils.readLines(xslResource))
        }
        typesResource.withCloseable {
            FileUtils.writeLines(typesFile, IOUtils.readLines(typesResource))
        }

        xsltArgs << ["style": xslFile]

        def mapperArgs = ["type": "glob", "from": "*.xml", "to": "*.${fileSuffix}.xml"]
        def prefixParamArgs = ["name": "nsprefix", "expression": fileSuffix + "_"]

        dictionaries.each { File dictionary ->

            if (dictionary.name.startsWith("FIXT")) {
                return
            }

            xsltArgs << ["includes": dictionary.name]

            ant.xslt(xsltArgs) {
                mapper(mapperArgs)
                param(prefixParamArgs)
            }
        }
    }

}

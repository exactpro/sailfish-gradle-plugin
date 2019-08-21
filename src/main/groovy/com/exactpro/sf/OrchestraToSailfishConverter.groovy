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
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class OrchestraToSailfishConverter extends DefaultTask {

    @Input
    URI orchestraXml
    @Input
    @Optional
    URI fixtXml
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
                println 'Cant convert orchestra file to quickfixj file'
                throw e
            }
        }

        if (fixtXml) {
            fixtXml.toURL().withInputStream { stream ->
                def fixtFile = new File(orchestraTarget, 'FIXT11.xml')
                fixtFile << stream
                println "$fixtFile will be used"
            }
        }

        def fixDictionary = new FileNameFinder().getFileNames(orchestraTarget.absolutePath, 'FIX*.xml').first()
        println "$fixDictionary will be used"

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
        def sessionDictionary = new File(inputDirectory, "FIXT11.xml")
        def sessionDictionaryArgs = sessionDictionary.exists() ? ["name": "sessionDictionary", "expression": sessionDictionary.getAbsolutePath()] : [:]

        dictionaries.each { File dictionary ->

            if (dictionary.name.startsWith("FIXT")) {
                return
            }

            xsltArgs << ["includes": dictionary.name]

            ant.xslt(xsltArgs) {
                mapper(mapperArgs)
                param(prefixParamArgs)
                if (sessionDictionaryArgs) {
                    param(sessionDictionaryArgs)
                }
            }
        }
    }

}

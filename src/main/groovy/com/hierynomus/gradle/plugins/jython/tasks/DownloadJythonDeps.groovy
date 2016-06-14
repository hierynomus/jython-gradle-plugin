/*
 * Copyright (C)2015 - Jeroen van Erp <jeroen@hierynomus.com>
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
 */
package com.hierynomus.gradle.plugins.jython.tasks

import com.hierynomus.gradle.plugins.jython.JythonExtension
import groovy.text.SimpleTemplateEngine
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.http.client.methods.HttpGet
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class DownloadJythonDeps extends DefaultTask {

    String configuration

    JythonExtension extension

    @OutputDirectory
    File outputDir

    @TaskAction
    def process() {
        project.configurations.getByName(configuration).allDependencies.withType(ExternalModuleDependency.class)*.each { d ->
            String name = d.name
            logger.lifecycle("Downloading Jython library: $name with version ${d.version}")

            def acceptClosure = getAcceptClosure(d)

            for (String repository : extension.sourceRepositories) {
                def engine = new SimpleTemplateEngine()
                def template = engine.createTemplate(repository)
                def repo = template.make(['dep': d])
                logger.lifecycle("Trying: $repo")
                boolean found = false

                def http = new HTTPBuilder(repo.toString())
                http.request(Method.GET) {
                    response.success = { resp, body ->
                        logger.debug "Got response: ${resp.statusLine}"
                        logger.debug "Response length: ${resp.getFirstHeader('Content-Length')}"
                        ArchiveInputStream stream = UnArchiveLib.getArchiveInputStream(repo.toString(), body)
                        try {
                            UnArchiveLib.uncompressToOutputDir(stream, outputDir, acceptClosure)
                        } finally {
                            stream.close()
                        }
                        found = true
                    }
                }

                if (found) break
            }
        }
    }

    Closure<Boolean> getAcceptClosure(ExternalModuleDependency externalModuleDependency) {
        def artifacts = externalModuleDependency.artifacts
        if (!artifacts) {
            UnArchiveLib.pythonModule(externalModuleDependency.name)
        } else if (artifacts.size() == 1 && artifacts[0].classifier) {
            // Dealing with a renamed module
            UnArchiveLib.pythonModule(artifacts[0].classifier)
        } else {
            // Dealing with explicit artifact(s)
            def names = artifacts.collect(this.&asFileName)
            return { String name -> names.contains(name) }
        }
    }

    def asFileName(DependencyArtifact artifact) {
        String name = artifact.getName()
        if (artifact.extension != null) {
            name += ".${artifact.extension}"
        }
        return name
    }
}

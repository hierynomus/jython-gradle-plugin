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
import com.hierynomus.gradle.plugins.jython.dependency.PythonDependency
import com.hierynomus.gradle.plugins.jython.repository.Repository
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class DownloadJythonDeps extends DefaultTask {

    @Input
    String configuration

    @Internal
    JythonExtension extension

    @OutputDirectory
    File outputDir

    @TaskAction
    def process() {
        project.configurations.getByName(configuration).allDependencies.withType(ExternalModuleDependency.class)*.each { d ->
            String name = d.name
            logger.lifecycle("Downloading Jython library: $name with version ${d.version}")
            boolean found = false
            for (Repository r : extension.sourceRepositories) {
                File cachedDep = r.resolve(extension.pyCacheDir, d)
                if (cachedDep) {
                    found = true

                    if (!(d instanceof PythonDependency)) {
                        ArchiveInputStream stream = UnArchiveLib.getArchiveInputStream(cachedDep)
                        try {
                            UnArchiveLib.uncompressToOutputDir(stream, outputDir, getAcceptClosure(d))
                        } finally {
                            stream.close()
                        }
                    } else {
                        PythonDependency pd = d as PythonDependency
                        UnArchiveLib.unarchive(cachedDep, outputDir, pd, project)
                    }
                }

                if (found) break
            }

            if (!found) {
                throw new IllegalArgumentException("Could not find Jython library $d")
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

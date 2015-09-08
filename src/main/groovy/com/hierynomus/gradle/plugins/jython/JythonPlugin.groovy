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
package com.hierynomus.gradle.plugins.jython

import com.hierynomus.gradle.plugins.jython.tasks.DownloadJythonDeps
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class JythonPlugin implements Plugin<Project> {
    static final RUNTIME_SCOPE_CONFIGURATION = "jython"
    static final TEST_SCOPE_CONFIGURATION = "testJython"

    static final RUNTIME_DEP_DOWNLOAD = "jythonDownload"
    static final TEST_DEP_DOWNLOAD = "testJythonDownload"

    protected JythonExtension extension

    @Override
    void apply(Project project) {
        configureProject(project)
        createTasks(project)

        project.plugins.withType(JavaPlugin) {
            project.tasks.getByName(JavaPlugin.JAR_TASK_NAME).configure {
                from(project.tasks.getByName(RUNTIME_DEP_DOWNLOAD).asType(DownloadJythonDeps).outputDir) {
                    rename { name ->
                        return name.substring(name.indexOf('/') + 1)
                    }
                }
            }
        }
    }

    def configureProject(Project project) {
        project.configurations.create(RUNTIME_SCOPE_CONFIGURATION)
        project.configurations.create(TEST_SCOPE_CONFIGURATION)
        extension = project.extensions.create("jython", JythonExtension)
    }

    def createTasks(Project project) {
        project.tasks.create(RUNTIME_DEP_DOWNLOAD, DownloadJythonDeps).configure {
            configuration = RUNTIME_SCOPE_CONFIGURATION
            outputDir = project.file("${project.buildDir}/jython/main")
            extension = this.extension
        }
        project.tasks.create(TEST_DEP_DOWNLOAD, DownloadJythonDeps).configure {
            configuration = TEST_SCOPE_CONFIGURATION
            outputDir = project.file("${project.buildDir}/jython/test")
            extension = this.extension
        }
    }
}

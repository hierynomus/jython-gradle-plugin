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

import com.hierynomus.gradle.plugins.jython.dependency.PythonDependency
import com.hierynomus.gradle.plugins.jython.tasks.DownloadJythonDeps
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.util.ConfigureUtil

class JythonPlugin implements Plugin<Project> {
    static final RUNTIME_SCOPE_CONFIGURATION = "jython"
    static final TEST_SCOPE_CONFIGURATION = "testJython"

    static final RUNTIME_DEP_DOWNLOAD = "jythonDownload"
    static final TEST_DEP_DOWNLOAD = "testJythonDownload"

    protected JythonExtension extension

    @Override
    void apply(Project project) {
        configureProject(project)
        File cacheDir = createPythonCacheDir(project)
        extension.pyCacheDir = cacheDir

        createTasks(project)

        project.plugins.withType(JavaPlugin) {
            project.sourceSets.main.resources.srcDirs += (project.tasks.getByName(RUNTIME_DEP_DOWNLOAD) as DownloadJythonDeps).outputDir
            project.tasks.getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).configure {
                dependsOn project.tasks.getByName(RUNTIME_DEP_DOWNLOAD)
            }

            project.sourceSets.test.resources.srcDirs += (project.tasks.getByName(TEST_DEP_DOWNLOAD) as DownloadJythonDeps).outputDir
            project.tasks.getByName(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME).configure {
                dependsOn project.tasks.getByName(TEST_DEP_DOWNLOAD)
            }
        }
    }

    File createPythonCacheDir(Project project) {
        def dir = project.gradle.gradleUserHomeDir
        def pyCacheDir = new File(dir, "python")
        if (!pyCacheDir.exists()) {
            pyCacheDir.mkdir()
        }
        return pyCacheDir
    }

    def configureProject(Project project) {
        project.dependencies.ext.python = { depInfo, closure ->
            return PythonDependency.create(depInfo, closure)
        }

        project.configurations.create(RUNTIME_SCOPE_CONFIGURATION)
        project.configurations.create(TEST_SCOPE_CONFIGURATION)
        extension = project.extensions.create("jython", JythonExtension)
        extension.with {
            it.project = project
        }

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

package com.hierynomus.gradle.plugins.jython

import com.hierynomus.gradle.plugins.jython.tasks.DownloadJythonDeps
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

/**
 * Created by ajvanerp on 08/09/15.
 */
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
    }
}

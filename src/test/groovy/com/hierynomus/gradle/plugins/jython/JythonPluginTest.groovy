package com.hierynomus.gradle.plugins.jython

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by ajvanerp on 08/09/15.
 */
class JythonPluginTest extends Specification {
    @Shared Project project
    @Shared File projectDir = new File("rootPrj")

    def setup() {
        projectDir.mkdirs()
        project = ProjectBuilder.builder().withProjectDir(projectDir).withName("test").build()
        project.apply plugin: 'jython'
    }

    def cleanup() {
        projectDir.deleteDir()
    }

    def "should download defined jython library dependency"() {
        setup:
        project.dependencies {
            jython ":boto3:1.1.3"
        }

        when:
        project.tasks.getByName(JythonPlugin.RUNTIME_DEP_DOWNLOAD).execute()

        then:
        new File(project.buildDir, "jython/main/boto3/__init__.py").exists()
    }
}

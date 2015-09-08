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

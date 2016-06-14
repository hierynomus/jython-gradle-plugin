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

import org.gradle.api.tasks.testing.TestResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class JythonPluginIntegrationTest extends Specification {

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    @Unroll
    def "can execute hello world task with Gradle version #gradleVersion"() {
        given:
        buildFile << """
plugins {
    id "java"
    id "com.github.hierynomus.jython"
}

dependencies {
  jython ":boto3:1.1.3"
}"""

        when:
        def result = GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'jythonDownload')
                .build()

        then:
        result.output.contains('Downloading Jython library')
        result.task(":jythonDownload").outcome == TaskOutcome.SUCCESS

        where:
        gradleVersion << ['2.11', '2.12']
    }

}

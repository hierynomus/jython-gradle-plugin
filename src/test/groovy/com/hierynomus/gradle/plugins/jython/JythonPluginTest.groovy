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

import com.xebialabs.restito.server.StubServer
import org.apache.commons.compress.archivers.jar.JarArchiveEntry
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream
import org.glassfish.grizzly.http.util.HttpStatus
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Shared
import spock.lang.Specification

import static com.xebialabs.restito.semantics.Action.*
import static com.xebialabs.restito.semantics.Condition.*;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp

class JythonPluginTest extends Specification {
    @Shared Project project
    @Shared File projectDir = new File("rootPrj")
    @Shared StubServer server

    def setup() {
        server = new StubServer()
        server.run()
        projectDir.mkdirs()
        project = ProjectBuilder.builder().withProjectDir(projectDir).withName("test").build()
        project.apply plugin: 'jython'
        project.jython.sourceRepositories = [
                'http://localhost:' + server.getPort() + '/${dep.group}/${dep.name}/${dep.name}-${dep.version}.tar.gz',
                'http://localhost:' + server.getPort() + '/${dep.group}/${dep.name}/${dep.name}-${dep.version}.zip']
        whenHttp(server).match(get("/test/pylib/pylib-0.1.0.tar.gz")).then(ok(), resourceContent("pylib-0.1.0.tar.gz"), contentType("application/tar+gz"))
        whenHttp(server).match(get("/test/otherlib/otherlib-0.1.0.zip")).then(ok(), resourceContent("otherlib-0.1.0.zip"), contentType("application/zip"))
    }

    def cleanup() {
        projectDir.deleteDir()
        server.stop()
    }

    def "should download defined jython library dependency"() {
        setup:
        project.dependencies {
            jython "test:pylib:0.1.0"
        }

        when:
        project.tasks.getByName(JythonPlugin.RUNTIME_DEP_DOWNLOAD).execute()

        then:
        new File(project.buildDir, "jython/main/pylib/__init__.py").exists()
        !new File(project.buildDir, "jython/main/requirements.txt").exists()
    }

    def "should download jython library dependency not containing directory structure"() {
        setup:
        project.dependencies {
            jython "test:otherlib:0.1.0"
        }

        when:
        project.tasks.getByName(JythonPlugin.RUNTIME_DEP_DOWNLOAD).execute()

        then:
        new File(project.buildDir, "jython/main/otherlib/__init__.py").exists()
        !new File(project.buildDir, "jython/main/requirements.txt").exists()
    }

    def "should bundle runtime deps in jar if Java plugin is applied"() {
        setup:
        project.apply plugin: 'java'
        project.dependencies {
            jython "test:pylib:0.1.0"
        }

        when:
        project.tasks.getByName(JythonPlugin.RUNTIME_DEP_DOWNLOAD).execute()
        project.tasks.getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME).execute()
        project.tasks.getByName(JavaPlugin.JAR_TASK_NAME).execute()


        def archive = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME).asType(Jar).archivePath
        then:
        archive.exists()
        getEntriesOfJar(archive).contains("pylib/__init__.py")
    }

    def "should handle python libraries which are redirected to another url"() {
        setup:
        project.apply plugin: 'java'
        project.dependencies {
            jython "redirect:pylib:0.1.0"
        }
        whenHttp(server).match(get("/redirect/pylib/pylib-0.1.0.tar.gz")).then(status(HttpStatus.MOVED_PERMANENTLY_301), header("Location", "/test/pylib/pylib-0.1.0.tar.gz"))

        when:
        project.tasks.getByName(JythonPlugin.RUNTIME_DEP_DOWNLOAD).execute()

        then:
        new File(project.buildDir, "jython/main/pylib/__init__.py").exists()
    }

    def "should handle github python libraries for which the repository name is not the python module name"() {
        setup:
        project.apply plugin: 'java'
        project.dependencies {
            jython "test:renamed-lib:0.1.0:reallib"
        }
        whenHttp(server).match(get("/test/renamed-lib/renamed-lib-0.1.0.tar.gz")).then(ok(), resourceContent("renamed-lib-0.1.0.tar.gz"))

        when:
        project.tasks.getByName(JythonPlugin.RUNTIME_DEP_DOWNLOAD).execute()

        then:
        new File(project.buildDir, "jython/main/reallib/__init__.py").exists()
    }

    def "should support getting a single file from the module directory"() {
        setup:
        project.dependencies {
            jython("test:pylib:0.1.0") {
                artifact {
                    name = "pylib/other-artifact"
                    extension = "py"
                }
            }
        }

        when:
        project.tasks.getByName(JythonPlugin.RUNTIME_DEP_DOWNLOAD).execute()

        then:
        !new File(project.buildDir, "jython/main/pylib/__init__.py").exists()
        new File(project.buildDir, "jython/main/pylib/other-artifact.py").exists()
    }

    def "should support getting a single file from the tar.gz"() {
        setup:
        project.dependencies {
            jython("test:pylib:0.1.0") {
                artifact {
                    name = "artifact"
                    extension = "py"
                }
            }
        }

        when:
        project.tasks.getByName(JythonPlugin.RUNTIME_DEP_DOWNLOAD).execute()

        then:
        !new File(project.buildDir, "jython/main/pylib/__init__.py").exists()
        new File(project.buildDir, "jython/main/artifact.py").exists()
    }

    def "should work with new PythonDependency"() {
        setup:
        project.dependencies {
            jython python("test:pylib:0.1.0")
        }

        when:
        project.tasks.getByName(JythonPlugin.RUNTIME_DEP_DOWNLOAD).execute()

        then:
        new File(project.buildDir, "jython/main/pylib/__init__.py").exists()
        !new File(project.buildDir, "jython/main/requirements.txt").exists()
    }

    def "should define paths to copy in PythonDependency for extraction"() {
        setup:
        project.apply plugin: 'java'
        project.dependencies {
            jython python("test:pylib:0.1.0:sublib") {
                copy "src/sublib"
            }
        }

        when:
        project.tasks.getByName(JythonPlugin.RUNTIME_DEP_DOWNLOAD).execute()

        then:
        !new File(project.buildDir, "jython/main/src/sublib/__init__.py").exists()
        new File(project.buildDir, "jython/main/sublib/__init__.py").exists()
    }

    List<String> getEntriesOfJar(File archive) {
        def stream = new JarArchiveInputStream(new FileInputStream(archive))
        List<String> names = new ArrayList<>()
        try {
            JarArchiveEntry entry;
            while ((entry = stream.nextJarEntry) != null) {
                names.add(entry.name)
            }
        } finally {
            stream.close()
        }
        return names
    }
}

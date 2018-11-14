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
package com.hierynomus.gradle.plugins.jython.repository

import com.hierynomus.gradle.plugins.jython.JythonExtension
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

abstract class Repository implements Serializable {
    final Logger logger = Logging.getLogger(this.getClass())

    File resolve(JythonExtension extension, ExternalModuleDependency dep) {
        File cachePath = toCachePath(extension.pyCacheDir, dep)
        File cachedArtifact = listExistingArtifact(cachePath, dep)
        if (!cachedArtifact) {
            String url = getReleaseUrl(extension, dep)
            if (url) {
                logger.info("Downloading :${dep.name}:${dep.version} from $url")

                def http = newHTTPBuilder(extension, url)
                http.request(Method.GET) {
                    response.success = { resp, body ->
                        this.logger.debug "Got response: ${resp.statusLine}"
                        this.logger.debug "Response length: ${resp.getFirstHeader('Content-Length')}"
                        if (uri.path.endsWith(".zip")) {
                            cachedArtifact = new File(cachePath, artifactName(dep) + ".zip")
                        } else if (uri.path.endsWith(".tar.gz")) {
                            cachedArtifact = new File(cachePath, artifactName(dep) + ".tar.gz")
                        } else {
                            throw new IllegalArgumentException("Unknown Python artifact extension: $url for dependency $dep")
                        }
                        if (!cachedArtifact.getParentFile().exists()) {
                            cachedArtifact.getParentFile().mkdirs()
                        }
                        cachedArtifact.withOutputStream { os ->
                            def is = new BufferedInputStream(body as InputStream)
                            os << is
                            is.close()
                        }
                    }
                    response.failure = { resp, body ->
                        logger.debug("Got response: ${resp.statusLine} for url: $url, trying next...")
                    }
                }
            }
        } else {
            logger.info("Using cached artifact $cachedArtifact for depedency :${dep.name}:${dep.version}")
        }
        return cachedArtifact
    }

    HTTPBuilder newHTTPBuilder(JythonExtension extension, String url) {
        def http = new HTTPBuilder(url)
        def p = extension.project
        if (p.hasProperty("systemProp.http.proxyHost")) {
            configureProxy(p, "http", http)
        } else if (p.hasProperty("systemProp.https.proxyHost")) {
            configureProxy(p, "https", http)
        }
        return http
    }

    private def configureProxy(Project project, String scheme, HTTPBuilder http) {
        String proxyHost = project.property("systemProp.${scheme}.proxyHost") as String
        int proxyPort = project.property("systemProp.${scheme}.proxyPort") as Integer
        http.setProxy(proxyHost, proxyPort, scheme)
        if (project.hasProperty("systemProp.${scheme}.proxyUsername")) {
            String username = project.property("systemProp.${scheme}.proxyUsername") as String
            String password = project.property("systemProp.${scheme}.proxyPassword") as String
            http.auth.basic(proxyHost, proxyPort, username, password)
        }
    }

    /**
     * List the cache directory contents to check whether there is a pre-existing artifact
     * This is needed because we do not know beforehand without doing a network call what the
     * type of the artifact will be (zip, tar.gz, ...)
     *
     * @param artifactCachePath The path where the artifact is cached.
     * @param pythonDependency The dependency
     * @return The pre-existing artifact, or null
     */
    File listExistingArtifact(File artifactCachePath, ExternalModuleDependency pythonDependency) {
        String artifactName = artifactName(pythonDependency)
        def files = artifactCachePath.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                return name.startsWith(artifactName)
            }
        })
        if (files) {
            return files[0]
        } else {
            return null
        }
    }

    abstract String getReleaseUrl(JythonExtension extension, ExternalModuleDependency dep)

    String group(ExternalModuleDependency dep) {
        return dep.group
    }

    String artifactName(ExternalModuleDependency dep) {
        return "${dep.name}-${dep.version}"
    }

    private File toCachePath(File pyCacheDir, ExternalModuleDependency dep) {
        new File(pyCacheDir, "${group(dep)}/${dep.name}/${dep.version}")
    }

}

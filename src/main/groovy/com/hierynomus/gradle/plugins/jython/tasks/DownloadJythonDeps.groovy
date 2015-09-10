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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Created by ajvanerp on 08/09/15.
 */
class DownloadJythonDeps extends DefaultTask {

    String configuration

    JythonExtension extension

    @OutputDirectory
    File outputDir

    @TaskAction
    def process() {
        project.configurations.getByName(configuration).allDependencies*.each { d ->
            String name = d.name
            String version = d.version
            logger.debug("Downloading Jython library: $name with version $version")

            HttpGet req = new HttpGet(constructUrl(name, version))
            HttpClient client = new DefaultHttpClient()
            HttpResponse response = client.execute(req)
            if (response.statusLine.statusCode == 200) {
                logger.debug "Got response: ${response.statusLine}"
                logger.debug "Response length: ${response.getFirstHeader('Content-Length')}"
                UnTarJythonLib.uncompressToOutputDir(response.entity.content, outputDir, name)
            }
        }
    }

    String constructUrl(String name, String version) {
        return "${extension.sourceRepository}/${name.charAt(0)}/${name}/${name}-${version}.tar.gz"
    }

 }

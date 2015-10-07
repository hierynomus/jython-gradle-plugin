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

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.http.HttpResponse
import org.gradle.api.logging.Logging

class UnArchiveLib {
    static final def logger = Logging.getLogger(UnArchiveLib.class)

    static def getArchiveInputStream(String repo, HttpResponse response) {
        def buffered = new BufferedInputStream(response.entity.content)
        if (repo.endsWith(".zip")) {
            return new ZipArchiveInputStream(buffered)
        } else if (repo.endsWith(".tar.gz")) {
            return new TarArchiveInputStream(new GzipCompressorInputStream(buffered))
        } else {
            throw new IllegalArgumentException("Could not determine correct archive format for $repo")
        }
    }

    static def uncompressToOutputDir(ArchiveInputStream is, File output, Closure<Boolean> acceptor) {
        ArchiveEntry entry = is.getNextEntry()
        while (entry != null) {
            String entryName = substringAfterFirst(entry.getName(), '/')
            if (acceptor(entryName)) {
                File destPath = new File(output, entryName)
                logger.debug("Writing: ${destPath.getCanonicalPath()}")
                if (entry.isDirectory()) {
                    destPath.mkdirs()
                } else {
                    destPath.createNewFile()
                    byte[] btoRead = new byte[1024]
                    BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(destPath))
                    try {
                        int len
                        while ((len = is.read(btoRead)) != -1) {
                            bout.write(btoRead, 0, len)
                        }
                    } finally {
                        bout.close()
                        btoRead = null
                    }
                }
            } else {
                logger.debug("Skipping: ${entry.getName()}")
            }
            entry = is.getNextEntry()
        }
    }

    static Closure<Boolean> pythonModule(String name) { return { String entryName -> entryName.startsWith("$name/") } }

    static String substringAfterFirst(String orig, String ch) {
        assert ch.length() == 1
        return orig.substring(orig.indexOf(ch) + 1)
    }
}

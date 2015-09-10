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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.gradle.api.logging.Logging

class UnTarJythonLib {
    static final def logger = Logging.getLogger(UnTarJythonLib.class)

    static def uncompressToOutputDir(InputStream is, File output, String packageName) {
        TarArchiveInputStream tarIn = null;

        tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(is)))
        try {
            TarArchiveEntry tarEntry = tarIn.getNextTarEntry()
            while (tarEntry != null) {
                String entryName = substringAfterFirst(tarEntry.getName(), '/')
                if (entryName.startsWith(packageName + '/')) {
                    File destPath = new File(output, entryName)
                    logger.debug("Writing: ${destPath.getCanonicalPath()}")
                    if (tarEntry.isDirectory()) {
                        destPath.mkdirs()
                    } else {
                        destPath.createNewFile()
                        byte[] btoRead = new byte[1024]
                        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(destPath))
                        try {
                            int len = 0
                            while ((len = tarIn.read(btoRead)) != -1) {
                                bout.write(btoRead, 0, len)
                            }
                        } finally {
                            bout.close()
                            btoRead = null
                        }
                    }
                } else {
                    logger.debug("Skipping: ${tarEntry.getName()}")
                }
                tarEntry = tarIn.getNextTarEntry()
            }
        } finally {
            tarIn.close()
        }
    }

    static String substringAfterFirst(String orig, String ch) {
        assert ch.length() == 1
        return orig.substring(orig.indexOf(ch) + 1)
    }
}

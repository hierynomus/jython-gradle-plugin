package com.hierynomus.gradle.plugins.jython.tasks

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.gradle.api.logging.Logging

/**
 * Created by ajvanerp on 10/09/15.
 */
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

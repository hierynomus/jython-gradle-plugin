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
                uncompressToOutputDir(response.entity.content, outputDir, name)
            }
        }
    }

    String constructUrl(String name, String version) {
        return "${extension.sourceRepository}/${name.charAt(0)}/${name}/${name}-${version}.tar.gz"
    }

    def uncompressToOutputDir(InputStream is, File output, String packageName) {
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

    String substringAfterFirst(String orig, String ch) {
        assert ch.length() == 1
        return orig.substring(orig.indexOf(ch) + 1)
    }
}

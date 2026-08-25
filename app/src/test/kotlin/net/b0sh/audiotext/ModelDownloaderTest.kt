package net.b0sh.audiotext

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

class ModelDownloaderTest {

    @Test fun `extracts tar bz2 with nested files`() {
        withTempDir { tmp ->
            val archive = File(tmp, "test.tar.bz2")
            val outDir = File(tmp, "out")

            writeTarBz2(archive, mapOf(
                "mymodel/tokens.txt" to "hello\nworld",
                "mymodel/encoder.onnx" to "fake-onnx-data",
            ))

            ModelDownloader.extractTarBz2(archive, outDir)

            assertTrue(File(outDir, "mymodel").isDirectory)
            assertEquals("hello\nworld", File(outDir, "mymodel/tokens.txt").readText())
            assertEquals("fake-onnx-data", File(outDir, "mymodel/encoder.onnx").readText())
        }
    }

    @Test fun `rejects path traversal`() {
        withTempDir { tmp ->
            val archive = File(tmp, "evil.tar.bz2")
            writeTarBz2(archive, mapOf("../evil.txt" to "gotcha"))

            assertThrows(IllegalArgumentException::class.java) {
                ModelDownloader.extractTarBz2(archive, File(tmp, "out"))
            }
        }
    }

    @Test fun `extract calls onProgress after each file`() {
        withTempDir { tmp ->
            val archive = File(tmp, "test.tar.bz2")
            val outDir = File(tmp, "out")
            writeTarBz2(archive, mapOf(
                "mymodel/tokens.txt" to "hello\nworld",
                "mymodel/encoder.onnx" to "fake-onnx-data",
                "mymodel/decoder.onnx" to "fake-onnx-data-2",
            ))

            val progress = mutableListOf<Pair<Int, String>>()
            ModelDownloader.extractTarBz2(archive, outDir) { filesDone, current ->
                progress += filesDone to current
            }

            val fileNames = progress.map { it.second }
            assertTrue(fileNames.contains("tokens.txt"))
            assertTrue(fileNames.contains("encoder.onnx"))
            assertTrue(fileNames.contains("decoder.onnx"))
            assertEquals(3, progress.last().first)
        }
    }

    @Test fun `extract reports completed files even with dirs`() {
        withTempDir { tmp ->
            val archive = File(tmp, "test.tar.bz2")
            val outDir = File(tmp, "out")
            writeTarBz2(archive, mapOf(
                "mymodel/tokens.txt" to "hello\nworld",
                "mymodel/encoder.onnx" to "fake-onnx-data",
            ))

            var lastDone = -1
            ModelDownloader.extractTarBz2(archive, outDir) { filesDone, _ ->
                lastDone = filesDone
            }
            assertEquals(2, lastDone)
        }
    }

    @Test fun `catalog has expected structure`() {
        assertEquals(4, MODEL_CATALOG.size)
        assertTrue(MODEL_CATALOG.any { it.recommended })
        assertTrue(MODEL_CATALOG.all { it.archive.startsWith("sherpa-onnx-") })
        assertTrue(MODEL_CATALOG.all { it.sizeMb > 0 })
    }

    // -- helpers --

    private fun withTempDir(block: (File) -> Unit) {
        val tmp = Files.createTempDirectory("model-test").toFile()
        try { block(tmp) } finally { tmp.deleteRecursively() }
    }

    private fun writeTarBz2(file: File, entries: Map<String, String>) {
        TarArchiveOutputStream(BZip2CompressorOutputStream(FileOutputStream(file))).use { tar ->
            for ((name, content) in entries) {
                val bytes = content.toByteArray()
                tar.putArchiveEntry(TarArchiveEntry(name).apply { size = bytes.size.toLong() })
                tar.write(bytes)
                tar.closeArchiveEntry()
            }
        }
    }
}

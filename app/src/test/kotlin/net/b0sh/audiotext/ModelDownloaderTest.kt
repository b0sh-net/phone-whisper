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
        assertEquals(5, MODEL_CATALOG.size)
        assertTrue(MODEL_CATALOG.any { it.recommended })
        assertTrue(MODEL_CATALOG.all { it.source.isNotEmpty() && it.id.isNotEmpty() && it.sizeMb > 0 })
        assertEquals(MODEL_CATALOG.size, MODEL_CATALOG.map { it.id }.toSet().size)

        val archiveModels = MODEL_CATALOG.filter { it.archive != null }
        val fileModels = MODEL_CATALOG.filter { it.files.isNotEmpty() }
        assertEquals(4, archiveModels.size)
        assertEquals(1, fileModels.size)

        val kroko = MODEL_CATALOG.find { it.id == "kroko-128l-it" }
        assertNotNull(kroko)
        assertEquals("kroko-128l-it", kroko!!.dirName)
        assertEquals(4, kroko!!.files.size)
        assertTrue(kroko!!.files.all { it.localName.isNotEmpty() && it.remotePath.isNotEmpty() })
    }

    @Test fun `composes archive url from model source`() {
        val parakeet = MODEL_CATALOG.find { it.id.startsWith("sherpa-onnx-nemo-parakeet_tdt_ctc") }!!
        assertEquals(
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/" +
                "sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000-int8.tar.bz2",
            ModelDownloader.downloadUrl(parakeet)
        )

        // No regression: every archive model keeps its legacy GitHub source and URL shape
        MODEL_CATALOG.filter { it.archive != null }.forEach { m ->
            val url = ModelDownloader.downloadUrl(m)
            assertTrue(url.startsWith(SHERPA_MODELS_SOURCE))
            assertTrue(url.endsWith(".tar.bz2"))
        }
    }

    @Test fun `composes file url and local name for uncompressed model`() {
        val kroko = MODEL_CATALOG.find { it.id == "kroko-128l-it" }!!
        val encoder = kroko.files.find { it.localName == "encoder.int8.onnx" }!!

        assertEquals(
            "https://huggingface.co/hudaiapa88/sherpa-stt-onnx/resolve/main/" +
                "it/kroko_128l/encoder.int8.onnx?download=true",
            ModelDownloader.downloadUrl(kroko, encoder.remotePath)
        )
        assertEquals("encoder.int8.onnx", ModelDownloader.localNameFor(encoder.remotePath))
    }

    @Test fun `derives local name from remote path`() {
        assertEquals("tokens.txt", ModelDownloader.localNameFor("it/kroko_128l/tokens.txt?download=true"))
        assertEquals("model.onnx", ModelDownloader.localNameFor("path/to/model.onnx"))
        assertEquals("plain.txt", ModelDownloader.localNameFor("plain.txt"))
    }

    @Test fun `uncompressed model installed only when all files present`() {
        withTempDir { tmp ->
            val required = listOf("encoder.int8.onnx", "decoder.int8.onnx", "joiner.int8.onnx", "tokens.txt")

            File(tmp, "encoder.int8.onnx").writeText("x")
            File(tmp, "decoder.int8.onnx").writeText("x")
            File(tmp, "joiner.int8.onnx").writeText("x")
            File(tmp, "tokens.txt").writeText("a b")

            assertTrue(ModelDownloader.isModelDirInstalled(tmp, required))

            File(tmp, "joiner.int8.onnx").delete()
            assertFalse(ModelDownloader.isModelDirInstalled(tmp, required))
        }
    }

    @Test fun `uncompressed model requires common rule`() {
        withTempDir { tmp ->
            val required = listOf("encoder.int8.onnx", "tokens.txt")

            // Missing tokens file -> not installed even if encoder present
            File(tmp, "encoder.int8.onnx").writeText("x")
            assertFalse(ModelDownloader.isModelDirInstalled(tmp, required))

            // Missing model file -> not installed even if tokens present
            File(tmp, "encoder.int8.onnx").delete()
            File(tmp, "tokens.txt").writeText("a b")
            assertFalse(ModelDownloader.isModelDirInstalled(tmp, required))
        }
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
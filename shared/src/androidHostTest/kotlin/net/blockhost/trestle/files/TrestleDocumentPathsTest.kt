package net.blockhost.trestle.files

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TrestleDocumentPathsTest {
    @Test
    fun documentIdsRoundTripPathsInsideInstanceStorage() {
        val root = Files.createTempDirectory("trestle-documents").toFile()
        try {
            val log = root.resolve("instance/game/logs/latest.log").apply {
                requireNotNull(parentFile).mkdirs()
                writeText("test")
            }

            val documentId = TrestleDocumentPaths.documentIdFor(root, log)

            assertEquals("trestle-root/instance/game/logs/latest.log", documentId)
            assertEquals(log.canonicalFile, TrestleDocumentPaths.fileFor(root, documentId))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun documentIdsRejectPathsOutsideInstanceStorage() {
        val parent = Files.createTempDirectory("trestle-documents").toFile()
        try {
            val root = parent.resolve("instances").apply { mkdirs() }
            val outside = parent.resolve("credentials.json").apply { writeText("secret") }

            assertFailsWith<IllegalArgumentException> {
                TrestleDocumentPaths.documentIdFor(root, outside)
            }
            assertFailsWith<IllegalArgumentException> {
                TrestleDocumentPaths.fileFor(root, "trestle-root/../credentials.json")
            }
        } finally {
            parent.deleteRecursively()
        }
    }
}

package net.blockhost.trestle.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ResourceDescriptionTest {
    @Test
    fun convertsHtmlStructureAndEntitiesToMarkdown() {
        val html = """
            <h2>Features</h2>
            <p>Hello&nbsp;<strong>world</strong>.</p>
            <ul>
                <li>Fast</li>
                <li><a href="https://example.test/docs">Documentation</a></li>
            </ul>
        """.trimIndent()

        assertEquals(
            """
                ## Features

                Hello **world**.

                - Fast

                - [Documentation](https://example.test/docs)
            """.trimIndent(),
            normalizeResourceDescription(html),
        )
    }

    @Test
    fun preservesNativeMarkdownForTheRenderer() {
        val markdown = "**Latest release**\n\n[Read the notes](https://example.test/releases)"

        assertEquals(markdown, normalizeResourceDescription(markdown))
    }
}

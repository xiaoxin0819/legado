package io.legado.app.ui.main.bookshelf

import org.junit.Assert.assertFalse
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BookshelfReadProgressTest {

    @Test
    fun `all book item layouts remain free of reading progress views`() {
        bookItemLayouts.forEach { layout ->
            val document = parseProjectXml("src/main/res/layout/$layout")
            assertFalse(document.findElementsById("@+id/pb_read_progress").isNotEmpty())
            assertFalse(document.findElementsById("@+id/tv_read_percent").isNotEmpty())
        }
    }

    @Test
    fun `group layouts remain free of per-book progress`() {
        groupLayouts.forEach { layout ->
            val document = parseProjectXml("src/main/res/layout/$layout")
            assertFalse(document.findElementsById("@+id/pb_read_progress").isNotEmpty())
        }
    }

    @Test
    fun `bookshelf settings do not expose the progress switch`() {
        val document = parseProjectXml("src/main/res/layout/dialog_bookshelf_config.xml")
        assertFalse(document.findElementsById("@+id/sw_show_read_progress").isNotEmpty())
    }

    private fun parseProjectXml(pathInApp: String): Document {
        val file = listOf(File(pathInApp), File("app/$pathInApp"))
            .firstOrNull { it.isFile }
            ?: error("Missing project file: $pathInApp")
        return DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(file)
    }

    private fun Document.findElementsById(id: String): List<Element> {
        return getElementsByTagName("*").let { nodes ->
            (0 until nodes.length)
                .map { nodes.item(it) as Element }
                .filter { it.androidAttribute("id") == id }
        }
    }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(androidNamespace, name)

    private companion object {
        const val androidNamespace = "http://schemas.android.com/apk/res/android"

        val bookItemLayouts = listOf(
            "item_bookshelf_grid.xml",
            "item_bookshelf_grid2.xml",
            "item_bookshelf_list.xml",
            "item_bookshelf_list2.xml",
        )

        val groupLayouts = listOf(
            "item_bookshelf_grid_group.xml",
            "item_bookshelf_grid_group2.xml",
            "item_bookshelf_list_group.xml",
        )
    }
}

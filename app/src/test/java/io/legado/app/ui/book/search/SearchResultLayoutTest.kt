package io.legado.app.ui.book.search

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class SearchResultLayoutTest {

    @Test
    fun `result row keeps the restored compact cover geometry`() {
        val document = searchLayout()
        val cover = view(document, "iv_cover")

        assertEquals("80dp", cover.androidAttribute("layout_width"))
        assertEquals("110dp", cover.androidAttribute("layout_height"))
        assertEquals("centerCrop", cover.androidAttribute("scaleType"))
        assertEquals("parent", cover.appAttribute("layout_constraintTop_toTopOf"))
        assertEquals("parent", cover.appAttribute("layout_constraintBottom_toBottomOf"))
    }

    @Test
    fun `metadata remains bound to the restored cover height`() {
        val document = searchLayout()
        val metadata = view(document, "tv_author").parentNode as Element
        val introduction = view(document, "tv_introduce")

        assertEquals("0dp", metadata.androidAttribute("layout_height"))
        assertEquals("@id/iv_cover", metadata.appAttribute("layout_constraintBottom_toBottomOf"))
        assertEquals("@+id/tv_name", metadata.appAttribute("layout_constraintTop_toBottomOf"))
        assertEquals("match_parent", introduction.androidAttribute("layout_height"))
    }

    private fun searchLayout() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(
        listOf(
            File("src/main/res/layout/item_search.xml"),
            File("app/src/main/res/layout/item_search.xml"),
        ).firstOrNull { it.isFile } ?: error("item_search.xml not found")
    )

    private fun view(document: org.w3c.dom.Document, id: String): Element {
        val nodes = document.getElementsByTagName("*")
        return (0 until nodes.length)
            .asSequence()
            .map { nodes.item(it) as Element }
            .first { it.androidAttribute("id") == "@+id/$id" }
    }

    private fun Element.androidAttribute(name: String): String =
        getAttributeNS(ANDROID_NS, name)

    private fun Element.appAttribute(name: String): String =
        getAttributeNS(APP_NS, name)

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        private const val APP_NS = "http://schemas.android.com/apk/res-auto"
    }
}

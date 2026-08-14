package io.legado.app.config

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class CustomDefaultDataContractTest {

    @Test
    fun `custom default assets stay intact`() {
        val bookSources = jsonArray("bookSources.json")
        assertEquals(5, bookSources.size())
        assertEquals(
            setOf(
                "https://www.sudugu.org",
                "https://www.dbxsn.com",
                "https://bookshelf.html5.qq.com",
                "http://www.yetianlian.la#\uD83C\uDF83",
                "https://www.aaawz.cc",
            ),
            bookSources.map { it.asJsonObject["bookSourceUrl"].asString }.toSet(),
        )

        val rssSources = jsonArray("rssSources.json")
        assertEquals(5, rssSources.size())
        assertEquals(
            setOf(
                "https://www.yuque.com/legado",
                "https://pan.miaogongzi.net",
                "https://www.lanzout.com/b0bw8jwoh",
                "http://yckceo.vip",
                "https://shuyuan.yiove.com",
            ),
            rssSources.map { it.asJsonObject["sourceUrl"].asString }.toSet(),
        )

        val readConfigs = jsonArray("readConfig.json")
        assertEquals(6, readConfigs.size())
        val defaultConfig = readConfigs.last().asJsonObject
        assertEquals("\u6587\u5B57", defaultConfig["name"].asString)
        assertEquals("defaultReadFont.ttf", defaultConfig["textFont"].asString)
        assertEquals(34, defaultConfig["textSize"].asInt)
        assertEquals("#E8D2AA", defaultConfig["bgStr"].asString)

        val font = defaultDataFile("defaultReadFont.ttf")
        assertEquals(12_700_044L, font.length())
        assertEquals(
            "B73776F591BAE1B64A949FF9B527614919BFFF7C6987367E68E57AD0AF6468CC",
            font.sha256(),
        )
    }

    @Test
    fun `custom defaults remain wired into startup migration`() {
        val defaultData = projectFile(
            "app/src/main/java/io/legado/app/help/DefaultData.kt",
        ).readText()
        assertTrue(defaultData.contains("LocalConfig.needResetReadStyleDefaults"))
        assertTrue(defaultData.contains("ReadBookConfig.resetAll()"))
        assertTrue(defaultData.contains("appDb.bookSourceDao.allCount() == 0"))
        assertTrue(defaultData.contains("LocalConfig.needImportDefaultBookSources"))
        assertTrue(defaultData.contains("importDefaultBookSources()"))
        assertTrue(!defaultData.contains("LocalConfig.versionCode <"))

        val localConfig = projectFile(
            "app/src/main/java/io/legado/app/help/config/LocalConfig.kt",
        ).readText()
        assertTrue(localConfig.contains("isLastVersion(7, \"rssSourceVersion\")"))
        assertTrue(localConfig.contains("isLastVersion(2, \"readStyleDefaultVersion\")"))
        assertTrue(localConfig.contains("isLastVersion(1, \"defaultBookSourceVersion\")"))
    }

    private fun jsonArray(name: String) =
        JsonParser.parseString(defaultDataFile(name).readText()).asJsonArray

    private fun defaultDataFile(name: String) =
        projectFile("app/src/main/assets/defaultData/$name")

    private fun projectFile(relativePath: String) = File(repositoryRoot(), relativePath)

    private fun repositoryRoot(): File {
        val userDirectory = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        return generateSequence(userDirectory) { it.parentFile }
            .first { File(it, "app/src/main").isDirectory }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02X".format(it) }
    }
}

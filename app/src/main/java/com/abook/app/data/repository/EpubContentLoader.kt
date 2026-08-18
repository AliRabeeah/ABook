package com.abook.app.data.repository

import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File

data class EpubChapter(val index: Int, val htmlContent: String)

/**
 * يفكّ ضغط EPUB ويستخرج قائمة الفصول (Spine) كنصوص HTML خام،
 * جاهزة للعرض داخل WebView مع حقن CSS مخصص لاحقًا.
 */
class EpubContentLoader {

    fun loadChapters(filePath: String): List<EpubChapter> {
        val chapters = mutableListOf<EpubChapter>()
        val file = File(filePath)

        try {
            ZipFile(file).use { zip ->
                val containerEntry = zip.getEntry("META-INF/container.xml") ?: return emptyList()
                val containerXml = zip.getInputStream(containerEntry).bufferedReader().readText()
                val opfPath = Regex("full-path=\"([^\"]+)\"").find(containerXml)
                    ?.groupValues?.get(1) ?: return emptyList()

                val opfEntry = zip.getEntry(opfPath) ?: return emptyList()
                val opfXml = zip.getInputStream(opfEntry).bufferedReader().readText()
                val opfDir = opfPath.substringBeforeLast("/", "")

                // بناء خريطة id -> href من manifest
                val manifestItems = Regex("<item\\s+[^>]*/>").findAll(opfXml).mapNotNull { match ->
                    val itemTag = match.value
                    val id = Regex("id=\"([^\"]+)\"").find(itemTag)?.groupValues?.get(1)
                    val href = Regex("href=\"([^\"]+)\"").find(itemTag)?.groupValues?.get(1)
                    if (id != null && href != null) id to href else null
                }.toMap()

                // ترتيب القراءة من spine
                val spineIds = Regex("<itemref[^>]*idref=\"([^\"]+)\"[^>]*>").findAll(opfXml)
                    .map { it.groupValues[1] }.toList()

                spineIds.forEachIndexed { index, id ->
                    val href = manifestItems[id] ?: return@forEachIndexed
                    val fullPath = if (opfDir.isEmpty()) href else "$opfDir/$href"
                    val entry = zip.getEntry(fullPath) ?: return@forEachIndexed
                    val html = zip.getInputStream(entry).bufferedReader().readText()
                    chapters.add(EpubChapter(index, html))
                }
            }
        } catch (e: Exception) {
            // ملف تالف أو تركيبة غير متوقعة
        }

        return chapters
    }
}

@file:Suppress("unused")

package org.draken.tsukimix.core.parser.tachiyomi.model

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import org.draken.tsukimix.core.parser.external.model.MangaResult
import tsuki.model.Manga
import tsuki.model.MangaChapter
import tsuki.model.MangaPage
import org.draken.tsukimix.core.parser.external.model.Manga as ExternalManga
import org.draken.tsukimix.core.parser.external.model.toManga as toExternalManga
import org.draken.tsukimix.core.parser.external.model.toMangaChapter as toExternalMangaChapter
import org.draken.tsukimix.core.parser.external.model.toMangaPage as toExternalMangaPage
import org.draken.tsukimix.core.parser.external.model.toSChapter as toExternalSChapter
import org.draken.tsukimix.core.parser.external.model.toSManga as toExternalSManga

typealias TachiyomiMangaSource = ExternalManga
typealias TachiyomiLoadResult = MangaResult

fun SManga.toManga(
	source: TachiyomiMangaSource,
	fallbackUrl: String? = null,
	fallbackTitle: String? = null,
): Manga = toExternalManga(source, fallbackUrl, fallbackTitle)

fun Manga.toSManga(): SManga = toExternalSManga()

fun SChapter.toMangaChapter(
	source: TachiyomiMangaSource,
	mangaTitle: String,
	fallbackIndex: Int = 0,
): MangaChapter = toExternalMangaChapter(source, mangaTitle, fallbackIndex)

fun MangaChapter.toSChapter(): SChapter = toExternalSChapter()

fun Page.toMangaPage(
	source: TachiyomiMangaSource,
	resolvedUrl: String,
): MangaPage = toExternalMangaPage(source, resolvedUrl)

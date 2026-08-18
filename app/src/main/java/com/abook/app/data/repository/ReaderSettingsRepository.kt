package com.abook.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.readerSettingsDataStore by preferencesDataStore(name = "reader_settings")

enum class PageTurnEffect { SLIDE, FADE, CURL }
enum class AppThemeMode { SYSTEM, LIGHT, DARK }
enum class TextAlignMode { START, JUSTIFY }

data class ReadingSettings(
    val fontSizeSp: Float = 16f,
    val lineHeightMultiplier: Float = 1.5f,
    val letterSpacing: Float = 0f,
    val marginDp: Float = 20f,
    val brightness: Float = -1f,
    val forceDarkContent: Boolean = false,
    val nightFilterStrength: Float = 0f,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val pageTurnEffect: PageTurnEffect = PageTurnEffect.SLIDE,
    val textAlign: TextAlignMode = TextAlignMode.JUSTIFY,
    val keepScreenOn: Boolean = true,
    val useTwoPageLandscape: Boolean = false,
    val useTapZoneNavigation: Boolean = true,
    val useContinuousScroll: Boolean = false,
    val highContrastMode: Boolean = false
)

/** تجاوز اختياري لبعض إعدادات القراءة خاص بكتاب واحد فقط. القيم null تعني "استخدم الإعداد العام". */
data class BookSettingsOverride(
    val hasOverride: Boolean = false,
    val fontSizeSp: Float? = null,
    val lineHeightMultiplier: Float? = null,
    val forceDarkContent: Boolean? = null,
    val textAlign: TextAlignMode? = null
)

private object BookKeys {
    fun fontSize(bookId: Long) = floatPreferencesKey("book_${bookId}_font_size")
    fun lineHeight(bookId: Long) = floatPreferencesKey("book_${bookId}_line_height")
    fun forceDark(bookId: Long) = booleanPreferencesKey("book_${bookId}_force_dark")
    fun textAlign(bookId: Long) = stringPreferencesKey("book_${bookId}_text_align")
    fun hasOverride(bookId: Long) = booleanPreferencesKey("book_${bookId}_has_override")
}

private object Keys {
    val FONT_SIZE = floatPreferencesKey("font_size")
    val LINE_HEIGHT = floatPreferencesKey("line_height")
    val LETTER_SPACING = floatPreferencesKey("letter_spacing")
    val MARGIN = floatPreferencesKey("margin")
    val BRIGHTNESS = floatPreferencesKey("brightness")
    val FORCE_DARK = booleanPreferencesKey("force_dark_content")
    val NIGHT_FILTER = floatPreferencesKey("night_filter")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val PAGE_TURN = stringPreferencesKey("page_turn_effect")
    val TEXT_ALIGN = stringPreferencesKey("text_align")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    val TWO_PAGE = booleanPreferencesKey("two_page_landscape")
    val TAP_ZONE_NAV = booleanPreferencesKey("tap_zone_navigation")
    val CONTINUOUS_SCROLL = booleanPreferencesKey("continuous_scroll")
    val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
}

private fun readSettings(prefs: Preferences): ReadingSettings = ReadingSettings(
    fontSizeSp = prefs[Keys.FONT_SIZE] ?: 16f,
    lineHeightMultiplier = prefs[Keys.LINE_HEIGHT] ?: 1.5f,
    letterSpacing = prefs[Keys.LETTER_SPACING] ?: 0f,
    marginDp = prefs[Keys.MARGIN] ?: 20f,
    brightness = prefs[Keys.BRIGHTNESS] ?: -1f,
    forceDarkContent = prefs[Keys.FORCE_DARK] ?: false,
    nightFilterStrength = prefs[Keys.NIGHT_FILTER] ?: 0f,
    appThemeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.SYSTEM,
    pageTurnEffect = prefs[Keys.PAGE_TURN]?.let { runCatching { PageTurnEffect.valueOf(it) }.getOrNull() } ?: PageTurnEffect.SLIDE,
    textAlign = prefs[Keys.TEXT_ALIGN]?.let { runCatching { TextAlignMode.valueOf(it) }.getOrNull() } ?: TextAlignMode.JUSTIFY,
    keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
    useTwoPageLandscape = prefs[Keys.TWO_PAGE] ?: false,
    useTapZoneNavigation = prefs[Keys.TAP_ZONE_NAV] ?: true,
    useContinuousScroll = prefs[Keys.CONTINUOUS_SCROLL] ?: false,
    highContrastMode = prefs[Keys.HIGH_CONTRAST] ?: false
)

class ReaderSettingsRepository(private val context: Context) {

    val settingsFlow: Flow<ReadingSettings> = context.readerSettingsDataStore.data.map { readSettings(it) }

    suspend fun update(transform: (ReadingSettings) -> ReadingSettings) {
        context.readerSettingsDataStore.edit { prefs ->
            val current = readSettings(prefs)
            val updated = transform(current)

            prefs[Keys.FONT_SIZE] = updated.fontSizeSp
            prefs[Keys.LINE_HEIGHT] = updated.lineHeightMultiplier
            prefs[Keys.LETTER_SPACING] = updated.letterSpacing
            prefs[Keys.MARGIN] = updated.marginDp
            prefs[Keys.BRIGHTNESS] = updated.brightness
            prefs[Keys.FORCE_DARK] = updated.forceDarkContent
            prefs[Keys.NIGHT_FILTER] = updated.nightFilterStrength
            prefs[Keys.THEME_MODE] = updated.appThemeMode.name
            prefs[Keys.PAGE_TURN] = updated.pageTurnEffect.name
            prefs[Keys.TEXT_ALIGN] = updated.textAlign.name
            prefs[Keys.KEEP_SCREEN_ON] = updated.keepScreenOn
            prefs[Keys.TWO_PAGE] = updated.useTwoPageLandscape
            prefs[Keys.TAP_ZONE_NAV] = updated.useTapZoneNavigation
            prefs[Keys.CONTINUOUS_SCROLL] = updated.useContinuousScroll
            prefs[Keys.HIGH_CONTRAST] = updated.highContrastMode
        }
    }

    /** يدمج الإعداد العام مع تجاوز الكتاب (لو مفعّل) لإنتاج الإعدادات الفعلية المُطبَّقة أثناء القراءة. */
    fun effectiveSettingsFlow(bookId: Long): Flow<ReadingSettings> =
        context.readerSettingsDataStore.data.map { prefs ->
            val global = readSettings(prefs)
            val hasOverride = prefs[BookKeys.hasOverride(bookId)] ?: false
            if (!hasOverride) return@map global

            global.copy(
                fontSizeSp = prefs[BookKeys.fontSize(bookId)] ?: global.fontSizeSp,
                lineHeightMultiplier = prefs[BookKeys.lineHeight(bookId)] ?: global.lineHeightMultiplier,
                forceDarkContent = prefs[BookKeys.forceDark(bookId)] ?: global.forceDarkContent,
                textAlign = prefs[BookKeys.textAlign(bookId)]?.let {
                    runCatching { TextAlignMode.valueOf(it) }.getOrNull()
                } ?: global.textAlign
            )
        }

    fun bookOverrideFlow(bookId: Long): Flow<BookSettingsOverride> =
        context.readerSettingsDataStore.data.map { prefs ->
            BookSettingsOverride(
                hasOverride = prefs[BookKeys.hasOverride(bookId)] ?: false,
                fontSizeSp = prefs[BookKeys.fontSize(bookId)],
                lineHeightMultiplier = prefs[BookKeys.lineHeight(bookId)],
                forceDarkContent = prefs[BookKeys.forceDark(bookId)],
                textAlign = prefs[BookKeys.textAlign(bookId)]?.let { runCatching { TextAlignMode.valueOf(it) }.getOrNull() }
            )
        }

    suspend fun setBookOverride(bookId: Long, enabled: Boolean, settings: ReadingSettings) {
        context.readerSettingsDataStore.edit { prefs ->
            prefs[BookKeys.hasOverride(bookId)] = enabled
            if (enabled) {
                prefs[BookKeys.fontSize(bookId)] = settings.fontSizeSp
                prefs[BookKeys.lineHeight(bookId)] = settings.lineHeightMultiplier
                prefs[BookKeys.forceDark(bookId)] = settings.forceDarkContent
                prefs[BookKeys.textAlign(bookId)] = settings.textAlign.name
            }
        }
    }
}

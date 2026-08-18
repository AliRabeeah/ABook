package com.abook.app.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abook.app.data.repository.PageTurnEffect
import com.abook.app.data.repository.ReadingSettings
import com.abook.app.data.repository.TextAlignMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReadingSettings,
    onSettingsChange: ((ReadingSettings) -> ReadingSettings) -> Unit,
    onDismiss: () -> Unit,
    bookOverrideEnabled: Boolean = false,
    onToggleBookOverride: ((Boolean) -> Unit)? = null
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text("إعدادات القراءة", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            SettingLabel("حجم الخط: ${settings.fontSizeSp.toInt()}sp")
            Slider(
                value = settings.fontSizeSp,
                onValueChange = { onSettingsChange { s -> s.copy(fontSizeSp = it) } },
                valueRange = 12f..28f
            )

            SettingLabel("تباعد الأسطر: ${"%.1f".format(settings.lineHeightMultiplier)}")
            Slider(
                value = settings.lineHeightMultiplier,
                onValueChange = { onSettingsChange { s -> s.copy(lineHeightMultiplier = it) } },
                valueRange = 1.0f..2.5f
            )

            SettingLabel("تباعد الحروف: ${"%.1f".format(settings.letterSpacing)}")
            Slider(
                value = settings.letterSpacing,
                onValueChange = { onSettingsChange { s -> s.copy(letterSpacing = it) } },
                valueRange = 0f..3f
            )

            SettingLabel("هوامش الصفحة: ${settings.marginDp.toInt()}dp")
            Slider(
                value = settings.marginDp,
                onValueChange = { onSettingsChange { s -> s.copy(marginDp = it) } },
                valueRange = 8f..48f
            )

            SettingLabel("فلتر تقليل الضوء الأزرق (قراءة ليلية)")
            Slider(
                value = settings.nightFilterStrength,
                onValueChange = { onSettingsChange { s -> s.copy(nightFilterStrength = it) } },
                valueRange = 0f..1f
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("خلفية داكنة إجبارية", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.forceDarkContent,
                    onCheckedChange = { onSettingsChange { s -> s.copy(forceDarkContent = it) } }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("محاذاة النص للجانبين (Justify)", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.textAlign == TextAlignMode.JUSTIFY,
                    onCheckedChange = {
                        onSettingsChange { s ->
                            s.copy(textAlign = if (it) TextAlignMode.JUSTIFY else TextAlignMode.START)
                        }
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("إبقاء الشاشة مضاءة أثناء القراءة", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.keepScreenOn,
                    onCheckedChange = { onSettingsChange { s -> s.copy(keepScreenOn = it) } }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("التنقل بالضغط على جانبي الشاشة", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.useTapZoneNavigation,
                    onCheckedChange = { onSettingsChange { s -> s.copy(useTapZoneNavigation = it) } }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("التمرير المستمر (بدل تقليب الصفحات)", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.useContinuousScroll,
                    onCheckedChange = { onSettingsChange { s -> s.copy(useContinuousScroll = it) } }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("تباين عالٍ (لضعاف البصر)", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.highContrastMode,
                    onCheckedChange = { onSettingsChange { s -> s.copy(highContrastMode = it) } }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("تأثير تقليب الصفحات", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PageTurnEffect.values().forEach { effect ->
                    FilterChip(
                        selected = settings.pageTurnEffect == effect,
                        onClick = { onSettingsChange { s -> s.copy(pageTurnEffect = effect) } },
                        label = {
                            Text(
                                when (effect) {
                                    PageTurnEffect.SLIDE -> "انزلاق"
                                    PageTurnEffect.FADE -> "تلاشي"
                                    PageTurnEffect.CURL -> "قلب واقعي"
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (onToggleBookOverride != null) {
                HorizontalDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تخصيص هذه الإعدادات لهذا الكتاب فقط")
                        Text(
                            "بدون تفعيل هذا الخيار، أي تعديل أعلاه يُطبَّق على كل الكتب",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = bookOverrideEnabled, onCheckedChange = onToggleBookOverride)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

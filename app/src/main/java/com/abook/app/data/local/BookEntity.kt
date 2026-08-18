package com.abook.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BookFormat { EPUB, PDF, UNKNOWN }

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String?,
    val filePath: String,           // مسار الملف المحلي (content:// أو مسار داخلي مخزّن نسخة منه)
    val format: BookFormat,
    val coverPath: String?,         // مسار غلاف محفوظ محليًا (مستخرج من الملف أو مُحمّل من رابط)
    val customCoverUrl: String? = null, // الرابط الخارجي الأصلي لو المستخدم غيّر الغلاف
    val totalUnits: Int = 0,        // عدد الفصول (EPUB) أو الصفحات (PDF)
    val currentUnit: Int = 0,       // آخر فصل/صفحة وصل لها المستخدم
    val currentUnitProgress: Float = 0f, // نسبة التقدم داخل الفصل/الصفحة الحالية (0..1)
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastOpened: Long? = null
) {
    val progressFraction: Float
        get() = if (totalUnits <= 0) 0f else
            ((currentUnit + currentUnitProgress) / totalUnits).coerceIn(0f, 1f)
}

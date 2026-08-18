package com.abook.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AnnotationType { BOOKMARK, HIGHLIGHT }

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val type: AnnotationType,
    val unitIndex: Int,          // رقم الفصل/الصفحة
    val excerpt: String,         // نص مختصر للمعاينة بالقائمة الجانبية
    val note: String? = null,    // ملاحظة اختيارية على التظليل
    val colorHex: String = "#FF7A29",
    val dateCreated: Long = System.currentTimeMillis()
)

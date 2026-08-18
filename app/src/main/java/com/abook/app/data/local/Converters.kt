package com.abook.app.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromBookFormat(value: BookFormat): String = value.name

    @TypeConverter
    fun toBookFormat(value: String): BookFormat = BookFormat.valueOf(value)

    @TypeConverter
    fun fromAnnotationType(value: AnnotationType): String = value.name

    @TypeConverter
    fun toAnnotationType(value: String): AnnotationType = AnnotationType.valueOf(value)
}

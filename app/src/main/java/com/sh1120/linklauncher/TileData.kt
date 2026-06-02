package com.sh1120.linklauncher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class TileData(
    val id: String,
    val label: String,
    val url: String? = null,
    val packageName: String? = null,
    val colorArgb: Int,
    val tapCount: Int = 0 // タップ回数を保持するフィールドを追加
) {
    val color: Color get() = Color(colorArgb)
    
    constructor(id: String, label: String, url: String? = null, packageName: String? = null, color: Color, tapCount: Int = 0) : 
        this(id, label, url, packageName, color.toArgb(), tapCount)
}

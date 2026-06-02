package com.sh1120.linklauncher

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class TileViewModel : ViewModel() {
    val tiles = mutableStateListOf<TileData>()

    fun loadTiles(context: Context) {
        try {
            val prefs = context.getSharedPreferences("tiles_prefs", Context.MODE_PRIVATE)
            val jsonString = prefs.getString("tiles_list", null)
            if (jsonString != null) {
                val array = JSONArray(jsonString)
                val newTiles = mutableListOf<TileData>()
                val usedIds = mutableSetOf<String>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    var id = obj.optString("id", UUID.randomUUID().toString())
                    if (usedIds.contains(id)) {
                        id = UUID.randomUUID().toString()
                    }
                    usedIds.add(id)

                    newTiles.add(TileData(
                        id = id,
                        label = obj.optString("label", "No Name"),
                        url = obj.optString("url", null),
                        packageName = obj.optString("packageName", null),
                        colorArgb = obj.optInt("color", 0xFF757575.toInt()),
                        tapCount = obj.optInt("tapCount", 0) // tapCountの読み込みを追加
                    ))
                }
                tiles.clear()
                tiles.addAll(newTiles)
            } else {
                setupDefaultTiles()
            }
        } catch (e: Exception) {
            setupDefaultTiles()
        }
    }

    private fun setupDefaultTiles() {
        tiles.clear()
        tiles.addAll(listOf(
            TileData("1", "Google", url = "https://www.google.com", colorArgb = 0xFF4285F4.toInt()),
            TileData("2", "YouTube", url = "https://www.youtube.com", colorArgb = 0xFFFF0000.toInt()),
            TileData("3", "X", url = "https://twitter.com", colorArgb = 0xFF1DA1F2.toInt()),
            TileData("4", "Playストア", packageName = "com.android.vending", colorArgb = 0xFF34A853.toInt()),
            TileData("5", "設定", packageName = "com.android.settings", colorArgb = 0xFF757575.toInt())
        ))
    }

    fun saveTiles(context: Context) {
        try {
            val array = JSONArray()
            tiles.forEach { tile ->
                val obj = JSONObject().apply {
                    put("id", tile.id)
                    put("label", tile.label)
                    tile.url?.let { put("url", it) }
                    tile.packageName?.let { put("packageName", it) }
                    put("color", tile.colorArgb)
                    put("tapCount", tile.tapCount) // tapCountの保存を追加
                }
                array.put(obj)
            }
            context.getSharedPreferences("tiles_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("tiles_list", array.toString())
                .apply()
        } catch (e: Exception) {}
    }

    fun incrementTapCount(context: Context, tileId: String) {
        val index = tiles.indexOfFirst { it.id == tileId }
        if (index != -1) {
            val tile = tiles[index]
            tiles[index] = tile.copy(tapCount = tile.tapCount + 1)
            saveTiles(context)
        }
    }

    fun exportJson(): String {
        val array = JSONArray()
        tiles.forEach { tile ->
            val obj = JSONObject().apply {
                put("id", tile.id)
                put("label", tile.label)
                tile.url?.let { put("url", it) }
                tile.packageName?.let { put("packageName", it) }
                put("color", tile.colorArgb)
                put("tapCount", tile.tapCount) // エクスポートにも含める
            }
            array.put(obj)
        }
        return array.toString(4)
    }

    fun importJson(json: String): Boolean {
        return try {
            val array = JSONArray(json)
            val newTiles = mutableListOf<TileData>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                newTiles.add(TileData(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    label = obj.optString("label", "No Name"),
                    url = obj.optString("url", null),
                    packageName = obj.optString("packageName", null),
                    colorArgb = obj.optInt("color", 0xFF757575.toInt()),
                    tapCount = obj.optInt("tapCount", 0) // インポート時にも読み込む
                ))
            }
            tiles.clear()
            tiles.addAll(newTiles)
            true
        } catch (e: Exception) {
            false
        }
    }
}

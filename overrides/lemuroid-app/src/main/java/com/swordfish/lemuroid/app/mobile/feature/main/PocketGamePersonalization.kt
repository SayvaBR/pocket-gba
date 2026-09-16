package com.swordfish.lemuroid.app.mobile.feature.main

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.security.MessageDigest

/**
 * User-owned presentation metadata, independent of the ROM, scanned game database and saves.
 * fileUri is the stable key across library rescans; a hidden game is never deleted from disk.
 */
internal class PocketGamePersonalization private constructor(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("pocket_game_customizations_v1", Context.MODE_PRIVATE)
    private val revisions = mutableStateMapOf<String, Int>()

    private fun gameKey(game: Game): String = MessageDigest.getInstance("SHA-256")
        .digest(game.fileUri.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun read(game: Game, field: String): String? {
        val key = gameKey(game)
        // Observe changes made by the editor without altering the original database record.
        revisions[key]
        return preferences.getString("$field.$key", null)
    }

    private fun write(game: Game, field: String, value: String?) {
        val key = gameKey(game)
        val editor = preferences.edit()
        if (value == null) editor.remove("$field.$key") else editor.putString("$field.$key", value)
        editor.apply()
        revisions[key] = (revisions[key] ?: 0) + 1
    }

    fun title(game: Game): String = read(game, "title")?.takeIf { it.isNotBlank() } ?: game.title

    fun rename(game: Game, title: String) {
        write(game, "title", title.trim().take(120).takeIf { it.isNotBlank() && it != game.title })
    }

    fun customCover(game: Game): String? = read(game, "cover")

    fun setCustomCover(game: Game, uri: String?) = write(game, "cover", uri)

    fun hidden(game: Game): Boolean = read(game, "hidden") == "1"

    fun setHidden(game: Game, hidden: Boolean) = write(game, "hidden", if (hidden) "1" else null)

    companion object {
        @Volatile private var instance: PocketGamePersonalization? = null

        fun get(context: Context): PocketGamePersonalization = instance ?: synchronized(this) {
            instance ?: PocketGamePersonalization(context).also { instance = it }
        }
    }
}

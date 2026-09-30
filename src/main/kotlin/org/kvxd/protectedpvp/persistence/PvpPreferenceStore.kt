package org.kvxd.protectedpvp.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

class PvpPreferenceStore private constructor(
    private val path: Path,
    private val data: StoredData,
) {
    fun isPvpEnabled(playerId: UUID): Boolean = state(playerId).pvpEnabled

    fun protectionCooldownEndsAt(playerId: UUID): Long = state(playerId).protectionCooldownEndsAt

    fun setPvpEnabled(playerId: UUID, enabled: Boolean) {
        state(playerId).pvpEnabled = enabled
    }

    fun setProtectionCooldownEndsAt(playerId: UUID, timestamp: Long) {
        state(playerId).protectionCooldownEndsAt = timestamp
    }

    @Synchronized
    fun save() {
        Files.createDirectories(path.parent)
        val temporaryPath = path.resolveSibling("${path.fileName}.tmp")

        Files.newBufferedWriter(temporaryPath).use { writer -> GSON.toJson(data, writer) }

        try {
            Files.move(
                temporaryPath,
                path,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun state(playerId: UUID): PlayerState =
        data.players.getOrPut(playerId.toString()) { PlayerState() }

    private class StoredData(
        val players: MutableMap<String, PlayerState> = mutableMapOf(),
    )

    private class PlayerState(
        var pvpEnabled: Boolean = false,
        var protectionCooldownEndsAt: Long = 0,
    )

    companion object {
        private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()

        fun load(path: Path): PvpPreferenceStore {
            if (!Files.exists(path)) {
                return PvpPreferenceStore(path, StoredData())
            }

            Files.newBufferedReader(path).use { reader ->
                val data = GSON.fromJson(reader, StoredData::class.java) ?: StoredData()
                return PvpPreferenceStore(path, data)
            }
        }
    }
}

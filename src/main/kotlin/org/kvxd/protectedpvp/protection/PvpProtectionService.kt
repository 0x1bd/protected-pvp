package org.kvxd.protectedpvp.protection

import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.LevelResource
import org.kvxd.protectedpvp.persistence.PvpPreferenceStore
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.UUID
import kotlin.math.ceil

class PvpProtectionService {
    private val explosiveInteractionTracker = ExplosiveInteractionTracker()
    private val damageAttribution = DamageAttribution(explosiveInteractionTracker)
    private val combatTags = mutableMapOf<UUID, Long>()
    private var preferenceStore: PvpPreferenceStore? = null

    fun start(server: MinecraftServer) {
        val path = server.getWorldPath(LevelResource.ROOT).resolve(DATA_PATH)
        preferenceStore = try {
            PvpPreferenceStore.load(path)
        } catch (exception: Exception) {
            LOGGER.error("Could not load PvP preferences from {}", path, exception)
            PvpPreferenceStore.load(path.resolveSibling("protected-pvp-recovered.json"))
        }
    }

    fun stop(server: MinecraftServer) {
        save()
        preferenceStore = null
        combatTags.clear()
        explosiveInteractionTracker.clear()
    }

    fun save() {
        try {
            preferenceStore?.save()
        } catch (exception: Exception) {
            LOGGER.error("Could not save PvP preferences", exception)
        }
    }

    fun enablePvp(player: ServerPlayer): Int {
        val store = store()
        if (store.isPvpEnabled(player.uuid)) {
            player.sendSystemMessage(Component.literal("PvP is already enabled."))
            return 0
        }

        setPvpEnabled(player, true)
        player.sendSystemMessage(
            Component.literal("PvP enabled. Protection cannot be enabled again for 5 minutes."),
        )
        return 1
    }

    fun enableProtection(player: ServerPlayer): Int {
        val store = store()
        if (!store.isPvpEnabled(player.uuid)) {
            player.sendSystemMessage(Component.literal("PvP protection is already enabled."))
            return 0
        }

        val now = System.currentTimeMillis()
        remaining(combatTags[player.uuid], now)?.let { remaining ->
            player.sendSystemMessage(
                Component.literal("You are combat-tagged for ${formatDuration(remaining)}."),
            )
            return 0
        }

        remaining(store.protectionCooldownEndsAt(player.uuid), now)?.let { remaining ->
            player.sendSystemMessage(
                Component.literal("PvP protection is on cooldown for ${formatDuration(remaining)}."),
            )
            return 0
        }

        store.setPvpEnabled(player.uuid, false)
        save()
        player.sendSystemMessage(Component.literal("PvP protection enabled."))
        return 1
    }

    fun allowDamage(entity: LivingEntity, source: DamageSource, amount: Float): Boolean {
        if (entity !is ServerPlayer || amount <= 0) {
            return true
        }

        val attacker = damageAttribution.findAttackingPlayer(entity, source) ?: return true
        if (attacker.uuid == entity.uuid) {
            return true
        }

        val store = store()
        if (!store.isPvpEnabled(entity.uuid)) {
            attacker.displayClientMessage(Component.literal("That player has PvP protection."), true)
            return false
        }

        if (!store.isPvpEnabled(attacker.uuid)) {
            setPvpEnabled(attacker, true)
            attacker.sendSystemMessage(
                Component.literal("PvP enabled because you attacked an unprotected player."),
            )
        }

        val combatTagEndsAt = System.currentTimeMillis() + COMBAT_TAG_MILLIS
        combatTags[attacker.uuid] = combatTagEndsAt
        combatTags[entity.uuid] = combatTagEndsAt
        return true
    }

    fun recordExplosiveBlockInteraction(player: Player, level: Level, blockPos: net.minecraft.core.BlockPos) {
        explosiveInteractionTracker.record(player, level, blockPos)
    }

    private fun setPvpEnabled(player: ServerPlayer, enabled: Boolean) {
        val store = store()
        store.setPvpEnabled(player.uuid, enabled)
        if (enabled) {
            store.setProtectionCooldownEndsAt(
                player.uuid,
                System.currentTimeMillis() + PROTECTION_COOLDOWN_MILLIS,
            )
        }
        save()
    }

    private fun store(): PvpPreferenceStore =
        checkNotNull(preferenceStore) { "PvP preference store has not been loaded" }

    private fun remaining(endTimestamp: Long?, now: Long): Long? {
        val remaining = (endTimestamp ?: return null) - now
        return remaining.takeIf { it > 0 }
    }

    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = ceil(milliseconds / 1000.0).toLong()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes == 0L) "${seconds}s" else "${minutes}m ${seconds}s"
    }

    private companion object {
        private val LOGGER = LoggerFactory.getLogger(PvpProtectionService::class.java)
        private val DATA_PATH: Path = Path.of("data", "protected-pvp.json")
        const val COMBAT_TAG_MILLIS = 60_000L
        const val PROTECTION_COOLDOWN_MILLIS = 5 * 60_000L
    }
}

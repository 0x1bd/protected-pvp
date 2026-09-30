package org.kvxd.protectedpvp.protection

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BlockTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import java.util.UUID

class ExplosiveInteractionTracker {
    private var latestInteraction: Interaction? = null

    fun record(player: Player, level: Level, blockPos: BlockPos) {
        if (level.isClientSide || player !is ServerPlayer || player.isSpectator) {
            return
        }

        val blockState = level.getBlockState(blockPos)
        if (!blockState.`is`(BlockTags.BEDS) && !blockState.`is`(Blocks.RESPAWN_ANCHOR)) {
            return
        }

        latestInteraction = Interaction(
            player.uuid,
            level.dimension(),
            blockPos.immutable(),
            level.server!!.tickCount,
        )
    }

    fun findPlayer(level: ServerLevel, source: DamageSource): ServerPlayer? {
        if (!source.`is`(DamageTypes.BAD_RESPAWN_POINT)) {
            return null
        }

        val interaction = latestInteraction ?: return null
        val sourcePosition = source.sourcePosition ?: return null
        val currentTick = level.server.tickCount
        if (
            interaction.dimension != level.dimension() ||
            currentTick - interaction.serverTick !in 0..1 ||
            interaction.blockPos.center.distanceToSqr(sourcePosition) > MAX_DISTANCE_SQUARED
        ) {
            return null
        }

        return level.server.playerList.getPlayer(interaction.playerId)
    }

    fun clear() {
        latestInteraction = null
    }

    private data class Interaction(
        val playerId: UUID,
        val dimension: ResourceKey<Level>,
        val blockPos: BlockPos,
        val serverTick: Int,
    )

    private companion object {
        const val MAX_DISTANCE_SQUARED = 4.0
    }
}

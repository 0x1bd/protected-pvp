package org.kvxd.protectedpvp.protection

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.TraceableEntity
import java.util.UUID

class DamageAttribution(
    private val explosiveInteractionTracker: ExplosiveInteractionTracker,
) {
    fun findAttackingPlayer(victim: ServerPlayer, source: DamageSource): ServerPlayer? {
        resolveOwner(source.entity)?.let { return it }
        resolveOwner(source.directEntity)?.let { return it }
        return explosiveInteractionTracker.findPlayer(victim.serverLevel(), source)
    }

    private fun resolveOwner(entity: Entity?): ServerPlayer? {
        var current = entity
        val visited = HashSet<UUID>()

        while (current != null && visited.add(current.uuid)) {
            when (current) {
                is ServerPlayer -> return current
                is OwnableEntity -> current = current.owner
                is TraceableEntity -> current = current.owner
                else -> return null
            }
        }

        return null
    }
}

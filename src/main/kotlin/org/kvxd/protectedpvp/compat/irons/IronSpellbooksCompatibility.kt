package org.kvxd.protectedpvp.compat.irons

import io.redspace.ironsspellbooks.api.events.SpellDamageEvent
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.EventPriority
import net.neoforged.neoforge.common.NeoForge
import org.kvxd.protectedpvp.protection.PvpProtectionService

class IronSpellbooksCompatibility {
    fun findOwner(entity: Entity): Entity? = (entity as? IMagicSummon)?.summoner

    fun register(protectionService: PvpProtectionService) {
        NeoForge.EVENT_BUS.addListener<SpellDamageEvent>(EventPriority.HIGHEST) { event ->
            if (!protectionService.allowSpellDamage(event.entity, event.spellDamageSource, event.amount)) {
                event.isCanceled = true
            }
        }
    }
}

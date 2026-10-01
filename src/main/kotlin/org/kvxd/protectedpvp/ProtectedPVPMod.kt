package org.kvxd.protectedpvp

import net.minecraft.server.level.ServerLevel
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.LevelEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import org.kvxd.protectedpvp.command.PvpCommand
import org.kvxd.protectedpvp.protection.PvpProtectionService

@Mod(value = "protected_pvp", dist = [Dist.DEDICATED_SERVER])
class ProtectedPVPMod {
    init {
        val protectionService = PvpProtectionService()

        NeoForge.EVENT_BUS.addListener<RegisterCommandsEvent> { event ->
            PvpCommand.register(event.dispatcher, protectionService)
        }
        NeoForge.EVENT_BUS.addListener<LivingIncomingDamageEvent> { event ->
            if (!protectionService.allowDamage(event.entity, event.source, event.amount)) {
                event.isCanceled = true
            }
        }
        NeoForge.EVENT_BUS.addListener<PlayerInteractEvent.RightClickBlock> { event ->
            protectionService.recordExplosiveBlockInteraction(event.entity, event.level, event.pos)
        }
        NeoForge.EVENT_BUS.addListener<ServerStartedEvent> { event -> protectionService.start(event.server) }
        NeoForge.EVENT_BUS.addListener<LevelEvent.Save> { event ->
            if (event.level is ServerLevel) {
                protectionService.save()
            }
        }
        NeoForge.EVENT_BUS.addListener<ServerStoppingEvent> { event -> protectionService.stop(event.server) }
    }
}

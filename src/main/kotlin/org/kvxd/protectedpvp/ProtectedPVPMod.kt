package org.kvxd.protectedpvp

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.world.InteractionResult
import org.kvxd.protectedpvp.command.PvpCommand
import org.kvxd.protectedpvp.protection.PvpProtectionService

class ProtectedPVPMod : ModInitializer {
    override fun onInitialize() {
        val protectionService = PvpProtectionService()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            PvpCommand.register(dispatcher, protectionService)
        }
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(protectionService::allowDamage)
        UseBlockCallback.EVENT.register { player, level, _, hitResult ->
            protectionService.recordExplosiveBlockInteraction(player, level, hitResult.blockPos)
            InteractionResult.PASS
        }
        ServerLifecycleEvents.SERVER_STARTED.register(protectionService::start)
        ServerLifecycleEvents.BEFORE_SAVE.register { _, _, _ -> protectionService.save() }
        ServerLifecycleEvents.SERVER_STOPPING.register(protectionService::stop)
    }
}

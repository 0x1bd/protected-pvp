package org.kvxd.protectedpvp.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import org.kvxd.protectedpvp.protection.PvpProtectionService

object PvpCommand {
    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        protectionService: PvpProtectionService,
    ) {
        dispatcher.register(
            literal("pvp")
                .then(
                    literal("on").executes { context ->
                        protectionService.enablePvp(context.source.playerOrException)
                    },
                )
                .then(
                    literal("off").executes { context ->
                        protectionService.enableProtection(context.source.playerOrException)
                    },
                ),
        )
    }
}

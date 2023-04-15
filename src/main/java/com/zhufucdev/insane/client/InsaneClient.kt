package com.zhufucdev.insane.client

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.zhufucdev.insane.Speedrun
import com.zhufucdev.insane.state.ISpeedrun
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.text.Text

class InsaneClient : ClientModInitializer {
    private var speedrun: ISpeedrun? = null
    override fun onInitializeClient() {
        registerCommands()
    }

    private fun registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher: CommandDispatcher<FabricClientCommandSource?>, registryAccess: CommandRegistryAccess? ->
            dispatcher.register(
                    ClientCommandManager.literal("insane")
                            .then(
                                    ClientCommandManager.literal("speedrun")
                                            .then(ClientCommandManager.literal("stop").executes { context: CommandContext<FabricClientCommandSource> ->
                                                val source = context.source
                                                if (speedrun == null) {
                                                    source.sendError(Text.literal("No. You are not speedrunning."))
                                                    return@executes 1
                                                }
                                                source.sendFeedback(Text.literal("OK. You are no longer a speedrunner."))
                                                0
                                            })
                                            .executes { context: CommandContext<FabricClientCommandSource> ->
                                                val source = context.source
                                                source.sendFeedback(Text.literal("OK. You are a speedrunner now."))
                                                val speedrun = Speedrun(source)
                                                speedrun.start()
                                                this.speedrun = speedrun
                                                0
                                            }
                            )
            )
        })
    }
}

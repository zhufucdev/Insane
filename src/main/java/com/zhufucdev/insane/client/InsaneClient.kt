package com.zhufucdev.insane.client

import com.mojang.brigadier.CommandDispatcher
import com.zhufucdev.insane.Speedrun
import com.zhufucdev.insane.state.ISpeedrun
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.text.Text

private var mSpeedrun: ISpeedrun? = null
fun init() {
    registerCommands()
}

private fun registerCommands() {
    val speedrunCmd =
        ClientCommandManager.literal("speedrun")
            .then(ClientCommandManager.literal("stop").executes { context ->
                val source = context.source
                val speedrun = mSpeedrun
                if (speedrun == null || !speedrun.stop()) {
                    source.sendError(Text.literal("No. You are not speedrunning."))
                    return@executes 1
                }
                source.sendFeedback(Text.literal("OK. You are no longer a speedrunner."))
                0
            })
            .then(ClientCommandManager.literal("continue").executes { context ->
                val source = context.source
                mSpeedrun?.stop()
                source.sendFeedback(Text.literal("OK. You are a speedrunner now."))
                val speedrun = Speedrun(source)
                speedrun.start(false)
                mSpeedrun = speedrun
                0
            })
            .executes { context ->
                mSpeedrun?.stop()
                val source = context.source
                source.sendFeedback(Text.literal("OK. You are a speedrunner now."))
                val speedrun = Speedrun(source)
                speedrun.start(true)
                mSpeedrun = speedrun
                0
            }


    ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegistrationCallback { dispatcher: CommandDispatcher<FabricClientCommandSource?>, registryAccess: CommandRegistryAccess? ->
        dispatcher.register(
            ClientCommandManager.literal("insane")
                .then(speedrunCmd)
        )
    })
}

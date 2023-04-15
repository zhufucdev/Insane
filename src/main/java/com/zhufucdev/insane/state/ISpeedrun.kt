package com.zhufucdev.insane.state

import kotlinx.coroutines.Job
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

interface ISpeedrun {
    val runner: FabricClientCommandSource?
    fun start(): Job
    fun stop()
}

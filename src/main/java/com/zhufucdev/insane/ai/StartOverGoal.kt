package com.zhufucdev.insane.ai

import kotlinx.coroutines.delay
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient

class StartOverGoal : CompositeGoal(3) {
    override val goals: List<AbstractGoal> = listOf(
        GetToGroundGoal(),
        GetToBlockGoal(Blocks.WATER, lookAt = false, standOn = true)
    )

    override fun canSkip(): Boolean {
        return player.inventory.isEmpty
    }

    override suspend fun execute(): ExecuteResult {
        if (super.execute() == ExecuteResult.STOP) {
            return ExecuteResult.STOP
        }

        while (!player.showsDeathScreen()) {
            delay(100)
        }
        player.requestRespawn()
        MinecraftClient.getInstance().setScreen(null)

        return ExecuteResult.SUCCEEDED
    }
}
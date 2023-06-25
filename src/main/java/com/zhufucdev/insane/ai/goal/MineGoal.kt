package com.zhufucdev.insane.ai.goal

import com.zhufucdev.insane.ai.AbstractGoal
import com.zhufucdev.insane.ai.ExecuteResult
import com.zhufucdev.insane.baritone
import kotlinx.coroutines.delay
import net.minecraft.util.math.BlockPos

class MineGoal(private val pos1: BlockPos, private val pos2: BlockPos) : AbstractGoal() {
    override fun shouldExecute(): Boolean {
        return true
    }

    override suspend fun execute(): ExecuteResult {
        val baritone = player.baritone()
        baritone.builderProcess.clearArea(pos1, pos2)
        while (baritone.builderProcess.isActive) {
            delay(100)
        }
        return ExecuteResult.SUCCEEDED
    }
}
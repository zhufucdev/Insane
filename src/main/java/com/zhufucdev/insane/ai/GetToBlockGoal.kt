package com.zhufucdev.insane.ai

import baritone.api.pathing.goals.GoalXZ
import baritone.api.utils.Rotation
import com.zhufucdev.insane.Speedrun
import com.zhufucdev.insane.baritone
import kotlinx.coroutines.delay
import net.minecraft.block.Block
import net.minecraft.util.math.BlockPos
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

class GetToBlockGoal(private val block: Block, private val lookAt: Boolean, private val standOn: Boolean) :
    AbstractGoal() {
    override fun shouldExecute(): Boolean {
        return true
    }

    override suspend fun execute(): ExecuteResult {
        val baritone = player.baritone()
        baritone.getToBlockProcess.getToBlock(block)
        while (baritone.getToBlockProcess.isActive) {
            delay(Speedrun.MONITOR_INTERVAL)
        }

        fun findBlock(): BlockPos {
            var goalXz = player.blockPos
            for (x in player.blockX - 8 until player.blockX + 8) {
                for (z in player.blockZ - 8 until player.blockZ + 8) {
                    for (y in player.blockY - 3..player.blockY + 3) {
                        val state = player.world.getBlockState(BlockPos(x, y, z))
                        if (state.isOf(block)) {
                            goalXz = BlockPos(x, y, z)
                        }
                    }
                }
            }
            return goalXz
        }

        if (standOn) {
            val goalPos = findBlock()
            baritone.customGoalProcess.setGoalAndPath(GoalXZ(goalPos.x, goalPos.z))
            while (baritone.customGoalProcess.isActive) {
                delay(Speedrun.MONITOR_INTERVAL)
            }
        }

        if (lookAt) {
            val r = if (standOn) {
                Rotation(0F, PI.toFloat() / 2F)
            } else {
                val d = findBlock().toCenterPos().subtract(player.pos).normalize()
                Rotation(atan2(d.x, d.y).toFloat(), -asin(d.z).toFloat())
            }
            baritone.lookBehavior.updateTarget(r, true)
        }

        return ExecuteResult.SUCCEEDED
    }
}
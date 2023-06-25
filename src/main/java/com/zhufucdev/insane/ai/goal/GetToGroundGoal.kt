package com.zhufucdev.insane.ai.goal

import baritone.api.pathing.goals.GoalYLevel
import com.zhufucdev.insane.Speedrun
import com.zhufucdev.insane.ai.AbstractGoal
import com.zhufucdev.insane.ai.ExecuteResult
import com.zhufucdev.insane.baritone
import kotlinx.coroutines.delay
import net.minecraft.block.Blocks
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import kotlin.math.abs

class GetToGroundGoal : AbstractGoal() {
    private data class Cache(val playerPos: BlockPos, val world: World, val ground: Int)
    private var cache: Cache? = null

    private fun getGroundY(player: ClientPlayerEntity): Int {
        val cache = cache
        val playerPos = player.blockPos
        if (cache != null && cache.playerPos == playerPos && cache.world == player.world) {
            return cache.ground
        }

        val world = player.world
        var y = world.getTopY(Heightmap.Type.WORLD_SURFACE, player.blockX, player.blockZ)
        val yOrigin = y
        while (!world.getBlockState(BlockPos(playerPos.x, y, playerPos.z)).let {
                it.isOf(Blocks.GRASS_BLOCK) || it.isOf(Blocks.DIRT)
            }) {
            y--
            if (yOrigin - y > 10) {
                y = yOrigin
                break
            }
        }
        this.cache = Cache(playerPos, world, y)
        return y
    }

    override fun shouldExecute(): Boolean {
        return true
    }

    override fun canSkip(): Boolean {
        return abs(player.y - getGroundY(player) * 1.0) >= 3.0
    }

    override suspend fun execute(): ExecuteResult {
        val ground = getGroundY(player)
        player.baritone().customGoalProcess.setGoalAndPath(GoalYLevel(ground))
        while (player.pos.y < ground || abs(player.y - ground) > 5) {
            delay(Speedrun.MONITOR_INTERVAL)
        }
        return ExecuteResult.SUCCEEDED
    }
}
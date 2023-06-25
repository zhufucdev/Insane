package com.zhufucdev.insane.ai.goal

import com.zhufucdev.insane.ai.AbstractGoal
import com.zhufucdev.insane.ai.ExecuteResult
import kotlinx.coroutines.delay
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.item.Items
import net.minecraft.util.math.Direction

class StartOverGoal : CompositeGoal(3) {
    override val goals: List<AbstractGoal> = listOf(
        RequireBlockGoal(2, Blocks.SAND),
        SelectorGoal(
            condition = { p ->
                listOf(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH).any {
                    !p.clientWorld.getBlockState(p.blockPos.offset(it)).isAir
                            && !p.clientWorld.getBlockState(p.blockPos.offset(it).offset(Direction.UP)).isAir
                }
            },
            a = null,
            b = DigGoal(Direction.DOWN, 3)
        ),
        PlaceBlockGoal(Items.SAND, Direction.UP, 1)
    )

    override fun canSkip(): Boolean {
        return player.inventory.isEmpty
    }

    override suspend fun execute(): ExecuteResult {
        if (super.execute() == ExecuteResult.STOP) {
            return ExecuteResult.STOP
        }

        while (player.isAlive) {
            delay(100)
        }
        player.requestRespawn()
        MinecraftClient.getInstance().setScreen(null)

        return ExecuteResult.SUCCEEDED
    }
}
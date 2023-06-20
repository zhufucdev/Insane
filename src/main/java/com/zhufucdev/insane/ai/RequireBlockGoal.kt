package com.zhufucdev.insane.ai

import com.zhufucdev.insane.baritone
import com.zhufucdev.insane.mine
import net.minecraft.block.Block
import net.minecraft.client.network.ClientPlayerEntity

class RequireBlockGoal(private val quantity: Int, private vararg val blocksToMine: Block): AbstractGoal() {
    private val correspondingItems = buildSet { blocksToMine.forEach { add(it.asItem()) } }
    override fun shouldExecute(): Boolean {
        return correspondingItems.sumOf { player.inventory.count(it) } < quantity
    }

    override suspend fun execute(): ExecuteResult {
        mine(player, player.baritone(), quantity, *blocksToMine)
        return ExecuteResult.SUCCEEDED
    }
}
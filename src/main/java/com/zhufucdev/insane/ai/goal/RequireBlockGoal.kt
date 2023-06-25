package com.zhufucdev.insane.ai.goal

import com.zhufucdev.insane.ai.AbstractGoal
import com.zhufucdev.insane.ai.ExecuteResult
import com.zhufucdev.insane.baritone
import com.zhufucdev.insane.mine
import net.minecraft.block.Block

class RequireBlockGoal(private val quantity: Int, private vararg val blocksToMine: Block): AbstractGoal() {
    private val correspondingItems = buildSet { blocksToMine.forEach { add(it.asItem()) } }
    override fun canSkip(): Boolean {
        return correspondingItems.sumOf { player.inventory.count(it) } < quantity
    }

    override fun shouldExecute(): Boolean {
        return true
    }

    override suspend fun execute(): ExecuteResult {
        mine(player, player.baritone(), quantity, *blocksToMine)
        return ExecuteResult.SUCCEEDED
    }
}
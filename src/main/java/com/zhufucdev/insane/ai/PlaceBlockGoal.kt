package com.zhufucdev.insane.ai

import com.zhufucdev.insane.place
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos

class PlaceBlockGoal(private val item: Item, private val position: BlockPos? = null) : AbstractGoal() {
    override fun shouldExecute(): Boolean {
        return player.inventory.count(item) > 0
    }

    override suspend fun execute(): ExecuteResult {
        place(player, ItemStack(item), position)
        return ExecuteResult.SUCCEEDED
    }
}
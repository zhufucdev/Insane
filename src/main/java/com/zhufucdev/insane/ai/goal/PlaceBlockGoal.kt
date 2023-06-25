package com.zhufucdev.insane.ai.goal

import com.zhufucdev.insane.ai.AbstractGoal
import com.zhufucdev.insane.ai.BlockPosMem
import com.zhufucdev.insane.ai.ExecuteResult
import com.zhufucdev.insane.place
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i

class PlaceBlockGoal : AbstractGoal {
    private val item: Item
    private val position: BlockPos?
    private val offset: Vec3i?

    constructor(item: Item, position: BlockPos? = null) : super() {
        this.item = item
        this.position = position
        this.offset = null
    }

    constructor(item: Item, direction: Direction, distance: Int = 1) {
        this.item = item
        this.offset = direction.vector.multiply(distance)
        this.position = null
    }

    override fun shouldExecute(): Boolean {
        return player.inventory.count(item) > 0
    }

    override suspend fun execute(): ExecuteResult {
        val placedPos = place(player, ItemStack(item), position ?: player.blockPos.add(offset!!))
        memory.remember(BlockPosMem, item, placedPos)
        return ExecuteResult.SUCCEEDED
    }
}
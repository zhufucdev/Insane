package com.zhufucdev.insane.ai.goal

import com.zhufucdev.insane.ai.AbstractGoal
import com.zhufucdev.insane.ai.ExecuteResult
import com.zhufucdev.insane.baritone
import kotlinx.coroutines.delay
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i

class DigGoal : AbstractGoal {
    private val offset1: Vec3i
    private val offset2: Vec3i
    constructor(offset1: Vec3i, offset2: Vec3i) : super() {
        this.offset1 = offset1
        this.offset2 = offset2
    }

    constructor(direction: Direction, distance: Int) : super() {
        offset1 = Vec3i.ZERO
        offset2 = direction.vector.multiply(distance)
    }

    override fun shouldExecute(): Boolean {
        return true
    }

    override suspend fun execute(): ExecuteResult {
        val baritone = player.baritone()
        baritone.builderProcess.clearArea(player.blockPos.add(offset1), player.blockPos.add(offset2))
        while (baritone.builderProcess.isActive) {
            delay(100)
        }
        return ExecuteResult.SUCCEEDED
    }
}
package com.zhufucdev.insane.ai

import com.zhufucdev.insane.useRuntime
import com.zhufucdev.insane.useRuntimeBlocked
import net.minecraft.client.network.ClientPlayerEntity

class SelectorGoal(
    private val condition: (ClientPlayerEntity) -> Boolean,
    private val a: AbstractGoal?,
    private val b: AbstractGoal?
) : AbstractGoal() {
    override fun shouldExecute(): Boolean {
        return useRuntimeBlocked(a, b) {
            a?.shouldExecute() != false || b?.shouldExecute() != false
        }
    }

    override suspend fun execute(): ExecuteResult {
        return useRuntime(a, b) {
            if (condition(player)) {
                a?.execute()
            } else {
                b?.execute()
            } ?: ExecuteResult.SUCCEEDED
        }
    }
}
package com.zhufucdev.insane.ai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.minecraft.client.network.ClientPlayerEntity

class Schedular(private val player: ClientPlayerEntity, private val maxRetrials: Int, memory: Memory? = null) {
    private val lifecycle: CoroutineScope by lazy { CoroutineScope(Dispatchers.Default) }
    private val memory = memory ?: Memory()
    private val runtime by lazy { Runtime(player, this.memory) }

    private var current: AbstractGoal? = null

    val currentGoal: AbstractGoal? get() = current

    /**
     * @return either [ExecuteResult.SUCCEEDED] or [ExecuteResult.STOP]
     */
    private suspend fun resolve(goal: AbstractGoal, trial: Int, maxRetrials: Int): ExecuteResult {
        if (trial > maxRetrials) {
            return ExecuteResult.STOP
        }

        goal.runtime = runtime
        if (goal.canSkip()) {
            goal.runtime = null
            current = null
            return ExecuteResult.SUCCEEDED
        }

        if (goal.shouldExecute()) {
            current = goal
            val result = goal.execute()
            try {
                if (result == ExecuteResult.RETRY) {
                    return resolve(goal, trial + 1, maxRetrials)
                }
                return result
            } finally {
                current = null
                goal.runtime = null
            }
        }

        goal.dependencies.forEach {
            when (resolve(it, 1, this.maxRetrials)) {
                ExecuteResult.STOP -> return ExecuteResult.STOP
                else -> {}
            }
        }

        return resolve(goal, trial + 1, maxRetrials)
    }

    suspend fun resolve(vararg goal: AbstractGoal): ExecuteResult {
        goal.forEach {
            val result = resolve(it, 1, 1) // the goal max trials is not for such root goal
            if (result == ExecuteResult.STOP) {
                return ExecuteResult.STOP
            }
        }
        return ExecuteResult.SUCCEEDED
    }

    fun start(vararg goal: AbstractGoal) {
        lifecycle.launch {
            resolve(*goal)
        }
    }

    fun cancel() {
        lifecycle.cancel()
    }
}

data class Runtime(val playerEntity: ClientPlayerEntity, val memory: Memory)
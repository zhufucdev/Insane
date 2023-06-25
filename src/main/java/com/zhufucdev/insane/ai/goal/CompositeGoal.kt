package com.zhufucdev.insane.ai.goal

import com.zhufucdev.insane.ai.AbstractGoal
import com.zhufucdev.insane.ai.ExecuteResult
import com.zhufucdev.insane.ai.Schedular

abstract class CompositeGoal(private val maxRetries: Int) : AbstractGoal() {
    abstract val goals: List<AbstractGoal>
    private var current: Int = 0

    override fun shouldExecute(): Boolean {
        val currentGoal = goals[current]
        currentGoal.runtime = runtime
        try {
            return currentGoal.shouldExecute()
        } finally {
            currentGoal.runtime = null
        }
    }

    override suspend fun execute(): ExecuteResult {
        val schedular = Schedular(player, maxRetries, memory)
        return schedular.resolve(*goals.toTypedArray())
    }
}
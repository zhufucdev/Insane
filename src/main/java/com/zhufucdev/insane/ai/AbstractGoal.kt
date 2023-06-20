package com.zhufucdev.insane.ai

import net.minecraft.client.network.ClientPlayerEntity

/**
 * Basic component of an AI implementation to become speed runner
 *
 * When one goal is to be executed, [shouldExecute] is called first.
 * If it returns false, then the [dependencies] are to be executed.
 * If it returns true, then [execute] is called.
 */
abstract class AbstractGoal {
    var runtime: Runtime? = null

    protected fun requireRuntime(): Runtime {
        return runtime ?: error("runtime not available. Mostly due to resource overuse.")
    }

    protected val player: ClientPlayerEntity get() = requireRuntime().playerEntity
    protected val memory: Memory get() = requireRuntime().memory

    open val dependencies: List<AbstractGoal> get() = emptyList()
    abstract fun shouldExecute(): Boolean
    open fun canSkip(): Boolean = false
    abstract suspend fun execute(): ExecuteResult
}
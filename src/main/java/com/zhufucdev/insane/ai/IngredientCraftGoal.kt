package com.zhufucdev.insane.ai

import net.minecraft.recipe.Ingredient

class IngredientCraftGoal(private val ingredient: Ingredient, private val quantity: Int) : AbstractGoal() {
    override fun shouldExecute(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun execute(): ExecuteResult {
        TODO("Not yet implemented")
    }
}
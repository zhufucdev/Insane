package com.zhufucdev.insane.ai.goal

import com.zhufucdev.insane.ai.AbstractCraftGoal
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeManager
import net.minecraft.registry.DynamicRegistryManager

class IngredientCraftGoal(
    private val ingredient: Ingredient,
    override val quantity: Int = 1,
    private val skipIfPresent: Boolean = true
) : AbstractCraftGoal() {
    override val recipe: CraftingRecipe? by lazy {
        RecipeManager().values().firstOrNull {
            it is CraftingRecipe
                    && ingredient.matchingStacks.any { s -> it.getOutput(DynamicRegistryManager.EMPTY).isOf(s.item) }
        } as CraftingRecipe?
    }

    override fun canSkip(): Boolean {
        return skipIfPresent && player.inventory.containsAny { ingredient.matchingStacks.any { s -> s.isOf(it.item) } }
    }
}
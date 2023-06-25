package com.zhufucdev.insane.ai.goal

import com.zhufucdev.insane.ai.AbstractCraftGoal
import net.minecraft.item.Item
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.RecipeManager
import net.minecraft.registry.DynamicRegistryManager

open class CraftGoal(private val target: Item, override val quantity: Int = 1, private val skipIfPresent: Boolean = true) :
    AbstractCraftGoal() {
    override val recipe by lazy {
        RecipeManager().values().firstOrNull {
            it is CraftingRecipe && it.getOutput(DynamicRegistryManager.EMPTY).item == target
        } as CraftingRecipe?
    }

    override fun canSkip(): Boolean {
        return skipIfPresent && player.inventory.count(target) > 0
    }
}
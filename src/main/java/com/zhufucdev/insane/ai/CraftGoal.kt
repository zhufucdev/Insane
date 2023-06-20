package com.zhufucdev.insane.ai

import com.zhufucdev.insane.Speedrun
import com.zhufucdev.insane.craft
import com.zhufucdev.insane.openBlockInventory
import kotlinx.coroutines.delay
import net.fabricmc.loader.impl.util.log.Log
import net.fabricmc.loader.impl.util.log.LogCategory
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeManager
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.screen.CraftingScreenHandler

class CraftGoal(private val target: Item, private val quantity: Int = 1, private val skipIfPresent: Boolean = true) :
    AbstractGoal() {
    private val recipe by lazy {
        RecipeManager().values().firstOrNull {
            it is CraftingRecipe && it.getOutput(DynamicRegistryManager.EMPTY).item == target
        } as CraftingRecipe?
    }

    private fun getPracticalRecipe(player: ClientPlayerEntity): Array<Item?> {
        val recipe = recipe ?: return emptyArray()

        return recipe.ingredients.map { ingredient ->
            ingredient.matchingStacks.first { player.inventory.count(it.item) > 0 }.item
        }.toTypedArray()
    }

    private val useCraftingTable by lazy { recipe?.fits(3, 3) == true }

    private val countOf by lazy {
        buildMap<Ingredient, Int> {
            val recipe = recipe ?: return@buildMap

            recipe.ingredients.forEach { ingredient ->
                if (containsKey(ingredient)) {
                    put(ingredient, get(ingredient)!! + quantity)
                } else {
                    put(ingredient, quantity)
                }
            }
        }
    }

    override val dependencies: List<AbstractGoal> by lazy {
        buildList {
            countOf.forEach { add(IngredientCraftGoal(it.key, it.value)) }
            if (useCraftingTable) {
                add(CraftGoal(Items.CRAFTING_TABLE))
                add(PlaceBlockGoal(Items.CRAFTING_TABLE))
            }
        }
    }

    override fun shouldExecute(): Boolean {
        if (skipIfPresent && player.inventory.count(target) > 0) {
            return true
        }

        countOf.forEach { (ingredient, count) ->
            val existing = ingredient.matchingStacks.sumOf { player.inventory.count(it.item) }
            if (existing <= count) return false
            if (useCraftingTable && player.inventory.count(Items.CRAFTING_TABLE) <= 0) return false
        }
        return true
    }

    override suspend fun execute(): ExecuteResult {
        if (skipIfPresent && player.inventory.count(target) > 0) {
            return ExecuteResult.SUCCEEDED
        }

        if (useCraftingTable) {
            val tablePos = memory.think(BlockPosMem, Items.CRAFTING_TABLE)
            if (tablePos == null) {
                Log.warn(LogCategory.GENERAL, "CraftGoal exited due to missing crafting table")
                return ExecuteResult.STOP
            }
            val screen = openBlockInventory<CraftingScreenHandler>(player, tablePos)
            val slot = player.inventory.emptySlot
            for (i in 0 until quantity) {
                craft(player, getPracticalRecipe(player), slot, screen.screenHandler)
                delay(Speedrun.INTERACTION_INTERVAL)
            }
        } else {
            val slot = player.inventory.emptySlot
            for (i in 0 until quantity) {
                craft(player, getPracticalRecipe(player), slot, player.playerScreenHandler)
                delay(Speedrun.INTERACTION_INTERVAL)
            }
        }
        return ExecuteResult.SUCCEEDED
    }
}
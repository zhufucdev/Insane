package com.zhufucdev.insane.ai

import com.zhufucdev.insane.Speedrun
import com.zhufucdev.insane.ai.goal.CraftGoal
import com.zhufucdev.insane.ai.goal.IngredientCraftGoal
import com.zhufucdev.insane.ai.goal.PlaceBlockGoal
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
import net.minecraft.screen.CraftingScreenHandler

abstract class AbstractCraftGoal : AbstractGoal() {
    abstract val recipe: CraftingRecipe?
    abstract val quantity: Int

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
        countOf.forEach { (ingredient, count) ->
            val existing = ingredient.matchingStacks.sumOf { player.inventory.count(it.item) }
            if (existing <= count) return false
            if (useCraftingTable && player.inventory.count(Items.CRAFTING_TABLE) <= 0) return false
        }
        return true
    }

    private fun getPracticalRecipe(player: ClientPlayerEntity): Array<Item?> {
        val recipe = recipe ?: return emptyArray()

        return recipe.ingredients.map { ingredient ->
            ingredient.matchingStacks.maxBy { player.inventory.count(it.item) }.item
        }.toTypedArray()
    }


    private val useCraftingTable by lazy { recipe?.fits(2, 2) != true }

    protected val countOf by lazy {
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

    override suspend fun execute(): ExecuteResult {
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
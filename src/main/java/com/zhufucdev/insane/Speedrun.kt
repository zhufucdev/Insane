package com.zhufucdev.insane

import baritone.api.BaritoneAPI
import baritone.api.IBaritone
import baritone.api.schematic.FillSchematic
import baritone.api.utils.BlockOptionalMeta
import com.zhufucdev.insane.state.ISpeedrun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.AbstractRecipeScreenHandler
import net.minecraft.screen.CraftingScreenHandler
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import kotlin.coroutines.suspendCoroutine

class Speedrun(private val source: FabricClientCommandSource) : ISpeedrun {
    private val lifecycle = CoroutineScope(Dispatchers.Default)

    private val player: ClientPlayerEntity = source.player
    private val baritone: IBaritone = BaritoneAPI.getProvider().getBaritoneForPlayer(source.player)
    override val runner: FabricClientCommandSource
        get() = source

    private suspend fun getRidOfExistingItems() = suspendCoroutine { c ->
        source.sendFeedback(Text.literal("Insane: Getting rid of existing items"))
        val chest = if (player.inventory.count(Items.CHEST) <= 0) {
            val craftingTable: ItemStack =
                if (player.inventory.count(Items.CRAFTING_TABLE) <= 0) {
                    try {
                        baritone.mineProcess.mineByName(3, "oak_log")
                    } catch (e: IllegalStateException) {
                        // ignored
                    }
                    while ((player.inventory.main.firstOrNull { it.item == Items.OAK_LOG }?.count ?: 0) < 3) {
                        Thread.sleep(100L)
                    }
                    val targetSlot = player.inventory.emptySlot
                    val screen = InventoryScreen(player)
                    repeat(3) {
                        Thread.sleep(100L)
                        craft(arrayOf(Items.OAK_LOG), targetSlot, screen.screenHandler)
                    }
                    craft(Array(4) { Items.OAK_PLANKS }, screenHandler = screen.screenHandler)
                } else {
                    player.inventory.main.first { it.item == Items.CRAFTING_TABLE }
                }
            val pos = place(craftingTable)
            val tableState = player.world.getBlockState(pos)
            val playerScreen = InventoryScreen(player)
            val handler = tableState.createScreenHandlerFactory(player.world, pos)!!
                .createMenu(playerScreen.screenHandler.syncId, player.inventory, player) as CraftingScreenHandler
            val chestRecipe = Array(9) { Items.OAK_PLANKS }
            chestRecipe[4] = null
            craft(chestRecipe, screenHandler = handler)
        } else {
            player.inventory.main.first { it.item == Items.CHEST }
        }
        place(chest)
        c.resumeWith(Result.success(true))
    }

    private fun craft(
        ingredients: Array<Item?>,
        targetSlot: Int = player.inventory.emptySlot,
        screenHandler: AbstractRecipeScreenHandler<*>
    ): ItemStack {
        val client = MinecraftClient.getInstance()

        client.interactionManager!!.apply {
            for (i in 0 until 9) {
                if (ingredients.size <= i) break
                if (ingredients[i] == null) continue

                val from = screenHandler.getSlotIndex(
                    player.inventory,
                    player.inventory.getSlotWithStack(player.inventory.main.first { it.item == ingredients[i] })
                ).asInt
                clickSlot(
                    screenHandler.syncId,
                    from,
                    0,
                    SlotActionType.PICKUP,
                    player
                )
                Thread.sleep(100)
                clickSlot(
                    screenHandler.syncId,
                    1 + i, // top left to crafting slots,
                    1,
                    SlotActionType.PICKUP,
                    player
                )
                Thread.sleep(100)
                clickSlot(
                    screenHandler.syncId,
                    from,
                    0,
                    SlotActionType.PICKUP,
                    player
                )
                Thread.sleep(100)
            }
            clickSlot(
                screenHandler.syncId,
                screenHandler.craftingResultSlotIndex,
                0,
                SlotActionType.PICKUP,
                player
            )
            Thread.sleep(100)
            clickSlot(
                screenHandler.syncId,
                screenHandler.getSlotIndex(player.inventory, targetSlot).asInt,
                0,
                SlotActionType.PICKUP,
                player
            )
            return player.inventory.getStack(targetSlot)
        }
    }

    private fun place(item: ItemStack): BlockPos {
        var targetSlot = player.inventory.getSlotWithStack(item)
        if (targetSlot > 9) {
            val screen = InventoryScreen(player)
            val client = MinecraftClient.getInstance()
            client.interactionManager!!.apply {
                // move it to hot bar
                clickSlot(
                    screen.screenHandler.syncId,
                    screen.screenHandler.getSlotIndex(player.inventory, player.inventory.selectedSlot).asInt,
                    screen.screenHandler.getSlotIndex(player.inventory, targetSlot).asInt,
                    SlotActionType.SWAP,
                    player
                )
                targetSlot = player.inventory.selectedSlot
            }
        }

        player.inventory.selectedSlot = targetSlot
        val pos = Vec3i(player.blockX, player.blockY, player.blockZ)
        baritone.builderProcess.build(
            "something",
            FillSchematic(1, 1, 1, BlockOptionalMeta(Block.getBlockFromItem(item.item))),
            pos
        )
        while (player.world.getBlockState(BlockPos(pos)).isAir) {
            Thread.sleep(100)
        }
        return BlockPos(pos)
    }

    override fun start(): Job = lifecycle.launch {
        if (!player.inventory.isEmpty) {
            getRidOfExistingItems()
        }

        source.sendFeedback(Text.literal("Insane: Speedrun completed. You are now dream"))
    }

    override fun stop() {}
}

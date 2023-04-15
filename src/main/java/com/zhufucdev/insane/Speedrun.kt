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
import net.fabricmc.loader.impl.util.log.Log
import net.fabricmc.loader.impl.util.log.LogCategory
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.AbstractRecipeScreenHandler
import net.minecraft.screen.CraftingScreenHandler
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
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
        var craftingTablePos: BlockPos? = null
        var chestPos: BlockPos? = null
        while (!player.inventory.isEmpty) {
            var pos = chestPos
            if (pos == null) {
                val chest = if (player.inventory.count(Items.CHEST) <= 0) {
                    var craftingPos = craftingTablePos
                    if (craftingPos == null) {
                        val craftingTable: ItemStack =
                            if (player.inventory.count(Items.CRAFTING_TABLE) <= 0) {
                                try {
                                    baritone.mineProcess.mineByName(3, "oak_log")
                                } catch (e: IllegalStateException) {
                                    // ignored
                                }
                                while ((player.inventory.main.firstOrNull { it.item == Items.OAK_LOG }?.count
                                        ?: 0) < 3
                                ) {
                                    Thread.sleep(MONITOR_INTERVAL)
                                }
                                val targetSlot = player.inventory.emptySlot
                                val screen = InventoryScreen(player)
                                repeat(3) {
                                    Thread.sleep(MONITOR_INTERVAL)
                                    craft(arrayOf(Items.OAK_LOG), targetSlot, screen.screenHandler)
                                }
                                craft(Array(4) { Items.OAK_PLANKS }, screenHandler = screen.screenHandler)
                            } else {
                                player.inventory.main.first { it.item == Items.CRAFTING_TABLE }
                            }
                        craftingPos = place(craftingTable)
                        craftingTablePos = craftingPos
                    }
                    Thread.sleep(INTERACTION_INTERVAL)
                    val craftingScreen = openBlockInventory<CraftingScreenHandler>(craftingPos)
                    val chestRecipe = Array(9) { Items.OAK_PLANKS }
                    chestRecipe[4] = null
                    try {
                        craft(chestRecipe, screenHandler = craftingScreen.screenHandler)
                    } catch (_: NoSuchElementException) {
                        craftingTablePos = null
                        continue
                    }
                } else {
                    player.inventory.main.first { it.item == Items.CHEST }
                }
                try {
                    MinecraftClient.getInstance().setScreen(null)
                } catch (_: IllegalStateException) {
                    // ignored
                }
                Thread.sleep(INTERACTION_INTERVAL)

                pos = place(chest, craftingTablePos?.east())
                Thread.sleep(INTERACTION_INTERVAL)
            }
            val chestInventory = openBlockInventory<ScreenHandler>(pos)
            var playerItems = player.inventory.main.filter { !it.isEmpty }
            for (i in 0 until chestInventory.screenHandler.slots.size) {
                val item = playerItems[0]
                playerItems = playerItems.drop(1)
                putIn(player.inventory.getSlotWithStack(item), i, chestInventory.screenHandler)
                Thread.sleep(INTERACTION_INTERVAL)

                if (playerItems.isEmpty()) {
                    chestPos = pos
                    break
                }
            }

            (player.inventory.armor + player.inventory.offHand).forEach {
                // armors, left hand, may be surviving
                if (!it.isEmpty) {
                    val source = player.inventory.getSlotWithStack(it)
                    sortInventory(source, player.inventory.emptySlot)
                    Thread.sleep(INTERACTION_INTERVAL)
                }
            }
        }
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
                Thread.sleep(50)
                clickSlot(
                    screenHandler.syncId,
                    1 + i, // top left to crafting slots,
                    1,
                    SlotActionType.PICKUP,
                    player
                )
                Thread.sleep(50)
                clickSlot(
                    screenHandler.syncId,
                    from,
                    0,
                    SlotActionType.PICKUP,
                    player
                )
                Thread.sleep(INTERACTION_INTERVAL)
            }
            clickSlot(
                screenHandler.syncId,
                screenHandler.craftingResultSlotIndex,
                0,
                SlotActionType.PICKUP,
                player
            )
            Thread.sleep(INTERACTION_INTERVAL)
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

    private suspend fun mine(quantity: Int, vararg block: Block) = suspendCoroutine { c ->
        try {
            baritone.mineProcess.mine(quantity, *block)
            source.sendFeedback(Text.literal("Mining ${block.joinToString()}"))
        } catch (_: IllegalStateException) {
            // ignored
        }

        val items = block.map { it.asItem() }
        while (player.inventory.main.sumOf { it.takeIf { items.contains(it.item) }?.count ?: 0 } < quantity) {
            Thread.sleep(MONITOR_INTERVAL)
        }
        c.resumeWith(Result.success(player.inventory.main.filter { items.contains(it.item) }))
    }

    private fun place(item: ItemStack, position: BlockPos? = null): BlockPos {
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
        val pos = position ?: Vec3i(player.blockX, player.blockY, player.blockZ)
        baritone.builderProcess.build(
            "something",
            FillSchematic(1, 1, 1, BlockOptionalMeta(Block.getBlockFromItem(item.item))),
            pos
        )
        while (player.world.getBlockState(BlockPos(pos)).isAir) {
            Thread.sleep(MONITOR_INTERVAL)
        }
        return BlockPos(pos)
    }

    private fun <T : ScreenHandler> openBlockInventory(blockPos: BlockPos): HandledScreen<T> {
        val client = MinecraftClient.getInstance()
        var retries = 0
        while (retries < 10) {
            val opened = client.interactionManager!!.interactBlock(
                MinecraftClient.getInstance().player,
                Hand.MAIN_HAND,
                net.minecraft.util.hit.BlockHitResult(player.pos, Direction.UP, blockPos, false)
            ).isAccepted
            if (!opened) {
                Log.warn(LogCategory.LOG, "Unable to open the inventory. $retries retries")
            } else {
                break
            }
            Thread.sleep(RETRY_INTERVAL)
            retries ++
        }
        while (client.currentScreen == null) {
            Thread.sleep(MONITOR_INTERVAL)
        }
        return client.currentScreen!! as HandledScreen<T>
    }

    private fun putIn(playerSource: Int, targetSlot: Int, handler: ScreenHandler) {
        MinecraftClient.getInstance().interactionManager!!.apply {
            clickSlot(
                handler.syncId,
                handler.getSlotIndex(player.inventory, playerSource).asInt,
                0,
                SlotActionType.PICKUP,
                player
            )
            clickSlot(
                handler.syncId,
                targetSlot,
                0,
                SlotActionType.PICKUP,
                player
            )
        }
    }

    private fun sortInventory(source: Int, target: Int) {
        val screen = InventoryScreen(player)
        val handler = screen.screenHandler
        MinecraftClient.getInstance().interactionManager!!.apply {
            clickSlot(
                handler.syncId,
                handler.getSlotIndex(player.inventory, target).orElse(target),
                handler.getSlotIndex(player.inventory, source).orElse(source),
                SlotActionType.SWAP,
                player
            )
        }
    }

    override fun start(): Job = lifecycle.launch {
        try {
            if (!player.inventory.isEmpty) {
                getRidOfExistingItems()
            }

            var wood = mine(5, *WOOD_BLOCKS).first().item
            val playerInventoryHandler = player.playerScreenHandler
            var craftingTablePos: BlockPos? = null
            for (i in 0..3) {
                val plank = craft(arrayOf(wood), screenHandler = playerInventoryHandler).item

                when (i) {
                    0 -> {
                        val table = craft(Array(4) { plank }, screenHandler = playerInventoryHandler)
                        craftingTablePos = place(table)
                    }

                    1 -> craft(arrayOf(plank, null, plank), screenHandler = playerInventoryHandler)
                    2 -> {
                        if (craftingTablePos == null) error("Crafting table not found")
                        val inv = openBlockInventory<CraftingScreenHandler>(craftingTablePos)
                        craft(
                            arrayOf(plank, plank, plank, null, Items.STICK, null, null, Items.STICK),
                            screenHandler = inv.screenHandler
                        )
                    }
                }

                if (player.inventory.count(wood) <= 0) {
                    wood = player.inventory.main.firstOrNull { it.isWood }?.item
                        ?: mine(1, *WOOD_BLOCKS).first().item
                }
            }

            source.sendFeedback(Text.literal("Insane: Speedrun completed. You are now dream"))
        } catch (e: Exception) {
            source.sendError(Text.literal("Insane: Failed to complete the run. ${e.message}"))
            Log.error(LogCategory.GENERAL, "Failed to complete the run", e)
        }
    }

    override fun stop() {}

    private val WOOD_BLOCKS = arrayOf(
        Blocks.OAK_LOG, Blocks.ACACIA_LOG, Blocks.BIRCH_LOG,
        Blocks.CHERRY_LOG, Blocks.JUNGLE_LOG, Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG
    )

    private val ItemStack.isWood get() = WOOD_BLOCKS.contains(Block.getBlockFromItem(item))

    companion object {
        const val INTERACTION_INTERVAL = 50L
        const val MONITOR_INTERVAL = 100L
        const val RETRY_INTERVAL = 500L
    }
}

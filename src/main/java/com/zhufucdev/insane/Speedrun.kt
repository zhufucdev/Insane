package com.zhufucdev.insane

import baritone.api.BaritoneAPI
import baritone.api.IBaritone
import baritone.api.pathing.goals.GoalComposite
import baritone.api.pathing.goals.GoalNear
import baritone.api.pathing.goals.GoalYLevel
import baritone.api.schematic.FillSchematic
import baritone.api.utils.BlockOptionalMeta
import baritone.api.utils.BlockOptionalMetaLookup
import com.zhufucdev.insane.state.ISpeedrun
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.loader.impl.util.log.Log
import net.fabricmc.loader.impl.util.log.LogCategory
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.screen.AbstractRecipeScreenHandler
import net.minecraft.screen.CraftingScreenHandler
import net.minecraft.screen.FurnaceScreenHandler
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypeFilter
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i
import net.minecraft.world.Heightmap
import java.util.concurrent.CancellationException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil

class Speedrun(private val source: FabricClientCommandSource) : ISpeedrun {
    private val lifecycle = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    private val player: ClientPlayerEntity = source.player
    private val baritone: IBaritone = BaritoneAPI.getProvider().getBaritoneForPlayer(source.player)
    override val runner: FabricClientCommandSource
        get() = source

    private suspend fun getRidOfExistingItems() = suspendCoroutine { c ->
        source.sendFeedback(Text.literal("Insane: Getting rid of existing items"))
        var craftingTablePos: BlockPos? = null
        var chestPos: BlockPos? = null

        val y = player.world.getTopY(Heightmap.Type.WORLD_SURFACE, player.blockX, player.blockZ)
        source.sendFeedback(Text.literal("Insane: Get to y ~ $y"))
        baritone.customGoalProcess.setGoalAndPath(GoalYLevel(y))
        while (player.pos.y < y || abs(player.y - y) > 5) {
            Thread.sleep(MONITOR_INTERVAL)
        }

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
                putIn(player.inventory.getSlotWithStack(item), i, handler = chestInventory.screenHandler)
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

    private suspend fun mine(quantity: Int, vararg block: Block, targetItem: Item? = null) = suspendCoroutine { c ->
        val items = block.map { it.asItem() }.let { if (targetItem == null) it else it + targetItem }
        val blockMeta = BlockOptionalMetaLookup(*block)
        fun count() = player.inventory.main.sumOf { it.takeIf { items.contains(it.item) }?.count ?: 0 }

        while (count() < quantity) {
            if (!baritone.mineProcess.isActive) {
                try {
                    source.sendFeedback(Text.literal("Mining $blockMeta"))
                    baritone.mineProcess.mine(quantity, blockMeta)
                } catch (_: IllegalStateException) {
                    // ignored
                }
            }
            while (true) {
                val dropped = player.world.getEntitiesByType(
                    TypeFilter.instanceOf(ItemEntity::class.java),
                    Box(0.0, 0.0, 0.0, 5.0, 10.0, 5.0)
                ) {
                    items.contains(it.stack.item)
                }
                if (dropped.isNotEmpty()) {
                    if (!baritone.mineProcess.isActive) {
                        baritone.mineProcess.cancel()
                        baritone.customGoalProcess.setGoalAndPath(
                            GoalComposite(*dropped.map { GoalNear(it.blockPos, 1) }.toTypedArray())
                        )
                    }
                } else {
                    break
                }

                Thread.sleep(MONITOR_INTERVAL)
            }
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
                if (retries == 2) {
                    source.sendFeedback(Text.literal("Insane: Waiting for inventory to open"))
                }
            } else {
                break
            }
            Thread.sleep(RETRY_INTERVAL)
            retries++
        }
        while (client.currentScreen == null) {
            Thread.sleep(MONITOR_INTERVAL)
        }
        return client.currentScreen!! as HandledScreen<T>
    }

    private fun putIn(playerSource: Int, targetSlot: Int, handler: ScreenHandler, half: Boolean = false) {
        val btn = if (half) 1 else 0
        MinecraftClient.getInstance().interactionManager!!.apply {
            val playerSlot = handler.getSlotIndex(player.inventory, playerSource).asInt
            clickSlot(
                handler.syncId,
                playerSlot,
                btn,
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
            if (half) {
                clickSlot(
                    handler.syncId,
                    playerSlot,
                    0,
                    SlotActionType.PICKUP,
                    player
                )
            }
        }
    }

    private fun takeOut(invSource: Int, playerSlot: Int, handler: ScreenHandler, half: Boolean = false): ItemStack {
        val btn = if (half) 1 else 0
        val target = handler.getSlotIndex(player.inventory, playerSlot).asInt
        MinecraftClient.getInstance().interactionManager!!.apply {
            clickSlot(
                handler.syncId,
                invSource,
                btn,
                SlotActionType.PICKUP,
                player
            )
            clickSlot(
                handler.syncId,
                target,
                0,
                SlotActionType.PICKUP,
                player
            )
            if (half) {
                clickSlot(
                    handler.syncId,
                    target,
                    0,
                    SlotActionType.PICKUP,
                    player
                )
            }
        }
        return player.inventory.getStack(invSource)
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

    private suspend fun nextWood(mine: Boolean = true, quantity: Int = 1) =
        player.inventory.main.firstOrNull { it.isWood && player.inventory.count(it.item) >= quantity }?.item
            ?: if (mine) mine(quantity, *WOOD_BLOCKS).first().item else null

    private suspend fun nextPlank(mine: Boolean = true, quantity: Int = 1) =
        player.inventory.main.firstOrNull { it.isPlanks && player.inventory.count(it.item) >= quantity }?.item
            ?: if (mine) craft(
                arrayOf(nextWood(true, ceil(quantity / 4F).toInt())!!),
                screenHandler = player.playerScreenHandler
            ).item
            else null

    private fun nextItems(vararg item: Item, quantity: Int = 1) =
        player.inventory.main.firstOrNull { item.contains(it.item) && player.inventory.count(it.item) >= quantity }?.item

    private suspend fun requireCraftingTable(position: BlockPos? = null): BlockPos {
        val table = craft(Array(4) { nextPlank(quantity = 4)!! }, screenHandler = player.playerScreenHandler)
        return place(table, position)
    }

    private suspend fun craftPickaxe(craftingTable: BlockPos? = null, material: Item): BlockPos {
        val craftingTablePos = craftingTable ?: requireCraftingTable(player.blockPos.east())
        val sticks = nextItems(Items.STICK, quantity = 2) ?: craft(
            arrayOf(nextPlank(), null, nextPlank()),
            screenHandler = player.playerScreenHandler
        ).item
        val inv = openBlockInventory<CraftingScreenHandler>(craftingTablePos)
        craft(
            arrayOf(material, material, material, null, sticks, null, null, sticks),
            screenHandler = inv.screenHandler
        )
        return craftingTablePos
    }

    private suspend fun getToBlock(pos: BlockPos) = suspendCoroutine { c ->
        while (true) {
            if (pos.isWithinDistance(player.pos, 5.0)) {
                val direction = pos.toCenterPos().subtract(player.pos).normalize()
                val pitch = atan(direction.y).toFloat()
                val yaw = atan(direction.x / direction.z).toFloat()
                baritone.customGoalProcess.setGoalAndPath(null)
                player.yaw = yaw
                player.pitch = pitch
                c.resumeWith(Result.success(true))
                return@suspendCoroutine
            }
            if (!baritone.customGoalProcess.isActive) {
                baritone.customGoalProcess.setGoalAndPath(GoalNear(pos, 1))
            }
            Thread.sleep(MONITOR_INTERVAL)
        }
    }

    override fun start(clear: Boolean) {
        job = lifecycle.launch {
            try {
                if (clear && !player.inventory.isEmpty) {
                    getRidOfExistingItems()
                }

                var craftingTablePos: BlockPos? = null
                if (player.inventory.count(Items.WOODEN_PICKAXE) <= 0) {
                    var wood = mine(5, *WOOD_BLOCKS).first().item
                    val playerInventoryHandler = player.playerScreenHandler
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
                            wood = nextWood()
                        }
                    }
                }

                if (player.inventory.count(Items.STONE_PICKAXE) <= 0) {
                    val stone = mine(3, Blocks.STONE, Blocks.COBBLESTONE).first().item
                    craftingTablePos = craftPickaxe(material = stone)
                    mine(8, Blocks.STONE, Blocks.COBBLESTONE)
                }

                if (player.inventory.count(Items.IRON_PICKAXE) <= 0) {
                    if (craftingTablePos == null || !craftingTablePos.isWithinDistance(player.pos, 20.0)) {
                        craftingTablePos = requireCraftingTable()
                    }
                    getToBlock(craftingTablePos)
                    val furnaceItem = if (player.inventory.count(Items.FURNACE) > 0) {
                        player.inventory.main.first { it.item == Items.FURNACE }
                    } else {
                        val screen = openBlockInventory<CraftingScreenHandler>(craftingTablePos)
                        val recipe = Array(9) { Items.COBBLESTONE }.apply { set(4, null) }
                        craft(
                            recipe,
                            screenHandler = screen.screenHandler
                        )
                    }
                    val coals = BaritoneAPI.getProvider().worldScanner
                        .scanChunk(baritone.playerContext,
                            BlockOptionalMetaLookup(Blocks.COAL_ORE), player.chunkPos, 1, -1)
                    // coal first
                    val coalOre: ItemStack
                    val ironOre: ItemStack
                    if (coals.isNotEmpty()) {
                        coalOre = mine(2, Blocks.COAL_ORE, targetItem = Items.COAL).first()
                        ironOre = mine(3,
                            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                            targetItem = Items.RAW_IRON
                        ).first()
                    } else {
                        ironOre = mine(3,
                            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                            targetItem = Items.RAW_IRON
                        ).first()
                        coalOre = mine(1, Blocks.COAL_ORE, targetItem = Items.COAL).first()
                    }

                    val furnace = place(furnaceItem, player.blockPos.east())
                    val inv = openBlockInventory<FurnaceScreenHandler>(furnace)
                    putIn(
                        player.inventory.getSlotWithStack(ironOre),
                        0,
                        handler = inv.screenHandler
                    )
                    Thread.sleep(INTERACTION_INTERVAL)
                    putIn(
                        player.inventory.getSlotWithStack(coalOre),
                        1,
                        handler = inv.screenHandler
                    )

                    while (inv.screenHandler.isBurning) {
                        delay(100L)
                    }

                    val ironIngots =
                        takeOut(inv.screenHandler.craftingResultSlotIndex, player.inventory.emptySlot, inv.screenHandler)
                    craftPickaxe(craftingTablePos, ironIngots.item)
                }

                source.sendFeedback(Text.literal("Insane: Speedrun completed. You are now dream"))
            } catch (e: Exception) {
                source.sendError(Text.literal("Insane: Failed to complete the run. ${e.message}"))
                Log.error(LogCategory.GENERAL, "Failed to complete the run", e)
            }
        }
    }

    override fun stop(): Boolean {
        val job = this.job
        if (job?.isActive == true) {
            job.cancel(CancellationException())
            return true
        }
        return false
    }

    private val WOOD_BLOCKS = arrayOf(
        Blocks.OAK_LOG, Blocks.ACACIA_LOG, Blocks.BIRCH_LOG,
        Blocks.CHERRY_LOG, Blocks.JUNGLE_LOG, Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG
    )

    private val PLANK_BLOCKS = arrayOf(
        Blocks.OAK_PLANKS, Blocks.ACACIA_PLANKS, Blocks.BIRCH_PLANKS,
        Blocks.CHERRY_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.SPRUCE_PLANKS
    )

    private val ItemStack.isWood get() = WOOD_BLOCKS.contains(Block.getBlockFromItem(item))
    private val ItemStack.isPlanks get() = PLANK_BLOCKS.contains(Block.getBlockFromItem(item))

    companion object {
        const val INTERACTION_INTERVAL = 50L
        const val MONITOR_INTERVAL = 100L
        const val RETRY_INTERVAL = 500L
    }
}

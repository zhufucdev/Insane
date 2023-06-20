package com.zhufucdev.insane

import baritone.api.BaritoneAPI
import baritone.api.IBaritone
import baritone.api.pathing.goals.GoalComposite
import baritone.api.pathing.goals.GoalNear
import baritone.api.schematic.FillSchematic
import baritone.api.utils.BlockOptionalMeta
import baritone.api.utils.BlockOptionalMetaLookup
import com.zhufucdev.insane.ai.AbstractGoal
import kotlinx.coroutines.runBlocking
import net.fabricmc.loader.impl.util.log.Log
import net.fabricmc.loader.impl.util.log.LogCategory
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.screen.AbstractRecipeScreenHandler
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypeFilter
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i
import kotlin.coroutines.suspendCoroutine

fun ClientPlayerEntity.baritone(): IBaritone = BaritoneAPI.getProvider().getBaritoneForPlayer(this)

suspend fun mine(player: ClientPlayerEntity, baritone: IBaritone, quantity: Int, vararg block: Block, targetItem: Item? = null) = suspendCoroutine { c ->
    val items = block.map { it.asItem() }.let { if (targetItem == null) it else it + targetItem }
    val blockMeta = BlockOptionalMetaLookup(*block)
    fun count() = player.inventory.main.sumOf { it.takeIf { items.contains(it.item) }?.count ?: 0 }

    while (count() < quantity) {
        if (!baritone.mineProcess.isActive) {
            try {
                baritone.mineProcess.mine(quantity, blockMeta)
            } catch (_: IllegalStateException) {
                // ignored
            }
        }
        while (true) {
            val dropped = player.world.getEntitiesByType(
                TypeFilter.instanceOf(ItemEntity::class.java),
                player.visibilityBoundingBox
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

            Thread.sleep(Speedrun.MONITOR_INTERVAL)
        }
        Thread.sleep(Speedrun.MONITOR_INTERVAL)
    }

    c.resumeWith(Result.success(player.inventory.main.filter { items.contains(it.item) }))
}

fun craft(
    player: ClientPlayerEntity,
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
            Thread.sleep(Speedrun.INTERACTION_INTERVAL)
        }
        clickSlot(
            screenHandler.syncId,
            screenHandler.craftingResultSlotIndex,
            0,
            SlotActionType.PICKUP,
            player
        )
        Thread.sleep(Speedrun.INTERACTION_INTERVAL)
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


fun place(player: ClientPlayerEntity, item: ItemStack, position: BlockPos? = null): BlockPos {
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
    player.baritone().builderProcess.build(
        "something",
        FillSchematic(1, 1, 1, BlockOptionalMeta(Block.getBlockFromItem(item.item))),
        pos
    )
    while (player.world.getBlockState(BlockPos(pos)).isAir) {
        Thread.sleep(Speedrun.MONITOR_INTERVAL)
    }
    return BlockPos(pos)
}

fun <T : ScreenHandler> openBlockInventory(player: ClientPlayerEntity, blockPos: BlockPos): HandledScreen<T> {
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
                player.sendMessage(Text.literal("Insane: Waiting for inventory to open"))
            }
        } else {
            break
        }
        Thread.sleep(Speedrun.RETRY_INTERVAL)
        retries++
    }
    while (client.currentScreen == null) {
        Thread.sleep(Speedrun.MONITOR_INTERVAL)
    }
    return client.currentScreen!! as HandledScreen<T>
}

fun putIn(player: ClientPlayerEntity, playerSource: Int, targetSlot: Int, handler: ScreenHandler, half: Boolean = false) {
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

fun takeOut(player: ClientPlayerEntity, invSource: Int, playerSlot: Int, handler: ScreenHandler, half: Boolean = false): ItemStack {
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

private fun sortInventory(player: ClientPlayerEntity, source: Int, target: Int) {
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

suspend fun <T> AbstractGoal.useRuntime(vararg goal: AbstractGoal?, block: suspend () -> T): T {
    try {
        goal.forEach { it?.runtime = this.runtime }
        return block()
    } finally {
        goal.forEach { it?.runtime = null }
    }
}

fun <T> AbstractGoal.useRuntimeBlocked(vararg goal: AbstractGoal?, block: () -> T): T {
    return runBlocking {
        useRuntime(*goal, block = block)
    }
}
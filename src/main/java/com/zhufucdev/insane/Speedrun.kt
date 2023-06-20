package com.zhufucdev.insane

import baritone.api.BaritoneAPI
import baritone.api.IBaritone
import baritone.api.pathing.goals.GoalComposite
import baritone.api.pathing.goals.GoalGetToBlock
import baritone.api.pathing.goals.GoalNear
import baritone.api.pathing.goals.GoalYLevel
import baritone.api.schematic.FillSchematic
import baritone.api.utils.BlockOptionalMeta
import baritone.api.utils.BlockOptionalMetaLookup
import baritone.api.utils.Rotation
import com.zhufucdev.insane.ai.Schedular
import com.zhufucdev.insane.ai.StartOverGoal
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
import kotlin.math.*

class Speedrun(private val source: FabricClientCommandSource) : ISpeedrun {
    private val schedular = Schedular(source.player, 3)
    init {
        BaritoneAPI.getSettings().apply {
            allowSprint.value = true
        }
    }

    override val runner: FabricClientCommandSource
        get() = source

    override fun start(clear: Boolean) {
        if (clear) {
            schedular.start(StartOverGoal())
        }

        source.sendFeedback(Text.literal("Speedrun completed. You are now Dream."))
    }

    override fun stop(): Boolean {
        if (schedular.currentGoal == null)
            return false
        schedular.cancel()
        return true
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

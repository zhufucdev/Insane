package com.zhufucdev.insane

import baritone.api.BaritoneAPI
import com.zhufucdev.insane.ai.Schedular
import com.zhufucdev.insane.ai.StartOverGoal
import com.zhufucdev.insane.state.ISpeedrun
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.text.Text

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

package com.zhufucdev.insane

import baritone.api.BaritoneAPI
import baritone.api.IBaritone
import com.zhufucdev.insane.state.ISpeedrun
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import java.util.*

class Speedrun(private val source: FabricClientCommandSource) : ISpeedrun {
    private val player: ClientPlayerEntity
    private val baritone: IBaritone
    override fun getRunner(): FabricClientCommandSource {
        return source
    }

    private val ridOfExistingItems: Unit
        private get() {
            if (player.inventory.containsAny { itemStack: ItemStack -> Arrays.stream(WOOD_ID).anyMatch { id: Identifier? -> itemStack.registryEntry.matchesId(id) } }) {
            }
        }

    override fun start() {
        if (!player.inventory.isEmpty) {
            ridOfExistingItems
        }
    }

    override fun stop() {}

    init {
        baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(source.player)
        player = source.player
    }

    private fun isWood(item: ItemStack): Boolean {}

    companion object {
        private val WOOD_ID = arrayOf(
                Identifier("minecraft:oak_log")
        )
    }
}

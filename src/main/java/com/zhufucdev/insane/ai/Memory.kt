package com.zhufucdev.insane.ai

import net.minecraft.item.Item
import net.minecraft.util.math.BlockPos

class Memory {
    private val store = mutableMapOf<MemorizedItem<*, *>, MutableMap<*, *>>()
    @Suppress("UNCHECKED_CAST")
    fun <K, V> remember(type: MemorizedItem<K, V>, key: K, value: V) {
        if (store.containsKey(type)) {
            (store[type] as MutableMap<K, V>)[key] = value
        } else {
            store[type] = mutableMapOf(key to value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <K, V> think(type: MemorizedItem<K, V>, key: K): V? {
        return (store[type] as Map<K, V>)[key]
    }
}

sealed class MemorizedItem<K, V>(val keyType: Class<K>, val valueType: Class<V>)

object BlockPosMem : MemorizedItem<Item, BlockPos>(Item::class.java, BlockPos::class.java)
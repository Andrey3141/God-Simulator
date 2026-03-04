package com.printer.godsimulator

import android.graphics.Color
import kotlin.math.*
import kotlin.random.Random

enum class TileType(val color: Int) {
    GRASS(Color.rgb(34, 139, 34)),
    SAND(Color.rgb(194, 178, 128)),
    DIRT(Color.rgb(101, 67, 33)),
    WATER(Color.rgb(64, 164, 223)),
    STONE(Color.rgb(128, 128, 128)),
    SNOW(Color.rgb(255, 255, 255)),
    FOREST(Color.rgb(0, 100, 0)),
    DESERT(Color.rgb(238, 203, 173))
}

data class Tile(
    var type: TileType,
    var treeCount: Int = 0
)

class World(private val spriteManager: SpriteManager) {

    private val chunks = mutableMapOf<Pair<Int, Int>, Array<Array<Tile>>>()
    val chunkSize = 32

    private val creaturesByChunk = mutableMapOf<Pair<Int, Int>, MutableList<Creature>>()
    val allCreatures: List<Creature> get() = creaturesByChunk.values.flatten()

    private val chickensByChunk = mutableMapOf<Pair<Int, Int>, MutableList<Chicken>>()
    val allChickens: List<Chicken> get() = chickensByChunk.values.flatten()

    private val seed = Random.nextInt()

    init {
        // Генерируем центральные чанки
        for (cx in -2..2) {
            for (cy in -2..2) {
                getOrCreateChunk(cx, cy)
            }
        }
        spawnInitialCreatures(0, 0)
    }

    fun getTile(x: Int, y: Int): Tile {
        val chunkX = floor(x.toDouble() / chunkSize).toInt()
        val chunkY = floor(y.toDouble() / chunkSize).toInt()

        var localX = x - chunkX * chunkSize
        var localY = y - chunkY * chunkSize

        if (localX < 0) localX += chunkSize
        if (localY < 0) localY += chunkSize
        if (localX >= chunkSize) localX -= chunkSize
        if (localY >= chunkSize) localY -= chunkSize

        val chunk = getOrCreateChunk(chunkX, chunkY)
        return chunk[localX][localY]
    }

    private fun getOrCreateChunk(chunkX: Int, chunkY: Int): Array<Array<Tile>> {
        return chunks.getOrPut(Pair(chunkX, chunkY)) {
            generateChunk(chunkX, chunkY)
        }
    }

    private fun generateChunk(chunkX: Int, chunkY: Int): Array<Array<Tile>> {
        return Array(chunkSize) { x ->
            Array(chunkSize) { y ->
                val worldX = chunkX * chunkSize + x
                val worldY = chunkY * chunkSize + y
                generateTile(worldX, worldY)
            }
        }
    }

    private fun generateTile(x: Int, y: Int): Tile {
        val noise = (sin(x * 0.01) * cos(y * 0.01) +
                sin(x * 0.02 + seed) * cos(y * 0.02 + seed) +
                sin(x * 0.05) * sin(y * 0.05)) / 3.0
        val temp = (sin(y * 0.005) + 1) / 2

        return when {
            noise < -0.3 -> Tile(TileType.WATER)
            noise < -0.2 -> Tile(TileType.SAND)
            temp < 0.2 -> Tile(TileType.SNOW)
            temp > 0.8 && noise > 0.1 -> Tile(TileType.DESERT)
            noise > 0.3 && temp in 0.3..0.7 -> Tile(TileType.FOREST).apply { treeCount = (noise * 10).toInt().coerceIn(3, 10) }
            abs(noise) > 0.6 -> Tile(TileType.STONE)
            else -> if (Random.nextInt(100) < 70) Tile(TileType.GRASS) else Tile(TileType.DIRT)
        }
    }

    private fun spawnInitialCreatures(chunkX: Int, chunkY: Int) {
        val chickens = mutableListOf<Chicken>()
        val creatures = mutableListOf<Creature>()
        val baseX = chunkX * chunkSize
        val baseY = chunkY * chunkSize

        repeat(10) {
            val x = baseX + Random.nextInt(chunkSize)
            val y = baseY + Random.nextInt(chunkSize)
            if (getTile(x, y).type != TileType.WATER) {
                Chicken(x, y, spriteManager).also { it.world = this@World; chickens.add(it) }
            }
        }

        chickensByChunk[Pair(chunkX, chunkY)] = chickens
        creaturesByChunk[Pair(chunkX, chunkY)] = creatures
    }

    fun updateVisibleChunks(visibleChunks: Set<Pair<Int, Int>>) {
        for (chunk in visibleChunks) {
            if (chunk !in chunks) {
                getOrCreateChunk(chunk.first, chunk.second)
            }
        }
    }

    fun getChickensInChunks(chunks: Set<Pair<Int, Int>>): List<Chicken> =
        chunks.flatMap { chickensByChunk[it] ?: emptyList() }

    fun getCreaturesInChunks(chunks: Set<Pair<Int, Int>>): List<Creature> =
        chunks.flatMap { creaturesByChunk[it] ?: emptyList() }
}
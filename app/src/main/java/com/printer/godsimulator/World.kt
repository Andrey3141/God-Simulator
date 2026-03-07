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
    var treeCount: Int = 0,
    var grassGrowth: Float = 0f
)

class World(private val spriteManager: SpriteManager) {
    // ✅ PUBLIC для доступа из WorldSave и GameView
    val chunks = mutableMapOf<Pair<Int, Int>, Array<Array<Tile>>>()
    val chunkSize = GameConfig.CHUNK_SIZE

    val creaturesByChunk = mutableMapOf<Pair<Int, Int>, MutableList<Creature>>()
    val allCreatures: List<Creature> get() = creaturesByChunk.values.flatten()

    val chickensByChunk = mutableMapOf<Pair<Int, Int>, MutableList<Chicken>>()
    val allChickens: List<Chicken> get() = chickensByChunk.values.flatten()

    // ✅ Отслеживаем "открытые" чанки (которые игрок хотя бы раз видел)
    val discoveredChunks = mutableSetOf<Pair<Int, Int>>()

    private val seed = Random.nextInt()
    private var lastGrassGrowthTime = System.currentTimeMillis()

    init {
        // Генерируем центральные чанки при создании мира
        for (cx in -GameConfig.INITIAL_CHUNK_RADIUS..GameConfig.INITIAL_CHUNK_RADIUS) {
            for (cy in -GameConfig.INITIAL_CHUNK_RADIUS..GameConfig.INITIAL_CHUNK_RADIUS) {
                getOrCreateChunk(cx, cy)
                discoveredChunks.add(Pair(cx, cy))
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
        // ✅ 1. Сначала генерируем ВСЕ тайлы чанка
        val chunk = Array(chunkSize) { x ->
            Array(chunkSize) { y ->
                val worldX = chunkX * chunkSize + x
                val worldY = chunkY * chunkSize + y
                generateTile(worldX, worldY)
            }
        }

        // ✅ 2. Потом спавним кур, передавая готовый чанк (НЕ через getTile!)
        spawnChickensInChunk(chunkX, chunkY, chunk)

        return chunk
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
            noise > 0.3 && temp in 0.3..0.7 -> Tile(TileType.FOREST).apply {
                treeCount = (noise * 10).toInt().coerceIn(3, 10)
            }
            abs(noise) > 0.6 -> Tile(TileType.STONE)
            // ✅ Больше травы (85%), меньше грязи (15%)
            else -> if (Random.nextInt(100) < 85) Tile(TileType.GRASS) else Tile(TileType.DIRT)
        }
    }

    // ✅ ИСПРАВЛЕНО: принимаем готовый чанк, не вызываем getTile()
    private fun spawnChickensInChunk(chunkX: Int, chunkY: Int, chunk: Array<Array<Tile>>) {
        val chickens = mutableListOf<Chicken>()
        val chickenCount = Random.nextInt(GameConfig.CHICKENS_PER_CHUNK_MIN, GameConfig.CHICKENS_PER_CHUNK_MAX + 1)

        repeat(chickenCount) {
            var attempts = 0
            while (attempts < 30) {  // ✅ Увеличили попытки для надёжности
                val localX = Random.nextInt(chunkSize)
                val localY = Random.nextInt(chunkSize)
                val tile = chunk[localX][localY]

                // ✅ ИСПРАВЛЕНИЕ: Куры спавнятся ТОЛЬКО на траве, песке или грязи
                // ❌ Не спавнятся на: воде, лесе, камне, снегу, пустыне
                if (tile.type == TileType.GRASS || tile.type == TileType.SAND || tile.type == TileType.DIRT) {
                    val worldX = chunkX * chunkSize + localX
                    val worldY = chunkY * chunkSize + localY
                    Chicken(worldX, worldY, spriteManager).also {
                        it.world = this@World
                        chickens.add(it)
                    }
                    break
                }
                attempts++
            }
        }

        chickensByChunk[Pair(chunkX, chunkY)] = chickens
    }

    private fun spawnInitialCreatures(chunkX: Int, chunkY: Int) {
        creaturesByChunk[Pair(chunkX, chunkY)] = mutableListOf()
    }

    // ✅ Обновляем список "открытых" чанков
    fun markChunksAsDiscovered(visibleChunks: Set<Pair<Int, Int>>) {
        for (chunk in visibleChunks) {
            if (chunk !in chunks) {
                getOrCreateChunk(chunk.first, chunk.second)
            }
            discoveredChunks.add(chunk)
        }
    }

    // ✅ Обновляем рост травы
    fun updateGrassGrowth() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGrassGrowthTime < GameConfig.GRASS_GROWTH_TICK_MS) return

        lastGrassGrowthTime = currentTime

        for (chunk in chunks.values) {
            for (x in 0 until chunkSize) {
                for (y in 0 until chunkSize) {
                    val tile = chunk[x][y]

                    // ✅ Трава растёт
                    if (tile.type == TileType.GRASS && tile.grassGrowth < 100f) {
                        if (Random.nextFloat() < GameConfig.GRASS_GROWTH_CHANCE) {
                            tile.grassGrowth += 10f
                        }
                    }

                    // ✅ Трава может превратиться в лес
                    if (tile.type == TileType.GRASS && tile.grassGrowth >= 100f) {
                        if (Random.nextFloat() < GameConfig.GRASS_TO_FOREST_CHANCE) {
                            tile.type = TileType.FOREST
                            tile.treeCount = Random.nextInt(1, 4)
                            tile.grassGrowth = 0f
                        }
                    }

                    // ✅ Грязь зарастает травой
                    if (tile.type == TileType.DIRT) {
                        if (Random.nextFloat() < GameConfig.GRASS_GROWTH_CHANCE / 2) {
                            tile.type = TileType.GRASS
                            tile.grassGrowth = 50f
                        }
                    }
                }
            }
        }
    }

    fun updateVisibleChunks(visibleChunks: Set<Pair<Int, Int>>) {
        markChunksAsDiscovered(visibleChunks)
        updateGrassGrowth()

        // Генерируем соседние чанки заранее
        for (chunk in visibleChunks) {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    getOrCreateChunk(chunk.first + dx, chunk.second + dy)
                }
            }
        }
    }

    fun getChickensInChunks(chunks: Set<Pair<Int, Int>>): List<Chicken> =
        chunks.flatMap { chickensByChunk[it] ?: emptyList() }

    fun getCreaturesInChunks(chunks: Set<Pair<Int, Int>>): List<Creature> =
        chunks.flatMap { creaturesByChunk[it] ?: emptyList() }

    // ✅ Счётчик только открытых кур (из посещённых чанков)
    fun getDiscoveredChickenCount(): Int {
        return chickensByChunk
            .filter { it.key in discoveredChunks }
            .values
            .flatten()
            .size
    }

    // ✅ Общий счётчик всех кур (включая ещё не открытые)
    fun getTotalChickenCount(): Int = allChickens.size
}
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
    DESERT(Color.rgb(238, 203, 173)),
    TREE(Color.rgb(0, 150, 0))
}

data class Tile(
    var type: TileType,
    var treeCount: Int = 0,
    var grassGrowth: Float = 0f
)

class World(private val spriteManager: SpriteManager) {
    val chunks = mutableMapOf<Pair<Int, Int>, Array<Array<Tile>>>()
    val chunkSize = GameConfig.CHUNK_SIZE

    val creaturesByChunk = mutableMapOf<Pair<Int, Int>, MutableList<Creature>>()
    val allCreatures: List<Creature> get() = creaturesByChunk.values.flatten()

    val chickensByChunk = mutableMapOf<Pair<Int, Int>, MutableList<Chicken>>()
    val allChickens: List<Chicken> get() = chickensByChunk.values.flatten()

    val discoveredChunks = mutableSetOf<Pair<Int, Int>>()

    private val seed = Random.nextInt()
    private var lastGrassGrowthTime = System.currentTimeMillis()

    init {
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
        return getOrCreateChunk(chunkX, chunkY)[localX][localY]
    }

    private fun getOrCreateChunk(chunkX: Int, chunkY: Int): Array<Array<Tile>> {
        return chunks.getOrPut(Pair(chunkX, chunkY)) { generateChunk(chunkX, chunkY) }
    }

    private fun generateChunk(chunkX: Int, chunkY: Int): Array<Array<Tile>> {
        val chunk = Array(chunkSize) { x ->
            Array(chunkSize) { y ->
                val worldX = chunkX * chunkSize + x
                val worldY = chunkY * chunkSize + y
                generateTile(worldX, worldY)
            }
        }
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
            temp > 0.8 && noise > 0.1 -> Tile(TileType.SAND)
            noise > 0.3 && temp in 0.3..0.7 -> {
                if (Random.nextFloat() < 0.3f) Tile(TileType.TREE) else Tile(TileType.GRASS)
            }
            abs(noise) > 0.6 -> Tile(TileType.STONE)
            else -> if (Random.nextInt(100) < 85) Tile(TileType.GRASS) else Tile(TileType.DIRT)
        }
    }

    private fun spawnChickensInChunk(chunkX: Int, chunkY: Int, chunk: Array<Array<Tile>>) {
        val chickens = mutableListOf<Chicken>()
        val potentialCount = Random.nextInt(GameConfig.CHICKENS_PER_CHUNK_MIN, GameConfig.CHICKENS_PER_CHUNK_MAX + 1)

        repeat(potentialCount) {
            if (Random.nextFloat() >= GameConfig.CHICKEN_SPAWN_CHANCE) return@repeat

            var attempts = 0
            while (attempts < 20) {
                val localX = Random.nextInt(chunkSize)
                val localY = Random.nextInt(chunkSize)
                val tile = chunk[localX][localY]

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

    // ✅ НОВЫЙ МЕТОД: Расчёт шанса роста дерева на основе соседей
    private fun calculateTreeGrowthChance(chunk: Array<Array<Tile>>, x: Int, y: Int): Float {
        var totalChance = GameConfig.GRASS_TO_TREE_CHANCE_BASE

        // Проверяем 4 соседей (верх, низ, лево, право)
        val directions = listOf(Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0))

        for ((dx, dy) in directions) {
            val nx = x + dx
            val ny = y + dy

            // Проверяем границы чанка
            if (nx in 0 until chunkSize && ny in 0 until chunkSize) {
                val neighbor = chunk[nx][ny]

                when (neighbor.type) {
                    // ✅ Грязь: +6% к шансу
                    TileType.DIRT -> totalChance += GameConfig.TREE_GROWTH_BONUS_DIRT
                    // ✅ Трава: +5% к шансу (кумулятивно)
                    TileType.GRASS -> totalChance += GameConfig.TREE_GROWTH_BONUS_GRASS
                    // ✅ Песок: +0.1% к шансу
                    TileType.SAND -> totalChance += GameConfig.TREE_GROWTH_BONUS_SAND
                    // ❌ Вода/Снег/Камень: +0% (не влияют)
                    TileType.WATER, TileType.SNOW, TileType.STONE -> {}
                    // ❌ Дерево: не влияет на рост нового дерева рядом
                    TileType.TREE, TileType.FOREST, TileType.DESERT -> {}
                }
            }
        }

        // Ограничиваем шанс максимумом
        return totalChance.coerceAtMost(GameConfig.TREE_GROWTH_CHANCE_MAX)
    }

    fun convertGrassToDirt(x: Int, y: Int) {
        val chunkX = floor(x.toDouble() / chunkSize).toInt()
        val chunkY = floor(y.toDouble() / chunkSize).toInt()
        var localX = x - chunkX * chunkSize
        var localY = y - chunkY * chunkSize
        if (localX < 0) localX += chunkSize
        if (localY < 0) localY += chunkSize
        if (localX >= chunkSize) localX -= chunkSize
        if (localY >= chunkSize) localY -= chunkSize

        val chunk = chunks[Pair(chunkX, chunkY)] ?: return
        val tile = chunk[localX][localY]

        if (tile.type == TileType.GRASS) {
            tile.type = TileType.DIRT
            tile.grassGrowth = 0f
        }
    }

    fun markChunksAsDiscovered(visibleChunks: Set<Pair<Int, Int>>) {
        for (chunk in visibleChunks) {
            if (chunk !in chunks) getOrCreateChunk(chunk.first, chunk.second)
            discoveredChunks.add(chunk)
        }
    }

    fun updateGrassGrowth() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGrassGrowthTime < GameConfig.GRASS_GROWTH_TICK_MS) return
        lastGrassGrowthTime = currentTime

        for ((_, chunk) in chunks) {
            for (x in 0 until chunkSize) {
                for (y in 0 until chunkSize) {
                    val tile = chunk[x][y]

                    // ✅ Рост травы
                    if (tile.type == TileType.GRASS && tile.grassGrowth < 100f) {
                        if (Random.nextFloat() < GameConfig.GRASS_GROWTH_CHANCE) {
                            tile.grassGrowth += 10f
                        }
                    }

                    // ✅ Превращение травы в ДЕРЕВО (с учётом соседей)
                    if (tile.type == TileType.GRASS && tile.grassGrowth >= 100f) {
                        val treeChance = calculateTreeGrowthChance(chunk, x, y)
                        if (Random.nextFloat() < treeChance) {
                            tile.type = TileType.TREE
                            tile.grassGrowth = 0f
                        }
                    }

                    // ✅ Зарастание грязи травой
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

    fun getDiscoveredChickenCount(): Int {
        return chickensByChunk.filter { it.key in discoveredChunks }.values.flatten().size
    }

    fun getTotalChickenCount(): Int = allChickens.size
}
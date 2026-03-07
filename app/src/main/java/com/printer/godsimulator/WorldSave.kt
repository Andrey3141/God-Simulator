package com.printer.godsimulator
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object WorldSave {
    private const val PREFS_NAME = "god_simulator_save"
    private const val KEY_WORLD_DATA = "world_data"
    private const val KEY_AUTO_SAVE = "auto_save_enabled"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoSaveEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SAVE, true)
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SAVE, enabled).apply()
    }

    fun saveWorld(world: World) {
        val jsonData = JSONObject()

        // Сохраняем чанки
        val chunksArray = JSONArray()
        for ((chunkPos, chunk) in world.chunks) {
            val chunkObj = JSONObject()
            chunkObj.put("chunkX", chunkPos.first)
            chunkObj.put("chunkY", chunkPos.second)

            val tilesArray = JSONArray()
            for (x in 0 until world.chunkSize) {
                for (y in 0 until world.chunkSize) {
                    val tile = chunk[x][y]
                    val tileObj = JSONObject()
                    tileObj.put("type", tile.type.name)
                    tileObj.put("treeCount", tile.treeCount)
                    tileObj.put("grassGrowth", tile.grassGrowth)
                    tilesArray.put(tileObj)
                }
            }
            chunkObj.put("tiles", tilesArray)
            chunksArray.put(chunkObj)
        }
        jsonData.put("chunks", chunksArray)

        // Сохраняем кур
        val chickensArray = JSONArray()
        for ((chunkPos, chickens) in world.chickensByChunk) {
            for (chicken in chickens) {
                val chickenObj = JSONObject()
                chickenObj.put("chunkX", chunkPos.first)
                chickenObj.put("chunkY", chunkPos.second)
                chickenObj.put("tileX", chicken.tileX)
                chickenObj.put("tileY", chicken.tileY)
                chickenObj.put("direction", chicken.direction.name)
                chickenObj.put("state", chicken.state.name)
                chickensArray.put(chickenObj)
            }
        }
        jsonData.put("chickens", chickensArray)

        // Сохраняем существ
        val creaturesArray = JSONArray()
        for ((chunkPos, creatures) in world.creaturesByChunk) {
            for (creature in creatures) {
                val creatureObj = JSONObject()
                creatureObj.put("chunkX", chunkPos.first)
                creatureObj.put("chunkY", chunkPos.second)
                creatureObj.put("tileX", creature.tileX)
                creatureObj.put("tileY", creature.tileY)
                creatureObj.put("type", creature.type.name)
                creatureObj.put("gender", creature.gender.name)
                creatureObj.put("age", creature.age)
                creatureObj.put("health", creature.health)
                creatureObj.put("hunger", creature.hunger)
                creaturesArray.put(creatureObj)
            }
        }
        jsonData.put("creatures", creaturesArray)

        // Сохраняем открытые чанки
        val discoveredArray = JSONArray()
        for (chunk in world.discoveredChunks) {
            val chunkObj = JSONObject()
            chunkObj.put("chunkX", chunk.first)
            chunkObj.put("chunkY", chunk.second)
            discoveredArray.put(chunkObj)
        }
        jsonData.put("discoveredChunks", discoveredArray)

        prefs.edit().putString(KEY_WORLD_DATA, jsonData.toString()).apply()
    }

    fun loadWorld(world: World, spriteManager: SpriteManager): Boolean {
        val jsonDataStr = prefs.getString(KEY_WORLD_DATA, null) ?: return false
        val jsonData = JSONObject(jsonDataStr)

        // Загружаем чанки
        val chunksArray = jsonData.getJSONArray("chunks")
        for (i in 0 until chunksArray.length()) {
            val chunkObj = chunksArray.getJSONObject(i)
            val chunkX = chunkObj.getInt("chunkX")
            val chunkY = chunkObj.getInt("chunkY")

            val chunk = Array(world.chunkSize) { x ->
                Array(world.chunkSize) { y ->
                    Tile(TileType.GRASS)
                }
            }

            val tilesArray = chunkObj.getJSONArray("tiles")
            for (x in 0 until world.chunkSize) {
                for (y in 0 until world.chunkSize) {
                    val tileObj = tilesArray.getJSONObject(x * world.chunkSize + y)
                    chunk[x][y] = Tile(
                        TileType.valueOf(tileObj.getString("type")),
                        tileObj.getInt("treeCount"),
                        tileObj.getDouble("grassGrowth").toFloat()
                    )
                }
            }

            world.chunks[Pair(chunkX, chunkY)] = chunk
        }

        // Загружаем кур
        val chickensArray = jsonData.getJSONArray("chickens")
        for (i in 0 until chickensArray.length()) {
            val chickenObj = chickensArray.getJSONObject(i)
            val chunkX = chickenObj.getInt("chunkX")
            val chunkY = chickenObj.getInt("chunkY")
            val tileX = chickenObj.getInt("tileX")
            val tileY = chickenObj.getInt("tileY")

            val chicken = Chicken(tileX, tileY, spriteManager)
            chicken.direction = Direction.valueOf(chickenObj.getString("direction"))
            chicken.state = Chicken.State.valueOf(chickenObj.getString("state"))

            val chunkPos = Pair(chunkX, chunkY)
            if (world.chickensByChunk[chunkPos] == null) {
                world.chickensByChunk[chunkPos] = mutableListOf()
            }
            world.chickensByChunk[chunkPos]!!.add(chicken)
            chicken.world = world
        }

        // Загружаем существ
        val creaturesArray = jsonData.getJSONArray("creatures")
        for (i in 0 until creaturesArray.length()) {
            val creatureObj = creaturesArray.getJSONObject(i)
            val chunkX = creatureObj.getInt("chunkX")
            val chunkY = creatureObj.getInt("chunkY")
            val tileX = creatureObj.getInt("tileX")
            val tileY = creatureObj.getInt("tileY")

            val creature = Creature(
                tileX, tileY,
                CreatureType.valueOf(creatureObj.getString("type")),
                Gender.valueOf(creatureObj.getString("gender"))
            )
            creature.age = creatureObj.getInt("age")
            creature.health = creatureObj.getInt("health")
            creature.hunger = creatureObj.getInt("hunger")

            val chunkPos = Pair(chunkX, chunkY)
            if (world.creaturesByChunk[chunkPos] == null) {
                world.creaturesByChunk[chunkPos] = mutableListOf()
            }
            world.creaturesByChunk[chunkPos]!!.add(creature)
        }

        // Загружаем открытые чанки
        val discoveredArray = jsonData.getJSONArray("discoveredChunks")
        for (i in 0 until discoveredArray.length()) {
            val chunkObj = discoveredArray.getJSONObject(i)
            val chunkX = chunkObj.getInt("chunkX")
            val chunkY = chunkObj.getInt("chunkY")
            world.discoveredChunks.add(Pair(chunkX, chunkY))
        }

        return true
    }

    fun clearSave() {
        prefs.edit().remove(KEY_WORLD_DATA).apply()
    }
}
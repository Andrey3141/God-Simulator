package com.printer.godsimulator

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.floor

/**
 * 💾 Система сохранений мира
 *
 * Сохраняет в SharedPreferences:
 * - Чанки и тайлы (тип, количество деревьев, рост травы)
 * - Куриц (позиция, направление, состояние)
 * - Существ (позиция, тип, параметры)
 * - Открытые чанки (для счётчика)
 * - Настройки автосохранения
 *
 * Формат: JSON внутри SharedPreferences
 */
object WorldSave {

    private const val PREFS_NAME = "god_simulator_save"
    private const val KEY_WORLD_DATA = "world_data"
    private const val KEY_AUTO_SAVE = "auto_save_enabled"
    private const val KEY_VERSION = "save_version"

    // ✅ Текущая версия сохранения (для миграции)
    private const val CURRENT_SAVE_VERSION = 2

    private lateinit var prefs: SharedPreferences

    /**
     * ✅ Инициализация системы сохранений
     * Вызывать один раз при старте приложения
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * ✅ Проверка: включено ли автосохранение
     */
    fun isAutoSaveEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SAVE, true)
    }

    /**
     * ✅ Включение/выключение автосохранения
     */
    fun setAutoSaveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SAVE, enabled).apply()
    }

    /**
     * ✅ Сохранение всего мира
     * @param world объект мира для сохранения
     */
    fun saveWorld(world: World) {
        val jsonData = JSONObject()

        // ✅ Версия сохранения
        jsonData.put(KEY_VERSION, CURRENT_SAVE_VERSION)

        // ✅ Сохраняем чанки и тайлы
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

        // ✅ Сохраняем куриц
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

        // ✅ Сохраняем существ
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
                creatureObj.put("speed", creature.speed)
                creatureObj.put("state", creature.state.name)
                creaturesArray.put(creatureObj)
            }
        }
        jsonData.put("creatures", creaturesArray)

        // ✅ Сохраняем открытые чанки (для счётчика)
        val discoveredArray = JSONArray()
        for (chunk in world.discoveredChunks) {
            val chunkObj = JSONObject()
            chunkObj.put("x", chunk.first)
            chunkObj.put("y", chunk.second)
            discoveredArray.put(chunkObj)
        }
        jsonData.put("discoveredChunks", discoveredArray)

        // ✅ Сохраняем в SharedPreferences
        prefs.edit()
            .putString(KEY_WORLD_DATA, jsonData.toString())
            .putInt(KEY_VERSION, CURRENT_SAVE_VERSION)
            .apply()
    }

    /**
     * ✅ Загрузка мира из сохранения
     * @param world объект мира для заполнения
     * @param spriteManager для загрузки спрайтов куриц
     * @return true если загрузка успешна, false если сохранения нет
     */
    fun loadWorld(world: World, spriteManager: SpriteManager): Boolean {
        if (!prefs.contains(KEY_WORLD_DATA)) return false

        return try {
            val jsonData = JSONObject(prefs.getString(KEY_WORLD_DATA, "{}"))
            val saveVersion = jsonData.optInt(KEY_VERSION, 1)

            // ✅ Загружаем чанки и тайлы
            val chunksArray = jsonData.getJSONArray("chunks")
            for (i in 0 until chunksArray.length()) {
                val chunkObj = chunksArray.getJSONObject(i)
                val chunkX = chunkObj.getInt("chunkX")
                val chunkY = chunkObj.getInt("chunkY")

                val chunk = world.getOrCreateChunk(chunkX, chunkY)
                val tilesArray = chunkObj.getJSONArray("tiles")

                for (x in 0 until world.chunkSize) {
                    for (y in 0 until world.chunkSize) {
                        val index = x * world.chunkSize + y
                        if (index < tilesArray.length()) {
                            val tileObj = tilesArray.getJSONObject(index)
                            val tile = chunk[x][y]

                            // ✅ Безопасный парсинг типа тайла
                            tile.type = safeValueOf(tileObj.getString("type"), TileType.GRASS)
                            tile.treeCount = tileObj.optInt("treeCount", 0)
                            tile.grassGrowth = tileObj.optDouble("grassGrowth", 0.0).toFloat()
                        }
                    }
                }
            }

            // ✅ Загружаем куриц
            val chickensArray = jsonData.getJSONArray("chickens")
            for (i in 0 until chickensArray.length()) {
                val chickenObj = chickensArray.getJSONObject(i)
                val chunkX = chickenObj.getInt("chunkX")
                val chunkY = chickenObj.getInt("chunkY")

                val chicken = Chicken(
                    chickenObj.getInt("tileX"),
                    chickenObj.getInt("tileY"),
                    spriteManager
                )
                chicken.world = world
                chicken.direction = safeValueOf(chickenObj.getString("direction"), Direction.DOWN)

                // ✅ Безопасный парсинг состояния курицы (для обратной совместимости)
                chicken.state = safeValueOf(chickenObj.getString("state"), Chicken.State.IDLE)

                // ✅ Добавляем курицу в правильный чанк
                val chunkKey = Pair(chunkX, chunkY)
                world.chickensByChunk.getOrPut(chunkKey) { mutableListOf() }.add(chicken)
            }

            // ✅ Загружаем существ
            val creaturesArray = jsonData.getJSONArray("creatures")
            for (i in 0 until creaturesArray.length()) {
                val creatureObj = creaturesArray.getJSONObject(i)
                val chunkX = creatureObj.getInt("chunkX")
                val chunkY = creatureObj.getInt("chunkY")

                val creature = Creature(
                    creatureObj.getInt("tileX"),
                    creatureObj.getInt("tileY"),
                    safeValueOf(creatureObj.getString("type"), CreatureType.HUMAN),
                    safeValueOf(creatureObj.getString("gender"), Gender.MALE)
                )
                creature.age = creatureObj.optInt("age", 20)
                creature.health = creatureObj.optInt("health", 100)
                creature.hunger = creatureObj.optInt("hunger", 50)
                creature.speed = creatureObj.optDouble("speed", 1.5).toFloat()
                creature.state = safeValueOf(creatureObj.getString("state"), Creature.State.IDLE)

                // ✅ Добавляем существо в правильный чанк
                val chunkKey = Pair(chunkX, chunkY)
                world.creaturesByChunk.getOrPut(chunkKey) { mutableListOf() }.add(creature)
            }

            // ✅ Загружаем открытые чанки
            val discoveredArray = jsonData.optJSONArray("discoveredChunks")
            if (discoveredArray != null) {
                for (i in 0 until discoveredArray.length()) {
                    val chunkObj = discoveredArray.getJSONObject(i)
                    world.discoveredChunks.add(
                        Pair(chunkObj.getInt("x"), chunkObj.getInt("y"))
                    )
                }
            }

            true
        } catch (e: Exception) {
            // ✅ Если ошибка при загрузке — возвращаем false
            e.printStackTrace()
            false
        }
    }

    /**
     * ✅ Очистка сохранения (для кнопки "Новый мир")
     */
    fun clearSave() {
        prefs.edit()
            .remove(KEY_WORLD_DATA)
            .remove(KEY_VERSION)
            .apply()
    }

    /**
     * ✅ Проверка: есть ли сохранение
     */
    fun hasSave(): Boolean {
        return prefs.contains(KEY_WORLD_DATA)
    }

    /**
     * ✅ Универсальная функция для безопасного парсинга enum
     * Возвращает default если значение не найдено (для обратной совместимости)
     */
    private inline fun <reified T : Enum<T>> safeValueOf(name: String, default: T): T {
        return try {
            enumValueOf<T>(name)
        } catch (e: IllegalArgumentException) {
            // ✅ Старое сохранение с неизвестным значением — используем default
            default
        }
    }

    /**
     * ✅ Получение размера сохранения в байтах (для отладки)
     */
    fun getSaveSizeBytes(): Int {
        return prefs.getString(KEY_WORLD_DATA, "")?.length ?: 0
    }

    /**
     * ✅ Экспорт сохранения в строку (для отладки/бэкапа)
     */
    fun exportSave(): String? {
        return prefs.getString(KEY_WORLD_DATA, null)
    }

    /**
     * ✅ Импорт сохранения из строки (для отладки/восстановления)
     */
    fun importSave(jsonString: String): Boolean {
        return try {
            // ✅ Проверяем валидность JSON
            JSONObject(jsonString)
            prefs.edit().putString(KEY_WORLD_DATA, jsonString).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
}
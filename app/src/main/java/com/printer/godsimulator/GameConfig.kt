package com.printer.godsimulator

/**
 * Центральная конфигурация игры
 * Все параметры вынесены сюда для удобной настройки
 */
object GameConfig {

    // 🌍 Мир
    const val CHUNK_SIZE = 32
    const val INITIAL_CHUNK_RADIUS = 2

    // 🌱 Трава
    const val GRASS_GROWTH_TICK_MS = 10000L
    const val GRASS_GROWTH_CHANCE = 0.02f
    const val GRASS_TO_TREE_CHANCE_BASE = 0.005f
    const val TREE_GROWTH_BONUS_DIRT = 0.06f
    const val TREE_GROWTH_BONUS_GRASS = 0.05f
    const val TREE_GROWTH_BONUS_SAND = 0.001f
    const val TREE_GROWTH_CHANCE_MAX = 1.0f

    // 🐔 Курицы
    const val CHICKENS_PER_CHUNK_MIN = 0
    const val CHICKENS_PER_CHUNK_MAX = 25
    const val CHICKEN_SPAWN_CHANCE = 0.03f  // ✅ 3% шанс спавна каждой курицы

    // ✅ НОВЫЕ ПАРАМЕТРЫ ДЛЯ ПОВЕДЕНИЯ КУРИЦ
    /** 💬 Шанс что курица попробует поесть траву при простое (30% = 0.3) */
    const val CHICKEN_EAT_CHANCE = 0.3f

    /** 💬 Длительность поедания травы в кадрах @60FPS (60 кадров = ~1 секунда) */
    const val CHICKEN_EAT_DURATION_FRAMES = 60

    /** 💬 Разрешённые тайлы для спавна и ходьбы куриц (через запятую):
     *  Допустимые значения: GRASS, SAND, DIRT
     *  Запрещённые: WATER, TREE, FOREST, STONE, SNOW, DESERT */
    const val CHICKEN_WALKABLE_TILES = "GRASS,SAND,DIRT"

    const val CHICKEN_IDLE_TIME_MIN = 50
    const val CHICKEN_IDLE_TIME_MAX = 150
    const val CHICKEN_MOVE_SPEED = 0.05f
    const val CHICKEN_WALK_DISTANCE_MIN = 1
    const val CHICKEN_WALK_DISTANCE_MAX = 5

    // 🧍 Существa
    const val CREATURE_IDLE_TIME_MIN = 30
    const val CREATURE_IDLE_TIME_MAX = 90
    const val CREATURE_SPEED_MIN = 1f
    const val CREATURE_SPEED_MAX = 3f
    const val CREATURE_WALK_DISTANCE = 3

    // 🎮 Камера
    const val ZOOM_MIN = 0.5f
    const val ZOOM_MAX = 3.0f
    const val ZOOM_DEFAULT = 1.0f

    // ⚡ Производительность
    const val TARGET_FPS = 60
    const val FRAME_TIME_MS = 16L

    // 🎨 Отрисовка
    const val TILE_SIZE = 24
    const val TREE_MAX_COUNT = 3
}
package com.printer.godsimulator

/**
 * Центральная конфигурация игры
 * Все параметры вынесены сюда для удобной настройки
 */
object GameConfig {

    // 🌍 Мир
    const val CHUNK_SIZE = 32
    const val INITIAL_CHUNK_RADIUS = 2  // Сколько чанков генерировать вокруг центра

    // 🌱 Трава
    const val GRASS_GROWTH_TICK_MS = 10000L  // Проверка роста травы каждые 10 секунд
    const val GRASS_GROWTH_CHANCE = 0.02f   // 2% шанс роста травы за тик
    const val GRASS_TO_FOREST_CHANCE = 0.005f  // 0.5% шанс превращения в лес

    // 🐔 Курицы
    const val CHICKENS_PER_CHUNK_MIN = 0
    const val CHICKENS_PER_CHUNK_MAX = 25
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
    const val FRAME_TIME_MS = 16L  // 1000ms / 60 FPS ≈ 16ms

    // 🎨 Отрисовка
    const val TILE_SIZE = 24
    const val TREE_MAX_COUNT = 3
}
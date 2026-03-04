package com.printer.godsimulator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

enum class Direction {
    UP,    // строка 0 (вверх)
    LEFT,  // строка 1 (влево)
    DOWN,  // строка 2 (вниз)
    RIGHT  // строка 3 (вправо)
}

class SpriteManager(private val context: Context) {

    private val spriteSheets = mutableMapOf<String, Bitmap>()
    private val frames = mutableMapOf<String, List<List<Bitmap>>>() // [direction][frame]

    // Статичные кадры (для состояния покоя) - первый столбец
    private val staticFrames = mutableMapOf<String, List<Bitmap>>() // [direction]

    init {
        loadSpriteSheets()
        cutFrames()
    }

    private fun loadSpriteSheets() {
        spriteSheets["chicken"] = loadBitmap(R.drawable.chicken_walk)
    }

    private fun loadBitmap(resourceId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resourceId)
    }

    private fun cutFrames() {
        spriteSheets["chicken"]?.let { sheet ->
            val frameWidth = sheet.width / 4  // 32 пикселя
            val frameHeight = sheet.height / 4 // 32 пикселя

            val chickenFrames = mutableListOf<List<Bitmap>>()
            val chickenStatic = mutableListOf<Bitmap>()

            // Проходим по направлениям (ряды)
            for (row in 0 until 4) {
                val directionFrames = mutableListOf<Bitmap>()

                // Проходим по кадрам (колонки)
                for (col in 0 until 4) {
                    val frame = Bitmap.createBitmap(sheet,
                        col * frameWidth, row * frameHeight,
                        frameWidth, frameHeight)
                    directionFrames.add(frame)

                    // Статичный кадр - первый столбец (col == 0)
                    if (col == 0) {
                        chickenStatic.add(frame)
                    }
                }
                chickenFrames.add(directionFrames)
            }

            frames["chicken"] = chickenFrames
            staticFrames["chicken"] = chickenStatic
        }
    }

    // Для анимации (все кадры направления)
    fun getChickenFrames(direction: Direction): List<Bitmap> {
        return frames["chicken"]?.get(direction.ordinal) ?: emptyList()
    }

    // Для статичной картинки (первый кадр направления)
    fun getChickenStaticFrame(direction: Direction): Bitmap? {
        return staticFrames["chicken"]?.getOrNull(direction.ordinal)
    }
}
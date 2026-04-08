package com.printer.godsimulator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

enum class Direction {
    UP,
    LEFT,
    DOWN,
    RIGHT
}

class SpriteManager(private val context: Context) {
    private val spriteSheets = mutableMapOf<String, Bitmap>()
    private val frames = mutableMapOf<String, List<List<Bitmap>>>()
    private val eatGrassFrames = mutableMapOf<String, List<List<Bitmap>>>()
    private val eatWormFrames = mutableMapOf<String, List<List<Bitmap>>>()  // ✅ Кадры поедания червяка
    private val staticFrames = mutableMapOf<String, List<Bitmap>>()

    init {
        loadSpriteSheets()
        cutFrames()
        cutEatGrassFrames()
        cutEatWormFrames()
    }

    private fun loadSpriteSheets() {
        spriteSheets["chicken"] = loadBitmap(R.drawable.chicken_walk)
        spriteSheets["chicken_eat"] = loadBitmap(R.drawable.chicken_eat)
        spriteSheets["chicken_eat_worm"] = loadBitmap(R.drawable.chicken_eat)  // ✅ Новая анимация
    }

    private fun loadBitmap(resourceId: Int): Bitmap {
        return BitmapFactory.decodeResource(context.resources, resourceId)
    }

    private fun cutFrames() {
        spriteSheets["chicken"]?.let { sheet ->
            val frameWidth = sheet.width / 4
            val frameHeight = sheet.height / 4
            val chickenFrames = mutableListOf<List<Bitmap>>()
            val chickenStatic = mutableListOf<Bitmap>()

            for (row in 0 until 4) {
                val directionFrames = mutableListOf<Bitmap>()
                for (col in 0 until 4) {
                    val frame = Bitmap.createBitmap(sheet, col * frameWidth, row * frameHeight, frameWidth, frameHeight)
                    directionFrames.add(frame)
                    if (col == 0) chickenStatic.add(frame)
                }
                chickenFrames.add(directionFrames)
            }
            frames["chicken"] = chickenFrames
            staticFrames["chicken"] = chickenStatic
        }
    }

    private fun cutEatGrassFrames() {
        spriteSheets["chicken_eat"]?.let { sheet ->
            val frameWidth = sheet.width / 4
            val frameHeight = sheet.height / 4
            val chickenEatFrames = mutableListOf<List<Bitmap>>()

            for (row in 0 until 4) {
                val directionFrames = mutableListOf<Bitmap>()
                for (col in 0 until 4) {
                    val frame = Bitmap.createBitmap(sheet, col * frameWidth, row * frameHeight, frameWidth, frameHeight)
                    directionFrames.add(frame)
                }
                chickenEatFrames.add(directionFrames)
            }
            eatGrassFrames["chicken_eat"] = chickenEatFrames
        }
    }

    // ✅ Нарезка кадров для анимации поедания червяка
    private fun cutEatWormFrames() {
        spriteSheets["chicken_eat_worm"]?.let { sheet ->
            val frameWidth = sheet.width / 4
            val frameHeight = sheet.height / 4
            val chickenEatWormFrames = mutableListOf<List<Bitmap>>()

            for (row in 0 until 4) {
                val directionFrames = mutableListOf<Bitmap>()
                for (col in 0 until 4) {
                    val frame = Bitmap.createBitmap(sheet, col * frameWidth, row * frameHeight, frameWidth, frameHeight)
                    directionFrames.add(frame)
                }
                chickenEatWormFrames.add(directionFrames)
            }
            eatWormFrames["chicken_eat_worm"] = chickenEatWormFrames
        }
    }

    fun getChickenFrames(direction: Direction): List<Bitmap> {
        return frames["chicken"]?.get(direction.ordinal) ?: emptyList()
    }

    fun getChickenEatFrames(direction: Direction): List<Bitmap> {
        return eatGrassFrames["chicken_eat"]?.get(direction.ordinal) ?: emptyList()
    }

    // ✅ Получение кадров анимации поедания червяка
    fun getChickenEatWormFrames(direction: Direction): List<Bitmap> {
        return eatWormFrames["chicken_eat_worm"]?.get(direction.ordinal) ?: emptyList()
    }

    fun getChickenStaticFrame(direction: Direction): Bitmap? {
        return staticFrames["chicken"]?.getOrNull(direction.ordinal)
    }
}
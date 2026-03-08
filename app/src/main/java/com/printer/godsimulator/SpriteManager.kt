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
    private val eatFrames = mutableMapOf<String, List<List<Bitmap>>>()  // ✅ Кадры еды
    private val staticFrames = mutableMapOf<String, List<Bitmap>>()

    init {
        loadSpriteSheets()
        cutFrames()
        cutEatFrames()
    }

    private fun loadSpriteSheets() {
        spriteSheets["chicken"] = loadBitmap(R.drawable.chicken_walk)
        spriteSheets["chicken_eat"] = loadBitmap(R.drawable.chicken_eat)  // ✅ Загружаем анимацию еды
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

    // ✅ Нарезка кадров для анимации еды
    private fun cutEatFrames() {
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
            eatFrames["chicken_eat"] = chickenEatFrames
        }
    }

    fun getChickenFrames(direction: Direction): List<Bitmap> {
        return frames["chicken"]?.get(direction.ordinal) ?: emptyList()
    }

    // ✅ Получение кадров анимации еды
    fun getChickenEatFrames(direction: Direction): List<Bitmap> {
        return eatFrames["chicken_eat"]?.get(direction.ordinal) ?: emptyList()
    }

    fun getChickenStaticFrame(direction: Direction): Bitmap? {
        return staticFrames["chicken"]?.getOrNull(direction.ordinal)
    }
}
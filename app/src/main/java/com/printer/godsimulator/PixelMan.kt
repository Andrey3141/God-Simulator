package com.printer.godsimulator

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.random.Random

class PixelMan(
    private var tileX: Int,  // координата в тайлах
    private var tileY: Int,
    private val pixelSize: Int = 10,
    private val tileSize: Int = 16  // размер тайла
) {
    // Пиксельные координаты для отрисовки
    private var x: Float = (tileX * tileSize).toFloat()
    private var y: Float = (tileY * tileSize).toFloat()

    private val skinPaint = Paint().apply {
        color = Color.rgb(255, 220, 200)
        style = Paint.Style.FILL
    }

    private val shirtPaint = Paint().apply {
        color = Color.rgb(70, 130, 180)
        style = Paint.Style.FILL
    }

    private val shirtDarkPaint = Paint().apply {
        color = Color.rgb(50, 100, 150)
        style = Paint.Style.FILL
    }

    private val pantsPaint = Paint().apply {
        color = Color.rgb(110, 70, 50)
        style = Paint.Style.FILL
    }

    private val pantsDarkPaint = Paint().apply {
        color = Color.rgb(80, 50, 30)
        style = Paint.Style.FILL
    }

    private val shoesPaint = Paint().apply {
        color = Color.rgb(30, 30, 30)
        style = Paint.Style.FILL
    }

    private val hairPaint = Paint().apply {
        color = Color.rgb(101, 67, 33)
        style = Paint.Style.FILL
    }

    private val beltPaint = Paint().apply {
        color = Color.rgb(139, 69, 19)
        style = Paint.Style.FILL
    }

    private val eyePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val eyeWhitePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var directionX = 0
    private var directionY = 0
    private var changeDirectionCounter = 0
    private val directionChangeInterval = Random.nextInt(30, 90)

    // Флаг движения (анимируем переход между тайлами)
    private var isMoving = false
    private var targetX = x
    private var targetY = y
    private var moveSpeed = 2f  // скорость анимации

    fun update() {
        if (!isMoving) {
            // Стоим на месте - проверяем нужно ли сменить направление
            changeDirectionCounter++
            if (changeDirectionCounter >= directionChangeInterval) {
                changeDirection()
                changeDirectionCounter = 0
            }

            // Начинаем движение в выбранном направлении
            if (directionX != 0 || directionY != 0) {
                startMoving()
            }
        } else {
            // Анимируем движение к следующему тайлу
            moveToTarget()
        }
    }

    private fun changeDirection() {
        directionX = Random.nextInt(-1, 2)
        directionY = Random.nextInt(-1, 2)
    }

    private fun startMoving() {
        val newTileX = tileX + directionX
        val newTileY = tileY + directionY

        // Проверяем границы мира
        if (newTileX in 0 until (maxX / tileSize).toInt() &&
            newTileY in 0 until (maxY / tileSize).toInt()) {

            isMoving = true
            targetX = (newTileX * tileSize).toFloat()
            targetY = (newTileY * tileSize).toFloat()
        } else {
            // Уперлись в стену - меняем направление
            directionX = -directionX
            directionY = -directionY
        }
    }

    private fun moveToTarget() {
        // Плавно двигаемся к цели
        val dx = targetX - x
        val dy = targetY - y

        if (Math.abs(dx) < moveSpeed && Math.abs(dy) < moveSpeed) {
            // Достигли цели
            x = targetX
            y = targetY
            tileX = (x / tileSize).toInt()
            tileY = (y / tileSize).toInt()
            isMoving = false
        } else {
            // Продолжаем движение
            x += dx.coerceIn(-moveSpeed, moveSpeed)
            y += dy.coerceIn(-moveSpeed, moveSpeed)
        }
    }

    fun getTileX(): Int = tileX
    fun getTileY(): Int = tileY

    // Для камеры используем пиксельные координаты
    fun getX(): Float = x
    fun getY(): Float = y

    fun draw(canvas: Canvas) {
        drawShadow(canvas)
        drawLegs(canvas)
        drawPants(canvas)
        drawBelt(canvas)
        drawTorso(canvas)
        drawArms(canvas)
        drawNeck(canvas)
        drawHead(canvas)
        drawHair(canvas)
        drawFace(canvas)
        drawDetails(canvas)
    }

    private fun drawShadow(canvas: Canvas) {
        val shadowPaint = Paint().apply {
            color = Color.rgb(20, 20, 20)
            alpha = 50
        }
        canvas.drawRect(
            x + 2,
            y + pixelSize * 9 + 2,
            x + pixelSize * 5 - 2,
            y + pixelSize * 10 + 2,
            shadowPaint
        )
    }

    private fun drawLegs(canvas: Canvas) {
        drawPixelBlock(canvas, x, y + pixelSize * 9, 2, 1, shoesPaint)
        drawPixelBlock(canvas, x + pixelSize * 3, y + pixelSize * 9, 2, 1, shoesPaint)
        drawPixel(canvas, x + pixelSize, y + pixelSize * 9, Color.WHITE)
        drawPixel(canvas, x + pixelSize * 4, y + pixelSize * 9, Color.WHITE)
    }

    private fun drawPants(canvas: Canvas) {
        drawPixelBlock(canvas, x, y + pixelSize * 7, 5, 2, pantsPaint)
        drawPixel(canvas, x + pixelSize * 2, y + pixelSize * 7, pantsDarkPaint.color)
        drawPixel(canvas, x + pixelSize * 2, y + pixelSize * 8, pantsDarkPaint.color)
        drawPixel(canvas, x, y + pixelSize * 8, pantsDarkPaint.color)
        drawPixel(canvas, x + pixelSize * 4, y + pixelSize * 8, pantsDarkPaint.color)
    }

    private fun drawBelt(canvas: Canvas) {
        drawPixelBlock(canvas, x, y + pixelSize * 6, 5, 1, beltPaint)
        drawPixel(canvas, x + pixelSize * 2, y + pixelSize * 6, Color.YELLOW)
    }

    private fun drawTorso(canvas: Canvas) {
        drawPixelBlock(canvas, x, y + pixelSize * 2, 5, 4, shirtPaint)
        drawPixel(canvas, x + pixelSize, y + pixelSize * 3, shirtDarkPaint.color)
        drawPixel(canvas, x + pixelSize * 3, y + pixelSize * 3, shirtDarkPaint.color)
        drawPixel(canvas, x + pixelSize * 2, y + pixelSize * 4, shirtDarkPaint.color)
        drawPixel(canvas, x + pixelSize * 2, y + pixelSize * 2, Color.WHITE)
    }

    private fun drawArms(canvas: Canvas) {
        drawPixelBlock(canvas, x, y + pixelSize * 3, 1, 3, shirtPaint)
        drawPixel(canvas, x, y + pixelSize * 3, skinPaint.color)
        drawPixelBlock(canvas, x + pixelSize * 4, y + pixelSize * 3, 1, 3, shirtPaint)
        drawPixel(canvas, x + pixelSize * 4, y + pixelSize * 3, skinPaint.color)
        drawPixel(canvas, x, y + pixelSize * 5, shirtDarkPaint.color)
        drawPixel(canvas, x + pixelSize * 4, y + pixelSize * 5, shirtDarkPaint.color)
    }

    private fun drawNeck(canvas: Canvas) {
        drawPixelBlock(canvas, x + pixelSize * 2, y + pixelSize * 3, 1, 1, skinPaint)
    }

    private fun drawHead(canvas: Canvas) {
        drawPixelBlock(canvas, x + pixelSize, y, 3, 3, skinPaint)

        val blushPaint = Paint().apply {
            color = Color.rgb(255, 200, 200)
            alpha = 100
        }
        drawPixel(canvas, x + pixelSize, y + pixelSize * 2, blushPaint.color)
        drawPixel(canvas, x + pixelSize * 3, y + pixelSize * 2, blushPaint.color)
    }

    private fun drawHair(canvas: Canvas) {
        when {
            directionY < 0 -> {
                drawPixelBlock(canvas, x + pixelSize, y, 3, 2, hairPaint)
            }
            else -> {
                drawPixelBlock(canvas, x + pixelSize, y, 3, 1, hairPaint)
                drawPixel(canvas, x + pixelSize * 2, y + pixelSize, hairPaint.color)
            }
        }
    }

    private fun drawFace(canvas: Canvas) {
        drawPixel(canvas, x + pixelSize * 2, y + pixelSize, eyeWhitePaint.color)
        drawPixel(canvas, x + pixelSize * 3, y + pixelSize, eyeWhitePaint.color)

        when {
            directionX > 0 -> {
                drawPixel(canvas, x + pixelSize * 3, y + pixelSize, eyePaint.color)
                drawPixel(canvas, x + pixelSize * 2, y + pixelSize, eyePaint.color)
            }
            directionX < 0 -> {
                drawPixel(canvas, x + pixelSize * 2, y + pixelSize, eyePaint.color)
                drawPixel(canvas, x + pixelSize * 3, y + pixelSize, eyePaint.color)
            }
            directionY > 0 -> {
                drawPixel(canvas, x + pixelSize * 2, y + pixelSize * 2, eyePaint.color)
                drawPixel(canvas, x + pixelSize * 3, y + pixelSize * 2, eyePaint.color)
            }
            directionY < 0 -> {
                drawPixel(canvas, x + pixelSize * 2, y, eyePaint.color)
                drawPixel(canvas, x + pixelSize * 3, y, eyePaint.color)
            }
            else -> {
                drawPixel(canvas, x + pixelSize * 2, y + pixelSize, eyePaint.color)
                drawPixel(canvas, x + pixelSize * 3, y + pixelSize, eyePaint.color)
            }
        }
    }

    private fun drawDetails(canvas: Canvas) {
        drawPixel(canvas, x + pixelSize * 2, y + pixelSize * 4, Color.rgb(255, 215, 0))
        drawPixel(canvas, x + pixelSize * 2, y + pixelSize * 5, Color.rgb(255, 215, 0))
        drawPixel(canvas, x + pixelSize * 4, y + pixelSize * 4, Color.GRAY)
    }

    private fun drawPixel(canvas: Canvas, x: Float, y: Float, color: Int) {
        val paint = Paint().apply { this.color = color }
        canvas.drawRect(x, y, x + pixelSize, y + pixelSize, paint)
    }

    private fun drawPixelBlock(canvas: Canvas, startX: Float, startY: Float,
                               width: Int, height: Int, paint: Paint) {
        for (i in 0 until width) {
            for (j in 0 until height) {
                canvas.drawRect(
                    startX + i * pixelSize,
                    startY + j * pixelSize,
                    startX + (i + 1) * pixelSize,
                    startY + (j + 1) * pixelSize,
                    paint
                )
            }
        }
    }

    private fun drawPixelBlock(canvas: Canvas, startX: Float, startY: Float,
                               width: Int, height: Int, color: Int) {
        val paint = Paint().apply { this.color = color }
        drawPixelBlock(canvas, startX, startY, width, height, paint)
    }

    var maxX = 1000f
    var maxY = 1000f
}
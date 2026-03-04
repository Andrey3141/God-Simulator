package com.printer.godsimulator

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

enum class CreatureType {
    HUMAN, ANIMAL, MONSTER
}

enum class Gender {
    MALE, FEMALE
}

class Creature(
    var tileX: Int,
    var tileY: Int,
    var type: CreatureType = CreatureType.HUMAN,
    var gender: Gender = if (Random.nextBoolean()) Gender.MALE else Gender.FEMALE,
    private val pixelSize: Float = 8f,  // Меняем на Float
    val tileSize: Int = 24  // Оставляем Int для тайлов
) {
    // Пиксельные координаты для отрисовки
    private var x: Float = (tileX * tileSize + tileSize/2).toFloat()
    private var y: Float = (tileY * tileSize + tileSize/2).toFloat()

    // Характеристики
    var age = Random.nextInt(0, 80)
    var health = 100
    var hunger = Random.nextInt(0, 100)
    var speed = Random.nextFloat() * 2f + 1f

    // Состояние
    enum class State { IDLE, WALKING, EATING, SLEEPING, FIGHTING }
    var state = State.IDLE
    var stateTime = 0

    // Цели
    var targetX = tileX
    var targetY = tileY
    var isMoving = false

    // Внешность
    private val skinColor = when (type) {
        CreatureType.HUMAN -> Color.rgb(255, 220, 200)
        CreatureType.ANIMAL -> Color.rgb(160, 120, 80)
        CreatureType.MONSTER -> Color.rgb(100, 200, 100)
    }

    private val clothesColor = when (gender) {
        Gender.MALE -> Color.rgb(70, 130, 180)  // синий
        Gender.FEMALE -> Color.rgb(255, 100, 150)  // розовый
    }

    private val hairColor = Color.rgb(101, 67, 33)

    fun update() {
        stateTime++

        when (state) {
            State.IDLE -> {
                if (stateTime > Random.nextInt(30, 90)) {
                    startRandomWalk()
                }
            }
            State.WALKING -> {
                moveToTarget()
            }
            State.EATING -> {
                // Будет позже
            }
            State.SLEEPING -> {
                // Будет позже
            }
            State.FIGHTING -> {
                // Будет позже
            }
        }

        // Обновление потребностей
        hunger = (hunger + 0.1f).toInt().coerceAtMost(100)
        if (hunger > 80) {
            health = (health - 0.5f).toInt().coerceAtLeast(0)
        }

        // Смерть
        if (health <= 0 || age > 100) {
            // Помечаем на удаление
        }
    }

    private fun startRandomWalk() {
        val dx = Random.nextInt(-3, 4)
        val dy = Random.nextInt(-3, 4)

        targetX = tileX + dx
        targetY = tileY + dy

        // Проверка границ мира
        if (targetX in 0 until maxTiles && targetY in 0 until maxTiles) {
            state = State.WALKING
            isMoving = true
            stateTime = 0
        }
    }

    private fun moveToTarget() {
        val targetPixelX = (targetX * tileSize + tileSize/2).toFloat()
        val targetPixelY = (targetY * tileSize + tileSize/2).toFloat()

        val dx = targetPixelX - x
        val dy = targetPixelY - y

        if (abs(dx) < speed && abs(dy) < speed) {
            // Достигли цели
            x = targetPixelX
            y = targetPixelY
            tileX = targetX
            tileY = targetY
            state = State.IDLE
            isMoving = false
            stateTime = 0
        } else {
            // Двигаемся к цели
            x += dx.coerceIn(-speed, speed)
            y += dy.coerceIn(-speed, speed)
        }
    }

    fun draw(canvas: Canvas) {
        when (type) {
            CreatureType.HUMAN -> drawHuman(canvas)
            CreatureType.ANIMAL -> drawAnimal(canvas)
            CreatureType.MONSTER -> drawMonster(canvas)
        }

        // Рисуем индикатор здоровья если ранен
        if (health < 100) {
            drawHealthBar(canvas)
        }
    }

    private fun drawHuman(canvas: Canvas) {
        // Голова
        canvas.drawCircle(x, y - pixelSize * 2f, pixelSize * 1.5f, Paint().apply {
            color = skinColor
            isAntiAlias = true
        })

        // Глаза
        val eyePaint = Paint().apply { color = Color.BLACK }
        canvas.drawCircle(x - pixelSize/2f, y - pixelSize * 2.5f, pixelSize/3f, eyePaint)
        canvas.drawCircle(x + pixelSize/2f, y - pixelSize * 2.5f, pixelSize/3f, eyePaint)

        // Тело
        canvas.drawRect(x - pixelSize, y - pixelSize, x + pixelSize, y + pixelSize, Paint().apply {
            color = clothesColor
        })

        // Руки
        canvas.drawLine(x - pixelSize * 1.5f, y - pixelSize/2f, x - pixelSize, y, Paint().apply {
            color = clothesColor
            strokeWidth = pixelSize/2f
        })
        canvas.drawLine(x + pixelSize * 1.5f, y - pixelSize/2f, x + pixelSize, y, Paint().apply {
            color = clothesColor
            strokeWidth = pixelSize/2f
        })

        // Ноги
        canvas.drawLine(x - pixelSize/2f, y + pixelSize, x - pixelSize, y + pixelSize * 2f, Paint().apply {
            color = Color.rgb(80, 50, 30)
            strokeWidth = pixelSize/2f
        })
        canvas.drawLine(x + pixelSize/2f, y + pixelSize, x + pixelSize, y + pixelSize * 2f, Paint().apply {
            color = Color.rgb(80, 50, 30)
            strokeWidth = pixelSize/2f
        })

        // Волосы (по полу)
        if (gender == Gender.MALE) {
            canvas.drawCircle(x, y - pixelSize * 2.5f, pixelSize, Paint().apply {
                color = hairColor
            })
        } else {
            // Для женщин - хвостик или косичка
            canvas.drawCircle(x - pixelSize/2f, y - pixelSize * 3f, pixelSize/2f, Paint().apply {
                color = hairColor
            })
            canvas.drawCircle(x + pixelSize/2f, y - pixelSize * 3f, pixelSize/2f, Paint().apply {
                color = hairColor
            })
        }
    }

    private fun drawAnimal(canvas: Canvas) {
        // Простое животное (например, свинка)
        canvas.drawCircle(x, y, pixelSize * 2f, Paint().apply {
            color = Color.rgb(255, 200, 200)
            isAntiAlias = true
        })
        canvas.drawCircle(x - pixelSize, y - pixelSize, pixelSize, Paint().apply {
            color = Color.rgb(200, 150, 150)
            isAntiAlias = true
        })
        canvas.drawCircle(x + pixelSize, y - pixelSize, pixelSize, Paint().apply {
            color = Color.rgb(200, 150, 150)
            isAntiAlias = true
        })
        canvas.drawCircle(x - pixelSize/2f, y - pixelSize, pixelSize/3f, Paint().apply {
            color = Color.BLACK
        })
        canvas.drawCircle(x + pixelSize/2f, y - pixelSize, pixelSize/3f, Paint().apply {
            color = Color.BLACK
        })
    }

    private fun drawMonster(canvas: Canvas) {
        // Простой монстр
        canvas.drawCircle(x, y, pixelSize * 2f, Paint().apply {
            color = Color.rgb(100, 200, 100)
            isAntiAlias = true
        })
        canvas.drawRect(x - pixelSize, y, x + pixelSize, y + pixelSize * 2f, Paint().apply {
            color = Color.rgb(50, 150, 50)
        })
        canvas.drawCircle(x - pixelSize, y - pixelSize/2f, pixelSize/2f, Paint().apply {
            color = Color.RED
        })
        canvas.drawCircle(x + pixelSize, y - pixelSize/2f, pixelSize/2f, Paint().apply {
            color = Color.RED
        })
    }

    private fun drawHealthBar(canvas: Canvas) {
        val barWidth = pixelSize * 3f
        val barHeight = pixelSize / 2f
        val healthWidth = barWidth * health / 100f

        canvas.drawRect(x - barWidth/2f, y - pixelSize * 4f, x + barWidth/2f, y - pixelSize * 4f + barHeight, Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 1f
        })
        canvas.drawRect(x - barWidth/2f, y - pixelSize * 4f, x - barWidth/2f + healthWidth, y - pixelSize * 4f + barHeight, Paint().apply {
            color = Color.GREEN
        })
    }

    var maxTiles = 100
}
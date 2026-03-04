package com.printer.godsimulator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import kotlin.math.abs
import kotlin.random.Random

class Chicken(
    var tileX: Int,
    var tileY: Int,
    private val spriteManager: SpriteManager
) {

    var world: World? = null

    private var direction = Direction.DOWN
    private val walkAnimations = mutableMapOf<Direction, Animation>()
    private val staticFrames = mutableMapOf<Direction, Bitmap>()

    private var currentAnimation: Animation? = null
    private var currentStaticFrame: Bitmap? = null

    private var targetX = tileX
    private var targetY = tileY
    private var moveProgress = 0f
    private val moveSpeed = 0.05f

    enum class State { WALKING, IDLE }
    var state = State.IDLE
    private var idleTimer = 0
    private val idleTime = Random.nextInt(50, 150)

    init {
        // Загружаем анимации
        Direction.values().forEach { dir ->
            val frames = spriteManager.getChickenFrames(dir)
            walkAnimations[dir] = Animation(frames, 150)
            spriteManager.getChickenStaticFrame(dir)?.let { frame ->
                staticFrames[dir] = frame
            }
        }

        currentStaticFrame = staticFrames[Direction.DOWN]
        direction = Direction.DOWN
        startRandomWalk()
    }

    fun update() {
        when (state) {
            State.IDLE -> {
                currentAnimation = null
                currentStaticFrame = staticFrames[direction]

                idleTimer++
                if (idleTimer > idleTime) {
                    startRandomWalk()
                }
            }
            State.WALKING -> {
                currentAnimation = walkAnimations[direction]
                currentStaticFrame = null
                updateWalking()
            }
        }

        currentAnimation?.update()
    }

    private fun startRandomWalk() {
        val directions = listOf(
            Pair(0, -1), // вверх
            Pair(0, 1),  // вниз
            Pair(-1, 0), // влево
            Pair(1, 0)   // вправо
        )

        val shuffled = directions.shuffled()

        for ((dx, dy) in shuffled) {
            val newX = tileX + dx
            val newY = tileY + dy

            try {
                val tile = world?.getTile(newX, newY)
                if (tile != null && tile.type != TileType.WATER) {
                    targetX = newX
                    targetY = newY

                    direction = when {
                        dy < 0 -> Direction.UP
                        dy > 0 -> Direction.DOWN
                        dx < 0 -> Direction.LEFT
                        dx > 0 -> Direction.RIGHT
                        else -> direction
                    }

                    state = State.WALKING
                    moveProgress = 0f
                    return
                }
            } catch (e: Exception) {
                // Игнорируем
            }
        }

        state = State.IDLE
        idleTimer = 0
    }

    private fun updateWalking() {
        moveProgress += moveSpeed

        if (moveProgress >= 1f) {
            tileX = targetX
            tileY = targetY
            state = State.IDLE
            idleTimer = 0
        }
    }

    fun getPixelX(tileSize: Float): Float {
        return if (state == State.WALKING) {
            (tileX + (targetX - tileX) * moveProgress) * tileSize + tileSize / 2
        } else {
            tileX * tileSize + tileSize / 2
        }
    }

    fun getPixelY(tileSize: Float): Float {
        return if (state == State.WALKING) {
            (tileY + (targetY - tileY) * moveProgress) * tileSize + tileSize / 2
        } else {
            tileY * tileSize + tileSize / 2
        }
    }

    fun draw(canvas: Canvas, x: Float, y: Float, size: Float = 32f) {
        if (state == State.WALKING) {
            currentAnimation?.draw(canvas, x, y, size)
        } else {
            currentStaticFrame?.let { frame ->
                val destRect = Rect(
                    (x - size/2).toInt(),
                    (y - size/2).toInt(),
                    (x + size/2).toInt(),
                    (y + size/2).toInt()
                )
                canvas.drawBitmap(frame, null, destRect, null)
            }
        }
    }
}
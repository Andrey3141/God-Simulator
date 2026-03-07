package com.printer.godsimulator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import kotlin.random.Random

class Chicken(
    var tileX: Int,
    var tileY: Int,
    private val spriteManager: SpriteManager
) {
    var world: World? = null

    var direction = Direction.DOWN  // ✅ public для сохранения
    private val walkAnimations = mutableMapOf<Direction, Animation>()
    private val staticFrames = mutableMapOf<Direction, Bitmap>()

    private var currentAnimation: Animation? = null
    private var currentStaticFrame: Bitmap? = null

    private var targetX = tileX
    private var targetY = tileY
    private var moveProgress = 0f
    private val moveSpeed = GameConfig.CHICKEN_MOVE_SPEED

    enum class State { WALKING, IDLE }
    var state = State.IDLE  // ✅ public для сохранения
    private var idleTimer = 0
    private var idleTime = Random.nextInt(GameConfig.CHICKEN_IDLE_TIME_MIN, GameConfig.CHICKEN_IDLE_TIME_MAX)

    init {
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
            Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0)
        )
        val shuffled = directions.shuffled()
        val walkDistance = Random.nextInt(GameConfig.CHICKEN_WALK_DISTANCE_MIN, GameConfig.CHICKEN_WALK_DISTANCE_MAX + 1)

        for ((dx, dy) in shuffled) {
            val newX = tileX + dx * walkDistance
            val newY = tileY + dy * walkDistance

            try {
                val tile = world?.getTile(newX, newY)

                // ✅ ИСПРАВЛЕНИЕ: Куры НЕ ходят в воду и лес
                if (tile != null &&
                    tile.type != TileType.WATER &&
                    tile.type != TileType.FOREST &&
                    tile.type != TileType.STONE) {  // ✅ Дополнительно: не ходят по камням

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
                    idleTime = Random.nextInt(GameConfig.CHICKEN_IDLE_TIME_MIN, GameConfig.CHICKEN_IDLE_TIME_MAX)
                    return
                }
            } catch (e: Exception) {
                // Игнорируем
            }
        }

        // Если не нашли подходящий тайл — остаёмся на месте
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

    fun getPixelX(tileSize: Float): Float =
        if (state == State.WALKING) {
            (tileX + (targetX - tileX) * moveProgress) * tileSize + tileSize / 2
        } else {
            tileX * tileSize + tileSize / 2
        }

    fun getPixelY(tileSize: Float): Float =
        if (state == State.WALKING) {
            (tileY + (targetY - tileY) * moveProgress) * tileSize + tileSize / 2
        } else {
            tileY * tileSize + tileSize / 2
        }

    fun draw(canvas: Canvas, x: Float, y: Float, size: Float = 32f) {
        if (state == State.WALKING) {
            currentAnimation?.draw(canvas, x, y, size)
        } else {
            currentStaticFrame?.let { frame ->
                val destRect = Rect(
                    (x - size/2).toInt(), (y - size/2).toInt(),
                    (x + size/2).toInt(), (y + size/2).toInt()
                )
                canvas.drawBitmap(frame, null, destRect, null)
            }
        }
    }
}
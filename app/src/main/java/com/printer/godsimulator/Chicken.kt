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

    var direction = Direction.DOWN
    private val walkAnimations = mutableMapOf<Direction, Animation>()
    private val eatAnimations = mutableMapOf<Direction, Animation>()
    private val staticFrames = mutableMapOf<Direction, Bitmap>()

    private var currentAnimation: Animation? = null
    private var currentStaticFrame: Bitmap? = null

    private var targetX = tileX
    private var targetY = tileY
    private var moveProgress = 0f
    private val moveSpeed = GameConfig.CHICKEN_MOVE_SPEED

    enum class State { WALKING, IDLE, EATING }
    var state = State.IDLE
    private var idleTimer = 0
    private var eatTimer = 0
    private val idleTime = Random.nextInt(GameConfig.CHICKEN_IDLE_TIME_MIN, GameConfig.CHICKEN_IDLE_TIME_MAX)
    private val eatDuration = GameConfig.CHICKEN_EAT_DURATION_FRAMES

    // ✅ Список разрешённых тайлов для куриц
    private val walkableTiles = GameConfig.CHICKEN_WALKABLE_TILES
        .split(",")
        .map { it.trim() }
        .mapNotNull { TileType.values().find { type -> type.name == it } }
        .toSet()

    init {
        Direction.values().forEach { dir ->
            val walkFrames = spriteManager.getChickenFrames(dir)
            walkAnimations[dir] = Animation(walkFrames, 150)

            val eatFrames = spriteManager.getChickenEatFrames(dir)
            eatAnimations[dir] = Animation(eatFrames, 200)

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
                    tryToEatOrWalk()
                }
            }
            State.WALKING -> {
                currentAnimation = walkAnimations[direction]
                currentStaticFrame = null
                updateWalking()
            }
            State.EATING -> {
                currentAnimation = eatAnimations[direction]
                currentStaticFrame = null
                updateEating()
            }
        }
        currentAnimation?.update()
    }

    private fun tryToEatOrWalk() {
        // ✅ Проверяем шанс поесть (настраивается в GameConfig)
        if (Random.nextFloat() < GameConfig.CHICKEN_EAT_CHANCE) {
            val directions = listOf(Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0))
            for ((dx, dy) in directions) {
                val newX = tileX + dx
                val newY = tileY + dy
                try {
                    val tile = world?.getTile(newX, newY)
                    // ✅ Едим ТОЛЬКО траву
                    if (tile != null && tile.type == TileType.GRASS) {
                        direction = when {
                            dy < 0 -> Direction.UP
                            dy > 0 -> Direction.DOWN
                            dx < 0 -> Direction.LEFT
                            dx > 0 -> Direction.RIGHT
                            else -> direction
                        }
                        state = State.EATING
                        eatTimer = 0
                        idleTimer = 0
                        return
                    }
                } catch (e: Exception) {}
            }
        }
        // ✅ Нет травы или не захотели есть — идём гулять
        startRandomWalk()
    }

    private fun startRandomWalk() {
        val directions = listOf(Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0))
        val shuffled = directions.shuffled()
        val walkDistance = Random.nextInt(GameConfig.CHICKEN_WALK_DISTANCE_MIN, GameConfig.CHICKEN_WALK_DISTANCE_MAX + 1)

        for ((dx, dy) in shuffled) {
            val newX = tileX + dx * walkDistance
            val newY = tileY + dy * walkDistance

            try {
                // ✅ ПРОВЕРКА 1: Конечная точка должна быть разрешённым тайлом
                val targetTile = world?.getTile(newX, newY)
                if (targetTile == null || targetTile.type !in walkableTiles) {
                    continue
                }

                // ✅ ПРОВЕРКА 2: Проверяем КАЖДЫЙ тайл на пути (чтобы не проходить сквозь препятствия)
                var canPass = true
                for (step in 1..walkDistance) {
                    val checkX = tileX + dx * step
                    val checkY = tileY + dy * step
                    val checkTile = world?.getTile(checkX, checkY)
                    if (checkTile == null || checkTile.type !in walkableTiles) {
                        canPass = false
                        break
                    }
                }

                if (canPass) {
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
            } catch (e: Exception) {}
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

    private fun updateEating() {
        eatTimer++
        if (eatTimer >= eatDuration) {
            // ✅ Поели траву — превращаем в грязь
            world?.convertGrassToDirt(tileX, tileY)
            state = State.IDLE
            eatTimer = 0
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
        if (state == State.WALKING || state == State.EATING) {
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
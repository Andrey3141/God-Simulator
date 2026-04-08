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
    private val eatGrassAnimations = mutableMapOf<Direction, Animation>()
    private val eatWormAnimations = mutableMapOf<Direction, Animation>()
    private val staticFrames = mutableMapOf<Direction, Bitmap>()

    private var currentAnimation: Animation? = null
    private var currentStaticFrame: Bitmap? = null

    private var targetX = tileX
    private var targetY = tileY
    private var moveProgress = 0f
    private val moveSpeed = GameConfig.CHICKEN_MOVE_SPEED

    enum class State {
        IDLE,
        WALKING,
        EATING  // ✅ Добавлено состояние поедания
    }
    var state = State.IDLE
    private var idleTimer = 0
    private var eatTimer = 0
    private val idleTime = Random.nextInt(GameConfig.CHICKEN_IDLE_TIME_MIN, GameConfig.CHICKEN_IDLE_TIME_MAX)

    // ✅ Флаг: есть ли червяк в текущей грязи
    private var hasWorm = false

    init {
        Direction.values().forEach { dir ->
            val walkFrames = spriteManager.getChickenFrames(dir)
            walkAnimations[dir] = Animation(walkFrames, 150)

            val eatGrassFrames = spriteManager.getChickenEatFrames(dir)
            eatGrassAnimations[dir] = Animation(eatGrassFrames, 200)

            val eatWormFrames = spriteManager.getChickenEatWormFrames(dir)
            eatWormAnimations[dir] = Animation(eatWormFrames, 150)

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
                currentAnimation = eatGrassAnimations[direction]
                currentStaticFrame = null
                updateEatingGrass()
            }
            State.EATING -> {
                currentAnimation = eatWormAnimations[direction]
                currentStaticFrame = null
                updateEatingWorm()
            }
        }
        currentAnimation?.update()
    }

    private fun tryToEatOrWalk() {
        if (Random.nextFloat() < GameConfig.CHICKEN_EAT_CHANCE) {
            val directions = listOf(Pair(0, -1), Pair(0, 1), Pair(-1, 0), Pair(1, 0))
            for ((dx, dy) in directions) {
                val newX = tileX + dx
                val newY = tileY + dy
                try {
                    val tile = world?.getTile(newX, newY)

                    // ✅ Проверяем траву
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
                        hasWorm = false  // Сбрасываем флаг
                        return
                    }

                    // ✅ Проверяем грязь с червяком
                    if (tile != null && tile.type == TileType.MUD_WITH_WORM) {
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
                        hasWorm = true  // ✅ Червяк есть!
                        return
                    }
                } catch (e: Exception) {}
            }
        }
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
                val targetTile = world?.getTile(newX, newY)
                if (targetTile == null) continue

                // ✅ Разрешённые тайлы для ходьбы
                val isWalkable = when (targetTile.type) {
                    TileType.GRASS, TileType.SAND, TileType.DIRT, TileType.MUD_WITH_WORM -> true
                    else -> false
                }

                if (!isWalkable) continue

                var canPass = true
                for (step in 1..walkDistance) {
                    val checkX = tileX + dx * step
                    val checkY = tileY + dy * step
                    val checkTile = world?.getTile(checkX, checkY)
                    if (checkTile == null) {
                        canPass = false
                        break
                    }
                    val isStepWalkable = when (checkTile.type) {
                        TileType.GRASS, TileType.SAND, TileType.DIRT, TileType.MUD_WITH_WORM -> true
                        else -> false
                    }
                    if (!isStepWalkable) {
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

    // ✅ Поедание травы
    private fun updateEatingGrass() {
        eatTimer++
        if (eatTimer >= GameConfig.CHICKEN_EAT_GRASS_DURATION) {
            // ✅ Превращаем траву в грязь (с шансом на червяка)
            world?.convertGrassToDirt(tileX, tileY)

            // ✅ Если был червяк — переходим к поеданию червяка
            if (hasWorm) {
                state = State.EATING
                eatTimer = 0
            } else {
                state = State.IDLE
                eatTimer = 0
                idleTimer = 0
            }
        }
    }

    // ✅ Поедание червяка
    private fun updateEatingWorm() {
        eatTimer++
        if (eatTimer >= GameConfig.CHICKEN_EAT_WORM_DURATION) {
            // ✅ Превращаем грязь с червяком в обычную грязь
            world?.convertMudWithWormToDirt(tileX, tileY)
            state = State.IDLE
            eatTimer = 0
            idleTimer = 0
            hasWorm = false
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
        when (state) {
            State.WALKING, State.EATING -> {
                currentAnimation?.draw(canvas, x, y, size)
            }
            State.IDLE -> {
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
}
package org.lewapnoob.raycast

import java.awt.MouseInfo
import java.awt.event.KeyEvent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Player(private val renderCast: RenderCast, private val map: Map) {
    private val playerSize = 5.0
    private val margin = 2.0
    private var movementSpeed = 1.5 * SpeedMovement
    private val rotationSpeed = 2
    private val sensitivity = 0.07

    private var lastGridX = (positionX / tileSize).toInt()
    private var lastGridY = (positionY / tileSize).toInt()

    private fun canMoveTo(x: Double, y: Double, deltaX: Double, deltaY: Double): Pair<Boolean, Enemy?> {
        val left = x - playerSize / 2
        val right = x + playerSize / 2
        val top = y - playerSize / 2
        val bottom = y + playerSize / 2

        val gridLeft = ((left - margin) / tileSize).toInt()
        val gridRight = ((right + margin) / tileSize).toInt()
        val gridTop = ((top - margin) / tileSize).toInt()
        val gridBottom = ((bottom + margin) / tileSize).toInt()

        for (gridY in gridTop..gridBottom) {
            for (gridX in gridLeft..gridRight) {
                if (gridY !in map.grid.indices || gridX !in map.grid[0].indices ||
                    ((map.grid[gridY][gridX] == 1) ||
                            (map.grid[gridY][gridX] == 2) ||
                            (map.grid[gridY][gridX] == 12))) {
                    if (!noClip) return Pair(first = false, second = null)
                }
            }
        }

        // Player-enemy collision
        renderCast.getEnemies().forEach { enemy ->
            val dx = x - enemy.x
            val dy = y - enemy.y
            val distance = sqrt(x = dx * dx + dy * dy)
            if (distance < playerSize / 2 + 5.0) {
                return Pair(first = false, second = enemy)
            }
        }

        return Pair(first = true, second = null)
    }

    fun tryPushEnemy(enemy: Enemy, deltaX: Double, deltaY: Double): Boolean {
        if (enemy.isMoving) {
            // Sprawdzenie, czy przeciwnik porusza się w przeciwnym kierunku
            val dotProduct = deltaX * enemy.lastMoveX + deltaY * enemy.lastMoveY
            if (dotProduct < 0.0) {
                return false // opposite direction = no push
            }
        }
        // push enemy
        val newEnemyX = enemy.x + deltaX
        val newEnemyY = enemy.y + deltaY
        val (canMove, _) = enemy.canMoveTo(newX = newEnemyX, newY = newEnemyY) // Użycie Pair<Boolean, Enemy?>
        if (canMove) {
            enemy.x = newEnemyX
            enemy.y = newEnemyY
            enemy.lastMoveX = deltaX
            enemy.lastMoveY = deltaY
            enemy.isMoving = true
            return true
        }
        return false
    }

    private fun tryMove(deltaX: Double, deltaY: Double) {
        val newX = positionX + deltaX
        val newY = positionY + deltaY
        val (canMove, collidedEnemy) = canMoveTo(x = newX, y = newY, deltaX = deltaX, deltaY = deltaY)
        if (canMove) {
            positionX = newX
            positionY = newY
            return
        } else if (collidedEnemy != null) {
            // Try to push back a colliding enemy
            if (tryPushEnemy(collidedEnemy, deltaX, deltaY)) {
                positionX = newX
                positionY = newY
                return
            }
        }

        // Try moving in X only
        val newXOnly = positionX + deltaX
        val (canMoveX, collidedEnemyX) = canMoveTo(x = newXOnly, y = positionY, deltaX = deltaX, deltaY = 0.0)
        if (canMoveX) {
            movementSpeed = 1.00
            positionX = newXOnly
            return
        } else if (collidedEnemyX != null && tryPushEnemy(enemy = collidedEnemyX, deltaX = deltaX, deltaY = 0.0)) {
            movementSpeed = 1.00
            positionX = newXOnly
            return
        }

        // Try moving in Y only
        val newYOnly = positionY + deltaY
        val (canMoveY, collidedEnemyY) = canMoveTo(x = positionX, y = newYOnly, deltaX = 0.0, deltaY = deltaY)
        if (canMoveY) {
            movementSpeed = 1.00
            positionY = newYOnly
        } else if (collidedEnemyY != null && tryPushEnemy(enemy = collidedEnemyY, deltaX = 0.0, deltaY = deltaY)) {
            movementSpeed = 1.00
            positionY = newYOnly
        }
    }

    private fun checkKeyPickup() {
        val keysToDeactivate = mutableListOf<Key>()
        val medicationsToDeactivate = mutableListOf<Medication>()

        // Apply changes after iteration
        keysToDeactivate.forEach { key ->
            key.active = false
            keys += 1
            playSound(soundFile = "8exp.wav", volume = 0.65f)
        }
        medicationsToDeactivate.forEach { medication ->
            medication.active = false
            playerHealth += (medication.heal * HealBoost).toInt()
            playSound(soundFile = "8exp.wav", volume = 0.65f)
        }
    }

    fun w() {
        val deltaX = movementSpeed * cos(Math.toRadians(currentangle.toDouble())) * deltaTime * TARGET_FPS
        val deltaY = movementSpeed * sin(Math.toRadians(currentangle.toDouble())) * deltaTime * TARGET_FPS
        tryMove(deltaX, deltaY)
    }

    fun s() {
        val deltaX = -movementSpeed * cos(Math.toRadians(currentangle.toDouble())) * deltaTime * TARGET_FPS
        val deltaY = -movementSpeed * sin(Math.toRadians(currentangle.toDouble())) * deltaTime * TARGET_FPS
        tryMove(deltaX, deltaY)
    }

    fun a() {
        val deltaX = movementSpeed * cos(Math.toRadians(currentangle - 90.0)) * deltaTime * TARGET_FPS
        val deltaY = movementSpeed * sin(Math.toRadians(currentangle - 90.0)) * deltaTime * TARGET_FPS
        tryMove(deltaX, deltaY)
    }

    fun d() {
        val deltaX = movementSpeed * cos(Math.toRadians(currentangle + 90.0)) * deltaTime * TARGET_FPS
        val deltaY = movementSpeed * sin(Math.toRadians(currentangle + 90.0)) * deltaTime * TARGET_FPS
        tryMove(deltaX, deltaY)
    }

    fun anglea() {
        currentangle -= rotationSpeed
    }

    fun angled() {
        currentangle += rotationSpeed
    }

    fun updateAngleFromMouse() {
        if (MouseSupport) {
            currentangle += if (MouseInfo.getPointerInfo().location.x == 960) {
                0
            } else {
                (((MouseInfo.getPointerInfo().location.x) - 960) * sensitivity).toInt()
            }
        }
    }

    fun update(keysPressed: kotlin.collections.Map<Int, Boolean>) {
        if (!deathGUI) {
            if (keysPressed.getOrDefault(KeyEvent.VK_W, false)) w()
            if (keysPressed.getOrDefault(KeyEvent.VK_S, false)) s()
            if (keysPressed.getOrDefault(KeyEvent.VK_A, false)) a()
            if (keysPressed.getOrDefault(KeyEvent.VK_D, false)) d()
            if (keysPressed.getOrDefault(KeyEvent.VK_LEFT, false)) anglea()
            if (keysPressed.getOrDefault(KeyEvent.VK_RIGHT, false)) angled()
            if (((keysPressed.getOrDefault(KeyEvent.VK_W, false)) and (keysPressed.getOrDefault(KeyEvent.VK_SHIFT, false)))) {
                movementSpeed = 2.5 * SpeedMovement
            } else {
                movementSpeed = 1.5 * SpeedMovement
            }
        }

        val gridX = (positionX / tileSize).toInt()
        val gridY = (positionY / tileSize).toInt()
        if (gridX != lastGridX || gridY != lastGridY) {
            if (gridY in map.grid.indices && gridX in map.grid[0].indices && map.grid[gridY][gridX] == 5) {
                val direction = when {
                    gridX > lastGridX -> Map.Direction.LEFT
                    gridX < lastGridX -> Map.Direction.RIGHT
                    gridY > lastGridY -> Map.Direction.UP
                    else -> Map.Direction.DOWN
                }
                map.generateRoom(x = (positionX/tileSize).toInt(), y = (positionY/tileSize).toInt(), enterdirection = direction)
            }
            lastGridX = gridX
            lastGridY = gridY
        }

        if (playerHealth <= 0) {
            deathGUI = true
        }
        checkKeyPickup()
    }
}
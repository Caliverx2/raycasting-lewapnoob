package org.example.MainKt

import java.awt.MouseInfo
import java.awt.event.KeyEvent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Player(private val renderCast: RenderCast, private val map: Map) {
    private val playerSize = 5.0
    private val margin = 2.0
    private var movementSpeed = 1.5
    private val rotationSpeed = 2
    private val sensitivity = 0.07
    var playerHealth = 100

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
                if (gridY !in map.grid.indices || gridX !in map.grid[0].indices || map.grid[gridY][gridX] == 1) {
                    return Pair(false, null)
                }
            }
        }

        // Player-enemy collision
        renderCast.getEnemies().forEach { enemy ->
            val dx = x - enemy.x
            val dy = y - enemy.y
            val distance = sqrt(dx * dx + dy * dy)
            if (distance < playerSize / 2 + 5.0) {
                return Pair(false, enemy)
            }
        }

        return Pair(true, null)
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
        val (canMove, _) = enemy.canMoveTo(newEnemyX, newEnemyY) // Użycie Pair<Boolean, Enemy?>
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
        val (canMove, collidedEnemy) = canMoveTo(newX, newY, deltaX, deltaY)
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
        val (canMoveX, collidedEnemyX) = canMoveTo(newXOnly, positionY, deltaX, 0.0)
        if (canMoveX) {
            movementSpeed = 1.00
            positionX = newXOnly
            return
        } else if (collidedEnemyX != null && tryPushEnemy(collidedEnemyX, deltaX, 0.0)) {
            movementSpeed = 1.00
            positionX = newXOnly
            return
        }

        // Try moving in Y only
        val newYOnly = positionY + deltaY
        val (canMoveY, collidedEnemyY) = canMoveTo(positionX, newYOnly, 0.0, deltaY)
        if (canMoveY) {
            movementSpeed = 1.00
            positionY = newYOnly
        } else if (collidedEnemyY != null && tryPushEnemy(collidedEnemyY, 0.0, deltaY)) {
            movementSpeed = 1.00
            positionY = newYOnly
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
        if (keysPressed.getOrDefault(KeyEvent.VK_W, false) || keysPressed.getOrDefault(KeyEvent.VK_UP, false)) w()
        if (keysPressed.getOrDefault(KeyEvent.VK_S, false) || keysPressed.getOrDefault(KeyEvent.VK_DOWN, false)) s()
        if (keysPressed.getOrDefault(KeyEvent.VK_A, false)) a()
        if (keysPressed.getOrDefault(KeyEvent.VK_D, false)) d()
        if (keysPressed.getOrDefault(KeyEvent.VK_LEFT, false)) anglea()
        if (keysPressed.getOrDefault(KeyEvent.VK_RIGHT, false)) angled()
        if (((keysPressed.getOrDefault(KeyEvent.VK_W, false)) and (keysPressed.getOrDefault(KeyEvent.VK_SHIFT, false)))) {
            movementSpeed = 2.5
        } else {
            movementSpeed = 1.5
        }
    }
}
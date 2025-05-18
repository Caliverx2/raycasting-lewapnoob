package org.example.MainKt

import java.awt.Color
import java.awt.MouseInfo
import java.awt.event.KeyEvent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class Player(private val renderCast: RenderCast, private val map: Map) {
    private val playerSize = 5.0
    private val margin = 2.0
    private var movementSpeed = 1.5
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
                            (map.grid[gridY][gridX] == 2))) {
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

    private fun checkKeyPickup() {
        val keysToDeactivate = mutableListOf<Key>()
        val ammoToDeactivate = mutableListOf<Ammo>()
        val medicationsToDeactivate = mutableListOf<Medication>()

        // Apply changes after iteration
        keysToDeactivate.forEach { key ->
            key.active = false
            keys += 1
            renderCast.playSound("8exp.wav", volume = 0.65f)
        }
        medicationsToDeactivate.forEach { medication ->
            medication.active = false
            playerHealth += medication.heal
            renderCast.playSound("8exp.wav", volume = 0.65f)
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
        if (keysPressed.getOrDefault(KeyEvent.VK_W, false)) w()
        if (keysPressed.getOrDefault(KeyEvent.VK_S, false)) s()
        if (keysPressed.getOrDefault(KeyEvent.VK_A, false)) a()
        if (keysPressed.getOrDefault(KeyEvent.VK_D, false)) d()
        if (keysPressed.getOrDefault(KeyEvent.VK_LEFT, false)) anglea()
        if (keysPressed.getOrDefault(KeyEvent.VK_RIGHT, false)) angled()
        if (((keysPressed.getOrDefault(KeyEvent.VK_W, false)) and (keysPressed.getOrDefault(KeyEvent.VK_SHIFT, false)))) {
            movementSpeed = 2.5
        } else {
            movementSpeed = 1.5
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
                map.generateRoom((positionX/tileSize).toInt(), (positionY/tileSize).toInt(), direction)
            }
            lastGridX = gridX
            lastGridY = gridY
        }

        if (playerHealth <= 0) {
            positionX = (tileSize*11)-(tileSize/2)
            positionY = (tileSize*11)-(tileSize/2)

            level -= 2
            if (level <= 0) {
                level = 1
            }
            keys += level
            playerHealth = 100
            map.currentRooms = 0
            currentAmmo += 45

            enemies = mutableListOf<Enemy>()
            lightSources = mutableListOf<LightSource>()
            keysList = mutableListOf<Key>()
            medications = mutableListOf<Medication>()
            chests = mutableListOf<Chest>()
            ammo = mutableListOf<Ammo>()

            lightSources.add(LightSource(0.0, 0.0, color = Color(200, 200, 100), intensity = 0.75, range = 0.15, owner = "player"))
            enemies.add(Enemy((tileSize * 2) - (tileSize / 2), (tileSize * 2) - (tileSize / 2), health = 100, renderCast.enemyTextureId!!, renderCast, map, speed = (2.0 * ((18..19).random() / 10.0))))
            enemies.add(Enemy((tileSize * 2) - (tileSize / 2), (tileSize * 20) - (tileSize / 2), health = 100, renderCast.enemyTextureId!!, renderCast , map, speed = (2.0 * ((18..19).random() / 10.0))))
            enemies.add(Enemy((tileSize * 20) - (tileSize / 2), (tileSize * 20) - (tileSize / 2), health = 100, renderCast.enemyTextureId!!, renderCast, map, speed = (2.0 * ((18..19).random() / 10.0))))
            enemies.add(Enemy((tileSize * 20) - (tileSize / 2), (tileSize * 2) - (tileSize / 2), health = 100, renderCast.enemyTextureId!!, renderCast, map, speed = (2.0 * ((18..19).random() / 10.0))))
            lightSources.add(LightSource((enemies[0].x / tileSize), (enemies[0].y / tileSize), color = Color(20, 22, 255), intensity = 0.35, range = 1.5, owner = "${enemies[0]}"))
            lightSources.add(LightSource((enemies[1].x / tileSize), (enemies[1].y / tileSize), color = Color(255, 255, 22), intensity = 0.35, range = 1.5, owner = "${enemies[1]}"))
            lightSources.add(LightSource((enemies[2].x / tileSize), (enemies[2].y / tileSize), color = Color(22, 255, 22), intensity = 0.35, range = 1.5, owner = "${enemies[2]}"))
            lightSources.add(LightSource((enemies[3].x / tileSize), (enemies[3].y / tileSize), color = Color(255, 22, 22), intensity = 0.35, range = 1.5, owner = "${enemies[3]}"))
            // renderCast
            map.grid = arrayOf(
                intArrayOf(2,5,2,2,2,2,2,2,2,2,5,2,2,2,2,2,2,2,2,5,2),
                intArrayOf(5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5),
                intArrayOf(2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2),
                intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
                intArrayOf(2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2),
                intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
                intArrayOf(2,0,2,0,2,0,1,0,1,0,1,0,1,0,1,0,2,0,2,0,2),
                intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
                intArrayOf(2,0,2,0,2,0,1,0,1,1,0,1,1,0,1,0,2,0,2,0,2),
                intArrayOf(2,0,0,0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,0,0,2),
                intArrayOf(5,0,2,0,2,0,1,0,0,0,0,0,0,0,1,0,2,0,2,0,5),
                intArrayOf(2,0,0,0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,0,0,2),
                intArrayOf(2,0,2,0,2,0,1,0,1,1,0,1,1,0,1,0,2,0,2,0,2),
                intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
                intArrayOf(2,0,2,0,2,0,1,0,1,0,1,0,1,0,1,0,2,0,2,0,2),
                intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
                intArrayOf(2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2),
                intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
                intArrayOf(2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2),
                intArrayOf(5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5),
                intArrayOf(2,5,2,2,2,2,2,2,2,2,5,2,2,2,2,2,2,2,2,5,2)
            )
        }
        checkKeyPickup()
    }
}
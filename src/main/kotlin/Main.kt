package org.example.MainKt

//./gradlew shadowJar
//0-air 1-wall 2-black_wall 3-enemy 4-ammo 5-door 6-lightSource 7-medication 8-key 10-chest

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.awt.BasicStroke
import javax.swing.JFrame
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JPanel
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import java.util.PriorityQueue
import javax.imageio.ImageIO
import javax.swing.JLayeredPane
import kotlin.math.*
import kotlin.random.Random

var playerHealth: Int = 100
var level: Int = 1
var points: Int = 0
var keys: Int = 0

var map = true
var currentangle = 45
var tileSize = 40.0
val mapa = 0.5
var MouseSupport = false

val TARGET_FPS = 90
val FRAME_TIME_NS = 1_000_000_000 / TARGET_FPS
var deltaTime = 1.0 / TARGET_FPS

var positionX = (tileSize*11)-(tileSize/2)  //tile*positon - (half tile)
var positionY = (tileSize*11)-(tileSize/2)  //tile*positon - (half tile)
var enemies = mutableListOf<Enemy>()
var lightSources = mutableListOf<LightSource>()
var keysList = mutableListOf<Key>()
var medicationsList = mutableListOf<Medication>()
var chestsList = mutableListOf<Chest>()
var ammoList = mutableListOf<Ammo>()
var inventoryVisible = false
var openChest: Chest? = null
var lookchest = false
var playerInventory = MutableList<Item?>(9) { null }
var isShooting = false
var currentAmmo = 45

class Medication(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
    var heal: Int = 100
) {
    val size = 0.5 * tileSize // Medication size (radius)
    val pickupDistance = 0.7 * 2 * size // radius for pickup
    val amount: Int = 1
}

class Key(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
) {
    val size = 0.5 * tileSize // Key size (radius)
    val pickupDistance = 0.7 * 2 * size // radius for pickup
    val amount: Int = 1
}

class Ammo(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
    val amount: Int = 10
) {
    val size = 0.5 * tileSize
    val pickupDistance = 0.7 * 2 * size
}

class LightSource(
    var x: Double,
    var y: Double,
    var color: Color,
    var intensity: Double,
    var range: Double,
    var owner: String = ""
)

class Chest(
    var x: Double,
    var y: Double,
    var loot: MutableList<Item>,
    var active: Boolean = true,
) {
    val size = 1.5 * tileSize
    val pickupDistance = 1.3*2
}

data class Item(val type: ItemType, var quantity: Int = 1) {
    // Maksymalna ilość przedmiotów na slot
    companion object {
        const val MAX_KEYS_PER_SLOT = 64
        const val MAX_AMMO_PER_SLOT = 45
        const val MAX_MEDKIT_PER_SLOT = 2

        fun getMaxQuantity(type: ItemType): Int = when (type) {
            ItemType.KEY -> MAX_KEYS_PER_SLOT
            ItemType.AMMO -> MAX_AMMO_PER_SLOT
            ItemType.MEDKIT -> MAX_MEDKIT_PER_SLOT
        }
    }
}

enum class ItemType {
    MEDKIT, AMMO, KEY
}

class Enemy(
    var x: Double,
    var y: Double,
    var health: Int = 100,
    var texture: BufferedImage,
    private val renderCast: RenderCast,
    private val map: Map,
    var speed: Double = 0.9,
    val maxHeal: Int = (100 + (level * 7.5) * 2).toInt()
) {
    var path: List<Node> = emptyList()
    val size = 1.0
    private val margin = 1
    private var pathUpdateTimer = 30
    private val pathUpdateInterval = 60 // More frequent updates to adapt to enemy positions
    private var stuckCounter = 0
    private val maxStuckFrames = 60
    var lastMoveX = 0.0
    var lastMoveY = 0.0
    var isMoving = false
    private var smoothedMoveX = 0.3
    private var smoothedMoveY = 0.3
    private val smoothingFactor = 0.1
    private val MIN_PLAYER_DISTANCE = 0.5 * tileSize
    private var lastPlayerX = 0.0
    private var lastPlayerY = 0.0
    private var idleTimer = 0
    private val idleThreshold = (1.5 * TARGET_FPS).toInt()
    private var randomMoveTimer = 0
    private val randomMoveInterval = 300
    private val randomMoveDistance = tileSize * 0.5
    private val moveThreshold = tileSize * 3.5
    private var lastX = x
    private var lastY = y
    private var accumulatedDistance = 0.0
    private var isChasing = true
    private val CHASE_STOP_DISTANCE = 20.0 * tileSize
    private val CHASE_RESUME_DISTANCE = 15.0 * tileSize
    private val ENEMY_AVOIDANCE_RADIUS = 1.0 * tileSize // Distance to penalize other enemies
    private val ENEMY_AVOIDANCE_COST = 10.0 // Cost penalty for being near another enemy
    private val ALIGNMENT_PENALTY = 5.0 // Penalty for aligning with other enemies
    private var shootTimer = 0
    private val minShootInterval = (1.0 * TARGET_FPS).toInt()
    private val maxShootInterval = (2.5 * TARGET_FPS).toInt()
    private var nextShootTime = Random.nextInt(minShootInterval, maxShootInterval + 1)
    private val projectiles = mutableListOf<Projectile>()

    companion object {
        const val MIN_WALL_DISTANCE = 0.1
        const val DIRECT_MOVE_THRESHOLD = 3.0
        var PLAYER_MOVE_THRESHOLD = tileSize
        private var globalChaseTimer = 0
        private val pathCache = mutableMapOf<Pair<Node, Node>, List<Node>>()

        fun updateGlobalChaseTimer() { globalChaseTimer++ }
        fun resetGlobalChaseTimer() { globalChaseTimer = 0 }
        fun getGlobalChaseTimer(): Int = globalChaseTimer
    }

    data class Node(val x: Int, val y: Int)

    private inner class Projectile(
        var x: Double,
        var y: Double,
        val dx: Double,
        val dy: Double,
        val speed: Double = 5.0,
        val size: Double = 0.1 * tileSize,
        val damage: Int = ((2.5 * level)*1.5).toInt(),
        var active: Boolean = true,
        val lightSource: LightSource? = null
    ) {
        fun update() {
            if (!active) {
                lightSource?.let { lightSources.remove(it) }
                return
            }
            x += dx * speed * deltaTime * TARGET_FPS
            y += dy * speed * deltaTime * TARGET_FPS

            val gridX = (x / tileSize).toInt()
            val gridY = (y / tileSize).toInt()
            if (gridY in map.grid.indices && gridX in map.grid[0].indices) {
                if (map.grid[gridY][gridX] == 1 || map.grid[gridY][gridX] == 2) {
                    active = false
                    lightSource?.let { lightSources.remove(it) }
                    return
                }
            }

            val dxToPlayer = x - positionX
            val dyToPlayer = y - positionY
            val distanceToPlayer = sqrt(dxToPlayer * dxToPlayer + dyToPlayer * dyToPlayer)
            if (distanceToPlayer < size + (tileSize * 0.5)) {
                playerHealth -= damage
                val random = Random.nextFloat()
                if (playerHealth > 15) {
                    renderCast.playSound(when {
                        random < 0.20f -> "hurt1.wav"
                        random < 0.40f -> "hurt2.wav"
                        random < 0.60f -> "hurt3.wav"
                        random < 0.80f -> "hurt4.wav"
                        else -> "hurt5.wav"
                    }, volume = 0.65f)
                } else {
                    renderCast.playSound("hurt6.wav", volume = 0.65f)
                }
                active = false
                lightSource?.let { lightSources.remove(it) }
                if (playerHealth <= 0) {
                    playerHealth = 0
                }
            }

            lightSource?.let {
                it.x = x / tileSize
                it.y = y / tileSize
            }
        }

        fun render(g2: Graphics2D) {
            if (!active) return
            val playerGridX = positionX / tileSize
            val playerGridY = positionY / tileSize
            val miniMapSize = 200
            val offsetX = 10
            val offsetY = 10
            val maxRenderTiles = 25
            val tileScale = miniMapSize.toDouble() / maxRenderTiles
            val playerMapX = miniMapSize / 2 + offsetX
            val playerMapY = miniMapSize / 2 + offsetY

            val relativeX = (x / tileSize) - playerGridX
            val relativeY = (y / tileSize) - playerGridY
            val projX = (playerMapX + relativeX * tileScale).toInt()
            val projY = (playerMapY + relativeY * tileScale).toInt()
            if (projX >= offsetX && projX < miniMapSize + offsetX && projY >= offsetY && projY < miniMapSize + offsetY) {
                g2.color = Color.RED
                g2.fillOval(projX - 2, projY - 2, 4, 4)
            }
        }
    }

    private fun shootAtPlayer() {
        val dx = positionX - x
        val dy = positionY - y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance > 0) {
            val directionX = dx / distance
            val directionY = dy / distance
            val lightSource = LightSource(
                x = x / tileSize,
                y = y / tileSize,
                color = Color(255, 100, 100),
                intensity = 0.5,
                range = 0.2,
                owner = "projectile_${this.hashCode()}_${System.nanoTime()}"
            )
            lightSources.add(lightSource)
            projectiles.add(Projectile(x, y, directionX, directionY, lightSource = lightSource))
        }
    }

    private fun heuristic(node: Node, goal: Node): Double {
        return (abs(node.x - goal.x) + abs(node.y - goal.y)).toDouble()
    }

    private fun isAlignedWithEnemies(node: Node, enemies: List<Enemy>): Boolean {
        var sameRowCount = 0
        var sameColCount = 0
        val nodeX = (node.x + 0.5) * tileSize
        val nodeY = (node.y + 0.5) * tileSize
        enemies.forEach { other ->
            if (other !== this && other.health > 0) {
                val otherX = other.x
                val otherY = other.y
                if (abs(otherY - nodeY) < tileSize / 2) sameRowCount++
                if (abs(otherX - nodeX) < tileSize / 2) sameColCount++
            }
        }
        return sameRowCount >= 2 || sameColCount >= 2
    }

    private fun calculateNodeCost(node: Node, enemies: List<Enemy>): Double {
        var cost = 1.0
        val nodeX = (node.x + 0.5) * tileSize
        val nodeY = (node.y + 0.5) * tileSize

        enemies.forEach { other ->
            if (other !== this && other.health > 0) {
                val dx = nodeX - other.x
                val dy = nodeY - other.y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < ENEMY_AVOIDANCE_RADIUS) {
                    cost += ENEMY_AVOIDANCE_COST * (1.0 - distance / ENEMY_AVOIDANCE_RADIUS)
                }
            }
        }

        if (isAlignedWithEnemies(node, enemies)) {
            cost += ALIGNMENT_PENALTY
        }

        return cost
    }

    fun findPath(): List<Node> {
        if (health <= 0 || !isChasing) return emptyList()

        val startX = (x / tileSize).toInt()
        val startY = (y / tileSize).toInt()
        val goalX = (positionX / tileSize).toInt()
        val goalY = (positionY / tileSize).toInt()

        if (startY !in map.grid.indices || startX !in map.grid[0].indices ||
            goalY !in map.grid.indices || goalX !in map.grid[0].indices ||
            //0-air 1-wall 2-black_wall 3-enemy 4-ammo 5-door 6-lightSource 7-medication 8-key 10-chest
            (((map.grid[goalY][goalX] != 0) and
                    (map.grid[goalY][goalX] != 3) and
                    (map.grid[goalY][goalX] != 4) and
                    (map.grid[goalY][goalX] != 6) and
                    (map.grid[goalY][goalX] != 7) and
                    (map.grid[goalY][goalX] != 8) and
                    (map.grid[goalY][goalX] != 10)))
        ) {
            return emptyList()
        }

        val dxToGoal = (goalX - startX).toDouble()
        val dyToGoal = (goalY - startY).toDouble()
        val distanceToGoal = sqrt(dxToGoal * dxToGoal + dyToGoal * dyToGoal)
        if (distanceToGoal <= DIRECT_MOVE_THRESHOLD) {
            val (canMove, _) = canMoveTo((goalX + 0.5) * tileSize, (goalY + 0.5) * tileSize)
            if (canMove) {
                return listOf(Node(goalX, goalY))
            }
        }

        val cacheKey = Pair(Node(startX, startY), Node(goalX, goalY))
        pathCache[cacheKey]?.let { return it }

        data class AStarNode(val node: Node, val fScore: Double, val gScore: Double)

        val openSet = PriorityQueue<AStarNode>(compareBy { it.fScore })
        val startNode = Node(startX, startY)
        openSet.add(AStarNode(startNode, heuristic(startNode, Node(goalX, goalY)), 0.0))

        val cameFrom = mutableMapOf<Node, Node>()
        val gScore = mutableMapOf(startNode to 0.0)
        val fScore = mutableMapOf(startNode to heuristic(startNode, Node(goalX, goalY)))
        val enemies = renderCast.getEnemies()

        while (openSet.isNotEmpty()) {
            val current = openSet.poll().node
            if (current.x == goalX && current.y == goalY) {
                val path = mutableListOf(current)
                var curr = current
                while (curr in cameFrom) {
                    curr = cameFrom[curr]!!
                    path.add(curr)
                }
                val result = path.reversed()
                pathCache[cacheKey] = result
                return result
            }

            for (neighbor in getNeighbors(current)) {
                val tentativeGScore = gScore[current]!! + calculateNodeCost(neighbor, enemies)
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    fScore[neighbor] = tentativeGScore + heuristic(neighbor, Node(goalX, goalY))
                    openSet.add(AStarNode(neighbor, fScore[neighbor]!!, gScore[neighbor]!!))
                }
            }
        }

        return emptyList()
    }

    fun getNeighbors(node: Node): List<Node> {
        val neighbors = mutableListOf<Node>()
        val directions = listOf(
            Node(node.x + 1, node.y),
            Node(node.x - 1, node.y),
            Node(node.x, node.y + 1),
            Node(node.x, node.y - 1)
        )

        for (dir in directions) {
            if (dir.y in map.grid.indices && dir.x in map.grid[0].indices &&
                map.grid[dir.y][dir.x] != 1
            ) {
                neighbors.add(dir)
            }
        }
        return neighbors
    }

    private fun calculateRepulsion(enemies: List<Enemy>): Pair<Double, Double> {
        var repelX = 0.0
        var repelY = 0.0
        enemies.forEach { other ->
            if (other !== this && other.health > 0) {
                val dx = x - other.x
                val dy = y - other.y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance in 0.1..ENEMY_AVOIDANCE_RADIUS) {
                    val force = (ENEMY_AVOIDANCE_RADIUS - distance) / ENEMY_AVOIDANCE_RADIUS
                    repelX += (dx / distance) * force * speed
                    repelY += (dy / distance) * force * speed
                }
            }
        }
        return Pair(repelX, repelY)
    }

    fun update() {
        if (health <= 0) {
            path = emptyList()
            isMoving = false
            idleTimer = 0
            randomMoveTimer = 0
            accumulatedDistance = 0.0
            lastX = x
            lastY = y
            projectiles.forEach { projectile ->
                projectile.active = false
                projectile.lightSource?.let { lightSources.remove(it) }
            }
            projectiles.clear()
            lightSources.removeIf { it.owner == this.toString() }
            return
        }

        val deltaTime = 1.0 / TARGET_FPS
        val dx = x - lastX
        val dy = y - lastY
        val distanceMoved = sqrt(dx * dx + dy * dy)
        accumulatedDistance += distanceMoved
        lastX = x
        lastY = y

        if (accumulatedDistance >= moveThreshold) {
            idleTimer = 0
            accumulatedDistance = 0.0
        } else {
            idleTimer++
        }

        val dxToPlayer = x - positionX
        val dyToPlayer = y - positionY
        val distanceToPlayer = sqrt(dxToPlayer * dxToPlayer + dyToPlayer * dyToPlayer)

        if (isChasing && distanceToPlayer > CHASE_STOP_DISTANCE) {
            isChasing = false
            path = emptyList()
            isMoving = false
        } else if (!isChasing && distanceToPlayer < CHASE_RESUME_DISTANCE) {
            isChasing = true
        }

        shootTimer++
        if (shootTimer >= nextShootTime && isChasing && distanceToPlayer < CHASE_STOP_DISTANCE) {
            shootAtPlayer()
            shootTimer = 0
            nextShootTime = Random.nextInt(minShootInterval, maxShootInterval + 1)
        }

        projectiles.forEach { it.update() }
        projectiles.removeAll { !it.active }

        val playerMoved = sqrt((positionX - lastPlayerX) * (positionX - lastPlayerX) + (positionY - lastPlayerY) * (positionY - lastPlayerY)) > PLAYER_MOVE_THRESHOLD
        lastPlayerX = positionX
        lastPlayerY = positionY

        if ((getGlobalChaseTimer() >= TARGET_FPS || playerMoved) && isChasing) {
            path = findPath()
            pathUpdateTimer = 0
            stuckCounter = 0
            smoothedMoveX = 0.0
            smoothedMoveY = 0.0
            resetGlobalChaseTimer()
        }

        if (distanceToPlayer < MIN_PLAYER_DISTANCE && path.isEmpty() && isChasing) {
            val moveSpeed = speed * deltaTime * TARGET_FPS
            val rawMoveX = if (distanceToPlayer > 0) (dxToPlayer / distanceToPlayer) * moveSpeed else 0.0
            val rawMoveY = if (distanceToPlayer > 0) (dyToPlayer / distanceToPlayer) * moveSpeed else 0.0

            smoothedMoveX = smoothedMoveX * (1.0 - smoothingFactor) + rawMoveX * smoothingFactor
            smoothedMoveY = smoothedMoveY * (1.0 - smoothingFactor) + rawMoveY * smoothingFactor

            val (canMove, collidedEnemy) = canMoveTo(x + smoothedMoveX, y + smoothedMoveY)
            if (canMove) {
                x += smoothedMoveX
                y += smoothedMoveY
                lastMoveX = smoothedMoveX
                lastMoveY = smoothedMoveY
                isMoving = true
                stuckCounter = 0
            } else if (collidedEnemy != null && tryPush(collidedEnemy, smoothedMoveX, smoothedMoveY)) {
                x += smoothedMoveX
                y += smoothedMoveY
                lastMoveX = smoothedMoveX
                lastMoveY = smoothedMoveY
                isMoving = true
                stuckCounter = 0
            } else {
                stuckCounter++
            }
            return
        }

        pathUpdateTimer++
        if (pathUpdateTimer >= pathUpdateInterval || stuckCounter > maxStuckFrames) {
            if (isChasing) {
                path = findPath()
            }
            pathUpdateTimer = 0
            stuckCounter = 0
        }

        isMoving = path.isNotEmpty()
        if (path.isNotEmpty()) {
            val targetNode = path.first()
            val targetX = (targetNode.x + 0.5) * tileSize
            val targetY = (targetNode.y + 0.5) * tileSize

            val dx = targetX - x
            val dy = targetY - y
            val distance = sqrt(dx * dx + dy * dy)

            if (distance > 0.1) {
                val moveSpeed = speed * deltaTime * TARGET_FPS
                val rawMoveX = if (distance > 0) (dx / distance) * moveSpeed else 0.0
                val rawMoveY = if (distance > 0) (dy / distance) * moveSpeed else 0.0

                val (repelX, repelY) = calculateRepulsion(renderCast.getEnemies())
                smoothedMoveX = smoothedMoveX * (1.0 - smoothingFactor) + (rawMoveX + repelX) * smoothingFactor
                smoothedMoveY = smoothedMoveY * (1.0 - smoothingFactor) + (rawMoveY + repelY) * smoothingFactor

                val newX = x + smoothedMoveX
                val newY = y + smoothedMoveY

                val (canMove, collidedEnemy) = canMoveTo(newX, newY)
                if (canMove) {
                    x = newX
                    y = newY
                    lastMoveX = smoothedMoveX
                    lastMoveY = smoothedMoveY
                    stuckCounter = 0
                } else if (collidedEnemy != null) {
                    if (tryPush(collidedEnemy, smoothedMoveX, smoothedMoveY)) {
                        x = newX
                        y = newY
                        lastMoveX = smoothedMoveX
                        lastMoveY = smoothedMoveY
                        stuckCounter = 0
                    } else {
                        stuckCounter++
                        val nudgeX = x - (dx / distance) * 0.1
                        val nudgeY = y - (dy / distance) * 0.1
                        if (canMoveTo(nudgeX, nudgeY).first) {
                            x = nudgeX
                            y = nudgeY
                        }
                    }
                } else {
                    stuckCounter++
                    val nudgeX = x - (dx / distance) * 0.1
                    val nudgeY = y - (dy / distance) * 0.1
                    if (canMoveTo(nudgeX, nudgeY).first) {
                        x = nudgeX
                        y = nudgeY
                    }
                }
            } else {
                path = path.drop(1)
                stuckCounter = 0
                isMoving = path.isNotEmpty()
            }
        }

        randomMoveTimer++
        if (idleTimer >= idleThreshold && randomMoveTimer >= randomMoveInterval && isChasing) {
            val directions = listOf(
                Pair(1.0, 0.0), Pair(-1.0, 0.0), Pair(0.0, 1.0), Pair(0.0, -1.0)
            )
            val (moveX, moveY) = directions.random()
            val newX = x + moveX * randomMoveDistance
            val newY = y + moveY * randomMoveDistance

            val (canMove, _) = canMoveTo(newX, newY)
            if (canMove) {
                x = newX
                y = newY
                lastMoveX = moveX * randomMoveDistance
                lastMoveY = moveY * randomMoveDistance
                path = findPath()
                isMoving = true
                idleTimer = 0
                randomMoveTimer = 0
                accumulatedDistance = 0.0
            }
        }
    }

    fun renderProjectiles(g2: Graphics2D) {
        projectiles.forEach { it.render(g2) }
    }

    fun canMoveTo(newX: Double, newY: Double, exclude: Enemy? = null): Pair<Boolean, Enemy?> {
        val left = newX - size / 2
        val right = newX + size / 2
        val top = newY - size / 2
        val bottom = newY + size / 2

        val gridLeft = ((left - margin) / tileSize).toInt()
        val gridRight = ((right + margin) / tileSize).toInt()
        val gridTop = ((top - margin) / tileSize).toInt()
        val gridBottom = ((bottom + margin) / tileSize).toInt()

        for (gridY in gridTop..gridBottom) {
            for (gridX in gridLeft..gridRight) {
                if (gridY !in map.grid.indices || gridX !in map.grid[gridY].indices ||
                    ((map.grid[gridY][gridX] != 0) && (map.grid[gridY][gridX] != 5) &&
                            (map.grid[gridY][gridX] != 3) && (map.grid[gridY][gridX] != 6))) {
                    return Pair(false, null)
                }
            }
        }

        renderCast.getEnemies().forEach { otherEnemy ->
            if (otherEnemy !== this && otherEnemy !== exclude && otherEnemy.health > 0) {
                val dx = newX - otherEnemy.x
                val dy = newY - otherEnemy.y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < size) {
                    return Pair(false, otherEnemy)
                }
            }
        }

        val dx = newX - positionX
        val dy = newY - positionY
        val newDistance = sqrt(dx * dx + dy * dy)
        val currentDistance = sqrt((x - positionX) * (x - positionX) + (y - positionY) * (y - positionY))
        if (newDistance < MIN_PLAYER_DISTANCE && newDistance < currentDistance) {
            return Pair(false, null)
        }

        return Pair(true, null)
    }

    fun tryPush(otherEnemy: Enemy, moveX: Double, moveY: Double): Boolean {
        if (otherEnemy.isMoving) {
            val dotProduct = (moveX * otherEnemy.lastMoveX + moveY * otherEnemy.lastMoveY)
            if (dotProduct < 0) {
                return false
            }
        }
        val newEnemyX = otherEnemy.x + moveX * 0.5
        val newEnemyY = otherEnemy.y + moveY * 0.5
        val (canMove, _) = otherEnemy.canMoveTo(newEnemyX, newEnemyY, this)
        if (canMove) {
            otherEnemy.x = newEnemyX
            otherEnemy.y = newEnemyY
            otherEnemy.lastMoveX = moveX
            otherEnemy.lastMoveY = moveY
            otherEnemy.isMoving = true
            return true
        }
        return false
    }
}

fun main() = runBlocking {
    val frame = JFrame("rolada z gówna")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.iconImage = Toolkit.getDefaultToolkit().getImage(this::class.java.classLoader.getResource("icon/icon.jpg"))
    frame.isResizable = false
    frame.setSize(1366, 768)
    frame.setLocation(((Toolkit.getDefaultToolkit().screenSize.width - frame.width) / 2), ((Toolkit.getDefaultToolkit().screenSize.height - frame.height) / 2))

    frame.isVisible = true
    val map = Map()

    val renderCast = RenderCast(map)
    map.renderCast = renderCast

    val player = Player(renderCast, map)

    val layeredPane = JLayeredPane()
    layeredPane.setSize(1366, 768)
    layeredPane.setBounds(0, 0, 1366, 768)
    frame.add(layeredPane)

    val mapa = Mappingmap(map, renderCast)
    mapa.isOpaque = false
    mapa.layout = null
    mapa.setSize(1366, 768)
    mapa.setBounds(0, 0, 1366, 768)

    renderCast.isOpaque = false
    renderCast.setSize(1366, 768)
    renderCast.setBounds(0, 0, 1366, 768)

    layeredPane.add(mapa, 1)
    layeredPane.add(renderCast, 2)

    val keysPressed: MutableMap<Int, Boolean> = mutableMapOf()
    var centerX = frame.width / 2

    var remainingAmmo = 45
    var slotIndexAmmo = 0
    while (remainingAmmo > 0 && slotIndexAmmo < playerInventory.size) {
        val quantity = minOf(remainingAmmo, Item.MAX_AMMO_PER_SLOT)
        playerInventory[slotIndexAmmo] = Item(ItemType.AMMO, quantity)
        remainingAmmo -= quantity
        slotIndexAmmo++
    }

    var remainingKeys = 200
    var slotIndexKey = 1
    while (remainingKeys > 0 && slotIndexKey < playerInventory.size) {
        val quantity = minOf(remainingKeys, Item.MAX_KEYS_PER_SLOT)
        playerInventory[slotIndexKey] = Item(ItemType.KEY, quantity)
        remainingKeys -= quantity
        slotIndexKey++
    }

    frame.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                if (inventoryVisible) {
                    renderCast.handleInventoryClick(event.x, event.y)
                    return
                }
                renderCast.shotgun()
            }
        }

        override fun mouseReleased(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                isShooting = false
            }
        }
    })

    frame.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseMoved(e: MouseEvent) {
            player.updateAngleFromMouse()
            renderCast.repaint()
        }

        override fun mouseDragged(e: MouseEvent) {
            player.updateAngleFromMouse()
            renderCast.repaint()
        }
    })

    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            keysPressed[event.keyCode] = true
            when (event.keyCode) {

                KeyEvent.VK_SPACE -> {
                    if (!isShooting) {
                        renderCast.shotgun()
                    }
                }
            }
        }

        override fun keyReleased(event: KeyEvent) {
            keysPressed[event.keyCode] = false
            when (event.keyCode) {
                KeyEvent.VK_SPACE -> isShooting = false
                KeyEvent.VK_E -> inventoryVisible = !inventoryVisible
            }
        }

    })

    frame.addComponentListener(object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent?) {
            centerX = frame.x + frame.width / 2
        }
    })

    //fps counter
    var lastFrameTime = System.nanoTime()
    var lastFpsUpdate = System.nanoTime()

    while (true) {
        if (inventoryVisible) {
            renderCast.updateOpenChest()
        } else {
            openChest = null
        }

        val currentTime = System.nanoTime()
        val elapsedTime = currentTime - lastFrameTime

        if (elapsedTime >= (1_000_000_000 / 120)) {
            player.update(keysPressed)
            renderCast.repaint()
            mapa.repaint()

            lastFrameTime = currentTime

            if (currentTime - lastFpsUpdate >= 1_000_000_000L) {
                lastFpsUpdate = currentTime
            }
        }

        val timeToNextFrame = FRAME_TIME_NS - (System.nanoTime() - lastFrameTime)
        if (timeToNextFrame > 0) {
            delay((timeToNextFrame / 1_000_000))
        }
    }
}

class Map(var renderCast: RenderCast? = null) {
    var grid: Array<IntArray> = arrayOf(
        intArrayOf(2,5,2,2,2,2,2,2,2,2,5,2,2,2,2,2,2,2,2,5,2),
        intArrayOf(5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5),
        intArrayOf(2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2),
        intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
        intArrayOf(2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2),
        intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
        intArrayOf(2,0,2,0,2,0,1,0,1,0,1,0,1,0,1,0,2,0,2,0,2),
        intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
        intArrayOf(2,0,2,0,2,0,5,0,1,1,0,1,1,0,1,0,2,0,2,0,2),
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

    var gridRooms: Array<IntArray> = arrayOf(
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
    )

    val limitRooms = 27
    var currentRooms = 0
    enum class Direction { UP, DOWN, LEFT, RIGHT }

    data class RoomTemplate(
        val grid: Array<IntArray>,
        val scale: Int
    )

    fun generateRoom(x: Int = 0, y: Int = 0, enterdirection: Direction, roomTemplate: RoomTemplate = RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2, 5, 2, 2),
            intArrayOf(2, 0, 0, 0, 2),
            intArrayOf(5, 0, 3, 0, 5),
            intArrayOf(2, 0, 0, 0, 2),
            intArrayOf(2, 2, 5, 2, 2)
        ),
        scale = 5
    )) {
        var shift = 0
        if (((x + roomTemplate.scale) > grid.size) || ((y + roomTemplate.scale) > grid.size) || ((x - roomTemplate.scale) < 0) || ((y - roomTemplate.scale) < 0)) {
            val row = grid.size
            val col = grid.size

            if ((x + roomTemplate.scale) > grid.size) {
                println("$enterdirection") //LEFT
            }
            if ((y + roomTemplate.scale) > grid.size) {
                println("$enterdirection") //UP
            }
            if ((x - roomTemplate.scale) < 0) {
                print("$enterdirection") //RIGHT
            }
            if ((y - roomTemplate.scale) < 0) {
                print("$enterdirection") //DOWN
            }
        }

        if (keys > 0) {
            var clear = true
            for (XX in 0..(roomTemplate.scale-1)) {
                for (YY in 0..(roomTemplate.scale-1)) {
                    if(clear) {
                        if (enterdirection == Direction.UP) {
                            if (gridRooms[(y+YY)-(roomTemplate.scale-(roomTemplate.scale+1))][(x+XX)-(roomTemplate.scale/2)] != 0) {
                                clear = false
                            }
                        }
                        if (enterdirection == Direction.DOWN) {
                            if (gridRooms[(y+YY)-(roomTemplate.scale)][(x+XX)-(roomTemplate.scale/2)] != 0) {
                                clear = false
                            }
                        }
                        if (enterdirection == Direction.RIGHT) {
                            if (gridRooms[(y+YY)-(roomTemplate.scale/2)][(x+XX)-(roomTemplate.scale)] != 0) {
                                clear = false
                            }
                        }
                        if (enterdirection == Direction.LEFT) {
                            if (gridRooms[(y+YY)-(roomTemplate.scale/2)][(x+XX)-(roomTemplate.scale-(roomTemplate.scale+1))] != 0) {
                                clear = false
                            }
                        }
                    }
                }
            }
            if (clear) {
                grid[y][x] = 0
                for (XX in 0..(roomTemplate.scale-1)) {
                    for (YY in 0..(roomTemplate.scale-1)) {
                        val distGrid = when {
                            enterdirection == Direction.UP -> grid[(y+YY)-(roomTemplate.scale-(roomTemplate.scale+1))][(x+XX)-(roomTemplate.scale/2)] = roomTemplate.grid[YY][XX]
                            enterdirection == Direction.DOWN -> grid[(y+YY)-(roomTemplate.scale)][(x+XX)-(roomTemplate.scale/2)] = roomTemplate.grid[YY][XX]
                            enterdirection == Direction.RIGHT -> grid[(y+YY)-(roomTemplate.scale/2)][(x+XX)-roomTemplate.scale] = roomTemplate.grid[YY][XX]
                            else -> grid[(y+YY)-(roomTemplate.scale/2)][(x+XX)-(roomTemplate.scale-(roomTemplate.scale+1))] = roomTemplate.grid[YY][XX]
                        }
                        val distRooms = when {
                            enterdirection == Direction.UP -> gridRooms[(y+YY)-(roomTemplate.scale-(roomTemplate.scale+1))][(x+XX)-(roomTemplate.scale/2)] = roomTemplate.grid[YY][XX]
                            enterdirection == Direction.DOWN -> gridRooms[(y+YY)-(roomTemplate.scale)][(x+XX)-(roomTemplate.scale/2)] = roomTemplate.grid[YY][XX]
                            enterdirection == Direction.RIGHT -> gridRooms[(y+YY)-(roomTemplate.scale/2)][(x+XX)-roomTemplate.scale] = roomTemplate.grid[YY][XX]
                            else -> gridRooms[(y+YY)-(roomTemplate.scale/2)][(x+XX)-(roomTemplate.scale-(roomTemplate.scale+1))] = roomTemplate.grid[YY][XX]
                        }
                        distGrid
                        distRooms

                        val distItemX = when {
                            enterdirection == Direction.UP -> ((x+XX)-(roomTemplate.scale/2))
                            enterdirection == Direction.DOWN -> ((x+XX)-(roomTemplate.scale/2))
                            enterdirection == Direction.RIGHT -> ((x+XX)-roomTemplate.scale)
                            else -> ((x+XX)-(roomTemplate.scale-(roomTemplate.scale+1)))
                        }
                        val distItemY = when {
                            enterdirection == Direction.UP -> ((y+YY)-(roomTemplate.scale-(roomTemplate.scale+1)))
                            enterdirection == Direction.DOWN -> ((y+YY)-(roomTemplate.scale))
                            enterdirection == Direction.RIGHT -> ((y+YY)-(roomTemplate.scale/2))
                            else -> ((y+YY)-(roomTemplate.scale/2))
                        }

                        if (roomTemplate.grid[XX][YY] == 3) {
                            renderCast?.let {
                                enemies.add(
                                    Enemy(
                                        (tileSize * distItemX) - (tileSize / 2),
                                        (tileSize * distItemY) - (tileSize / 2),
                                        (100 + (level * 7.5) * 2).toInt(),
                                        renderCast!!.enemyTextureId!!,
                                        renderCast = it,
                                        this,
                                        speed = (2.0 * ((10..15).random() / 10.0))
                                    )
                                )
                                lightSources.add(
                                    LightSource(
                                        (distItemX + 0.5),
                                        (distItemY + 0.5),
                                        color = Color(20, 20, 200),
                                        intensity = 0.25,
                                        range = 1.0,
                                        owner = "${enemies[enemies.size - 1]}"
                                    )
                                )
                            } ?: throw IllegalStateException("renderCast is null")
                        }
                        if (roomTemplate.grid[XX][YY] == 4) {
                            ammoList.add(
                                Ammo(
                                    x = (tileSize * (distItemX+1)) - (tileSize / 2),
                                    y = (tileSize * (distItemY+1)) - (tileSize / 2),
                                    texture = renderCast?.ammoTextureID!!,
                                    active = true
                                )
                            )
                        }
                        if (roomTemplate.grid[XX][YY] == 6) {
                            renderCast?.let {
                                lightSources.add(
                                    LightSource(
                                        (distItemX + 0.5),
                                        (distItemY + 0.5),
                                        color = Color(200, 20, 20),
                                        intensity = 0.25,
                                        range = 3.0,
                                        owner = "skun"
                                    )
                                )
                                it.repaint()
                            }
                        }
                        if (roomTemplate.grid[XX][YY] == 7) {
                            val random = Random.nextFloat()
                            val healRNG = when {
                                random < 0.33f -> 15
                                random < 0.66f -> 25
                                else -> 35
                            }

                            renderCast?.let {
                                medicationsList.add(
                                    Medication(
                                        x = ((tileSize * (distItemX + 1)) - (tileSize / 2)),
                                        y = ((tileSize * (distItemY + 1)) - (tileSize / 2)),
                                        renderCast?.medicationTextureID!!,
                                        heal = healRNG
                                    )
                                )
                                it.repaint()
                            }
                        }
                        if (roomTemplate.grid[XX][YY] == 8) {
                            keysList.add(
                                Key(
                                    x = (tileSize * (distItemX+1)) - (tileSize / 2),
                                    y = (tileSize * (distItemY+1)) - (tileSize / 2),
                                    texture = renderCast?.keyTextureId!!,
                                    active = true
                                )
                            )
                        }
                        if (roomTemplate.grid[XX][YY] == 10) {
                            val items = mutableListOf<Item>()
                            val random = Random.nextFloat()
                            val itemCount = when {
                                random < 0.5f -> 1
                                random < 0.8f -> 2
                                else -> 3
                            }

                            for (i in 0 until itemCount) {
                                val itemType = when (Random.nextFloat()) {
                                    in 0.0f..0.4f -> ItemType.KEY
                                    in 0.4f..0.7f -> ItemType.AMMO
                                    else -> ItemType.MEDKIT
                                }
                                val quantity = when (itemType) {
                                    ItemType.KEY -> Random.nextInt(1, Item.MAX_KEYS_PER_SLOT / 4)
                                    ItemType.AMMO -> Random.nextInt(1, Item.MAX_AMMO_PER_SLOT / 4)
                                    ItemType.MEDKIT -> 1
                                }
                                items.add(Item(itemType, quantity))
                            }
                            println("skrzynka")
                            val spawnRNG = when {
                                random < 0.5f -> chestsList.add(Chest((tileSize * (distItemX+1)) - (tileSize / 2), (tileSize * (distItemY+1)) - (tileSize / 2), items))
                                random < 0.75f -> ammoList.add(Ammo((tileSize * (distItemX+1)) - (tileSize / 2), (tileSize * (distItemY+1)) - (tileSize / 2), texture = renderCast?.ammoTextureID!!, active = true, 6))
                                else -> println("loss chest")
                            }
                            spawnRNG
                        }
                    }
                }
                if (enterdirection == Direction.UP) {
                    grid[y+1][x] = 0
                }
                if (enterdirection == Direction.DOWN) {
                    grid[y-1][x] = 0
                }
                if (enterdirection == Direction.RIGHT) {
                    grid[y][x-1] = 0
                }
                if (enterdirection == Direction.LEFT) {
                    grid[y][x+1] = 0
                }
                val keysSlot = playerInventory.indexOfFirst { it?.type == ItemType.KEY && it.quantity > 0 }
                playerInventory[keysSlot]!!.quantity -= 1
                if (playerInventory[keysSlot]!!.quantity <= 0) {
                    playerInventory[keysSlot] = null
                }
            }
        }
    }
}

class Mappingmap(private val map: Map, private val renderCast: RenderCast) : JPanel() {
    private val miniMapSize = 200
    private val offsetX = 10
    private val offsetY = 10
    private var bufferedImage: BufferedImage? = null
    private var lastGrid: Array<IntArray>? = null
    private val maxRenderTiles = 25
    private val enemyPathColors = mutableMapOf<Enemy, Color>()
    var keypng: BufferedImage? = null
    var sliderpng: BufferedImage? = null
    private var font: Font? = null

    init {
        preferredSize = Dimension(miniMapSize + offsetX * 2, miniMapSize + offsetY * 2)
        isOpaque = false
        val fontStream = this::class.java.classLoader.getResourceAsStream("font/mojangles.ttf")
            ?: throw IllegalArgumentException("Font file not found: custom_font.ttf")
        font = Font.createFont(Font.TRUETYPE_FONT, fontStream)
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)

        sliderpng = ImageIO.read(this::class.java.classLoader.getResource("textures/slide.png"))
        keypng = ImageIO.read(this::class.java.classLoader.getResource("textures/key.png"))
    }

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        val g2 = v as Graphics2D

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val tileScale = miniMapSize.toDouble() / maxRenderTiles
        val playerMapX = miniMapSize / 2 + offsetX
        val playerMapY = miniMapSize / 2 + offsetY

        // pos player -> titesize
        val playerGridX = positionX / tileSize
        val playerGridY = positionY / tileSize

        val totalAmmo = playerInventory.filterNotNull()
            .filter { it.type == ItemType.AMMO }
            .sumOf { it.quantity }

        val totalKeys = playerInventory.filterNotNull()
            .filter { it.type == ItemType.KEY }
            .sumOf { it.quantity }

        // Cache map
        if (bufferedImage == null || !map.grid.contentDeepEquals(lastGrid)) {
            bufferedImage = BufferedImage(miniMapSize + offsetX * 2, miniMapSize + offsetY * 2, BufferedImage.TYPE_INT_ARGB)
            val bufferGraphics = bufferedImage!!.createGraphics()
            bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Clear Background
            bufferGraphics.color = Color(0, 0, 0, 0)
            bufferGraphics.fillRect(0, 0, miniMapSize + offsetX * 2, miniMapSize + offsetY * 2)

            // Calculate the rendering range around the player
            val startX = (playerGridX - maxRenderTiles / 2).toInt().coerceIn(0, map.grid[0].size - 1)
            val endX = (playerGridX + maxRenderTiles / 2).toInt().coerceIn(0, map.grid[0].size - 1)
            val startY = (playerGridY - maxRenderTiles / 2).toInt().coerceIn(0, map.grid.size - 1)
            val endY = (playerGridY + maxRenderTiles / 2).toInt().coerceIn(0, map.grid.size - 1)

            // Draw the map with the offset relative to the player, Calculate tile position relative to player position
            for (row in startY..endY) {
                for (col in startX..endX) {
                    val relativeX = col - playerGridX
                    val relativeY = row - playerGridY
                    val x = (playerMapX + relativeX * tileScale).toInt()
                    val y = (playerMapY + relativeY * tileScale).toInt()
                    val scaledTileSize = tileScale.toInt() + 1

                    // Only draw if the tile is within the boundaries of the minimap
                    if (x >= offsetX && x < miniMapSize + offsetX && y >= offsetY && y < miniMapSize + offsetY) {
                        when (map.grid[row][col]) {
                            1 -> {
                                bufferGraphics.color = Color(0, 255, 0)
                                bufferGraphics.fillRect(x, y, scaledTileSize, scaledTileSize)
                            }
                            2 -> {
                                bufferGraphics.color = Color(80, 100, 80)
                                bufferGraphics.fillRect(x, y, scaledTileSize, scaledTileSize)
                            }
                            5 -> {
                                bufferGraphics.color = Color.YELLOW
                                bufferGraphics.fillRect(x, y, scaledTileSize, scaledTileSize)
                            }
                        }
                    }
                }
            }
            bufferedImage == null
            bufferGraphics.dispose()
        }

        g2.drawImage(bufferedImage, 0, 0, null)


        sliderpng?.let {
            val offsetx = 3
            val offsety = 232
            g2.drawImage(it, offsetx, offsety, (50*4)+offsetx, (20*4)+offsety, 0, 0, it.width, it.height, null)
        }

        keypng?.let {
            val offsetx = 10
            val offsety = 250
            g2.drawImage(it, offsetx, offsety, (19*3)+offsetx, (16*3)+offsety, 0, 0, it.width, it.height, null)
        }

        val enemies = renderCast.getEnemies()
        // Draw enemies and their enemy paths. Assign them a random color. Set the path color for this enemy
        enemies.forEach { enemy ->
            if (!enemyPathColors.containsKey(enemy)) {
                enemyPathColors[enemy] = Color(((72-44)..255).random(), (72..255).random(), ((72+44)..255).random(), 144)
            }
            g2.color = enemyPathColors[enemy]
            enemy.path.forEach { node ->
                val relativeX = node.x - playerGridX
                val relativeY = node.y - playerGridY
                val pointX = (playerMapX + relativeX * tileScale).toInt()
                val pointY = (playerMapY + relativeY * tileScale).toInt()
                if (pointX >= offsetX && pointX < miniMapSize + offsetX && pointY >= offsetY && pointY < miniMapSize + offsetY) {
                    g2.fillOval(pointX - 3, pointY - 3, 5, 5)
                }
            }
        }

        enemies.forEach { enemy ->
            val relativeX = (enemy.x / tileSize) - playerGridX
            val relativeY = (enemy.y / tileSize) - playerGridY
            val enemyX = (playerMapX + relativeX * tileScale).toInt()
            val enemyY = (playerMapY + relativeY * tileScale).toInt()
            if (enemyX >= offsetX && enemyX < miniMapSize + offsetX && enemyY >= offsetY && enemyY < miniMapSize + offsetY) {
                if (enemy.health > 0) {
                    g2.color = Color.RED
                    g2.fillRect(enemyX - 3, enemyY - 3, 9, 9)
                } else {
                    g2.color = Color(0, 197, 197, 200)
                    g2.fillRect(enemyX - 3, enemyY - 3, 7, 7)
                }
            }
        }
        // draw key
        keysList.forEach { key ->
            if (key.active) {
                val relativeX = (key.x / tileSize) - playerGridX
                val relativeY = (key.y / tileSize) - playerGridY
                val keyX = (playerMapX + relativeX * tileScale).toInt()
                val keyY = (playerMapY + relativeY * tileScale).toInt()
                if (keyX >= offsetX && keyX < miniMapSize + offsetX && keyY >= offsetY && keyY < miniMapSize + offsetY) {
                    g2.color = Color.YELLOW
                    g2.fillOval(keyX - 3, keyY - 3, 6, 6)
                }
            }
        }

        //draw chest
        chestsList.forEach { chest ->
            if (chest.active) {
                val relativeX = (chest.x / tileSize) - playerGridX
                val relativeY = (chest.y / tileSize) - playerGridY
                val chestX = (playerMapX + relativeX * tileScale).toInt()
                val chestY = (playerMapY + relativeY * tileScale).toInt()
                if (chestX >= offsetX && chestX < miniMapSize + offsetX && chestY >= offsetY && chestY < miniMapSize + offsetY) {
                    g2.color = Color(150,75,0)
                    g2.fillOval(chestX - 3, chestY - 3, 6, 6)
                }
            }
        }

        //draw medication
        medicationsList.forEach { medication ->
            if (medication.active) {
                val relativeX = (medication.x / tileSize) - playerGridX
                val relativeY = (medication.y / tileSize) - playerGridY
                val medX = (playerMapX + relativeX * tileScale).toInt()
                val medY = (playerMapY + relativeY * tileScale).toInt()
                if (medX >= offsetX && medX < miniMapSize + offsetX && medY >= offsetY && medY < miniMapSize + offsetY) {
                    g2.color = Color.GREEN
                    g2.fillOval(medX - 3, medY - 3, 6, 6)
                }
            }
        }

        //draw ammo
        ammoList.forEach { ammo ->
            if (ammo.active) {
                val relativeX = (ammo.x / tileSize) - playerGridX
                val relativeY = (ammo.y / tileSize) - playerGridY
                val ammoX = (playerMapX + relativeX * tileScale).toInt()
                val ammoY = (playerMapY + relativeY * tileScale).toInt()
                if (ammoX >= offsetX && ammoX < miniMapSize + offsetX && ammoY >= offsetY && ammoY < miniMapSize + offsetY) {
                    g2.color = Color.DARK_GRAY
                    g2.fillOval(ammoX - 3, ammoY - 3, 6, 6)
                }
            }
        }

        // draw player
        val angleRad = Math.toRadians(currentangle.toDouble())
        val lineLength = 10.0
        val playerX2 = playerMapX + (lineLength * cos(angleRad)).toInt()
        val playerY2 = playerMapY + (lineLength * sin(angleRad)).toInt()

        g2.color = Color.YELLOW
        g2.stroke = BasicStroke(2f)
        g2.drawLine(playerMapX, playerMapY, playerX2, playerY2)
        g2.color = Color.darkGray
        g2.fillRect(playerMapX - 2, playerMapY - 2, 5, 5)

        g2.color = Color.white
        g2.fillRect(683, 384, 3, 3)

        g2.color = Color.YELLOW
        g2.font = font?.deriveFont(Font.BOLD, 17f) ?: Font("Arial", Font.BOLD, 17)

        g2.drawString("${renderCast.getRenderFps()*2}", 1366 - 50, 20)

        g2.drawString("HEAL: ${playerHealth}", 10, 340)
        g2.drawString("LEVEL: ${level}", 10, 360)
        g2.drawString("POINTS: ${points}", 10, 380)
        g2.drawString("AMMO: ${totalAmmo}", 10, 400)
        if (lookchest and !inventoryVisible) {
            g2.drawString("Click E", (1366+g2.font.size)/2, (768+g2.font.size)/2)
        }
        g2.font = font?.deriveFont(Font.BOLD, 50f) ?: Font("Arial", Font.BOLD, 50)
        g2.drawString("${totalKeys}", 85, 290)
        keys = totalKeys
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(miniMapSize + offsetX * 2, miniMapSize + offsetY * 2)
    }
}
package org.lewapnoob.raycast

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class Enemy(
    var x: Double,
    var y: Double,
    var health: Int = 100,
    var texture: BufferedImage,
    private val renderCast: RenderCast,
    private val map: Map,
    val speed: Double = 0.9,
    val maxHeal: Int = (100 + (level * 7.5) * 2).toInt(),
    val damage: Int = ((2.5 * level)*1.5).toInt(),
    var enemyShotAccuracy: Int = 10,
    val enemyType: Int = 0
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
    private val minShootInterval = (1.25 * TARGET_FPS).toInt()
    private val maxShootInterval = (2.5 * TARGET_FPS).toInt()
    private var nextShootTime = Random.nextInt(from = minShootInterval, until = maxShootInterval + 1)
    private val projectiles = mutableListOf<Projectile>()

    companion object {
        const val DIRECT_MOVE_THRESHOLD = 3.0
        var PLAYER_MOVE_THRESHOLD = tileSize
        private var globalChaseTimer = 0
        private val pathCache = mutableMapOf<Pair<Node, Node>, List<Node>>()

        fun resetGlobalChaseTimer() { globalChaseTimer = 0 }
        fun getGlobalChaseTimer(): Int = globalChaseTimer
    }

    data class Node(val x: Int, val y: Int)

    inner class Projectile(
        var x: Double,
        var y: Double,
        val dx: Double,
        val dy: Double,
        val speed: Double = 5.0,
        val size: Double = 0.1 * tileSize,
        var active: Boolean = true,
        val lightSource: LightSource? = null
    ) {
        private var elapsedTime: Double = 0.0 // Timer to track elapsed time
        private val lifetime: Double = 1.3 // Lifetime in seconds

        fun update() {
            if (!active) {
                lightSource?.let { lightSources.remove(element = it) }
                return
            }

            elapsedTime += deltaTime
            if (elapsedTime >= lifetime) {
                active = false
                lightSource?.let { lightSources.remove(element = it) }
                return
            }

            x += dx * speed * deltaTime * TARGET_FPS
            y += dy * speed * deltaTime * TARGET_FPS

            val gridX = (x / tileSize).toInt()
            val gridY = (y / tileSize).toInt()
            if (gridY in map.grid.indices && gridX in map.grid[0].indices) {
                if (map.grid[gridY][gridX] == 1 || map.grid[gridY][gridX] == 2 || map.grid[gridY][gridX] == 5) {
                    active = false
                    lightSource?.let { lightSources.remove(element = it) }
                    return
                }
            }

            val dxToPlayer = x - positionX
            val dyToPlayer = y - positionY
            val distanceToPlayer = sqrt(x = dxToPlayer * dxToPlayer + dyToPlayer * dyToPlayer)
            if (distanceToPlayer < size + (tileSize * 0.5)) {
                if (!godMode) {
                    playerHealth -= damage
                    val random = Random.nextFloat()
                    if (playerHealth > 15) {
                        playSound(soundFile = when {
                            random < 0.20f -> "hurt1.wav"
                            random < 0.40f -> "hurt2.wav"
                            random < 0.60f -> "hurt3.wav"
                            random < 0.80f -> "hurt4.wav"
                            else -> "hurt5.wav"
                        }, volume = 0.65f)
                    } else {
                        playSound(soundFile = "hurt6.wav", volume = 0.65f)
                    }
                }
                active = false
                lightSource?.let { lightSources.remove(element = it) }
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
        val dx = positionX - x + Random.nextInt(from = -enemyShotAccuracy, until = enemyShotAccuracy)
        val dy = positionY - y + Random.nextInt(from = -enemyShotAccuracy, until = enemyShotAccuracy)
        val distance = sqrt(dx * dx + dy * dy)
        if (distance > 0) {
            val directionX = dx / distance
            val directionY = dy / distance
            val lightSource = LightSource(
                x = x / tileSize,
                y = y / tileSize,
                color = Color(255, 70, 70),
                intensity = 0.5,
                range = 0.2,
                owner = "projectile_${this.hashCode()}_${System.nanoTime()}"
            )
            lightSources.add(lightSource)
            projectiles.add(Projectile(x= x, y = y, dx = directionX, dy = directionY, lightSource = lightSource))
        }
    }

    private fun heuristic(node: Node, goal: Node): Double {
        return (abs(n = node.x - goal.x) + abs(n = node.y - goal.y)).toDouble()
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
                if (abs(x = otherY - nodeY) < tileSize / 2) sameRowCount++
                if (abs(x = otherX - nodeX) < tileSize / 2) sameColCount++
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
                val distance = sqrt(x = dx * dx + dy * dy)
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

        if ((startY !in map.grid.indices || startX !in map.grid[0].indices ||
            goalY !in map.grid.indices || goalX !in map.grid[0].indices) or
            ((map.grid[goalY][goalX] == 1) or
                    (map.grid[goalY][goalX] == 2) or
                    (map.grid[goalY][goalX] == 5) or
                    (map.grid[goalY][goalX] == 12)
                    )
        ) {
            return emptyList()
        }

        val dxToGoal = (goalX - startX).toDouble()
        val dyToGoal = (goalY - startY).toDouble()
        val distanceToGoal = sqrt(x = dxToGoal * dxToGoal + dyToGoal * dyToGoal)
        if (distanceToGoal <= DIRECT_MOVE_THRESHOLD) {
            val (canMove, _) = canMoveTo(newX = (goalX + 0.5) * tileSize, newY = (goalY + 0.5) * tileSize)
            if (canMove) {
                return listOf(Node(x = goalX, y = goalY))
            }
        }

        val cacheKey = Pair(first =  Node(x = startX, y = startY), second = Node(x = goalX, y = goalY))
        pathCache[cacheKey]?.let { return it }

        data class AStarNode(val node: Node, val fScore: Double, val gScore: Double)

        val openSet = PriorityQueue<AStarNode>(compareBy { it.fScore })
        val startNode = Node(x = startX, y = startY)
        openSet.add(AStarNode(startNode, fScore = heuristic(startNode, goal = Node(x = goalX, y = goalY)), gScore = 0.0))

        val cameFrom = mutableMapOf<Node, Node>()
        val gScore = mutableMapOf(startNode to 0.0)
        val fScore = mutableMapOf(startNode to heuristic(startNode, goal = Node(x = goalX, y = goalY)))
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

            for (neighbor in getNeighbors(node = current)) {
                val tentativeGScore = gScore[current]!! + calculateNodeCost(node = neighbor, enemies)
                if (tentativeGScore < gScore.getOrDefault(key = neighbor, defaultValue = Double.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    fScore[neighbor] = tentativeGScore + heuristic(node = neighbor, goal = Node(x = goalX, y = goalY))
                    openSet.add(AStarNode(node = neighbor, fScore = fScore[neighbor]!!, gScore = gScore[neighbor]!!))
                }
            }
        }

        return emptyList()
    }

    fun getNeighbors(node: Node): List<Node> {
        val neighbors = mutableListOf<Node>()
        val directions = listOf(
            Node(x = node.x + 1, y = node.y),
            Node(x = node.x - 1, y = node.y),
            Node(x = node.x, y = node.y + 1),
            Node(x = node.x, y = node.y - 1)
        )

        for (dir in directions) {
            if (dir.y in map.grid.indices && dir.x in map.grid[0].indices &&
                map.grid[dir.y][dir.x] != 1 &&
                map.grid[dir.y][dir.x] != 2 &&
                map.grid[dir.y][dir.x] != 5
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
                val distance = sqrt(x = dx * dx + dy * dy)
                if (distance in 0.1..ENEMY_AVOIDANCE_RADIUS) {
                    val force = (ENEMY_AVOIDANCE_RADIUS - distance) / ENEMY_AVOIDANCE_RADIUS
                    repelX += (dx / distance) * force * speed
                    repelY += (dy / distance) * force * speed
                }
            }
        }
        return Pair(first = repelX, second = repelY)
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
                projectile.lightSource?.let { lightSources.remove(element = it) }
            }
            projectiles.clear()
            lightSources.removeIf { it.owner == this.toString() }
            return
        }

        val deltaTime = 1.0 / TARGET_FPS
        val dx = x - lastX
        val dy = y - lastY
        val distanceMoved = sqrt(x = dx * dx + dy * dy)
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
        val distanceToPlayer = sqrt(x = dxToPlayer * dxToPlayer + dyToPlayer * dyToPlayer)

        if (isChasing && distanceToPlayer > CHASE_STOP_DISTANCE) {
            isChasing = false
            path = emptyList()
            isMoving = false
        } else if (!isChasing && distanceToPlayer < CHASE_RESUME_DISTANCE) {
            isChasing = true
        }

        shootTimer++
        if (shootTimer >= nextShootTime && isChasing && distanceToPlayer < (maxRayDistance*tileSize-3)) {
            shootAtPlayer()
            shootTimer = 0
            nextShootTime = Random.nextInt(from = minShootInterval, until = maxShootInterval + 1)
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

            val (canMove, collidedEnemy) = canMoveTo(newX = x + smoothedMoveX, newY = y + smoothedMoveY)
            if (canMove) {
                x += smoothedMoveX
                y += smoothedMoveY
                lastMoveX = smoothedMoveX
                lastMoveY = smoothedMoveY
                isMoving = true
                stuckCounter = 0
            } else if (collidedEnemy != null && tryPush(otherEnemy = collidedEnemy, moveX = smoothedMoveX, moveY = smoothedMoveY)) {
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
            val distance = sqrt(x = dx * dx + dy * dy)

            if (distance > 0.1) {
                val moveSpeed = speed * deltaTime * TARGET_FPS
                val rawMoveX = (dx / distance) * moveSpeed
                val rawMoveY = (dy / distance) * moveSpeed

                val (repelX, repelY) = calculateRepulsion(enemies = renderCast.getEnemies())
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
                    if (tryPush(otherEnemy = collidedEnemy, moveX = smoothedMoveX, moveY = smoothedMoveY)) {
                        x = newX
                        y = newY
                        lastMoveX = smoothedMoveX
                        lastMoveY = smoothedMoveY
                        stuckCounter = 0
                    } else {
                        stuckCounter++
                        val nudgeX = x - (dx / distance) * 0.1
                        val nudgeY = y - (dy / distance) * 0.1
                        if (canMoveTo(newX = nudgeX, newY = nudgeY).first) {
                            x = nudgeX
                            y = nudgeY
                        }
                    }
                } else {
                    stuckCounter++
                    val nudgeX = x - (dx / distance) * 0.1
                    val nudgeY = y - (dy / distance) * 0.1
                    if (canMoveTo(newX = nudgeX, newY = nudgeY).first) {
                        x = nudgeX
                        y = nudgeY
                    }
                }
            } else {
                path = path.drop(n = 1)
                stuckCounter = 0
                isMoving = path.isNotEmpty()
            }
        }

        randomMoveTimer++
        if (idleTimer >= idleThreshold && randomMoveTimer >= randomMoveInterval && isChasing) {
            val directions = listOf(
                Pair(first = 1.0, second = 0.0), Pair(first = -1.0, second = 0.0), Pair(first = 0.0, second = 1.0), Pair(first = 0.0, second = -1.0)
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
                if ((gridY !in map.grid.indices || gridX !in map.grid[gridY].indices) or
                    ((map.grid[gridY][gridX] == 1) ||
                    (map.grid[gridY][gridX] == 2) ||
                    (map.grid[gridY][gridX] == 5) ||
                            (map.grid[gridY][gridX] == 12))) {
                    return Pair(first = false, second = null)
                }
            }
        }

        renderCast.getEnemies().forEach { otherEnemy ->
            if (otherEnemy !== this && otherEnemy !== exclude && otherEnemy.health > 0) {
                val dx = newX - otherEnemy.x
                val dy = newY - otherEnemy.y
                val distance = sqrt(x = dx * dx + dy * dy)
                if (distance < size) {
                    return Pair(first = false, second = otherEnemy)
                }
            }
        }

        val dx = newX - positionX
        val dy = newY - positionY
        val newDistance = sqrt(x = dx * dx + dy * dy)
        val currentDistance = sqrt(x = (x - positionX) * (x - positionX) + (y - positionY) * (y - positionY))
        if (newDistance < MIN_PLAYER_DISTANCE && newDistance < currentDistance) {
            return Pair(first = false, second = null)
        }

        return Pair(first = true, second = null)
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
        val (canMove, _) = otherEnemy.canMoveTo(newX = newEnemyX, newY = newEnemyY, exclude = this)
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
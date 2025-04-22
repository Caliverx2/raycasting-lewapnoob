package org.example.MainKt

//./gradlew shadowJar

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
import java.awt.MouseInfo
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.Robot
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JLayeredPane
import kotlin.concurrent.fixedRateTimer
import kotlin.collections.Map
import kotlin.math.*


var map = true
var currentangle = 45
var tileSize = 40.0 // Rozmiar kafelka na mapie
val mapa = 0.075
var fps = 84
var MouseSupport = false

var positionX = (tileSize*2)-(tileSize/2)  //kafelek*pozycja - (pół kafelka)
var positionY = (tileSize*2)-(tileSize/2)  //kafelek*pozycja - (pół kafelka)

class Enemy(var x: Double, var y: Double, var health: Int = 10, var texture: BufferedImage, private val renderCast: RenderCast) {
    private val map = Map()
    private val speed = 0.5
    val size = 2.0
    private val margin = 0.15
    private var path: List<Node> = emptyList()
    private var pathUpdateTimer = 0
    private val pathUpdateInterval = 120*2
    private var stuckCounter = 0
    private val maxStuckFrames = 60
    var lastMoveX = 0.0 // Track last movement direction
    var lastMoveY = 0.0
    var isMoving = false // Track if enemy is actively moving

    // Check if the enemy can move to a position (walls, other enemies, player)
    fun canMoveTo(newX: Double, newY: Double, exclude: Enemy? = null): Pair<Boolean, Enemy?> {
        // Wall collision
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
                if (gridY !in map.grid.indices || gridX !in map.grid[gridY].indices || map.grid[gridY][gridX] != 0) {
                    return Pair(false, null)
                }
            }
        }

        // Enemy-enemy collision
        renderCast.getEnemies().forEach { otherEnemy ->
            if (otherEnemy !== this && otherEnemy !== exclude) {
                val dx = newX - otherEnemy.x
                val dy = newY - otherEnemy.y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < size) {
                    return Pair(false, otherEnemy)
                }
            }
        }


        return Pair(true, null)
    }

    // Try to push another enemy
    fun tryPush(otherEnemy: Enemy, moveX: Double, moveY: Double): Boolean {
        if (otherEnemy.isMoving) {
            // Check if moving in opposite directions
            val dotProduct = (moveX * otherEnemy.lastMoveX + moveY * otherEnemy.lastMoveY)
            if (dotProduct < 0) {
                return false // Moving in opposite directions, no push
            }
        }
        // Push the other enemy
        val newEnemyX = otherEnemy.x + moveX
        val newEnemyY = otherEnemy.y + moveY
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

    fun update() {
        if ((stuckCounter > maxStuckFrames) and (health > 0)) {
            // Wygeneruj nową ścieżkę, próbując ominąć gracza
            path = findPath()
            pathUpdateTimer = 0
            stuckCounter = 0
            return
        }
        else {
            if (health > 0) {
                stuckCounter++
                // Próba lekkiego losowego przesunięcia
                val randomDirection = listOf(-1.0, 1.0).random()
                val nudgeX = x + randomDirection * 0.2
                val nudgeY = y + randomDirection * 0.2
                if (canMoveTo(nudgeX, nudgeY).first) {
                    x = nudgeX
                    y = nudgeY
                }
            }
        }


        pathUpdateTimer++
        if (pathUpdateTimer >= pathUpdateInterval || stuckCounter > maxStuckFrames) {
            path = findPath()
            pathUpdateTimer = 0
            stuckCounter = 0
        }

        isMoving = path.isNotEmpty() // Update movement state
        if (path.isNotEmpty() and (health > 0)) {
            val targetNode = path.first()
            val targetX = (targetNode.x + 0.5) * tileSize
            val targetY = (targetNode.y + 0.5) * tileSize

            val dx = targetX - x
            val dy = targetY - y
            val distance = sqrt(dx * dx + dy * dy)

            if (distance > 0.1) {
                val moveX = (dx / distance) * speed
                val moveY = (dy / distance) * speed
                val newX = x + moveX
                val newY = y + moveY

                val (canMove, collidedEnemy) = canMoveTo(newX, newY)
                if (canMove) {
                    x = newX
                    y = newY
                    lastMoveX = moveX
                    lastMoveY = moveY
                    stuckCounter = 0
                } else if (collidedEnemy != null) {
                    // Try to push the collided enemy
                    if (tryPush(collidedEnemy, moveX, moveY)) {
                        x = newX
                        y = newY
                        lastMoveX = moveX
                        lastMoveY = moveY
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
        } else {
            isMoving = false
        }
    }

    fun findPath(): List<Node> {
        val startX = (x / tileSize).toInt()
        val startY = (y / tileSize).toInt()
        val goalX = (positionX / tileSize).toInt()
        val goalY = (positionY / tileSize).toInt()

        val openSet = mutableListOf(Node(startX, startY))
        val closedSet = mutableSetOf<Node>()
        val cameFrom = mutableMapOf<Node, Node>()
        val gScore = mutableMapOf(Node(startX, startY) to 0.0)
        val fScore = mutableMapOf(Node(startX, startY) to heuristic(startX, startY, goalX, goalY))

        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { fScore.getOrDefault(it, Double.MAX_VALUE) }!!

            if (abs(startX - goalX) + abs(startY - goalY) <= 1) {
                return emptyList()
            }

            if (current.x == goalX && current.y == goalY) {
                return reconstructPath(cameFrom, current)
            }

            openSet.remove(current)
            closedSet.add(current)

            for (neighbor in getNeighbors(current)) {
                if (closedSet.contains(neighbor)) continue

                val tentativeGScore = gScore.getOrDefault(current, Double.MAX_VALUE) + 1.0
                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor)
                } else if (tentativeGScore >= gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    continue
                }

                cameFrom[neighbor] = current
                gScore[neighbor] = tentativeGScore
                fScore[neighbor] = tentativeGScore + heuristic(neighbor.x, neighbor.y, goalX, goalY)
            }
        }
        return emptyList()
    }

    fun heuristic(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        return abs(x1 - x2) + abs(y1 - y2).toDouble()
    }

    fun getNeighbors(node: Node): List<Node> {
        val neighbors = mutableListOf<Node>()
        val directions = listOf(
            Node(node.x + 1, node.y), Node(node.x - 1, node.y),
            Node(node.x, node.y + 1), Node(node.x, node.y - 1)
        )
        for (dir in directions) {
            if (dir.y in map.grid.indices &&
                dir.x in map.grid[0].indices &&
                map.grid[dir.y][dir.x] != 1 &&
                renderCast.getEnemies().none { other -> other.x.toInt() == dir.x && other.y.toInt() == dir.y }
            ) {
                neighbors.add(dir)
            }

        }
        return neighbors
    }

    fun reconstructPath(cameFrom: Map<Node, Node>, current: Node): List<Node> {
        val path = mutableListOf(current)
        var curr = current
        while (cameFrom.containsKey(curr)) {
            curr = cameFrom[curr]!!
            path.add(curr)
        }
        return path.reversed()
    }
}

data class Node(val x: Int, val y: Int)

class Player(private val renderCast: RenderCast) {
    private val map = Map()
    private val playerSize = 5.0
    private val margin = 2.0
    private var movementSpeed = 1.5
    private val rotationSpeed = 2
    private val sensitivity = 0.07
    var playerHealth = 100

    private fun canMoveTo(x: Double, y: Double, deltaX: Double, deltaY: Double): Pair<Boolean, Enemy?> {
        // Wall collision
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
                if (gridY !in map.grid.indices || gridX !in map.grid[gridY].indices || ((map.grid[gridY][gridX] != 0) && (map.grid[gridY][gridX] != 5))) {
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

    // Try to push an enemy
    fun tryPushEnemy(enemy: Enemy, deltaX: Double, deltaY: Double): Boolean {
        if (enemy.isMoving) {
            // Check if enemy is moving in opposite direction
            val dotProduct = (deltaX * enemy.lastMoveX + deltaY * enemy.lastMoveY)
            if (dotProduct < 0) {
                return false // Opposite direction, no push
            }
        }
        // Push the enemy
        val newEnemyX = enemy.x + deltaX
        val newEnemyY = enemy.y + deltaY
        val (canMove, _) = enemy.canMoveTo(newEnemyX, newEnemyY)
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
            // Try to push the collided enemy
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
            movementSpeed = 1.25
            positionX = newXOnly
            return
        } else if (collidedEnemyX != null && tryPushEnemy(collidedEnemyX, deltaX, 0.0)) {
            movementSpeed = 1.25
            positionX = newXOnly
            return
        }

        // Try moving in Y only
        val newYOnly = positionY + deltaY
        val (canMoveY, collidedEnemyY) = canMoveTo(positionX, newYOnly, 0.0, deltaY)
        if (canMoveY) {
            movementSpeed = 1.25
            positionY = newYOnly
        } else if (collidedEnemyY != null && tryPushEnemy(collidedEnemyY, 0.0, deltaY)) {
            movementSpeed = 1.25
            positionY = newYOnly
        }
    }

    fun w(offset: Double = movementSpeed) {
        val deltaX = offset * cos(Math.toRadians(currentangle.toDouble()))
        val deltaY = offset * sin(Math.toRadians(currentangle.toDouble()))
        tryMove(deltaX, deltaY)
    }

    fun s() {
        val deltaX = -movementSpeed * cos(Math.toRadians(currentangle.toDouble()))
        val deltaY = -movementSpeed * sin(Math.toRadians(currentangle.toDouble()))
        tryMove(deltaX, deltaY)
    }

    fun a() {
        val deltaX = movementSpeed * cos(Math.toRadians(currentangle - 90.0))
        val deltaY = movementSpeed * sin(Math.toRadians(currentangle - 90.0))
        tryMove(deltaX, deltaY)
    }

    fun d() {
        val deltaX = movementSpeed * cos(Math.toRadians(currentangle + 90.0))
        val deltaY = movementSpeed * sin(Math.toRadians(currentangle + 90.0))
        tryMove(deltaX, deltaY)
    }

    fun anglea() {
        currentangle -= rotationSpeed
        if ((currentangle > 360)) {
            currentangle = 0
        }
        if ((currentangle < 0)) {
            currentangle = 360
        }
    }

    fun angled() {
        currentangle += rotationSpeed
        if ((currentangle > 360)) {
            currentangle = 0
        }
        if ((currentangle < 0)) {
            currentangle = 360
        }
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

    fun update(keysPressed: Map<Int, Boolean>) {
        if (keysPressed.getOrDefault(KeyEvent.VK_W, false) || keysPressed.getOrDefault(KeyEvent.VK_UP, false)) w()
        if (keysPressed.getOrDefault(KeyEvent.VK_S, false) || keysPressed.getOrDefault(KeyEvent.VK_DOWN, false)) s()
        if (keysPressed.getOrDefault(KeyEvent.VK_A, false)) a()
        if (keysPressed.getOrDefault(KeyEvent.VK_D, false)) d()
        if (keysPressed.getOrDefault(KeyEvent.VK_LEFT, false)) anglea()
        if (keysPressed.getOrDefault(KeyEvent.VK_RIGHT, false)) angled()
    }
}

class RenderCast : JPanel() {
    private val map = Map()
    private val screenWidth = 320
    private val screenHeight = 200
    private val fov = 90.0
    private val textureSize = 64
    private val rayCount = screenWidth
    private val wallHeight = 32.0

    private var textures: Array<BufferedImage> = arrayOf()
    var enemyTextureId: BufferedImage? = null
    private var floorTexture: BufferedImage? = null
    private var ceilingTexture: BufferedImage? = null
    private val buffer: BufferedImage
    private val bufferGraphics: Graphics

    private val minBrightness = 0.25
    private val maxBrightness = 1.0
    private val shadeDistanceScale = 10.0
    private val fogColor = Color(180, 180, 180)
    private val fogDensity = 0.15

    private val rayCosines = DoubleArray(rayCount)
    private val raySines = DoubleArray(rayCount)
    private val rayAngles = DoubleArray(rayCount)

    private var lastFrameTime = System.nanoTime()
    private var frameCount = 0

    private var enemies = mutableListOf<Enemy>()
    private var visibleEnemies = mutableListOf<Triple<Enemy, Int, Double>>() // (Enemy, screenX, distance)

    // Declare zBuffer as a class-level property
    private val zBuffer = DoubleArray(rayCount) { Double.MAX_VALUE }

    fun getEnemies(): List<Enemy> = enemies

    init {
        isOpaque = true
        try {
            enemyTextureId = ImageIO.read(this::class.java.classLoader.getResource("textures/boguch.jpg"))
            floorTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/floor.jpg"))
            ceilingTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/ceiling.jpg"))
            textures = arrayOf(
                ImageIO.read(this::class.java.classLoader.getResource("textures/bricks.jpg")),
                ImageIO.read(this::class.java.classLoader.getResource("textures/gold.jpg"))
            )
        } catch (e: Exception) {
            println("Error loading textures: ${e.message}")
            floorTexture = createTexture(Color.darkGray)
            ceilingTexture = createTexture(Color.lightGray)
            textures = arrayOf(
                createTexture(Color(90, 39, 15)),
                createTexture(Color(255, 215, 0))
            )
            enemyTextureId = createTexture(Color(255, 68, 68))
        }
        // Enemies in open spaces near player start (21.5, 21.5)
        enemies.add(Enemy((tileSize * 6) - (tileSize / 2), (tileSize * 12) - (tileSize / 2), 100, enemyTextureId!!, this))
        enemies.add(Enemy((tileSize * 8) - (tileSize / 2), (tileSize * 8) - (tileSize / 2), 100, enemyTextureId!!, this))

        buffer = BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB)
        bufferGraphics = buffer.createGraphics()

        val rayAngleStep = fov / (rayCount - 1)
        for (ray in 0 until rayCount) {
            val relativeAngle = -fov / 2 + ray * rayAngleStep
            rayAngles[ray] = relativeAngle
            val rayAngleRad = Math.toRadians(relativeAngle)
            rayCosines[ray] = cos(rayAngleRad)
            raySines[ray] = sin(rayAngleRad)
        }
    }

    private fun createTexture(color: Color): BufferedImage {
        val texture = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_RGB)
        val g = texture.createGraphics()
        g.color = color
        g.fillRect(0, 0, textureSize, textureSize)
        g.dispose()
        return texture
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        renderWallsToBuffer()

        val g2d = g as Graphics2D
        val scaleX = width.toDouble() / screenWidth
        val scaleY = height.toDouble() / screenHeight
        g2d.scale(scaleX, scaleY)
        g2d.drawImage(buffer, 0, 0, null)
        g2d.scale(1.0 / scaleX, 1.0 / scaleY)
    }

    private fun renderWallsToBuffer() {
        enemies.forEach { it.update() }

        val currentTime = System.nanoTime()
        frameCount++
        val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0
        if (deltaTime >= 1.0) {
            fps = (frameCount / deltaTime).toInt()
            frameCount = 0
            lastFrameTime = currentTime
        }

        val playerAngleRad = Math.toRadians(currentangle.toDouble())
        val playerHeight = wallHeight / 4.0
        val horizonOffset = 0.0
        val playerPosX = positionX / tileSize
        val playerPosY = positionY / tileSize

        bufferGraphics.color = Color.BLACK
        bufferGraphics.fillRect(0, 0, screenWidth, screenHeight)

        // Reset zBuffer for this frame
        zBuffer.fill(Double.MAX_VALUE)
        visibleEnemies.clear() // Reset visible enemies each frame

        // Wall, floor, ceiling, and enemy visibility check
        for (ray in 0 until rayCount) {
            val rayAngle = currentangle + rayAngles[ray]
            val rayAngleRad = Math.toRadians(rayAngle)
            val relativeCos = rayCosines[ray]
            val relativeSin = raySines[ray]
            val cosPlayerAngle = cos(playerAngleRad)
            val sinPlayerAngle = sin(playerAngleRad)
            val rayDirX = relativeCos * cosPlayerAngle - relativeSin * sinPlayerAngle
            val rayDirY = relativeCos * sinPlayerAngle + relativeSin * cosPlayerAngle

            var mapX = playerPosX.toInt()
            var mapY = playerPosY.toInt()
            val deltaDistX = if (rayDirX == 0.0) 1e30 else abs(1 / rayDirX)
            val deltaDistY = if (rayDirY == 0.0) 1e30 else abs(1 / rayDirY)
            var stepX: Int
            var stepY: Int
            var sideDistX: Double
            var sideDistY: Double
            var side = 0

            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (playerPosX - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1.0 - playerPosX) * deltaDistX
            }
            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (playerPosY - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1.0 - playerPosY) * deltaDistY
            }

            var hitWall = false
            var wallType = 0
            var distance = 0.0
            var hitX = 0.0
            var hitY = 0.0

            while (!hitWall) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                }
                if (mapY !in map.grid.indices || mapX !in map.grid[0].indices) {
                    break
                }
                if (map.grid[mapY][mapX] == 1 || map.grid[mapY][mapX] == 5) {
                    hitWall = true
                    wallType = map.grid[mapY][mapX]
                }
            }

            if (hitWall) {
                distance = if (side == 0) {
                    (mapX - playerPosX + (1 - stepX) / 2.0) / rayDirX
                } else {
                    (mapY - playerPosY + (1 - stepY) / 2.0) / rayDirY
                }
                if (distance < 0.01) distance = 0.01
                val angleDiff = abs(playerAngleRad - rayAngleRad)
                if (angleDiff < PI / 2) {
                    distance *= cos(angleDiff)
                } else {
                    hitWall = false
                }
                if (side == 0) {
                    hitX = mapX.toDouble() + (if (stepX > 0) 0.0 else 1.0)
                    hitY = playerPosY + (hitX - playerPosX) * (rayDirY / rayDirX)
                } else {
                    hitY = mapY.toDouble() + (if (stepY > 0) 0.0 else 1.0)
                    hitX = playerPosX + (hitY - playerPosY) * (rayDirX / rayDirY)
                }
                zBuffer[ray] = distance
            }

            // Check for enemy intersections with this ray
            enemies.forEach { enemy ->
                val dx = enemy.x / tileSize - playerPosX
                val dy = enemy.y / tileSize - playerPosY
                // Project enemy onto ray direction
                val rayLength = dx * rayDirX + dy * rayDirY
                if (rayLength > 0) { // Enemy is in front of player
                    val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
                    // Check if enemy is close enough to the ray (within half size)
                    if (perpendicularDistance < enemy.size / 2 / tileSize+0.01) {
                        // Check if enemy is closer than wall
                        if (!hitWall || rayLength < 500) {
                            // Calculate screen X based on ray index
                            val angleRatio = rayAngles[ray] / (fov / 2)
                            val screenX = (screenWidth / 2 + angleRatio * screenWidth / 2).toInt()
                            // Add to visible enemies (avoid duplicates)
                            if (visibleEnemies.none { it.first === enemy }) {
                                visibleEnemies.add(Triple(enemy, screenX, rayLength))
                            }
                        }
                    }
                }
            }

            val lineHeight = if (hitWall) {
                ((wallHeight * screenHeight) / (distance * tileSize)).toInt().coerceIn(0, screenHeight * 2)
            } else {
                0
            }
            val drawStart = (-lineHeight / 2 + screenHeight / 2 + horizonOffset).coerceAtLeast(0.0).toInt()
            val drawEnd = (lineHeight / 2 + screenHeight / 2 + horizonOffset).coerceAtMost(screenHeight.toDouble()).toInt()

            if (hitWall) {
                var textureX = if (side == 0) {
                    val blockY = mapY.toDouble()
                    val relativeY = hitY - blockY
                    relativeY.coerceIn(0.0, 1.0) * textureSize
                } else {
                    val blockX = mapX.toDouble()
                    val relativeX = hitX - blockX
                    relativeX.coerceIn(0.0, 1.0) * textureSize
                }
                if (side == 0 && rayDirX > 0 || side == 1 && rayDirY < 0) {
                    textureX = textureSize - textureX - 1
                }
                val texture = textures[if (wallType == 1) 0 else 1]
                for (y in drawStart until drawEnd) {
                    val wallY = (y - (screenHeight / 2.0 + horizonOffset) + lineHeight / 2.0) * wallHeight / lineHeight
                    val textureY = (wallY * textureSize / wallHeight).toInt().coerceIn(0, textureSize - 1)
                    val finalTextureX = textureX.toInt().coerceIn(0, textureSize - 1)
                    val color = texture.getRGB(finalTextureX, textureY)
                    val shadeFactor = (1.0 - (distance / shadeDistanceScale)).coerceIn(minBrightness, maxBrightness)
                    val originalColor = Color(color)
                    val shadedColor = Color(
                        (originalColor.red * shadeFactor).toInt().coerceIn(0, 255),
                        (originalColor.green * shadeFactor).toInt().coerceIn(0, 255),
                        (originalColor.blue * shadeFactor).toInt().coerceIn(0, 255)
                    )
                    val fogFactor = 1.0 - exp(-fogDensity * distance)
                    val finalColor = Color(
                        ((1.0 - fogFactor) * shadedColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * shadedColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * shadedColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                    )
                    buffer.setRGB(ray, y, finalColor.rgb)
                }
            }

            for (y in 0 until screenHeight) {
                if (hitWall && y in drawStart until drawEnd) continue

                val isCeiling = y < (screenHeight / 2 + horizonOffset)
                val texture = if (isCeiling) ceilingTexture else floorTexture
                if (texture == null) {
                    buffer.setRGB(ray, y, Color.GRAY.rgb)
                    continue
                }

                val rowDistance = if (isCeiling) {
                    (playerHeight * screenHeight / 2) / (10.0 * ((screenHeight / 2.0 + horizonOffset) - y + 0.0))
                } else {
                    (playerHeight * screenHeight / 2) / (10.0 * (y - (screenHeight / 2.0 + horizonOffset) + 0.0))
                }
                if (rowDistance < 0.01 || rowDistance > 50.0) continue

                val floorX = playerPosX + rowDistance * rayDirX + 100
                val floorY = playerPosY + rowDistance * rayDirY + 100

                val textureScale = 2.0
                val textureX = ((floorY / textureScale * textureSize) % textureSize).toInt().coerceIn(0, textureSize - 1)
                val textureY = ((floorX / textureScale * textureSize) % textureSize).toInt().coerceIn(0, textureSize - 1)

                val color = texture.getRGB(textureX, textureY)
                val shadeFactor = (1.0 - (rowDistance / shadeDistanceScale)).coerceIn(minBrightness, maxBrightness)
                val originalColor = Color(color)
                val shadedColor = Color(
                    (originalColor.red * shadeFactor).toInt().coerceIn(0, 255),
                    (originalColor.green * shadeFactor).toInt().coerceIn(0, 255),
                    (originalColor.blue * shadeFactor).toInt().coerceIn(0, 255)
                )
                val fogFactor = 1.0 - exp(-fogDensity * rowDistance)
                val finalColor = Color(
                    ((1.0 - fogFactor) * shadedColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                    ((1.0 - fogFactor) * shadedColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                    ((1.0 - fogFactor) * shadedColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                )

                buffer.setRGB(ray, y, finalColor.rgb)
            }
        }

        // Render visible enemies as 2D overlays
        renderEnemies()

        repaint()
    }

    private fun renderEnemies() {
        // Sort enemies by distance (farthest to nearest) to ensure correct rendering order
        visibleEnemies.sortByDescending { it.third }

        visibleEnemies.forEach { (enemy, screenX, distance) ->
            // Perspective-correct sprite size based on enemy height
            val enemyHeight = wallHeight / 2 // Enemy height is half the wall height
            val minSize = 16.0 // Minimum sprite size in pixels
            val maxSize = 128.0 // Maximum sprite size in pixels
            val spriteSize = ((enemyHeight * screenHeight) / (distance * tileSize)).coerceIn(minSize, maxSize).toInt()

            // Calculate floor position at this distance
            val floorY = (screenHeight / 2 + (wallHeight * screenHeight) / (2 * distance * tileSize)).toInt()

            // Position sprite with bottom at floorY
            val drawStartY = (floorY - spriteSize).coerceIn(0, screenHeight - 1)
            val drawEndY = floorY.coerceIn(0, screenHeight - 1)
            val drawStartX = (screenX - spriteSize / 2).coerceIn(0, screenWidth - 1)
            val drawEndX = (screenX + spriteSize / 2).coerceIn(0, screenWidth - 1)

            // Draw sprite pixel by pixel, checking z-buffer for occlusion
            for (x in drawStartX until drawEndX) {
                // Ensure x is within zBuffer bounds
                if (x < 0 || x >= zBuffer.size) continue

                // Check if enemy is closer than the wall at this column
                if (distance < zBuffer[x]) {
                    val textureX = ((x - drawStartX) * enemy.texture.width / spriteSize).coerceIn(0, enemy.texture.width - 1)
                    for (y in drawStartY until drawEndY) {
                        val textureY = ((y - drawStartY) * enemy.texture.height / spriteSize).coerceIn(0, enemy.texture.height - 1)
                        val color = enemy.texture.getRGB(textureX, textureY)
                        // Only draw if pixel is not transparent (optional, if texture has alpha)
                        if ((color and 0xFF000000.toInt()) != 0) {
                            buffer.setRGB(x, y, color)
                        }
                    }
                }
            }
        }
    }

    fun shotgun(player: Player) {
        val shotAngleRad = Math.toRadians(currentangle.toDouble())
        val playerPosX = positionX / tileSize
        val playerPosY = positionY / tileSize
        val rayDirX = cos(shotAngleRad)
        val rayDirY = sin(shotAngleRad)

        // Raycasting setup
        var mapX = playerPosX.toInt()
        var mapY = playerPosY.toInt()
        val deltaDistX = if (rayDirX == 0.0) 1e30 else abs(1 / rayDirX)
        val deltaDistY = if (rayDirY == 0.0) 1e30 else abs(1 / rayDirY)
        var stepX: Int
        var stepY: Int
        var sideDistX: Double
        var sideDistY: Double
        var side = 10

        if (rayDirX < 0) {
            stepX = -1
            sideDistX = (playerPosX - mapX) * deltaDistX
        } else {
            stepX = 1
            sideDistX = (mapX + 1.0 - playerPosX) * deltaDistX
        }
        if (rayDirY < 0) {
            stepY = -1
            sideDistY = (playerPosY - mapY) * deltaDistY
        } else {
            stepY = 1
            sideDistY = (mapY + 1.0 - playerPosY) * deltaDistY
        }

        var hitWall = false
        var wallDistance = Double.MAX_VALUE

        // Raycasting loop to find wall distance
        while (!hitWall) {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX
                mapX += stepX
                side = 0
            } else {
                sideDistY += deltaDistY
                mapY += stepY
                side = 1
            }
            if (mapY !in map.grid.indices || mapX !in map.grid[0].indices) {
                break
            }
            if (map.grid[mapY][mapX] == 1 || map.grid[mapY][mapX] == 5) {
                hitWall = true
                wallDistance = if (side == 0) {
                    (mapX - playerPosX + (1 - stepX) / 2.0) / rayDirX
                } else {
                    (mapY - playerPosY + (1 - stepY) / 2.0) / rayDirY
                }
                if (wallDistance < 0.01) wallDistance = 0.01
            }
        }

        // Check for enemy hits
        enemies.toList().forEach { enemy ->
            val dx = enemy.x / tileSize - playerPosX
            val dy = enemy.y / tileSize - playerPosY
            // Project enemy onto ray direction
            val rayLength = dx * rayDirX + dy * rayDirY
            if (rayLength > 0 && rayLength < wallDistance) { // Enemy in front and before wall
                //player.w(-1.0)
                val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
                // Check if enemy is within ray path
                if ((perpendicularDistance < (enemy.size*200) / 2 / tileSize) and (enemy.health > 0)) {
                    // Check angle to enemy
                    val angleToEnemy = atan2(dy, dx)
                    val angleDiff = abs(angleToEnemy - shotAngleRad)
                    if (angleDiff < Math.toRadians(15.0)) { // Widened to ±30°
                        enemy.health -= 25
                        println("trafiono, enemy health=${enemy.health}, enemy=${enemy}")
                        if (enemy.health <= 0) {
                            //enemies.remove(enemy)
                            try {
                                enemy.texture = ImageIO.read(this::class.java.classLoader.getResource("textures/boguch_bochen_chlepa.jpg"))
                            }
                            catch (e: Exception) {
                                enemy.texture = createTexture(Color.black)
                            }
                        }
                    }
                }
            }
        }
        repaint()
    }
}

fun main() = runBlocking {
    val frame = JFrame("rolada z gówna")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.iconImage = Toolkit.getDefaultToolkit().getImage(this::class.java.classLoader.getResource("icon/icon.jpg"))
    frame.isResizable = false
    frame.setSize(1366, 768)
    frame.setLocation(((Toolkit.getDefaultToolkit().screenSize.width - frame.width) / 2), ((Toolkit.getDefaultToolkit().screenSize.height - frame.height) / 2))

    frame.cursor = frame.toolkit.createCustomCursor(
        BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
        Point(0, 0),
        "invisible"
    )

    frame.isVisible = true

    val layeredPane = JLayeredPane()
    layeredPane.setSize(1366, 768)
    layeredPane.setBounds(0, 0, 1366, 768)
    frame.add(layeredPane)

    val renderCast = RenderCast()
    val mapa = Mappingmap(renderCast)
    mapa.isOpaque = false
    mapa.layout = null
    mapa.setSize(1366, 768)
    mapa.setBounds(0, 0, 1366, 768)

    renderCast.isOpaque = false
    renderCast.setSize(1366, 768)
    renderCast.setBounds(0, 0, 1366, 768)

    layeredPane.add(mapa, 1)
    layeredPane.add(renderCast, 2)

    val player = Player(renderCast) // Pass renderCast to Player
    val keysPressed: MutableMap<Int, Boolean> = mutableMapOf()
    var centerX = frame.width / 2

    frame.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                renderCast.shotgun(player)
            } else {
                println("Naciśnięto klawisz: ${event.button}")
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
                KeyEvent.VK_SPACE -> renderCast.shotgun(player)
            }
        }

        override fun keyReleased(event: KeyEvent) {
            keysPressed[event.keyCode] = false
            when (event.keyCode) {
                KeyEvent.VK_M -> map = true
            }
        }
    })

    frame.addComponentListener(object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent?) {
            centerX = frame.x + frame.width / 2
        }
    })

    fixedRateTimer(name = "player-update", initialDelay = 100, period = 16) {
        player.update(keysPressed)
        renderCast.repaint()
        mapa.repaint()
    }

    while (MouseSupport) {
        delay(75)
        Robot().mouseMove(MouseInfo.getPointerInfo().location.x, 0)
        Robot().mouseMove(960, 0)
        Robot().mouseMove(960, 384)
    }
}

class Map {
    // Wartości: 1-ściana, 0-pusta przestrzeń, 5-początek i koniec labiryntu
    val grid: Array<IntArray> = arrayOf(
        intArrayOf(5,5,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(5,0,0,0,0,0,0,0,1,0,1,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,0,1,1,1,1,1,0,1,0,1,1,1,0,1,1,1,0,1,1,1,1,1,0,1,0,1,1,1),
        intArrayOf(1,0,1,0,1,0,0,0,1,0,0,0,0,0,0,0,1,0,1,0,1,0,0,0,1,0,1,0,0,0,1),
        intArrayOf(1,0,1,0,1,0,1,1,1,0,1,1,1,1,1,0,1,0,1,1,1,1,1,0,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,1,0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,0,1,1,1,1,1,0,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,0,0,1,0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,1,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,0,1,0,1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,0,1,0,1,1,1,0,1),
        intArrayOf(1,0,0,0,1,0,1,0,1,0,1,0,0,0,1,0,1,0,0,0,0,0,1,0,1,0,1,0,1,0,1),
        intArrayOf(1,1,1,0,1,1,1,0,1,0,1,0,1,1,1,0,1,0,1,0,1,1,1,0,1,1,1,0,1,0,1),
        intArrayOf(1,0,1,0,0,0,0,0,1,0,1,0,0,0,0,0,1,0,1,0,0,0,1,0,1,0,0,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,0,1,1,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,0,0,0,0,0,1,0,0,0,0,0,1,0,1,0,0,0,1,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,0,1,0,1,1,1,0,1,0,1,1,1),
        intArrayOf(1,0,0,0,1,0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,0,1,1,1,1,1,0,1,1,1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1),
        intArrayOf(1,0,1,0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,0,0,1,0,1,0,1,0,1,0,1,0,1),
        intArrayOf(1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,1,1,1,1,0,1,0,1,1,1,0,1,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,0,0,0,0,0,0,0,0,1,0,1,0,1,0,0,0,1,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,0,1,1,1,1,1,1,1,1,1,0,1,0,1,0,1,0,1,0,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1,0,1,0,0,0,0,0,1,0,1),
        intArrayOf(1,0,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1,0,1,1,1,0,1,0,1),
        intArrayOf(1,0,0,0,1,0,0,0,0,0,0,0,1,0,1,0,0,0,1,0,1,0,0,0,0,0,1,0,1,0,1),
        intArrayOf(1,0,1,1,1,1,1,0,1,0,1,1,1,0,1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,1,0,1,0,1,0,0,0,1,0,0,0,1,0,0,0,0,0,1,0,1,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,0,1,0,1,0,1,0,1,1,1,0,1,1,1,1,1,0,1,0,1,1,1),
        intArrayOf(1,0,1,0,1,0,0,0,1,0,0,0,1,0,1,0,1,0,1,0,0,0,1,0,1,0,0,0,0,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,0,1,0,1,1,1,0,1,0,1,0,1,0,1,0,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,1,0,1,0,1,0,0,0,0,0,0,0,0,0,1,0,1,0,0,0,0,0,0,0,5),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,5,5)
    )
}

class Mappingmap(private val renderCast: RenderCast) : JPanel() {
    private val map = Map()
    private val miniMapSize = 200
    private val offsetX = 10
    private val offsetY = 10

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        val g2 = v as Graphics2D

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val mapWidth = map.grid[0].size
        val mapHeight = map.grid.size
        val tileScale = minOf(miniMapSize.toDouble() / mapWidth, miniMapSize.toDouble() / mapHeight)

        for (row in map.grid.indices) {
            for (col in map.grid[row].indices) {
                val x = (col * tileScale).toInt()
                val y = (row * tileScale).toInt()
                val tileSize = tileScale.toInt() + 1

                when (map.grid[row][col]) {
                    1 -> {
                        g2.color = Color(0, 255, 0)
                        g2.fillRect(x + offsetX, y + offsetY, tileSize, tileSize)
                    }
                    5 -> {
                        g2.color = Color.YELLOW
                        g2.fillRect(x + offsetX, y + offsetY, tileSize, tileSize)
                    }
                }
            }
        }

        // Rysuj przeciwników na minimapie
        renderCast.getEnemies().forEach { enemy ->
            val enemyX = offsetX + (enemy.x / tileSize * tileScale).toInt()
            val enemyY = offsetY + (enemy.y / tileSize * tileScale).toInt()
            g2.color = Color.RED
            g2.fillRect(enemyX - 3, enemyY - 3, 6, 6)
        }

        // Rysuj pozycję gracza na minimapie
        val playerX = offsetX + (positionX / tileSize * tileScale).toInt()
        val playerY = offsetY + (positionY / tileSize * tileScale).toInt()
        val angleRad = Math.toRadians(currentangle.toDouble())
        val lineLength = 10.0
        val playerX2 = playerX + (lineLength * cos(angleRad)).toInt()
        val playerY2 = playerY + (lineLength * sin(angleRad)).toInt()

        g2.color = Color.YELLOW
        g2.stroke = BasicStroke(2f)
        g2.drawLine(playerX, playerY, playerX2, playerY2)
        g2.color = Color.darkGray
        g2.fillRect(playerX - 2, playerY - 2, 5, 5)

        g2.color = Color.white
        g2.fillRect(683, 384 , 3, 3)

        g2.color = Color.YELLOW
        g2.font = Font("BOLD", Font.BOLD, 17)
        g2.drawString("FPS: $fps", 1366 - 90, 20)
        isOpaque = false
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(miniMapSize + offsetX * 2, miniMapSize + offsetY * 2)
    }
}
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
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JLayeredPane
import kotlin.math.*


var map = true
var currentangle = 45
var tileSize = 40.0
val mapa = 0.5
var MouseSupport = false

val TARGET_FPS = 90
val FRAME_TIME_NS = 1_000_000_000 / (90*1)
var deltaTime = 1.0 / TARGET_FPS

var positionX = (tileSize*2)-(tileSize/2)  //tile*positon - (half tile)
var positionY = (tileSize*2)-(tileSize/2)  //tile*positon - (half tile)
var enemies = mutableListOf<Enemy>()
var lightSources = mutableListOf<LightSource>()
var isShooting = false


class LightSource(
    var x: Double,
    var y: Double,
    var color: Color,
    var intensity: Double,
    var range: Double,
    var owner: String = ""
)

class Enemy(var x: Double, var y: Double, var health: Int = 10, var texture: BufferedImage, private val renderCast: RenderCast, private val map: Map, var speed: Double = 0.9) {
    var path: List<Node> = emptyList()
    val size = 1.0
    private val margin = 1
    private var pathUpdateTimer = 30
    private val pathUpdateInterval = 120
    private var stuckCounter = 0
    private val maxStuckFrames = 60
    var lastMoveX = 0.0
    var lastMoveY = 0.0
    var isMoving = false
    private var smoothedMoveX = 0.0
    private var smoothedMoveY = 0.0
    private val smoothingFactor = 0.1//0.05
    private val MIN_PLAYER_DISTANCE = 1.5 * tileSize
    private var lastPlayerX = 0.0
    private var lastPlayerY = 0.0

    private var idleTimer = 0
    private val idleThreshold = (1.5 * TARGET_FPS).toInt()
    private var randomMoveTimer = 0
    private val randomMoveInterval = 300
    private val randomMoveDistance = tileSize * 1.5
    private val moveThreshold = tileSize * 3.5
    private var lastX = x
    private var lastY = y
    private var accumulatedDistance = 0.0
    private var isChasing = true
    private val CHASE_STOP_DISTANCE = 20.0 * tileSize // Odległość zatrzymania pościgu
    private val CHASE_RESUME_DISTANCE = 15.0 * tileSize // Odległość wznowienia pościgu

    companion object {
        const val MIN_WALL_DISTANCE = 0.1
        const val DIRECT_MOVE_THRESHOLD = 3.0
        var PLAYER_MOVE_THRESHOLD = tileSize
        private var globalChaseTimer = 0
        private val pathCache = mutableMapOf<Pair<Node, Node>, List<Node>>()

        fun updateGlobalChaseTimer() {
            globalChaseTimer++
        }

        fun resetGlobalChaseTimer() {
            globalChaseTimer = 0
        }

        fun getGlobalChaseTimer(): Int = globalChaseTimer
    }

    data class Node(val x: Int, val y: Int)

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
                if (gridY !in map.grid.indices || gridX !in map.grid[gridY].indices || ((map.grid[gridY][gridX] != 0) and (map.grid[gridY][gridX] != 5) and (map.grid[gridY][gridX] != 3) and (map.grid[gridY][gridX] != 6))) {
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

    fun findPath(): List<Node> {
        if (health <= 0 || !isChasing) return emptyList()

        val startX = (x / tileSize).toInt()
        val startY = (y / tileSize).toInt()
        val goalX = (positionX / tileSize).toInt()
        val goalY = (positionY / tileSize).toInt()

        if (startY !in map.grid.indices || startX !in map.grid[0].indices ||
            goalY !in map.grid.indices || goalX !in map.grid[0].indices ||
            ((map.grid[goalY][goalX] != 0) and (map.grid[goalY][goalX] != 5) and (map.grid[goalY][goalX] != 3) and (map.grid[goalY][goalX] != 6))
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

        val queue = ArrayDeque<Node>().apply { add(Node(startX, startY)) }
        val visited = Array(map.grid.size) { BooleanArray(map.grid[0].size) }
        val cameFrom = Array(map.grid.size) { arrayOfNulls<Node>(map.grid[0].size) }
        visited[startY][startX] = true

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.x == goalX && current.y == goalY) {
                val path = mutableListOf(current)
                var curr = current
                while (cameFrom[curr.y][curr.x] != null) {
                    curr = cameFrom[curr.y][curr.x]!!
                    path.add(curr)
                }
                val result = path.reversed()
                pathCache[cacheKey] = result
                return result
            }

            for (neighbor in getNeighbors(current)) {
                if (!visited[neighbor.y][neighbor.x]) {
                    visited[neighbor.y][neighbor.x] = true
                    cameFrom[neighbor.y][neighbor.x] = current
                    queue.add(neighbor)
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
                map.grid[dir.y][dir.x] != 1 &&
                map.getWallDistances()[dir.y][dir.x] >= MIN_WALL_DISTANCE
            ) {
                neighbors.add(dir)
            }
        }
        return neighbors
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

        // Logic for interrupting/resuming the chase
        if (isChasing && distanceToPlayer > CHASE_STOP_DISTANCE) {
            isChasing = false
            path = emptyList()
            isMoving = false
        } else if (!isChasing && distanceToPlayer < CHASE_RESUME_DISTANCE) {
            isChasing = true
        }

        val playerMoved =
            sqrt((positionX - lastPlayerX) * (positionX - lastPlayerX) + (positionY - lastPlayerY) * (positionY - lastPlayerY)) > PLAYER_MOVE_THRESHOLD
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

            val newX = x + smoothedMoveX
            val newY = y + smoothedMoveY

            val (canMove, collidedEnemy) = canMoveTo(newX, newY)
            if (canMove) {
                x = newX
                y = newY
                lastMoveX = smoothedMoveX
                lastMoveY = smoothedMoveY
                isMoving = true
                stuckCounter = 0
            } else if (collidedEnemy != null && tryPush(collidedEnemy, smoothedMoveX, smoothedMoveY)) {
                x = newX
                y = newY
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

                smoothedMoveX = smoothedMoveX * (1.0 - smoothingFactor) + rawMoveX * smoothingFactor
                smoothedMoveY = smoothedMoveY * (1.0 - smoothingFactor) + rawMoveY * smoothingFactor

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
                Pair(1.0, 0.0),
                Pair(-1.0, 0.0),
                Pair(0.0, 1.0),
                Pair(0.0, -1.0)
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
            } else {
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

    frame.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
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
                KeyEvent.VK_SPACE -> {
                    isShooting = false
                }
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
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,0,1,0,1,1,1,0,1,0,0,1),
        intArrayOf(1,0,1,0,1,0,0,0,1,0,1,0,0,1),
        intArrayOf(1,0,1,1,1,0,1,0,1,0,1,0,0,1),
        intArrayOf(1,0,1,0,0,0,1,0,0,0,1,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,0,1,1,1,1,0,5),
        intArrayOf(1,0,0,0,0,0,1,0,1,0,0,0,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,1,1,0,0,5),
        intArrayOf(1,0,1,0,1,0,0,0,0,0,1,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,0,0,5),
        intArrayOf(1,0,0,0,1,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,0,1,1,1,1,1,0,1,1,0,5),
        intArrayOf(1,0,0,0,1,0,1,0,1,0,0,0,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,1,1,1,0,5),
        intArrayOf(1,0,1,0,1,0,1,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,0,1,1,1,1,1,1,0,5),
        intArrayOf(1,0,0,0,0,0,1,0,0,0,0,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,5),
        intArrayOf(1,1,1,1,5,1,5,1,5,1,5,1,5,1)
    )

    // Data classes and enum for room generation
    val limitRooms = 9*9
    var currentRooms = 0
    var enemyTextureId: BufferedImage? = ImageIO.read(this::class.java.classLoader.getResource("textures/boguch.jpg"))
    data class GridPoint(val x: Int, val y: Int)
    data class RoomTemplate(val grid: Array<IntArray>, val entrances: List<GridPoint>)
    enum class Direction { UP, DOWN, LEFT, RIGHT }

    // Room templates
    private val templates = listOf(
        // Template 1: Small 5x5 room, entrances: top, right
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(2, 2, 0, 2, 2),
                intArrayOf(2, 0, 0, 0, 2),
                intArrayOf(2, 0, 6, 0, 2),
                intArrayOf(2, 0, 0, 0, 2),
                intArrayOf(2, 2, 2, 2, 2)
            ),
            entrances = listOf(GridPoint(2, -2))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(1, 1, 1, 1, 1),
                intArrayOf(1, 0, 0, 0, 1),
                intArrayOf(1, 0, 3, 0, 1),
                intArrayOf(1, 0, 0, 0, 1),
                intArrayOf(1, 1, 0, 1, 1)
            ),
            entrances = listOf(GridPoint(2, 6))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(2, 2, 2, 2, 2),
                intArrayOf(2, 0, 0, 0, 2),
                intArrayOf(2, 0, 6, 0, 0),
                intArrayOf(2, 0, 0, 0, 2),
                intArrayOf(2, 2, 2, 2, 2)
            ),
            entrances = listOf(GridPoint(6, 2))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(1, 1, 1, 1, 1),
                intArrayOf(1, 0, 0, 0, 1),
                intArrayOf(0, 0, 3, 0, 1),
                intArrayOf(1, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 1, 1)
            ),
            entrances = listOf(GridPoint(-2, 2))
        ),

        // Template 2: Medium 7x7 room, entrances: left, right, bottom
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(2, 2, 2, 5, 2, 2, 2),
                intArrayOf(2, 0, 0, 0, 0, 0, 2),
                intArrayOf(2, 0, 6, 0, 6, 0, 2),
                intArrayOf(0, 0, 0, 3, 0, 3, 5),
                intArrayOf(2, 0, 6, 0, 6, 0, 2),
                intArrayOf(2, 0, 0, 0, 0, 0, 2),
                intArrayOf(2, 2, 2, 5, 2, 2, 2)
            ),
            entrances = listOf(GridPoint(-2, 3))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(1, 1, 1, 5, 1, 1, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 6, 0, 6, 0, 1),
                intArrayOf(5, 3, 0, 3, 0, 0, 0),
                intArrayOf(1, 0, 6, 0, 6, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 5, 1, 1, 1)
            ),
            entrances = listOf(GridPoint(8, 3))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(2, 2, 2, 0, 2, 2, 2),
                intArrayOf(2, 0, 0, 0, 0, 0, 2),
                intArrayOf(2, 0, 6, 0, 6, 0, 2),
                intArrayOf(5, 0, 0, 3, 0, 0, 5),
                intArrayOf(2, 0, 6, 0, 6, 0, 2),
                intArrayOf(2, 0, 0, 3, 0, 0, 2),
                intArrayOf(2, 2, 2, 5, 2, 2, 2)
            ),
            entrances = listOf(GridPoint(3, -2))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(1, 1, 1, 5, 1, 1, 1),
                intArrayOf(1, 0, 0, 3, 0, 0, 1),
                intArrayOf(1, 0, 6, 0, 6, 0, 1),
                intArrayOf(5, 0, 0, 3, 0, 0, 5),
                intArrayOf(1, 0, 6, 0, 6, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 0, 1, 1, 1)
            ),
            entrances = listOf(GridPoint(3, 8))
        ),

        // Template 3: Large 9x9 room, entrances: top, bottom
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(1, 1, 1, 1, 0, 1, 1, 1, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 6, 0, 0, 0, 6, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(5, 0, 0, 0, 0, 0, 0, 0, 5),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 6, 0, 0, 0, 6, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1)
            ),
            entrances = listOf(GridPoint(4, -2))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2),
                intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
                intArrayOf(2, 0, 6, 0, 0, 0, 6, 0, 2),
                intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
                intArrayOf(5, 0, 0, 0, 0, 0, 0, 0, 5),
                intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
                intArrayOf(2, 0, 6, 0, 0, 0, 6, 0, 2),
                intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
                intArrayOf(2, 2, 2, 2, 0, 2, 2, 2, 2)
            ),
            entrances = listOf(GridPoint(4, 10))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(1, 1, 1, 1, 5, 1, 1, 1, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 6, 0, 0, 0, 6, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 6, 0, 0, 0, 6, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 1, 5, 1, 1, 1, 1)
            ),
            entrances = listOf(GridPoint(-2, 4))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(2, 2, 2, 2, 5, 2, 2, 2, 2),
                intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
                intArrayOf(2, 0, 6, 0, 0, 0, 6, 0, 2),
                intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
                intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0),
                intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
                intArrayOf(2, 0, 6, 0, 0, 0, 6, 0, 2),
                intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
                intArrayOf(2, 2, 2, 2, 5, 2, 2, 2, 2)
            ),
            entrances = listOf(GridPoint(10, 4))
        ),

        // Template 4: Rectangular 6x8 room, entrances: left, top
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(1, 1, 1, 1, 1, 5, 1, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(0, 0, 0, 0, 3, 0, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 5, 1, 1, 1, 1)
            ),
            entrances = listOf(GridPoint(-2, 2))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(1, 1, 1, 1, 1, 0, 1, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 3, 0, 0, 0, 0, 5),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 5, 1, 1, 1, 1)
            ),
            entrances = listOf(GridPoint(5, -2))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(1, 1, 1, 1, 1, 5, 1, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 3, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 0),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 5, 1, 1, 1, 1)
            ),
            entrances = listOf(GridPoint(9, 3))
        ),
        RoomTemplate(
            grid = arrayOf(
                intArrayOf(1, 1, 1, 1, 1, 5, 1, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(5, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 3, 0, 1),
                intArrayOf(1, 0, 0, 0, 0, 0, 0, 1),
                intArrayOf(1, 1, 1, 0, 1, 1, 1, 1)
            ),
            entrances = listOf(GridPoint(3, 7))
        )
    )

    // Track room connections and last entrance
    private val roomConnections = mutableListOf<Pair<GridPoint, GridPoint>>()
    private var lastEntrance: GridPoint? = null
    private var wallDistances: Array<Array<Double>>? = null

    fun getWallDistances(): Array<Array<Double>> {
        if (wallDistances == null) {
            wallDistances = precomputeWallDistances()
        }
        return wallDistances!!
    }

    private fun precomputeWallDistances(): Array<Array<Double>> {
        val height = grid.size
        val width = grid[0].size
        val distances = Array(height) { Array(width) { Double.MAX_VALUE } }

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (grid[y][x] == 1) continue
                var minDistance = Double.MAX_VALUE
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val checkX = x + dx
                        val checkY = y + dy
                        if (checkY in grid.indices && checkX in grid[0].indices && grid[checkY][checkX] == 1) {
                            val distance = sqrt((dx * dx + dy * dy).toDouble())
                            minDistance = minOf(minDistance, distance)
                        }
                    }
                }
                distances[y][x] = minDistance
            }
        }
        return distances
    }

    fun updateWallDistances() {
        wallDistances = null
    }

    fun expandGridIfNeeded(offsetX: Int, offsetY: Int, roomWidth: Int, roomHeight: Int) {
        val currentHeight = grid.size
        val currentWidth = grid[0].size

        val minX = minOf(0, offsetX)
        val maxX = maxOf(currentWidth, offsetX + roomWidth)
        val minY = minOf(0, offsetY)
        val maxY = maxOf(currentHeight, offsetY + roomHeight)

        val newWidth = maxX - minX
        val newHeight = maxY - minY
        val offsetXInNewGrid = -minX
        val offsetYInNewGrid = -minY

        val newGrid = Array(newHeight) { IntArray(newWidth) { 1 } }

        for (y in 0 until currentHeight) {
            for (x in 0 until currentWidth) {
                newGrid[y + offsetYInNewGrid][x + offsetXInNewGrid] = grid[y][x]
            }
        }

        grid = newGrid
        updateWallDistances()
    }

    private fun checkOverlap(
        offsetX: Int,
        offsetY: Int,
        template: RoomTemplate,
        triggerPoint: GridPoint,
        connectionPoint: GridPoint
    ): Boolean {
        val roomWidth = template.grid[0].size
        val roomHeight = template.grid.size

        for (y in 0 until roomHeight) {
            for (x in 0 until roomWidth) {
                val mapX = offsetX + x
                val mapY = offsetY + y
                if (mapY in grid.indices && mapX in grid[0].indices) {
                    if (template.grid[y][x] == 1) continue
                    if (mapX == triggerPoint.x && mapY == triggerPoint.y) continue
                    if (mapX == connectionPoint.x && mapY == connectionPoint.y) continue
                    if (grid[mapY][mapX] != 1) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun generateRoom(triggerPoint: GridPoint, entryDirection: Direction): GridPoint? {
        if (currentRooms < limitRooms) {
            val connectionPoint = when (entryDirection) {
                Direction.UP -> GridPoint(triggerPoint.x, triggerPoint.y - 1)
                Direction.DOWN -> GridPoint(triggerPoint.x, triggerPoint.y + 1)
                Direction.LEFT -> GridPoint(triggerPoint.x - 1, triggerPoint.y)
                Direction.RIGHT -> GridPoint(triggerPoint.x + 1, triggerPoint.y)
            }

            // Prefer entrances that align with the entry direction
            val preferredEntrances = when (entryDirection) {
                Direction.UP -> { entrance: GridPoint -> entrance.y == templates[0].grid.size - 1 } // Bottom entrance
                Direction.DOWN -> { entrance: GridPoint -> entrance.y == 0 } // Top entrance
                Direction.LEFT -> { entrance: GridPoint -> entrance.x == templates[0].grid[0].size - 1 } // Right entrance
                Direction.RIGHT -> { entrance: GridPoint -> entrance.x == 0 } // Left entrance
            }

            val shuffledTemplates = templates.shuffled()
            var selectedTemplate: RoomTemplate? = null
            var alignedEntrance: GridPoint? = null
            var offsetX = 1
            var offsetY = 0

            // First pass: try preferred entrances
            for (template in shuffledTemplates) {
                val validEntrances = template.entrances.filter(preferredEntrances).shuffled()
                for (entrance in validEntrances) {
                    offsetX = connectionPoint.x - entrance.x
                    offsetY = connectionPoint.y - entrance.y
                    if (checkOverlap(offsetX, offsetY, template, triggerPoint, connectionPoint)) {
                        selectedTemplate = template
                        alignedEntrance = entrance
                        break
                    }
                }
                if (selectedTemplate != null) break
            }

            // Second pass: try any entrance if no preferred ones work
            if (selectedTemplate == null) {
                for (template in shuffledTemplates) {
                    val shuffledEntrances = template.entrances.shuffled()
                    for (entrance in shuffledEntrances) {
                        offsetX = connectionPoint.x - entrance.x
                        offsetY = connectionPoint.y - entrance.y
                        if (checkOverlap(offsetX, offsetY, template, triggerPoint, connectionPoint)) {
                            selectedTemplate = template
                            alignedEntrance = entrance
                            break
                        }
                    }
                    if (selectedTemplate != null) break
                }
            }

            // Fallback: Try adjusting position slightly
            if (selectedTemplate == null) {
                for (template in shuffledTemplates) {
                    val entrance = template.entrances.random()
                    val offsets = listOf(
                        Pair(0, 0), Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1)
                    ).shuffled()
                    for ((dx, dy) in offsets) {
                        offsetX = connectionPoint.x - entrance.x + dx
                        offsetY = connectionPoint.y - entrance.y + dy
                        if (checkOverlap(offsetX, offsetY, template, triggerPoint, connectionPoint)) {
                            selectedTemplate = template
                            alignedEntrance = entrance
                            break
                        }
                    }
                    if (selectedTemplate != null) break
                }
            }

            if (selectedTemplate == null || alignedEntrance == null) {
                return null
            }

            val roomWidth = selectedTemplate.grid[0].size
            val roomHeight = selectedTemplate.grid.size

            expandGridIfNeeded(offsetX, offsetY, roomWidth, roomHeight)

            // Copy a template to the grid
            for (y in 0 until roomHeight) {
                for (x in 0 until roomWidth) {
                    val mapX = offsetX + x
                    val mapY = offsetY + y
                    if (mapY in grid.indices && mapX in grid[0].indices) {
                        grid[mapY][mapX] = selectedTemplate.grid[y][x]
                    }
                }
            }

            // Adding enemies and light sources for squares 3 and 6
            for (y in 0 until roomHeight) {
                for (x in 0 until roomWidth) {
                    val mapX = offsetX + x
                    val mapY = offsetY + y
                    if (mapY in grid.indices && mapX in grid[0].indices) {
                        if (selectedTemplate.grid[y][x] == 3) {
                            renderCast?.let {
                                enemies.add(Enemy(
                                    (tileSize * mapX) - (tileSize / 2),
                                    (tileSize * mapY) - (tileSize / 2),
                                    100,
                                    enemyTextureId!!,
                                    renderCast = it,
                                    this,
                                    speed = (2.0 * ((10..15).random() / 10.0))
                                ))
                                lightSources.add(LightSource(
                                    (mapX + 0.5),
                                    (mapY + 0.5),
                                    color = Color(20, 20, 200),
                                    intensity = 0.25,
                                    range = 1.0,
                                    owner = "${enemies[enemies.size - 1]}"
                                ))
                                println(lightSources.get(lightSources.size - 1).owner)
                            } ?: throw IllegalStateException("skip it")
                        }
                        if (selectedTemplate.grid[y][x] == 6) {
                            renderCast?.let {
                                lightSources.add(LightSource(
                                    (mapX + 0.5),
                                    (mapY + 0.5),
                                    color = Color(200, 20, 20),
                                    intensity = 0.25,
                                    range = 3.0,
                                    owner = "skun"
                                ))
                                it.repaint()
                            }
                        }
                    }
                }
            }

            // setting triggerPoint i lastEntrance
            grid[triggerPoint.y][triggerPoint.x] = 0

            lastEntrance?.let {
                if (it.y in grid.indices && it.x in grid[0].indices) {
                    grid[it.y][it.x] = 0
                }
            }

            val newEntrances = selectedTemplate.entrances.filter { it != alignedEntrance }
            val newEntrance = newEntrances.randomOrNull()
            if (newEntrance != null) {
                val newEntrancePoint = GridPoint(offsetX + newEntrance.x, offsetY + newEntrance.y)
                grid[newEntrancePoint.y][newEntrancePoint.x] = 5
                lastEntrance = newEntrancePoint
                roomConnections.add(Pair(triggerPoint, newEntrancePoint))
                updateWallDistances()
                return newEntrancePoint
            }

            currentRooms += 1

            updateWallDistances()
            return null
        }
        return null
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

    init {
        preferredSize = Dimension(miniMapSize + offsetX * 2, miniMapSize + offsetY * 2)
        isOpaque = false
    }

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        val g2 = v as Graphics2D

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val tileScale = miniMapSize.toDouble() / maxRenderTiles

        // Player position on the minimap (always in the center)
        val playerMapX = miniMapSize / 2 + offsetX
        val playerMapY = miniMapSize / 2 + offsetY

        // pos player -> titesize
        val playerGridX = positionX / tileSize
        val playerGridY = positionY / tileSize

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

        val enemies = renderCast.getEnemies()
        // Draw enemies and their enemy paths. Assign them a random color. Set the path color for this enemy
        enemies.forEach { enemy ->
            if (!enemyPathColors.containsKey(enemy)) {
                enemyPathColors[enemy] = Color(((72-44)..255).random(), (72..255).random(), ((72+44)..255).random(), 144)
            }
            // Ustaw kolor ścieżki dla tego wroga
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
        g2.font = Font("BOLD", Font.BOLD, 17)
        g2.drawString("FPS: ${renderCast.getRenderFps()*2}", 1366 - 90, 20)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(miniMapSize + offsetX * 2, miniMapSize + offsetY * 2)
    }
}
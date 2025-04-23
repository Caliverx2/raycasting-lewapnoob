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
import javax.swing.JLayeredPane
import kotlin.math.*


var map = true
var currentangle = 45
var tileSize = 40.0 // Rozmiar kafelka na mapie
val mapa = 0.5//0.075
var fps = 84
var MouseSupport = false

val TARGET_FPS = 70
val FRAME_TIME_NS = 1_000_000_000 / TARGET_FPS
var deltaTime = 1.0 / TARGET_FPS

var positionX = (tileSize*2)-(tileSize/2)  //kafelek*pozycja - (pół kafelka)
var positionY = (tileSize*2)-(tileSize/2)  //kafelek*pozycja - (pół kafelka)

class Enemy(var x: Double, var y: Double, var health: Int = 10, var texture: BufferedImage, private val renderCast: RenderCast, var speed: Double = 0.9) {
    private val map = Map()
    val size = 1.0 // Rozmiar przeciwnika
    private val margin = 10 // Margines dla kolizji
    private var path: List<Node> = emptyList() // Aktualna ścieżka
    private var pathUpdateTimer = 15 // Licznik do aktualizacji ścieżki
    private val pathUpdateInterval = 120 // Aktualizacja co 120 klatek
    private var stuckCounter = 0 // Licznik zablokowania
    private val maxStuckFrames = 60 // Maksymalna liczba klatek zablokowania
    var lastMoveX = 0.0 // Ostatni ruch w osi X
    var lastMoveY = 0.0 // Ostatni ruch w osi Y
    var isMoving = false // Czy przeciwnik się porusza
    private var smoothedMoveX = 0.0 // Wygładzony ruch w osi X
    private var smoothedMoveY = 0.0 // Wygładzony ruch w osi Y
    private val smoothingFactor = 1.55 // Współczynnik wygładzania
    private val MIN_PLAYER_DISTANCE = 2.0 * tileSize // Minimalna odległość od gracza (np. 80.0)
    private var chaseTimer = 0 // Licznik klatek do wznowienia pościgu

    companion object {
        const val TARGET_FPS = 60 // Docelowe FPS dla Enemy
    }

    // Struktura węzła dla ścieżki
    data class Node(val x: Int, val y: Int)

    // Sprawdzenie, czy można się poruszyć na daną pozycję (ściany, inni przeciwnicy, gracz)
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
                if (gridY !in map.grid.indices || gridX !in map.grid[gridY].indices || map.grid[gridY][gridX] == 1) {
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

        // Enemy-player collision
        val dx = newX - positionX
        val dy = newY - positionY
        val newDistance = sqrt(dx * dx + dy * dy)
        val currentDistance = sqrt((x - positionX) * (x - positionX) + (y - positionY) * (y - positionY))
        if (newDistance < MIN_PLAYER_DISTANCE && newDistance < currentDistance) {
            return Pair(false, null) // Blokuj ruch, jeśli zbliża do gracza poniżej MIN_PLAYER_DISTANCE
        }

        return Pair(true, null)
    }

    // Próba przepchnięcia innego przeciwnika
    fun tryPush(otherEnemy: Enemy, moveX: Double, moveY: Double): Boolean {
        if (otherEnemy.isMoving) {
            // Sprawdzenie, czy poruszają się w przeciwnych kierunkach
            val dotProduct = (moveX * otherEnemy.lastMoveX + moveY * otherEnemy.lastMoveY)
            if (dotProduct < 0) {
                return false // Przeciwne kierunki, brak przepychania
            }
        }
        // Przepchnij innego przeciwnika
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

    // Algorytm BFS (Breadth-First Search)
    fun findPath(): List<Node> {
        val startX = (x / tileSize).toInt()
        val startY = (y / tileSize).toInt()
        val goalX = (positionX / tileSize).toInt()
        val goalY = (positionY / tileSize).toInt()

        if (startY !in map.grid.indices || startX !in map.grid[0].indices ||
            goalY !in map.grid.indices || goalX !in map.grid[0].indices ||
            map.grid[goalY][goalX] == 1) {
            return emptyList()
        }

        val queue = ArrayDeque<Node>().apply { add(Node(startX, startY)) }
        val visited = mutableSetOf<Node>()
        val cameFrom = mutableMapOf<Node, Node>()
        visited.add(Node(startX, startY))

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.x == goalX && current.y == goalY) {
                // Rekonstrukcja ścieżki
                val path = mutableListOf(current)
                var curr = current
                while (cameFrom.containsKey(curr)) {
                    curr = cameFrom[curr]!!
                    path.add(curr)
                }
                return path.reversed()
            }

            for (neighbor in getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor)
                    cameFrom[neighbor] = current
                    queue.add(neighbor)
                }
            }
        }

        return emptyList() // Brak ścieżki
    }

    // Pobieranie sąsiadów dla pathfindingu
    fun getNeighbors(node: Node): List<Node> {
        val neighbors = mutableListOf<Node>()
        val directions = listOf(
            Node(node.x + 1, node.y), Node(node.x - 1, node.y),
            Node(node.x, node.y + 1), Node(node.x, node.y - 1)
        )
        for (dir in directions) {
            if (dir.y in map.grid.indices && dir.x in map.grid[0].indices && map.grid[dir.y][dir.x] != 1) {
                neighbors.add(dir)
            }
        }
        return neighbors
    }

    // Metoda aktualizacji przeciwnika
    fun update() {
        val deltaTime = 1.0 / TARGET_FPS
        chaseTimer++ // Zwiększ licznik klatek

        // Sprawdzenie odległości od gracza
        val dxToPlayer = x - positionX
        val dyToPlayer = y - positionY
        val distanceToPlayer = sqrt(dxToPlayer * dxToPlayer + dyToPlayer * dyToPlayer)

        // Wznowienie pościgu co TARGET_FPS klatek
        if (chaseTimer >= TARGET_FPS && health > 0) {
            path = findPath() // Szukaj nowej trasy
            pathUpdateTimer = 0 // Zresetuj timer aktualizacji ścieżki
            stuckCounter = 0 // Zresetuj licznik zablokowania
            smoothedMoveX = 0.0 // Zresetuj wygładzanie
            smoothedMoveY = 0.0
            chaseTimer = 0 // Zresetuj licznik pościgu
        }

        // Ruch oddalający, jeśli zbyt blisko gracza i brak ścieżki
        if (distanceToPlayer < MIN_PLAYER_DISTANCE && path.isEmpty() && health > 0) {
            val moveSpeed = speed * deltaTime * TARGET_FPS
            val rawMoveX = if (distanceToPlayer > 0) (dxToPlayer / distanceToPlayer) * moveSpeed else 0.0
            val rawMoveY = if (distanceToPlayer > 0) (dyToPlayer / distanceToPlayer) * moveSpeed else 0.0

            // Wygładzanie ruchu oddalającego
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
            return // Pomiń normalny ruch BFS
        }

        pathUpdateTimer++
        if ((pathUpdateTimer >= pathUpdateInterval || stuckCounter > maxStuckFrames) && health > 0) {
            path = findPath()
            pathUpdateTimer = 0
            stuckCounter = 0
        }

        isMoving = path.isNotEmpty() // Aktualizacja stanu ruchu
        if (path.isNotEmpty() && health > 0) {
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

                // Wygładzanie ruchu
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
                    // Próba przepchnięcia innego przeciwnika
                    if (tryPush(collidedEnemy, smoothedMoveX, smoothedMoveY)) {
                        x = newX
                        y = newY
                        lastMoveX = smoothedMoveX
                        lastMoveY = smoothedMoveY
                        stuckCounter = 0
                    } else {
                        stuckCounter++
                        // „Nudge” w przeciwnym kierunku
                        val nudgeX = x - (dx / distance) * 0.1
                        val nudgeY = y - (dy / distance) * 0.1
                        if (canMoveTo(nudgeX, nudgeY).first) {
                            x = nudgeX
                            y = nudgeY
                        }
                    }
                } else {
                    stuckCounter++
                    // „Nudge” w przeciwnym kierunku
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
}

data class Node(val x: Int, val y: Int)

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


    var lastFrameTime = System.nanoTime()
    var frameCount = 0
    var lastFpsUpdate = System.nanoTime()

    while (true) {
        val currentTime = System.nanoTime()
        val elapsedTime = currentTime - lastFrameTime // Poprawiona linia

        // Aktualizuj grę tylko, jeśli minął odpowiedni czas (dla TARGET_FPS)
        if (elapsedTime >= FRAME_TIME_NS) {
            player.update(keysPressed)
            renderCast.repaint()
            mapa.repaint()

            frameCount++
            lastFrameTime = currentTime

            // Oblicz FPS co sekundę
            if (currentTime - lastFpsUpdate >= 1_000_000_000) { // 1 sekunda
                fps = frameCount
                frameCount = 0
                lastFpsUpdate = currentTime
            }
        }

        // Krótka pauza, aby nie obciążać procesora
        delay(10)
    }

    /*
    while (MouseSupport) {
        delay(75)
        Robot().mouseMove(MouseInfo.getPointerInfo().location.x, 0)
        Robot().mouseMove(960, 0)
        Robot().mouseMove(960, 384)
    } */
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
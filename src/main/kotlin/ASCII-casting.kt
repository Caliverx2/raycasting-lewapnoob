package org.example
/*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.text.SimpleAttributeSet
import kotlin.concurrent.fixedRateTimer
import kotlin.math.*

var map = true
var positionX = 100
var positionY = 100
var currentangle = 0
var shotx = 0.0
var shoty = 0.0

class Player {
    private val map = Map()
    private val tileSize = 30
    private val playerSize = 2
    private val margin = 1
    private var movementSpeed = 2.5
    private var lastMouseX: Int? = null
    private val sensitivity = 0.01

    private fun canMoveTo(x: Int, y: Int): Boolean {
        val left = x - playerSize / 2
        val right = x + playerSize / 2
        val top = y - playerSize / 2
        val bottom = y + playerSize / 2

        val gridLeft = (left - margin) / tileSize
        val gridRight = (right + margin) / tileSize
        val gridTop = (top - margin) / tileSize
        val gridBottom = (bottom + margin) / tileSize

        for (gridY in gridTop..gridBottom) {
            for (gridX in gridLeft..gridRight) {
                if (gridY !in map.grid.indices || gridX !in map.grid[gridY].indices || ((map.grid[gridY][gridX] != 0) && (map.grid[gridY][gridX] != 5))) {
                    return false
                }
            }
        }
        return true
    }

    fun w() {
        val anglex = (positionX + movementSpeed * cos(Math.toRadians(currentangle.toDouble()))).toInt()
        val angley = (positionY + movementSpeed * sin(Math.toRadians(currentangle.toDouble()))).toInt()
        if (canMoveTo(anglex, angley)) {
            positionX = anglex
            positionY = angley
        }
    }

    fun s() {
        val anglex = (positionX + (-movementSpeed) * cos(Math.toRadians(currentangle.toDouble()))).toInt()
        val angley = (positionY + (-movementSpeed) * sin(Math.toRadians(currentangle.toDouble()))).toInt()
        if (canMoveTo(anglex, angley)) {
            positionX = anglex
            positionY = angley
        }
    }

    fun a() {
        val anglex = (positionX + movementSpeed * cos(Math.toRadians(currentangle - 90.0))).toInt()
        val angley = (positionY + movementSpeed * sin(Math.toRadians(currentangle - 90.0))).toInt()
        if (canMoveTo(anglex, angley)) {
            positionX = anglex
            positionY = angley
        }
    }

    fun d() {
        val anglex = (positionX + movementSpeed * cos(Math.toRadians(currentangle + 90.0))).toInt()
        val angley = (positionY + movementSpeed * sin(Math.toRadians(currentangle + 90.0))).toInt()
        if (canMoveTo(anglex, angley)) {
            positionX = anglex
            positionY = angley
        }
    }

    fun anglea() {
        currentangle -= 2
    }

    fun angled() {
        currentangle += 2
    }

    fun updateAngleFromMouse(mouseX: Int, centerX: Int) {
        if (MouseInfo.getPointerInfo().location.x > centerX) {
            currentangle += (((MouseInfo.getPointerInfo().location.x) - centerX) * sensitivity).toInt()
        }
        if (MouseInfo.getPointerInfo().location.x < centerX) {
            currentangle += (((MouseInfo.getPointerInfo().location.x) - centerX) * sensitivity).toInt()
        }
        lastMouseX = centerX
    }
}

class Tlo : JPanel() {
    init {
        isOpaque = true
    }

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.LIGHT_GRAY
        v.fillRect(0, 0, 1368 * 2, 768 / 2)
        v.color = Color.LIGHT_GRAY
        v.fillRect(0, 384, 1368 * 2, (768 / 2))
    }
}

class RenderCast : JTextPane() {
    private val timer = fixedRateTimer(name = "ray-rend", initialDelay = 250, period = 150) { render() }
    private val columns = 80
    private val rows = 25
    private val rayCount = columns
    private val rayDistances = DoubleArray(rayCount) { 0.0 }
    private val hitTileValues = IntArray(rayCount) { 0 }
    private val fov = 80.0
    private val angleIncrement = fov / rayCount
    private val map = Map()
    private var brightness = 0.85

    init {
        font = Font("Courier New", Font.PLAIN, 14)
        isEditable = false
        background = Color.BLACK
    }

    fun shotgun() {
        val rayAngle = Math.toRadians(currentangle.toDouble())
        var distance = 0.0
        var distansofgun = 0.0
        var hit = false
        var tileValue = 0

        while (distance < 300.0 && !hit) {
            val rayX = positionX + distance * cos(rayAngle)
            val rayY = positionY + distance * sin(rayAngle)

            val gridX = (rayX / 30.0).toInt()
            val gridY = (rayY / 30.0).toInt()

            if (gridY in map.grid.indices && gridX in map.grid[gridY].indices && map.grid[gridY][gridX] >= 1) {
                tileValue = map.grid[gridY][gridX]
                distansofgun = sqrt((rayX - positionX).pow(2) + (rayY - positionY).pow(2))
                shotx = rayX
                shoty = rayY
                hit = true
            }

            distance += 0.25
        }

        if (!hit) {
            distansofgun = 300.0
        }
    }

    private fun render() {
        for (i in 0 until rayCount) {
            val rayAngle = Math.toRadians(currentangle + (i * angleIncrement - fov / 2))
            var distance = 0.0
            var hit = false
            var tileValue = 0

            while (distance < 300.0 && !hit) {
                val rayX = positionX + distance * cos(rayAngle)
                val rayY = positionY + distance * sin(rayAngle)

                val gridX = (rayX / 30.0).toInt()
                val gridY = (rayY / 30.0).toInt()

                if (gridY in map.grid.indices && gridX in map.grid[gridY].indices && map.grid[gridY][gridX] >= 1) {
                    tileValue = map.grid[gridY][gridX]
                    rayDistances[i] = sqrt((rayX - positionX).pow(2) + (rayY - positionY).pow(2))
                    hitTileValues[i] = tileValue
                    hit = true
                }

                distance += 0.25
            }

            if (!hit) {
                rayDistances[i] = 300.0
                hitTileValues[i] = 0
            }
        }
        updateAsciiRender()
    }

    private fun updateAsciiRender() {
        val doc = styledDocument
        doc.remove(0, doc.length)

        val chars = arrayOf(".", ",", ":", ";", "'", "\"", "^", "~", "-", "=", "_", "*", "+", "#", "%", "@", "$", "&", "X", "M")

        for (row in 0 until rows) {
            val builder = StringBuilder()
            val attributes = SimpleAttributeSet()

            for (col in 0 until columns) {
                val relativeAngle = Math.toRadians(col * angleIncrement - fov / 2)
                val correctedDistance = rayDistances[col] * cos(relativeAngle)
                val wallHeight = (800.0 / correctedDistance).toInt().coerceAtMost(rows - 2)
                val wallTop = (rows - wallHeight) / 2
                val wallBottom = wallTop + wallHeight

                val intensity = (1.0 - (correctedDistance / 300.0)).coerceIn(0.2, 1.0)
                val grayValue = (intensity * 255 * brightness).toInt().coerceIn(50, 255)

                when {
                    row < wallTop -> {
                        //StyleConstants.setForeground(attributes, Color.BLUE.darker())
                        builder.append('*')
                    }
                    row in wallTop until wallBottom -> {
                        val charIndex = (19 * intensity).toInt().coerceIn(0, 19) // Mapowanie odległości na indeks znaku
                        val char = chars[charIndex]
                        builder.append(char)
                    }
                    else -> {
                        //StyleConstants.setForeground(attributes, Color.GREEN.darker())
                        builder.append('*')
                    }
                }
            }
            while (builder.length < columns) {
                builder.append(' ')
            }
            builder.append('\n')
            doc.insertString(doc.length, builder.toString(), attributes)
        }
    }
}

class PlayerOnScreen : JPanel() {
    private val timer2 = fixedRateTimer(name = "ray-calc", initialDelay = 500, period = 1) { pozycjagracza() }
    private var x = 0
    private var y = 0
    private var xposs = 1
    private var yposs = 1

    private fun pozycjagracza() {
        x = positionX
        y = positionY
        val angleru = Math.toRadians(currentangle.toDouble())
        xposs = (x / 3 + 10 * cos(angleru)).toInt()
        yposs = (y / 3 + 10 * sin(angleru)).toInt()
        repaint()
    }

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.green
        v.fillRect((x - 2) / 3, (y - 2) / 3, 4, 4)

        val g2 = v as Graphics2D
        g2.color = Color.yellow
        g2.stroke = BasicStroke(2f)
        g2.drawLine(x / 3, y / 3, xposs, yposs)
    }
}

class Map {
    val grid: Array<IntArray> = arrayOf(
        intArrayOf(5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(5, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1, 1, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1),
        intArrayOf(1, 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1),
        intArrayOf(1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1),
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 5),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 5, 5)
    )
}

class Mappingmap : JPanel() {
    private val map = Map()
    private val mnoznik = 10

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.gray
        for (row in map.grid.indices) {
            for (col in map.grid[row].indices) {
                if (map.grid[row][col] == 1) {
                    v.color = Color.gray
                    v.fillRect(col * mnoznik, row * mnoznik, mnoznik, mnoznik)
                }
                if (map.grid[row][col] == 5) {
                    v.color = Color.YELLOW
                    v.fillRect(col * mnoznik, row * mnoznik, mnoznik, mnoznik)
                }
            }
        }
        isOpaque = false
    }
}

fun main() = runBlocking {
    val frame = JFrame("rolada z gówna")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.isResizable = false
    frame.setSize(1366, 768)
    frame.setLocation(((Toolkit.getDefaultToolkit().screenSize.width - frame.width) / 2), ((Toolkit.getDefaultToolkit().screenSize.height - frame.height) / 2))

    frame.cursor = frame.toolkit.createCustomCursor(
        java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB),
        java.awt.Point(0, 0),
        "invisible"
    )

    frame.isVisible = true

    val layeredPane = JLayeredPane()
    layeredPane.setSize(1366, 768)
    layeredPane.setBounds(0, 0, 1366, 768)
    frame.add(layeredPane)

    val mapa = Mappingmap()
    mapa.isOpaque = false
    mapa.layout = null
    mapa.setSize(1366, 768)
    mapa.setBounds(0, 0, 1366, 768)

    val ekran = Tlo()
    ekran.isOpaque = true
    ekran.layout = null
    ekran.setSize(1366, 768)
    ekran.setBounds(0, 0, 1366, 768)

    val playerOnScreen = PlayerOnScreen()
    playerOnScreen.isOpaque = false
    playerOnScreen.setSize(1366, 768)
    playerOnScreen.setBounds(0, 0, 1366, 768)

    val renderCast = RenderCast()
    renderCast.isOpaque = false
    renderCast.setSize(1366, 768)
    renderCast.setBounds(0, 0, 1366, 768)

    frame.add(ekran)
    layeredPane.add(mapa, 3)
    layeredPane.add(playerOnScreen, 4)
    layeredPane.add(renderCast, 6)

    val player = Player()

    var centerX = frame.width / 2
    var centerY = frame.height / 2

    frame.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                renderCast.shotgun()
            } /*else {
                println("Naciśnięto klawisz: ${event.button}")
            }*/
        }
    })

    frame.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseMoved(e: MouseEvent) {
            player.updateAngleFromMouse(e.x, centerX)
        }

        override fun mouseDragged(e: MouseEvent) {
            player.updateAngleFromMouse(e.x, centerX)
        }
    })

    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            when (event.keyCode) {
                KeyEvent.VK_W -> player.w()
                KeyEvent.VK_S -> player.s()
                KeyEvent.VK_A -> player.a()
                KeyEvent.VK_D -> player.d()
                KeyEvent.VK_LEFT -> player.anglea()
                KeyEvent.VK_RIGHT -> player.angled()
                KeyEvent.VK_SPACE -> renderCast.shotgun()
                KeyEvent.VK_UP -> player.w()
                KeyEvent.VK_DOWN -> player.s()
            }
        }

        override fun keyReleased(e: KeyEvent?) {
            super.keyReleased(e)
            when (e?.keyCode) {
                KeyEvent.VK_M -> map = true
            }
        }
    })

    frame.addComponentListener(object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent?) {
            centerX = frame.x + frame.width / 2
            centerY = frame.y + frame.height / 2
        }
    })

    while (true) {
        delay(65)
        Robot().mouseMove(960, 384)
    }
} */
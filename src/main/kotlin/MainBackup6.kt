package org.example.MainKt

//./gradlew shadowJar

import java.awt.BasicStroke
import javax.swing.JFrame
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JPanel
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Line2D
import javax.swing.JLayeredPane
import kotlin.concurrent.fixedRateTimer
import kotlin.math.*
/*
var positionX = 100
var positionY = 100
var currentangle = 0
var shotx = 0.0
var shoty = 0.0

class Player {
    private val map = Map()
    private val tileSize = 30
    private val playerSize = 2 // Rozmiar gracza w pikselach (np. szerokość/wysokość prostokąta)
    private val margin = 1 // Margines błędu w pikselach
    private var movementSpeed = 2.5

    private fun canMoveTo(x: Int, y: Int): Boolean {
        // Oblicz granice prostokąta reprezentującego gracza
        val left = x - playerSize / 2
        val right = x + playerSize / 2
        val top = y - playerSize / 2
        val bottom = y + playerSize / 2

        // Sprawdź kafelki, w których znajdują się rogi prostokąta gracza
        val gridLeft = (left - margin) / tileSize
        val gridRight = (right + margin) / tileSize
        val gridTop = (top - margin) / tileSize
        val gridBottom = (bottom + margin) / tileSize

        // Sprawdź, czy wszystkie kafelki w obszarze są wolne (nie są ścianami)
        for (gridY in gridTop..gridBottom) {
            for (gridX in gridLeft..gridRight) {
                // Sprawdź granice siatki i czy kafelek jest ścianą
                if (gridY !in map.grid.indices || gridX !in map.grid[gridY].indices || ((map.grid[gridY][gridX] != 0) and (map.grid[gridY][gridX] != 5))) {
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
        currentangle -= 2 //9
    }

    fun angled() {
        currentangle += 2 //9
    }
}

class Tlo : JPanel() {
    init {
        isOpaque = true
    }
    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.lightGray
        v.fillRect(0, 0, 1368*2, 768/2)   //57, 32
        v.color = Color.darkGray
        v.fillRect(0, 384, 1368*2, (768/2))   //57, 32
    }
}

class RenderCast : JPanel() {
    private val timer = fixedRateTimer(name = "ray-rend", initialDelay = 250, period = 150) { render() }
    private val lines = mutableListOf<Line2D.Double>()

    private val rayCount = 190
    private val rayDistances = DoubleArray(rayCount) { 0.0 }
    private val hitTileValues = IntArray(rayCount) { 0 }
    private val fov = 80.0
    private val angleIncrement = fov / rayCount

    private val map = Map()
    private var brightness = 0.85 // Domyślna jasność (1.0 = pełna jasność)

    init {
        for (row in map.grid.indices) {
            for (col in map.grid[row].indices) {
                if (map.grid[row][col] >= 1) {
                    val x = col * 30.0
                    val y = row * 30.0
                    lines.add(Line2D.Double(x, y, x + 30.0, y))
                    lines.add(Line2D.Double(x, y + 30.0, x + 30.0, y + 30.0))
                    lines.add(Line2D.Double(x, y, x, y + 30.0))
                    lines.add(Line2D.Double(x + 30.0, y, x + 30.0, y + 30.0))
                }
            }
        }
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
        repaint()
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
        repaint()
    }

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        val stripWidth = (1366.0 / rayCount).toInt()
        val startX = ((1366.0 - stripWidth * rayCount) / 2).toInt()

        for (i in 0 until rayCount) {
            val relativeAngle = Math.toRadians(i * angleIncrement - fov / 2)
            val correctedDistance = rayDistances[i] * cos(relativeAngle)

            fun map(value: Double, fromLow: Double, fromHigh: Double, toLow: Double, toHigh: Double): Double {
                return (value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow) + toLow
            }

            val intensity = map(correctedDistance, 0.0, 300.0, 1.0, 0.1)
            val blueValue = (intensity * 180).toInt().coerceIn(0, 180)

            // Ustaw kolor z uwzględnieniem jasności
            v.color = if (hitTileValues[i] == 5) {
                // Dla wartości 5 (żółty): skaluj jasność
                val yellowR = (Color.YELLOW.red * brightness).toInt().coerceIn(0, 255)
                val yellowG = (Color.YELLOW.green * brightness).toInt().coerceIn(0, 255)
                val yellowB = (Color.YELLOW.blue * brightness).toInt().coerceIn(0, 255)
                Color(yellowR, yellowG, yellowB)
            } else {
                // Dla ścian (niebieski): skaluj jasność
                val scaledBlue = (blueValue * brightness).toInt().coerceIn(0, 255)
                Color(0, 0, scaledBlue)
            }

            val stripHeight = (5000 / correctedDistance).toInt().coerceAtMost(768)
            v.fillRect(
                (startX + i * stripWidth), ((768 - stripHeight) / 2), stripWidth, (stripHeight *  1.5).toInt())

            val g2 = v as Graphics2D
            g2.color = Color.red
            g2.stroke = BasicStroke(5f)
            g2.drawLine((shotx/3).toInt(), (shoty/3).toInt(), (shotx/3).toInt(), (shoty/3).toInt())
            g2.color = Color.lightGray
            g2.drawLine(1366/2, 768/2, 1366/2, 768/2)
        }       // 1366, 768
    }
}

class PlayerOnScreen : JPanel() {
    private val timer2 = fixedRateTimer(name = "ray-calc", initialDelay = 500, period = 1) {pozycjagracza()}

    private var x = 0 //positionX
    private var y = 0 //positionY
    private var xposs = 1 //CurrentRayPositionX
    private var yposs = 1 //CurrentRayPositionY

    private fun pozycjagracza() {
        x = positionX
        y = positionY

        //rayAngle = Math.toRadians(angle)   obliczanie kąta promienia w radianach
        //xPozycjaPromienia = (x + distance * cos(rayAngle)).toInt()  wzór: promień wokół fov gracza
        //yPozycjaPromienia = (y + distance * sin(rayAngle)).toInt()

        val angleru = Math.toRadians(currentangle.toDouble())
        xposs = (x/3 + 10 * cos(angleru)).toInt()
        yposs = (y/3 + 10 * sin(angleru)).toInt()
        repaint()
    }
    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.green
        v.fillRect((x-2)/3, (y-2)/3, 4, 4)

        val g2 = v as Graphics2D
        g2.color = Color.yellow
        g2.stroke = BasicStroke(2f)
        g2.drawLine((x)/3, (y)/3, (xposs).toInt(), (yposs).toInt())
    }
}

fun main() {
    //podstawa wyświetlania
    val frame = JFrame("rolada z gówna")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    //frame.isAlwaysOnTop = true
    //frame.isUndecorated = true
    frame.iconImage = Toolkit.getDefaultToolkit().getImage("src/main/resources/icon/icon.jpg" )
    frame.isResizable = false
    frame.setSize(1366, 768)
    frame.setLocation(((Toolkit.getDefaultToolkit().screenSize.width - frame.width)/2), ((Toolkit.getDefaultToolkit().screenSize.height - frame.height)/2))
    frame.isVisible = true

    //warstwy
    val layeredPane = JLayeredPane()
    layeredPane.setSize(1366, 768)
    layeredPane.setBounds(0, 0, 1366, 768)
    frame.add(layeredPane)

    val mapa = Mappingmap()
    mapa.hasFocus()
    mapa.isOpaque = false
    mapa.layout = null
    mapa.setSize(1366, 768)
    mapa.setBounds(0, 0, 1366, 768)

    val ekran = Tlo()
    ekran.hasFocus()
    ekran.isOpaque = true
    ekran.layout = null
    ekran.setSize(1366, 768)
    ekran.setBounds(0, 0, 1366, 768)

    val playerOnScreen = PlayerOnScreen()
    playerOnScreen.hasFocus()
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

    frame.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                RenderCast().shotgun()
            }

            else {
                println("Naciśnięto klawisz: ${event.button}")
            }
        }
    })

    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            when (event.keyCode) {
                KeyEvent.VK_W -> Player().w()
                KeyEvent.VK_S -> Player().s()
                KeyEvent.VK_A -> Player().a()
                KeyEvent.VK_D -> Player().d()
                KeyEvent.VK_LEFT -> Player().anglea()
                KeyEvent.VK_RIGHT -> Player().angled()

                //KeyEvent.VK_SPACE -> println("kosmos bratku")
                //else -> println("Naciśnięto klawisz: ${event.keyCode}")
            }
        }
    })
}

class Map {
    val grid: Array<IntArray> = arrayOf(
        //wartości: 1-ściana, 0-pusta przestrzeń, 5-początek i koniec labiryntu
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(5,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1,0,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,1,0,0,0,1,0,1,0,0,0,1,0,1),
        intArrayOf(1,1,1,1,1,1,1,0,1,0,1,0,1,1,1,0,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,1,0,0,0,1,0,0,0,1,0,1,0,0,0,1),
        intArrayOf(1,0,1,0,1,0,1,1,1,1,1,1,1,0,1,0,1,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,0,0,0,0,1,0,1,0,1,0,1,0,1),
        intArrayOf(1,1,1,0,1,0,1,0,1,1,1,0,1,0,1,0,1,0,1,1,1),
        intArrayOf(1,0,0,0,1,0,1,0,0,0,1,0,0,0,1,0,1,0,0,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,1,0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,1,1,1,1,0,1,0,1,1,1,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,0,0,1,0,1,0,1,0,1,0,0,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,1,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,0,0,0,1,0,1,0,0,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,1,1,1,1,0,1,0,1,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,0,0,0,0,0,1,0,1,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,0,1,1,1,1,1,0,1,0,1,0,1),
        intArrayOf(1,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,5),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
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
                if (map.grid[row][col] > 1) {
                    v.color = Color.YELLOW
                    v.fillRect(col * mnoznik, row * mnoznik, mnoznik, mnoznik)
                }
            }
        }
        isOpaque = false
    }
} */
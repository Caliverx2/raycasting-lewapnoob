package org.example.MainKt
/*
import java.awt.BasicStroke
import java.awt.BorderLayout
import javax.swing.JFrame
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JPanel
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.awt.geom.Line2D
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread
import kotlin.random.Random

var xd = 120.0
var yd = 60.0
var directionn = Point(-10, -10)


class PanelRay : JPanel() {
    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.red
        for (i in 0..62 step 1) {
            val y = 128 //Random.nextInt(128, 384)
            val x = 0 + (i * 22)
            v.fillRect(x, y, 22, 256)
        }
        isOpaque = false
    }
}

class PanelRay2 : JPanel() {
    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.lightGray
        val x = 0
        val y = 0
        v.fillRect(x, y, 1368, 384)
    }
}

class PanelRay3 : JPanel() {
    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.black
        val x = 0
        val y = 0//384
        v.fillRect(x, y, 1368, 384)
    }
}

class PlayerPositionMap : JPanel() {
    private var x = 3
    private var y = 3

    init {
        isOpaque = false
        thread {
            while (true) {
                x = xd.toInt()
                y = yd.toInt()
                repaint()
                Thread.sleep(25)
            }
        }
    }

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.green
        v.fillRect(x, y, 8, 8)
    }
}

class Mappingmap : JPanel() {
    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.darkGray
        for (i in 0..5 step 1) {
            val y = 0
            val x = 0
            if (Map().id1[i] == 1) {
                v.fillRect(x + (i * 30), y, 30, 30)
            }
        }
        for (i in 0..5 step 1) {
            val y = 30
            val x = 0
            if (Map().id2[i] == 1) {
                v.fillRect(x + (i * 30), y, 30, 30)
            }
        }
        for (i in 0..5 step 1) {
            val y = 60
            val x = 0
            if (Map().id3[i] == 1) {
                v.fillRect(x + (i * 30), y, 30, 30)
            }
        }
        for (i in 0..5 step 1) {
            val y = 90
            val x = 0
            if (Map().id4[i] == 1) {
                v.fillRect(x + (i * 30), y, 30, 30)
            }
        }
        for (i in 0..5 step 1) {
            val y = 120
            val x = 0
            if (Map().id5[i] == 1) {
                v.fillRect(x + (i * 30), y, 30, 30)
            }
        }
        isOpaque = false
    }
}

class Map {
    val id1: List<Int> = listOf(1, 1, 1, 1, 1, 1)
    val id2: List<Int> = listOf(1, 0, 0, 0, 0, 1)
    val id3: List<Int> = listOf(1, 0, 1, 0, 0, 1)
    val id4: List<Int> = listOf(1, 0, 0, 0, 0, 1)
    val id5: List<Int> = listOf(1, 1, 1, 1, 1, 1)
}

class Player {
    fun w() {
        yd -= 10.0
    }

    fun s() {
        yd += 10.0
    }

    fun a() {
        xd -= 10.0
    }

    fun d() {
        xd += 10.0
    }
}


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

class RayPanel : JPanel() {
    private var rayX = xd
    private var rayY = yd
    private var direction = Point(-10, -10)
    private val lines = mutableListOf<Line2D.Double>()
    private val timer = fixedRateTimer(name = "ray-timer", initialDelay = 0, period = 50) {
        updateRayPosition()
    }
    private var intersectionX = 0.0
    private var intersectionY = 0.0

    init {
        preferredSize = Dimension(1366, 768)
        isOpaque = false
        background = Color.GREEN

        // Dodajemy przykładowe linie
        for (i in 0..5 step 1) {
            if (Map().id1[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0, 30.0 + (i*30), 0.0)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0, 30.0 + (i*30), 30.0)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0, 0.0 + (i*30), 30.0)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0, 30.0 + (i*30), 30.0)) //Prawa
            }
        }
        for (i in 0..5 step 1) {
            if (Map().id2[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 30, 30.0 + (i*30), 0.0 + 30)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 30, 30.0 + (i*30), 30.0 + 30)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 30, 0.0 + (i*30), 30.0 + 30)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 30, 30.0 + (i*30), 30.0 + 30)) //Prawa
            }
        }
        for (i in 0..5 step 1) {
            if (Map().id3[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 60, 30.0 + (i*30), 0.0 + 60)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 60, 30.0 + (i*30), 30.0 + 60)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 60, 0.0 + (i*30), 30.0 + 60)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 60, 30.0 + (i*30), 30.0 + 60)) //Prawa
            }
        }
        for (i in 0..5 step 1) {
            if (Map().id4[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 90, 30.0 + (i*30), 0.0 + 90)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 90, 30.0 + (i*30), 30.0 + 90)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 90, 0.0 + (i*30), 30.0 + 90)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 90, 30.0 + (i*30), 30.0 + 90)) //Prawa
            }
        }
        for (i in 0..5 step 1) {
            if (Map().id5[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 120, 30.0 + (i*30), 0.0 + 120)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 120, 30.0 + (i*30), 30.0 + 120)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 120, 0.0 + (i*30), 30.0 + 120)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 120, 30.0 + (i*30), 30.0 + 120)) //Prawa
            }
        }
    }

    private fun updateRayPosition() {
        direction = directionn
        rayX += direction.x
        rayY += direction.y
        //rayY = yd
        //rayX = xd
        checkCollision()
        repaint()
    }

    private fun checkCollision() {
        for (line in lines) {
            if (line.intersects(rayX, rayY, 5.1, 5.1)) {
                intersectionX = rayX
                intersectionY = rayY
                onCollision()
                return
            }
        }
    }

    private fun onCollision() {
        // Wysłanie sygnału - wyświetlenie współrzędnych punktu przecięcia
        //println("x: $intersectionX, y: $intersectionY")
        rayX = xd
        rayY = yd
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        // Rysowanie linii
        g.color = Color.WHITE
        val g2 = g as Graphics2D
        g2.stroke = BasicStroke(5f)
        for (line in lines) {
            g2.draw(line)
        }

        g.color = Color.RED
        g.fillOval(intersectionX.toInt(), intersectionY.toInt(), 9, 9)

        g.color = Color.YELLOW
        g.fillOval(rayX.toInt(), rayY.toInt(), 6, 6)
    }
}

fun main() {
    Map()

    //podstawa wyświetlania
    val frame = JFrame("Raycasting2D")
    frame.setSize(1366, 768)
    frame.layout = BorderLayout()
    //frame.iconImage = Toolkit.getDefaultToolkit().getImage("app/src/main/icon/icon.png")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.isResizable = false

    val layeredPane = JLayeredPane()
    layeredPane.setSize(1366, 768)
    layeredPane.setBounds(0, 0, 1866, 1768)
    frame.add(layeredPane)

    //grafika
    val panel = PanelRay()
    panel.setSize(1366, 768)
    panel.setBounds(0, 96, 1366, 768)
    panel.isOpaque = true
    panel.layout = null

    val panel2 = PanelRay2()
    panel2.isOpaque = true
    panel2.layout = null
    panel2.setSize(1366, 0)
    panel2.setBounds(0, 0, 1366, 384)

    val panel3 = PanelRay3()
    panel3.isOpaque = true
    panel3.layout = null
    panel3.setSize(1366, 0)
    panel3.setBounds(0, 384, 1366, 384)

    val mapa = Mappingmap()
    mapa.hasFocus()
    mapa.isOpaque = true
    mapa.layout = null
    mapa.setSize(1366, 768)
    mapa.setBounds(0, 0, 1366, 768)

    val RayPane = RayPanel()
    RayPane.hasFocus()
    RayPane.setSize(1366, 768)
    RayPane.setBounds(0, 0, 1366, 768)



    layeredPane.add(mapa, JLayeredPane.DEFAULT_LAYER)
    layeredPane.add(panel, JLayeredPane.PALETTE_LAYER)
    layeredPane.add(RayPane, JLayeredPane.PALETTE_LAYER)
    frame.add(panel3) //dolny
    frame.add(panel2) //gorny

    //wykrywanie klawiszy

    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            if ("${event.keyCode}" == "38") {
                println("sex")
            }
            when (event.keyCode) {
                KeyEvent.VK_W -> Player().w()
                KeyEvent.VK_S -> Player().s()
                KeyEvent.VK_A -> Player().a()
                KeyEvent.VK_D -> Player().d()
                KeyEvent.VK_SPACE -> println("Naciśnięto klawisz SPACE")
                KeyEvent.VK_LEFT -> println("<-")
                KeyEvent.VK_RIGHT -> println("->")
                //else -> println("Naciśnięto klawisz: ${event.keyCode}")
            }
        }
    })

    val RuchMyszy = object : JPanel(), MouseMotionListener {
        init {
            addMouseMotionListener(this)
        }

        override fun mouseDragged(e: MouseEvent) {
            //println("Mysz przeciągnięta: ${e.x}, ${e.y}")
        }

        override fun mouseMoved(e: MouseEvent) {
            //println("Mysz poruszona: ${e.x}")
            if (e.x < 37) {
                directionn = Point(0, -10)
            }
            if ((e.x > 37) and (e.x < 74)) {
                directionn = Point(10, -10)
            }
            if ((e.x > 74) and (e.x < 111)) {
                directionn = Point(10, 0)
            }
            if ((e.x > 111) and (e.x < 148)) {
                directionn = Point(10, 10)
            }
            if ((e.x > 148) and (e.x < 185)) {
                directionn = Point(0, 10)
            }
            if ((e.x > 185) and (e.x < 222)) {
                directionn = Point(-10, 10)
            }
            if ((e.x > 222) and (e.x < 259)) {
                directionn = Point(-10, 0)
            }
            if (e.x > 259) {
                directionn = Point(-10, -10)
            }
        }
    } //1366, 768         683, 384
    RuchMyszy.setSize(300, 300)
    RuchMyszy.setBounds(533, 234, 300, 300)
    frame.add(RuchMyszy)

    val Plposmap = PlayerPositionMap()
    Plposmap.setSize(500, 500)
    Plposmap.setBounds(0, 0, 500, 500)

    layeredPane.add(Plposmap, JLayeredPane.POPUP_LAYER)
    SwingUtilities.invokeLater {
        frame.contentPane.add(RayPanel())
        frame.isVisible = true
        frame.pack()
    }
} */
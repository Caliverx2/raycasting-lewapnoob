package org.example.MainKt
/*
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
import java.awt.geom.Line2D
import javax.swing.JLayeredPane
import kotlin.concurrent.fixedRateTimer
import kotlin.math.*

var positionX = 100
var positionY = 100
var currentangle = 0
var angle = -45.0
var distance = 3.0
val valuess = Array(180) { it + 1.0 }
var Returnss = 0
var resetRotate = false

class Player {
    fun w() {
        val anglex = (positionX + 5 * cos(Math.toRadians(currentangle.toDouble()))).toInt()
        val angley = (positionY + 5 * sin(Math.toRadians(currentangle.toDouble()))).toInt()
        positionX = anglex
        positionY = angley
    }
    fun s() {
        val anglex = (positionX + (-5) * cos(Math.toRadians(currentangle.toDouble()))).toInt()
        val angley = (positionY + (-5) * sin(Math.toRadians(currentangle.toDouble()))).toInt()
        positionX = anglex
        positionY = angley
    }
    fun a() {
        val anglex = (positionX + (5) * cos(Math.toRadians(currentangle-90.toDouble()))).toInt()
        val angley = (positionY + (5) * sin(Math.toRadians(currentangle-90.toDouble()))).toInt()
        positionX = anglex
        positionY = angley
    }
    fun d() {
        val anglex = (positionX + (5) * cos(Math.toRadians(currentangle+90.toDouble()))).toInt()
        val angley = (positionY + (5) * sin(Math.toRadians(currentangle+90.toDouble()))).toInt()
        positionX = anglex
        positionY = angley
    }

    fun anglea() {
        //currentangle += 9
        resetRotate = true
        currentangle -= 9
        angle = currentangle - 45.0
        Returnss = 0
    }

    fun angled() {
        resetRotate = true
        //currentangle += 9
        currentangle += 9
        angle = currentangle - 45.0
        Returnss = 0
    }
}

class Tlo : JPanel() {
    init {
        isOpaque = true
    }
    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.darkGray
        val x = 0
        val y = 0
        v.fillRect(x, y, 1368*2, 768*2)   //57, 32
    }
}

class RenderCast : JPanel() {
    private val timer = fixedRateTimer(name = "ray-rend", initialDelay = 500, period = 1) {Render()}

    private var angleUI = 0.0
    private val lines = mutableListOf<Line2D.Double>()
    private var x = 0
    private var y = 0
    private var xpos = 1.0
    private var ypos = 1.0
    private var repatt = 1
    private var angle2 = 45.0
    private var returns = 179

    init {
        for (i in 0..7 step 1) {
            if (Map().id1[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0, 30.0 + (i*30), 0.0)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0, 30.0 + (i*30), 30.0)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0, 0.0 + (i*30), 30.0)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0, 30.0 + (i*30), 30.0)) //Prawa
            }
        }
        for (i in 0..7 step 1) {
            if (Map().id2[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 30, 30.0 + (i*30), 0.0 + 30)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 30, 30.0 + (i*30), 30.0 + 30)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 30, 0.0 + (i*30), 30.0 + 30)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 30, 30.0 + (i*30), 30.0 + 30)) //Prawa
            }
        }
        for (i in 0..7 step 1) {
            if (Map().id3[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 60, 30.0 + (i*30), 0.0 + 60)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 60, 30.0 + (i*30), 30.0 + 60)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 60, 0.0 + (i*30), 30.0 + 60)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 60, 30.0 + (i*30), 30.0 + 60)) //Prawa
            }
        }
        for (i in 0..7 step 1) {
            if (Map().id4[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 90, 30.0 + (i*30), 0.0 + 90)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 90, 30.0 + (i*30), 30.0 + 90)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 90, 0.0 + (i*30), 30.0 + 90)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 90, 30.0 + (i*30), 30.0 + 90)) //Prawa
            }
        }
        for (i in 0..7 step 1) {
            if (Map().id5[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 120, 30.0 + (i*30), 0.0 + 120)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 120, 30.0 + (i*30), 30.0 + 120)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 120, 0.0 + (i*30), 30.0 + 120)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 120, 30.0 + (i*30), 30.0 + 120)) //Prawa
            }
        }
    }

    //RayCast
    private fun Render() {
        angleUI = Math.toRadians(angle2)
        x = positionX
        y = positionY

        xpos = (x + distance * cos(angleUI))
        ypos = (y + distance * sin(angleUI))

        repeat(3) {
            repeat(repatt) {
                for (line in lines) {
                    if (line.intersects(xpos, ypos, 1.9, 1.9)) {
                        if ((currentangle > 360.1) or (currentangle < -360.1)) {
                            currentangle = 0
                        }
                        if ( (angle2 > (currentangle + 45.0)) or (angle2 < (currentangle - 45.0)) ) {
                            angle2 = currentangle + 45.0
                            returns = 179
                            resetRotate = true
                        }

                        valuess[returns] = sqrt(((xpos - positionX)).pow(2) + ((ypos - positionY)).pow(2))
                        if ((returns == 0)) {
                            returns = 180
                        }
                        angle2 -= 0.5
                        distance = 0.0
                        returns -= 1
                    }
                }
                if (distance > 300.0) {
                    distance = 0.0
                    angle2 -= 0.5
                }
                distance += 0.25
            }
        }
    }

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        val g = v as Graphics2D

        v.color = Color.green
        for(it in 1..180) {
            if ((resetRotate) and (it != 180)) {
                continue
            }

            if ((resetRotate) and (it == 180)) {
                returns = 179
                angle2 = currentangle + 45.0
                resetRotate = false
                continue
            }

            if (!resetRotate) {
                fun map(value: Double, fromLow: Double, fromHigh: Double, toLow: Double, toHigh: Double): Double {
                    return (value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow) + toLow
                }
                val mappedValue = log(map(valuess[it-1], 0.0, 93.0, 250.0, 75.0), 15.0)

                v.color = Color(0, 0, (mappedValue*50).toInt() )
                v.fillRect(50 + ((it-1) * 6), 384, 10, (5000 / (valuess[it-1])).toInt())
                v.fillRect(50 + ((it-1) * 6), 384, 10, (5000 / (-(valuess[it-1]))).toInt())
            }
        }
        g.stroke = BasicStroke(2f)
        g.color = Color.RED
        g.drawLine(x, y, xpos.toInt(), ypos.toInt())
        //for (i in 1 .. 90) {
        //  if (i < 90) {
        //      v.drawLine(positionX, positionY, (positionX + 50 * cos(Math.toRadians((angle*i)))).toInt(), (positionY + 50 * sin(Math.toRadians(angle))).toInt())
        //  }
        //}
    }
}

class PlayerOnScreen : JPanel() {
    private val lines = mutableListOf<Line2D.Double>()
    private val timer2 = fixedRateTimer(name = "ray-calc", initialDelay = 500, period = 1) {pozycjagracza()}

    private var x = 0 //positionX
    private var y = 0 //positionY
    private var xposs = 1 //CurrentRayPositionX
    private var yposs = 1 //CurrentRayPositionY
    init {
        isOpaque = false
        for (i in 0..7 step 1) {
            if (Map().id1[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0, 30.0 + (i*30), 0.0)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0, 30.0 + (i*30), 30.0)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0, 0.0 + (i*30), 30.0)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0, 30.0 + (i*30), 30.0)) //Prawa
            }
        }
        for (i in 0..7 step 1) {
            if (Map().id2[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 30, 30.0 + (i*30), 0.0 + 30)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 30, 30.0 + (i*30), 30.0 + 30)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 30, 0.0 + (i*30), 30.0 + 30)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 30, 30.0 + (i*30), 30.0 + 30)) //Prawa
            }
        }
        for (i in 0..7 step 1) {
            if (Map().id3[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 60, 30.0 + (i*30), 0.0 + 60)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 60, 30.0 + (i*30), 30.0 + 60)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 60, 0.0 + (i*30), 30.0 + 60)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 60, 30.0 + (i*30), 30.0 + 60)) //Prawa
            }
        }
        for (i in 0..7 step 1) {
            if (Map().id4[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 90, 30.0 + (i*30), 0.0 + 90)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 90, 30.0 + (i*30), 30.0 + 90)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 90, 0.0 + (i*30), 30.0 + 90)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 90, 30.0 + (i*30), 30.0 + 90)) //Prawa
            }
        }
        for (i in 0..7 step 1) {
            if (Map().id5[i] == 1) {
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 120, 30.0 + (i*30), 0.0 + 120)) //Góra
                lines.add(Line2D.Double(0.0 + (i*30) , 30.0 + 120, 30.0 + (i*30), 30.0 + 120)) //Dół
                lines.add(Line2D.Double(0.0 + (i*30), 0.0 + 120, 0.0 + (i*30), 30.0 + 120)) //Lewa
                lines.add(Line2D.Double(30.0 + (i*30), 0.0 + 120, 30.0 + (i*30), 30.0 + 120)) //Prawa
            }
        }
    }

    private fun pozycjagracza() {
        x = positionX
        y = positionY

        //rayAngle = Math.toRadians(angle)   obliczanie kąta promienia w radianach
        //xPozycjaPromienia = (x + distance * cos(rayAngle)).toInt()  wzór: promień wokół fov gracza
        //yPozycjaPromienia = (y + distance * sin(rayAngle)).toInt()

        val angleru = Math.toRadians(currentangle.toDouble())
        xposs = (x + 30 * cos(angleru)).toInt()
        yposs = (y + 30 * sin(angleru)).toInt()
        repaint()
    }
    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.green
        v.fillRect(x-4, y-4, 8, 8)

        val g2 = v as Graphics2D
        g2.color = Color.lightGray
        g2.stroke = BasicStroke(2f)
        g2.drawLine(x, y, xposs, yposs)

        g2.color = Color.cyan
        g2.stroke = BasicStroke(2f)
        g2.drawLine(x, y, (x + 10 * cos(Math.toRadians(currentangle+45.toDouble()))).toInt(), (y + 10 * sin(Math.toRadians(currentangle+45.toDouble()))).toInt())

        g2.color = Color.cyan
        g2.stroke = BasicStroke(2f)
        g2.drawLine(x, y, (x + 10 * cos(Math.toRadians(currentangle-45.toDouble()))).toInt(), (y + 10 * sin(Math.toRadians(currentangle-45.toDouble()))).toInt())
    }
}


fun main() {
    //podstawa wyświetlania
    val frame = JFrame("rolada z gówna")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.isAlwaysOnTop = true
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


    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            when (event.keyCode) {
                KeyEvent.VK_W -> Player().w()
                KeyEvent.VK_S -> Player().s()
                KeyEvent.VK_A -> Player().a()
                KeyEvent.VK_D -> Player().d()
                KeyEvent.VK_SPACE -> println("kosmos bratku")
                KeyEvent.VK_LEFT -> Player().anglea()//println("<-")
                KeyEvent.VK_RIGHT -> Player().angled()//println("->")

                //KeyEvent.VK_U -> Player().DistanceToHigh()
                //KeyEvent.VK_B -> Player().DistanceToLow()

                //else -> println("Naciśnięto klawisz: ${event.keyCode}")
            }
        }
    })
    println("miau")
}

class Map {
    val id1: List<Int> = listOf(1, 1, 1, 1, 1, 1, 1, 1)
    val id2: List<Int> = listOf(1, 0, 1, 0, 0, 0, 0, 1)
    val id3: List<Int> = listOf(1, 0, 0, 0, 0, 0, 0, 1)
    val id4: List<Int> = listOf(1, 0, 1, 0, 0, 0, 0, 1)
    val id5: List<Int> = listOf(1, 1, 1, 1, 1, 1, 1 ,1)
}

class Mappingmap : JPanel() {
    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.gray
        for (i in 0..7 step 1) {
            val y = 0
            val x = 0
            if (Map().id1[i] == 1) {
                v.fillRect(x + (i * 30), y, 30, 30)
            }
        }
        for (i in 0..7 step 1) {
            val y = 30
            val x = 0
            if (Map().id2[i] == 1) {
                v.fillRect(x + (i * 30), y, 30, 30)
            }
        }
        for (i in 0..7 step 1) {
            val y = 60
            val x = 0
            if (Map().id3[i] == 1) {
                v.fillRect(x + (i * 30), y, 30, 30)
            }
        }
        for (i in 0..7 step 1) {
            val y = 90
            val x = 0
            if (Map().id4[i] == 1) {
                v.fillRect(x + (i * 30), y, 30, 30)
            }
        }
        for (i in 0..7 step 1) {
            val y = 120
            val x = 0
            if (Map().id5[i] == 1) {
                v.fillRect(x + (i * 30), y, 30, 30)
            }
        }
        isOpaque = false
    }
} */
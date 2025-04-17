package org.example.MainKt

//./gradlew shadowJar
/*
import com.sun.jdi.IntegerValue
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
import kotlin.math.pow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

var actualDistance = 1.0
var positionX = 100
var positionY = 100
var currentangle = 0
var angle = -45.0
var distance = 3.0
val valuess = Array(180) { it + 1.0 }
var Returnss = 0
var resetRotate = false

class Ekran : JPanel(){
    init {
        isOpaque = true
    }

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.black

        v.fillRect(x, 192, 1, 384)   //57, 32
    }
}

class Player {
    fun w() {
        positionY -= 2
    }
    fun s() {
        positionY += 2
    }
    fun a() {
        positionX -= 2
    }
    fun d() {
        positionX += 2
    }

    fun anglea() {
        //currentangle += 9
        resetRotate = true
        currentangle -= 9
        angle = currentangle - 45.0
        Returnss = 0
        println(currentangle)
    }

    fun angled() {
        resetRotate = true
        //currentangle += 9
        currentangle += 9
        angle = currentangle - 45.0
        Returnss = 0
        println(currentangle)
    }

    fun DistanceToHigh() {
        distance += 3.0
    }

    fun DistanceToLow() {
        distance -= 3.0
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
    private val timer = fixedRateTimer(name = "ray-rend", initialDelay = 500, period = 2) {Render()}

    private var angleUI = 0.0
    private val lines = mutableListOf<Line2D.Double>()
    private var x = 0
    private var y = 0
    private var xpos = 1
    private var ypos = 1
    private var xposs = 1
    private var yposs = 1
    private var xposRAY = 1
    private var yposRAY = 1
    private var repatt = 1
    private var angle2 = 45.0
    private var Returnss2 = 179



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

    fun Render() {
        angleUI = Math.toRadians(angle2)
        x = positionX
        y = positionY

        xpos = (x + distance * cos(angleUI)).toInt()
        ypos = (y + distance * sin(angleUI)).toInt()

        repeat(1) {
            repeat(repatt) {
                if ((currentangle >= 360) or (currentangle <= -360)) {
                    currentangle = 0
                }
                for (line in lines) {
                    if (line.intersects(xpos.toDouble(), ypos.toDouble(), 0.5, 0.5)) {
                        if ((angle2 > currentangle + 45) or (angle2 < currentangle - 45)) {
                            angle2 = currentangle + 45.0
                        }
                        if (Returnss2 == 0) {
                            Returnss2 = 179
                        }

                        angle2 -= 0.5
                        distance = 0.0
                        xposRAY = xpos
                        yposRAY = ypos
                        valuess[Returnss2] = sqrt(((xposRAY - positionX).toDouble()).pow(2) + ((yposRAY - positionY).toDouble()).pow(2))
                        Returnss2 -= 1
                    }
                }
                if (distance > 230.0) {
                    distance = 0.0
                }
                distance += 0.25
            }
        }
    }

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        val g = v as Graphics2D

        v.color = Color.green
        for (i in 0..179) {
            if ((resetRotate == true) and (i != 179)) {
                continue
            }

            if ((resetRotate == true) and (i == 179)) {
                Returnss2 = 179
                angle2 = currentangle + 45.0
                valuess.fill(200.0)
                resetRotate = false
            }

            if (resetRotate == false) {
                fun map(value: Double, fromLow: Double, fromHigh: Double, toLow: Double, toHigh: Double): Double {
                    return (value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow) + toLow
                }
                val mappedValue = map(valuess[i], 0.0, 100.0, 250.0, 500.0)

                v.color = Color(0, 0, (50000/mappedValue).toInt() )
                v.fillRect(50 + (i * 7), 384, 7, (5000 / valuess[i]).toInt())
                v.fillRect(50 + (i * 7), 384, 7, (5000 / -valuess[i]).toInt())
            }
        }
        g.stroke = BasicStroke(2f)
        g.color = Color.RED
        g.drawLine(x, y, xpos, ypos)
        //for (i in 1 .. 90) {
        //  if (i < 90) {
        //      v.drawLine(positionX, positionY, (positionX + 50 * cos(Math.toRadians((angle*i)))).toInt(), (positionY + 50 * sin(Math.toRadians(angle))).toInt())
        //  }
        //}
    }
}


class PlayerOnScreen : JPanel() {
    private var angleUI = 0.0

    private val lines = mutableListOf<Line2D.Double>()
    //private val timer = fixedRateTimer(name = "ray-send", initialDelay = 500, period = 1) {reception()}
    private val timer2 = fixedRateTimer(name = "ray-calc", initialDelay = 500, period = 1) {pozycjagracza()}

    private var x = 0
    private var y = 0
    private var xpos = 1
    private var ypos = 1
    private var xposs = 1
    private var yposs = 1
    private var xposRAY = 1
    private var yposRAY = 1
    private var repatt = 0
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

    fun pozycjagracza() {
        x = positionX
        y = positionY
        angleUI = Math.toRadians(angle)
        var angleru= Math.toRadians(currentangle.toDouble())

        xpos = (x + distance * cos(angleUI)).toInt()
        ypos = (y + distance * sin(angleUI)).toInt()

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

        v.color = Color.BLUE
        v.fillOval(xposRAY-4, yposRAY-4, 8, 8)
    }
}


fun main() {
    //podstawa wyświetlania
    val frame = JFrame("rolada z gówna")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.iconImage = Toolkit.getDefaultToolkit().getImage("src/main/resources/icon/icon.jpg" )
    frame.isResizable = false
    frame.setSize(1366, 768)
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

    val Ekran = Tlo()
    Ekran.hasFocus()
    Ekran.isOpaque = true
    Ekran.layout = null
    Ekran.setSize(1366, 768)
    Ekran.setBounds(0, 0, 1366, 768)

    val RenderScreen = Ekran()
    RenderScreen.hasFocus()
    RenderScreen.isOpaque = false
    RenderScreen.layout = null
    RenderScreen.setSize(1366, 768)
    RenderScreen.setBounds(0, 0, 1366, 768)

    val PlayerOnScreen = PlayerOnScreen()
    PlayerOnScreen.hasFocus()
    PlayerOnScreen.isOpaque = false
    PlayerOnScreen.setSize(1366, 768)
    PlayerOnScreen.setBounds(0, 0, 1366, 768)

    val RenderCast = RenderCast()
    //RenderCast.hasFocus()
    RenderCast.isOpaque = false
    RenderCast.setSize(1366, 768)
    RenderCast.setBounds(0, 0, 1366, 768)


    //frame.add(RenderCast)
    //frame.add(RenderScreen)
    frame.add(Ekran)
    layeredPane.add(RenderScreen, 2)
    layeredPane.add(mapa, 3)
    layeredPane.add(PlayerOnScreen, 4)   //JLayeredPane.PALETTE_LAYER
    layeredPane.add(RenderCast, 6)



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
    val id2: List<Int> = listOf(1, 0, 0, 0, 0, 0, 0, 1)
    val id3: List<Int> = listOf(1, 0, 1, 0, 0, 0, 0, 1)
    val id4: List<Int> = listOf(1, 0, 0, 0, 0, 0, 0, 1)
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

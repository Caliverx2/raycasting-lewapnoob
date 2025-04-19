package org.example.MainKt

import java.awt.BasicStroke
import java.awt.Color
import javax.swing.JFrame
import javax.swing.JPanel
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.Timer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.coroutines.*
import kotlinx.coroutines.*
import java.awt.MouseInfo

class CubePanel : JPanel(), ActionListener {
    private var angleX = 0.0
    private var angleY = 0.0
    private val timer = Timer(16, this)

    init {
        timer.start()
        background = Color.BLACK
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.translate(width / 2, height / 2)
        g2d.scale(1.0, 1.0)
        g2d.color = Color.WHITE
        drawCube(g2d)
    }

    private fun drawCube(g2d: Graphics2D) {
        val vertices = arrayOf(
            doubleArrayOf(-1.0, -1.0, -1.0),
            doubleArrayOf(1.0, -1.0, -1.0),
            doubleArrayOf(1.0, 1.0, -1.0),
            doubleArrayOf(-1.0, 1.0, -1.0),
            doubleArrayOf(-1.0, -1.0, 1.0),
            doubleArrayOf(1.0, -1.0, 1.0),
            doubleArrayOf(1.0, 1.0, 1.0),
            doubleArrayOf(-1.0, 1.0, 1.0)
        )

        val transformed = vertices.map { vertex ->
            val x = vertex[0] * cos(angleY) - vertex[2] * sin(angleY)
            val z = vertex[0] * sin(angleY) + vertex[2] * cos(angleY)
            val y = vertex[1] * cos(angleX) - z * sin(angleX)
            doubleArrayOf(x, y, z * cos(angleX) + vertex[1] * sin(angleX))
        }

        val edges = arrayOf(
            intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 0),
            intArrayOf(4, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 4),
            intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7)
        )

        for (edge in edges) {
            val start = transformed[edge[0]]
            val end = transformed[edge[1]]
            g2d.stroke = BasicStroke(2f)
            g2d.drawLine((start[0] * 100).toInt(), (start[1] * 100).toInt(), (end[0] * 100).toInt(), (end[1] * 100).toInt())
        }
    }

    override fun actionPerformed(e: ActionEvent) {
        val robot = Robot()
        if (angleX.toInt() != 1) {
            angleX += 0.01
        }
        if (angleX.toInt() == 1) {
            angleY += 0.01
            //robot.mouseMove(500, 500)
            //robot.mouseMove(501, 501)

            //robot.keyPress(KeyEvent.VK_CONTROL)
            //robot.keyPress(KeyEvent.VK_S)
            //robot.keyRelease(KeyEvent.VK_S)
            //robot.keyRelease(KeyEvent.VK_CONTROL)
        }
        repaint()
    }
}

fun main() = runBlocking{
    val robot = Robot()
    val frame = JFrame("Obracający się sześcian")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.add(CubePanel())
    frame.isAlwaysOnTop = true
    frame.setSize(600, 600)
    frame.setLocation(((Toolkit.getDefaultToolkit().screenSize.width - frame.width)/2), ((Toolkit.getDefaultToolkit().screenSize.height - frame.height)/2))
    frame.isVisible = true
    frame.isFocused



    delay(1000)
    robot.keyPress(KeyEvent.VK_WINDOWS)
    robot.keyRelease(KeyEvent.VK_WINDOWS)
    delay(500)
    robot.keyPress(KeyEvent.VK_D)
    robot.keyRelease(KeyEvent.VK_D)
    robot.keyPress(KeyEvent.VK_I)
    robot.keyRelease(KeyEvent.VK_I)
    robot.keyPress(KeyEvent.VK_S)
    robot.keyRelease(KeyEvent.VK_S)
    robot.keyPress(KeyEvent.VK_C)
    robot.keyRelease(KeyEvent.VK_C)
    robot.keyPress(KeyEvent.VK_O)
    robot.keyRelease(KeyEvent.VK_O)
    robot.keyPress(KeyEvent.VK_R)
    robot.keyRelease(KeyEvent.VK_R)
    robot.keyPress(KeyEvent.VK_D)
    robot.keyRelease(KeyEvent.VK_D)
    delay(500)
    robot.keyPress(KeyEvent.VK_ENTER)
    robot.keyRelease(KeyEvent.VK_ENTER)
}
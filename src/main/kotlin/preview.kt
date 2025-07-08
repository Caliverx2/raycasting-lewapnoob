import javax.swing.*
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.*

val MAP = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1),
    intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1),
    intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1),
    intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
)
const val MAP_WIDTH = 12
const val MAP_HEIGHT = 12

data class Player(var x: Double, var y: Double, var dir: Double)
var player = Player(x = 2.5, y = 2.5, dir = 0.0)

const val FOV = PI / 2.0
const val NUM_RAYS = 80
const val MAX_DEPTH = 15.0

val keysPressed = mutableSetOf<Int>()

class RaycasterPanel : JPanel() {

    private lateinit var wallTexture: BufferedImage

    init {
        preferredSize = Dimension(800, 600)
        isFocusable = true
        background = Color.BLACK

        try {
            wallTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/black_bricks.png"))
            println("Tekstura załadowana pomyślnie: ${wallTexture.width}x${wallTexture.height}")
        } catch (e: Exception) {
            System.err.println("Błąd ładowania tekstury: ${e.message}")
            e.printStackTrace()
            wallTexture = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB).apply {
                val g = createGraphics()
                g.color = Color.GRAY
                g.fillRect(0, 0, 64, 64)
                g.dispose()
            }
        }

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                e?.keyCode?.let { keysPressed.add(it) }
            }

            override fun keyReleased(e: KeyEvent?) {
                e?.keyCode?.let { keysPressed.remove(it) }
            }
        })

        Timer(16) {
            inputUpdate()
            repaint()
        }.start()
    }

    private fun castRay(angle: Double): RayHitInfo {
        val dx = cos(angle)
        val dy = sin(angle)
        var dist = 0.0
        val step = 0.05

        while (dist < MAX_DEPTH) {
            val x = player.x + dx * dist
            val y = player.y + dy * dist

            val cx = floor(x).toInt()
            val cy = floor(y).toInt()

            if (cx < 0 || cx >= MAP_WIDTH || cy < 0 || cy >= MAP_HEIGHT) {
                break
            }
            if (MAP[cy][cx] == 1) {
                val hitXExact = x
                val hitYExact = y

                var hitWallSide: WallSide = WallSide.NONE
                if (abs(hitXExact - cx.toDouble()) < 0.05 || abs(hitXExact - (cx + 1).toDouble()) < 0.05) {
                    hitWallSide = WallSide.VERTICAL
                } else if (abs(hitYExact - cy.toDouble()) < 0.05 || abs(hitYExact - (cy + 1).toDouble()) < 0.05) {
                    hitWallSide = WallSide.HORIZONTAL
                }

                return RayHitInfo(dist, hitXExact, hitYExact, hitWallSide)
            }
            dist += step
        }
        return RayHitInfo(MAX_DEPTH, 0.0, 0.0, WallSide.NONE)
    }

    data class RayHitInfo(val distance: Double, val hitX: Double, val hitY: Double, val hitSide: WallSide)

    enum class WallSide {
        HORIZONTAL, VERTICAL, NONE
    }

    private fun inputUpdate() {
        val rotationSpeed = Math.toRadians(5.0)
        val moveSpeed = 0.1

        val targetX = player.x
        val targetY = player.y

        if (keysPressed.contains(KeyEvent.VK_LEFT) || keysPressed.contains(KeyEvent.VK_A)) {
            player.dir -= rotationSpeed
        }
        if (keysPressed.contains(KeyEvent.VK_RIGHT) || keysPressed.contains(KeyEvent.VK_D)) {
            player.dir += rotationSpeed
        }

        var moveDx = 0.0
        var moveDy = 0.0

        if (keysPressed.contains(KeyEvent.VK_UP) || keysPressed.contains(KeyEvent.VK_W)) {
            moveDx = cos(player.dir) * moveSpeed
            moveDy = sin(player.dir) * moveSpeed
        }
        if (keysPressed.contains(KeyEvent.VK_DOWN) || keysPressed.contains(KeyEvent.VK_S)) {
            moveDx = -cos(player.dir) * moveSpeed
            moveDy = -sin(player.dir) * moveSpeed
        }

        val newX = player.x + moveDx
        val newY = player.y + moveDy

        val testPlayerX = floor(newX).toInt()
        val testPlayerY = floor(newY).toInt()

        if (testPlayerX >= 0 && testPlayerX < MAP_WIDTH && testPlayerY >= 0 && testPlayerY < MAP_HEIGHT) {
            if (MAP[player.y.toInt()][testPlayerX] == 0) {
                player.x = newX
            } else {
                val currentMapX = player.x.toInt()
                if (testPlayerX != currentMapX && MAP[player.y.toInt()][currentMapX] == 0) {
                }
            }

            if (MAP[testPlayerY][player.x.toInt()] == 0) {
                player.y = newY
            } else {
                val currentMapY = player.y.toInt()
                if (testPlayerY != currentMapY && MAP[currentMapY][player.x.toInt()] == 0) {
                }
            }
        }
    }


    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val graphics2D = g as Graphics2D
        val sw = width.toDouble()
        val sh = height.toDouble()

        val columnWidth = sw / NUM_RAYS

        for (i in 0 until NUM_RAYS) {
            val rayAngle = player.dir - (FOV / 2) + (i.toDouble() / (NUM_RAYS - 1)) * FOV
            val hitInfo = castRay(rayAngle)
            var dist = hitInfo.distance

            val angleDifference = rayAngle - player.dir
            dist *= cos(angleDifference)

            val brightness = (255 - dist * 10).coerceIn(0.0, 255.0)

            val colHeight = (sh / (dist + 0.1)).toInt()
            val y = ((sh - colHeight) / 2).toInt()

            val xStart = (i * columnWidth).toInt()
            val currentColumnWidth = (columnWidth).toInt()

            if (dist < MAX_DEPTH) {
                var wallX: Double

                if (hitInfo.hitSide == WallSide.VERTICAL) {
                    wallX = hitInfo.hitY - floor(hitInfo.hitY)
                } else {
                    wallX = hitInfo.hitX - floor(hitInfo.hitX)
                }

                val textureX = (wallX * wallTexture.width).toInt()

                graphics2D.color = Color.BLACK

                for (pixelY in 0 until colHeight) {
                    val textureY = ((pixelY / colHeight.toDouble()) * wallTexture.height).toInt()
                    val color = Color(wallTexture.getRGB(textureX, textureY))

                    val r = (color.red * (brightness / 255.0)).toInt().coerceIn(0, 255)
                    val g = (color.green * (brightness / 255.0)).toInt().coerceIn(0, 255)
                    val b = (color.blue * (brightness / 255.0)).toInt().coerceIn(0, 255)

                    graphics2D.color = Color(r, g, b)
                    graphics2D.drawLine(xStart, y + pixelY, xStart + currentColumnWidth -1 , y + pixelY)
                }
            } else {
                graphics2D.color = Color.BLACK
                graphics2D.fillRect(xStart, 0, currentColumnWidth, sh.toInt())
            }
        }
    }
}

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("test raycast Swing")
        val panel = RaycasterPanel()

        frame.add(panel)
        frame.pack()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        panel.requestFocusInWindow()
    }
}
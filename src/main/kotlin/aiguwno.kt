import java.awt.*
import java.awt.event.*
import javax.swing.*
import kotlin.math.*

const val TILE = 32
const val FOV = Math.PI / 3
const val NUM_RAYS = 120
const val MAX_DIST = 1000.0

class GameMap {
    val grid: Array<IntArray> = arrayOf(
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

    fun isWall(x: Int, y: Int): Boolean {
        return grid.getOrNull(y)?.getOrNull(x)?.let { it == 1 } ?: true
    }

    fun isInside(x: Int, y: Int): Boolean = x in 0 until grid[0].size && y in 0 until grid.size
}

data class Player(var x: Double, var y: Double, var angle: Double)

class RaycastingApp : JPanel(), KeyListener {
    private val map = GameMap()
    private val player: Player

    init {
        // Znajdź punkt startowy (5)
        var startX = 1
        var startY = 1
        loop@ for (y in map.grid.indices) {
            for (x in map.grid[y].indices) {
                if (map.grid[y][x] == 5) {
                    startX = x
                    startY = y
                    break@loop
                }
            }
        }
        player = Player(startX * TILE + TILE / 2.0, startY * TILE + TILE / 2.0, 0.0)

        preferredSize = Dimension(800, 600)
        isFocusable = true
        addKeyListener(this)
        Timer(16) { repaint() }.start()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.color = Color.BLACK
        g.fillRect(0, 0, width, height)

        drawMap(g as Graphics2D)
        castRays(g)
        drawPlayer(g)
    }

    private fun drawMap(g: Graphics2D) {
        for (y in map.grid.indices) {
            for (x in map.grid[y].indices) {
                when (val v = map.grid[y][x]) {
                    1 -> g.color = Color.DARK_GRAY
                    5 -> g.color = Color.BLUE
                    else -> g.color = Color.LIGHT_GRAY
                }
                g.fillRect(x * TILE, y * TILE, TILE, TILE)
                g.color = Color.BLACK
                g.drawRect(x * TILE, y * TILE, TILE, TILE)
            }
        }
    }

    private fun drawPlayer(g: Graphics2D) {
        g.color = Color.RED
        g.fillOval((player.x - 4).toInt(), (player.y - 4).toInt(), 8, 8)
        g.drawLine(
            player.x.toInt(), player.y.toInt(),
            (player.x + cos(player.angle) * 20).toInt(),
            (player.y + sin(player.angle) * 20).toInt()
        )
    }

    private fun castRays(g: Graphics2D) {
        val startAngle = player.angle - FOV / 2
        val step = FOV / NUM_RAYS

        for (i in 0 until NUM_RAYS) {
            val angle = startAngle + i * step
            var distance = 0.0
            var hit = false
            var x = player.x
            var y = player.y

            val dx = cos(angle)
            val dy = sin(angle)

            while (!hit && distance < MAX_DIST) {
                x += dx
                y += dy
                distance += 1.0
                val mapX = (x / TILE).toInt()
                val mapY = (y / TILE).toInt()
                if (map.isWall(mapX, mapY)) hit = true
            }

            val shade = 255 - min(255, (distance * 0.5).toInt())
            g.color = Color(0, 0, shade)
            val lineHeight = (5000 / (distance + 1)).toInt()
            val drawX = 400 + i * 3 / 2
            val drawY = height / 2 - lineHeight / 2
            g.fillRect(drawX, drawY, 2, lineHeight)
        }
    }

    override fun keyPressed(e: KeyEvent) {
        val speed = 5.0
        when (e.keyCode) {
            KeyEvent.VK_LEFT -> player.angle -= 0.1
            KeyEvent.VK_RIGHT -> player.angle += 0.1
            KeyEvent.VK_UP -> {
                val nx = player.x + cos(player.angle) * speed
                val ny = player.y + sin(player.angle) * speed
                if (!map.isWall((nx / TILE).toInt(), (ny / TILE).toInt())) {
                    player.x = nx
                    player.y = ny
                }
            }
            KeyEvent.VK_DOWN -> {
                val nx = player.x - cos(player.angle) * speed
                val ny = player.y - sin(player.angle) * speed
                if (!map.isWall((nx / TILE).toInt(), (ny / TILE).toInt())) {
                    player.x = nx
                    player.y = ny
                }
            }
        }
        repaint()
    }

    override fun keyReleased(e: KeyEvent) {}
    override fun keyTyped(e: KeyEvent) {}
}

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Raycasting 2.5D Viewer")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.contentPane.add(RaycastingApp())
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}

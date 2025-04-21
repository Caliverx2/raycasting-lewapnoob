package org.example.MainKt
/*
//./gradlew shadowJar

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.awt.BasicStroke
import javax.swing.JFrame
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JPanel
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.Robot
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JLayeredPane
import kotlin.concurrent.fixedRateTimer
import kotlin.math.*


var map = true
var positionX = 100.0
var positionY = 100.0
var currentangle = 0
var shotx = 0.0
var shoty = 0.0

class Player(private val renderCast: RenderCast, private val playerOnScreen: PlayerOnScreen) {
    private val map = Map()
    private val tileSize = 30
    private val playerSize = 2
    private val margin = 2
    private var movementSpeed = 1.5
    private val rotationSpeed = 2 // Prędkość obrotu (w stopniach na klatkę)
    private val sensitivity = 0.03 // Czułość myszy

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
                if (gridY !in map.grid.indices || gridX !in map.grid[gridY].indices || ((map.grid[gridY][gridX] != 0) and (map.grid[gridY][gridX] != 5))) {
                    return false
                }
            }
        }
        return true
    }

    fun w() {
        val anglex = (positionX + movementSpeed * cos(Math.toRadians(currentangle.toDouble())))
        val angley = (positionY + movementSpeed * sin(Math.toRadians(currentangle.toDouble())))
        if (canMoveTo((anglex).toInt(), (angley).toInt())) {
            positionX = anglex
            positionY = angley
        }
    }

    fun s() {
        val anglex = (positionX + (-movementSpeed) * cos(Math.toRadians(currentangle.toDouble())))
        val angley = (positionY + (-movementSpeed) * sin(Math.toRadians(currentangle.toDouble())))
        if (canMoveTo(anglex.toInt(), angley.toInt())) {
            positionX = anglex
            positionY = angley
        }
    }

    fun a() {
        val anglex = (positionX + movementSpeed * cos(Math.toRadians(currentangle - 90.0)))
        val angley = (positionY + movementSpeed * sin(Math.toRadians(currentangle - 90.0)))
        if (canMoveTo(anglex.toInt(), angley.toInt())) {
            positionX = anglex
            positionY = angley
        }
    }

    fun d() {
        val anglex = (positionX + movementSpeed * cos(Math.toRadians(currentangle + 90.0)))
        val angley = (positionY + movementSpeed * sin(Math.toRadians(currentangle + 90.0)))
        if (canMoveTo(anglex.toInt(), angley.toInt())) {
            positionX = anglex
            positionY = angley
        }
    }

    fun anglea() {
        currentangle -= rotationSpeed
    }

    fun angled() {
        currentangle += rotationSpeed
    }

    fun updateAngleFromMouse(mouseX: Int, centerX: Int) {
        if (MouseInfo.getPointerInfo().location.x == 960) {
            currentangle += 0
        } else {
            currentangle += (((MouseInfo.getPointerInfo().location.x) - 960) * sensitivity).toInt()
        }
    }

    // Poprawiona metoda update z jawnym typem Map<Int, Boolean>
    fun update(keysPressed: kotlin.collections.Map<Int, Boolean>) {
        // Ruch
        if (keysPressed.getOrDefault(KeyEvent.VK_W, false) || keysPressed.getOrDefault(KeyEvent.VK_UP, false)) w()
        if (keysPressed.getOrDefault(KeyEvent.VK_S, false) || keysPressed.getOrDefault(KeyEvent.VK_DOWN, false)) s()
        if (keysPressed.getOrDefault(KeyEvent.VK_A, false)) a()
        if (keysPressed.getOrDefault(KeyEvent.VK_D, false)) d()

        // Obrót
        if (keysPressed.getOrDefault(KeyEvent.VK_LEFT, false)) anglea()
        if (keysPressed.getOrDefault(KeyEvent.VK_RIGHT, false)) angled()
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
    private val map = Map()
    private val screenWidth = 1366
    private val screenHeight = 768
    private val fov = 90.0 // Pole widzenia w stopniach
    private val textureSize = 64 // Rozmiar tekstury 64x64
    private val rayCount = screenWidth // Pełna liczba promieni dla lepszej jakości
    private val wallHeight = 32.0 // Stała wysokość ściany w przestrzeni gry
    private val tileSize = 30.0 // Rozmiar kafelka na mapie
    private var textures: Array<BufferedImage> = arrayOf()
    private val buffer: BufferedImage // Bufor do renderowania sceny
    private val bufferGraphics: Graphics // Graphics dla bufora

    // Parametry cieniowania (proste, oparte tylko na odległości)
    private val minBrightness = 0.6
    private val maxBrightness = 1.0
    private val shadeDistanceScale = 5.0

    // Parametry mgły (wykładniczy model)
    private val fogColor = Color(180, 180, 180) // Jaśniejszy szary dla subtelniejszego efektu
    private val fogDensity = 0.04 // Gęstość mgły

    // Bufor dla wartości trygonometrycznych (optymalizacja)
    private val rayCosines = DoubleArray(rayCount)
    private val raySines = DoubleArray(rayCount)
    private val rayAngles = DoubleArray(rayCount) // Bufor dla kątów promieni

    init {
        isOpaque = false
        textures = arrayOf(
            ImageIO.read(File("src/main/resources/bricks.png")),
            createTexture(Color.YELLOW)
        )
        // Inicjalizacja bufora
        buffer = BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB)
        bufferGraphics = buffer.createGraphics()

        // Wstępne obliczenie kątów względnych i wartości trygonometrycznych
        val rayAngleStep = fov / (rayCount - 1)
        for (ray in 0 until rayCount) {
            val relativeAngle = -fov / 2 + ray * rayAngleStep
            rayAngles[ray] = relativeAngle
            val rayAngleRad = Math.toRadians(relativeAngle)
            rayCosines[ray] = cos(rayAngleRad)
            raySines[ray] = sin(rayAngleRad)
        }
    }

    private fun createTexture(color: Color): BufferedImage {
        val texture = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_RGB)
        val g = texture.createGraphics()
        g.color = color
        g.fillRect(0, 0, textureSize, textureSize)
        g.dispose()
        return texture
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        // Renderowanie do bufora
        renderWallsToBuffer()
        // Rysowanie bufora na panelu
        g.drawImage(buffer, 0, 0, null)
    }

    private fun renderWallsToBuffer() {
        val playerAngleRad = Math.toRadians(currentangle.toDouble())

        // Wyczyść bufor (ustaw tło na ciemnoszary)
        bufferGraphics.color = Color(50, 50, 50)
        bufferGraphics.fillRect(0, 0, screenWidth, screenHeight)

        for (ray in 0 until rayCount) {
            // Oblicz kąt promienia z uwzględnieniem aktualnego kąta gracza
            val rayAngle = currentangle + rayAngles[ray]
            val rayAngleRad = Math.toRadians(rayAngle)

            // Kierunek promienia (używamy buforowanych wartości)
            val relativeCos = rayCosines[ray]
            val relativeSin = raySines[ray]

            // Obrót kierunku promienia względem kąta gracza
            val cosPlayerAngle = cos(playerAngleRad)
            val sinPlayerAngle = sin(playerAngleRad)
            val rayDirX = relativeCos * cosPlayerAngle - relativeSin * sinPlayerAngle
            val rayDirY = relativeCos * sinPlayerAngle + relativeSin * cosPlayerAngle

            // Pozycja na mapie
            var mapX = (positionX / tileSize).toInt()
            var mapY = (positionY / tileSize).toInt()

            // DDA (Digital Differential Analyzer)
            val deltaDistX = if (rayDirX == 0.0) 1e30 else abs(1 / rayDirX)
            val deltaDistY = if (rayDirY == 0.0) 1e30 else abs(1 / rayDirY)

            var stepX = 0
            var stepY = 0
            var sideDistX = 0.0
            var sideDistY = 0.0
            var side = 0 // 0 dla X, 1 dla Y

            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (positionX / tileSize - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1.0 - positionX / tileSize) * deltaDistX
            }
            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (positionY / tileSize - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1.0 - positionY / tileSize) * deltaDistY
            }

            var hitWall = false
            var wallType = 0
            var distance = 0.0
            var hitX = 0.0
            var hitY = 0.0

            // Pętla DDA
            while (!hitWall) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                }

                // Sprawdź, czy jesteśmy w granicach mapy
                if (mapY !in map.grid.indices || mapX !in map.grid[0].indices) {
                    break
                }

                if (map.grid[mapY][mapX] == 1 || map.grid[mapY][mapX] == 5) {
                    hitWall = true
                    wallType = map.grid[mapY][mapX]
                }
            }

            if (!hitWall) continue // Brak trafienia, pomijamy promień

            // Oblicz odległość z korekcją rybiego oka
            distance = if (side == 0) {
                (mapX - positionX / tileSize + (1 - stepX) / 2.0) / rayDirX
            } else {
                (mapY - positionY / tileSize + (1 - stepY) / 2.0) / rayDirY
            }
            if (distance < 0.01) distance = 0.01

            val angleDiff = abs(playerAngleRad - rayAngleRad)
            if (angleDiff < PI / 2) {
                distance *= cos(angleDiff)
            } else {
                continue
            }

            // Oblicz pozycję trafienia na podstawie pozycji bloku i proporcji
            if (side == 0) {
                hitX = mapX.toDouble() + (if (stepX > 0) 0.0 else 1.0)
                hitY = positionY / tileSize + (hitX - positionX / tileSize) * (rayDirY / rayDirX)
            } else {
                hitY = mapY.toDouble() + (if (stepY > 0) 0.0 else 1.0)
                hitX = positionX / tileSize + (hitY - positionY / tileSize) * (rayDirX / rayDirY)
            }

            // Mapowanie textureX z wyrównaniem do granic bloku
            var textureX = if (side == 0) {
                val blockY = mapY.toDouble()
                val relativeY = hitY - blockY
                val adjustedRelativeY = relativeY.coerceIn(0.0, 1.0)
                adjustedRelativeY * textureSize
            } else {
                val blockX = mapX.toDouble()
                val relativeX = hitX - blockX
                val adjustedRelativeX = relativeX.coerceIn(0.0, 1.0)
                adjustedRelativeX * textureSize
            }

            // Korekcja orientacji tekstury w zależności od kierunku promienia
            if (side == 0 && rayDirX > 0 || side == 1 && rayDirY < 0) {
                textureX = textureSize - textureX - 1
            }

            // Oblicz wysokość ściany na ekranie
            val lineHeight = ((wallHeight * screenHeight) / (distance * tileSize)).toInt().coerceIn(0, screenHeight * 2)
            val drawStart = (-lineHeight / 2 + screenHeight / 2).toInt().coerceAtLeast(0)
            val drawEnd = (lineHeight / 2 + screenHeight / 2).toInt().coerceAtMost(screenHeight)

            // Proste cieniowanie oparte tylko na odległości
            val shadeFactor = (1.0 - (distance / shadeDistanceScale)).coerceIn(minBrightness, maxBrightness)

            // Wykładniczy model mgły
            val fogFactor = 1.0 - exp(-fogDensity * distance)

            // Renderowanie tekstury do bufora
            val texture = textures[if (wallType == 1) 0 else 1]
            for (y in drawStart until drawEnd) {
                val wallY = (y - screenHeight / 2.0 + lineHeight / 2.0) * wallHeight / lineHeight
                val textureY = (wallY * textureSize / wallHeight).toInt().coerceIn(0, textureSize - 1)

                // Pobieranie koloru z tekstury
                val finalTextureX = textureX.toInt().coerceIn(0, textureSize - 1)
                var color = texture.getRGB(finalTextureX, textureY)

                // Cieniowanie
                val originalColor = Color(color)
                val shadedColor = Color(
                    (originalColor.red * shadeFactor).toInt().coerceIn(0, 255),
                    (originalColor.green * shadeFactor).toInt().coerceIn(0, 255),
                    (originalColor.blue * shadeFactor).toInt().coerceIn(0, 255)
                )

                // Mieszanie z mgłą
                val finalColor = Color(
                    ((1.0 - fogFactor) * shadedColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                    ((1.0 - fogFactor) * shadedColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                    ((1.0 - fogFactor) * shadedColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                )

                // Ustawianie piksela w buforze
                buffer.setRGB(ray, y, finalColor.rgb)
            }
        }
    }

    fun shotgun() {
        val shotAngleRad = Math.toRadians(currentangle.toDouble())
        shotx = positionX + 100 * cos(shotAngleRad)
        shoty = positionY + 100 * sin(shotAngleRad)
        repaint()
    }
}

class PlayerOnScreen : JPanel() {
    private val timer2 = fixedRateTimer(name = "ray-calc", initialDelay = 500, period = 1) {pozycjagracza()}

    private var x = 0.0 //positionX
    private var y = 0.0 //positionY
    private var xposs = 1 //CurrentRayPositionX
    private var yposs = 1 //CurrentRayPositionY

    private fun pozycjagracza() {
        x = positionX
        y = positionY

        val angleru = Math.toRadians(currentangle.toDouble())
        xposs = (x/3 + 10 * cos(angleru)).toInt()
        yposs = (y/3 + 10 * sin(angleru)).toInt()
        repaint()
    }
    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        v.color = Color.green
        v.fillRect(((x-2)/3).toInt(), ((y-2)/3).toInt(), 4, 4)

        val g2 = v as Graphics2D
        g2.color = Color.yellow
        g2.stroke = BasicStroke(2f)
        g2.drawLine(((x)/3).toInt(), ((y)/3).toInt(), (xposs), (yposs))
    }
}

fun main() = runBlocking {
    // Podstawa wyświetlania
    val frame = JFrame("rolada z gówna")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.iconImage = Toolkit.getDefaultToolkit().getImage("src/main/resources/icon/icon.jpg")
    frame.isResizable = false
    frame.setSize(1366, 768)
    frame.setLocation(((Toolkit.getDefaultToolkit().screenSize.width - frame.width) / 2), ((Toolkit.getDefaultToolkit().screenSize.height - frame.height) / 2))

    // Ukrycie kursora
    frame.cursor = frame.toolkit.createCustomCursor(
        java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB),
        java.awt.Point(0, 0),
        "invisible"
    )

    frame.isVisible = true

    // Warstwy
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

    // Inicjalizacja gracza
    val player = Player(renderCast, playerOnScreen)

    // Śledzenie stanu klawiszy z jawnym typem kotlin.collections.MutableMap<Int, Boolean>
    val keysPressed: MutableMap<Int, Boolean> = mutableMapOf()

    // Środek okna
    var centerX = frame.width / 2
    var centerY = frame.height / 2

    // Obsługa myszy (strzał)
    frame.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                renderCast.shotgun()
            } else {
                println("Naciśnięto klawisz: ${event.button}")
            }
        }
    })

    // Obsługa ruchu myszy
    frame.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseMoved(e: MouseEvent) {
            player.updateAngleFromMouse(e.x, centerX)
            renderCast.repaint()
            playerOnScreen.repaint()
        }

        override fun mouseDragged(e: MouseEvent) {
            player.updateAngleFromMouse(e.x, centerX)
            renderCast.repaint()
            playerOnScreen.repaint()
        }
    })

    // Obsługa klawiatury
    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            keysPressed[event.keyCode] = true
            // Specjalne akcje (np. strzał, przełączanie mapy) nadal obsługujemy bezpośrednio
            when (event.keyCode) {
                KeyEvent.VK_SPACE -> renderCast.shotgun()
            }
        }

        override fun keyReleased(event: KeyEvent) {
            keysPressed[event.keyCode] = false
            when (event.keyCode) {
                KeyEvent.VK_M -> map = true
            }
        }
    })

    // Obsługa przesunięcia okna
    frame.addComponentListener(object : java.awt.event.ComponentAdapter() {
        override fun componentMoved(e: java.awt.event.ComponentEvent?) {
            centerX = frame.x + frame.width / 2
            centerY = frame.y + frame.height / 2
        }
    })

    // Pętla czasowa do aktualizacji ruchu gracza
    fixedRateTimer(name = "player-update", initialDelay = 0, period = 16) {
        player.update(keysPressed)
        renderCast.repaint()
        playerOnScreen.repaint()
    }

    // Pętla do resetowania pozycji myszy
    while (true) {
        delay(75)
        Robot().mouseMove(MouseInfo.getPointerInfo().location.x, 0)
        Robot().mouseMove(960, 0)
        Robot().mouseMove(960, 384)
    }
}


class Map {
    // Wartości: 1-ściana, 0-pusta przestrzeń, 5-początek i koniec labiryntu
    val grid: Array<IntArray> = arrayOf(
        intArrayOf(5,5,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(5,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1),
        intArrayOf(1,0,0,1,1,1,1,1,1,1,1,1,1,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,1,1,0,0,1,0,0,1,1,1,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,1,0,0,1,0,0,1),
        intArrayOf(1,0,0,1,0,0,1,1,1,1,0,0,1,0,0,1),
        intArrayOf(1,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,1,1,0,0,1,0,0,1,0,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,1,0,0,1,0,0,5),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,5,5)
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
                    v.fillRect((1366/2)-2,(768/2)-2, 4,4)
                }
            }
        }
        isOpaque = false
    }
}
*/
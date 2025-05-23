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
import java.awt.Dimension
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
import javax.imageio.ImageIO
import javax.swing.JLayeredPane
import kotlin.concurrent.fixedRateTimer
import kotlin.math.*


var map = true
var currentangle = 0
var shotx = 0.0
var shoty = 0.0
var tileSize = 40.0 // Rozmiar kafelka na mapie
val mapa = 0.075
var fps = 84
var MouseSupport = false

var positionX = (tileSize*3)-(tileSize/2)  //kafelek*pozycja - (pół kafelka)
var positionY = (tileSize*2)-(tileSize/2)  //kafelek*pozycja - (pół kafelka)

class Player() {
    private val map = Map()
    private val playerSize = 5
    private val margin = 2
    private var movementSpeed = 1.5
    private val rotationSpeed = 2 // Prędkość obrotu (w stopniach na klatkę)
    private val sensitivity =  0.07 // Czułość myszy
    var playerHealth = 100

    private fun canMoveTo(x: Int, y: Int): Boolean {
        val left = x - playerSize / 2
        val right = x + playerSize / 2
        val top = y - playerSize / 2
        val bottom = y + playerSize / 2

        val gridLeft = ((left - margin) / tileSize).toInt()
        val gridRight = ((right + margin) / tileSize).toInt()
        val gridTop = ((top - margin) / tileSize).toInt()
        val gridBottom = ((bottom + margin) / tileSize).toInt()

        for (gridY in gridTop..gridBottom) {
            for (gridX in gridLeft..gridRight) {
                if (gridY !in map.grid.indices || gridX !in map.grid[gridY].indices || ((map.grid[gridY][gridX] != 0) and (map.grid[gridY][gridX] != 5))) {
                    return false
                }
            }
        }
        return true
    }

    // Metoda pomocnicza do obsługi ruchu z rozbiciem na osie X i Y
    private fun tryMove(deltaX: Double, deltaY: Double) {
        // Najpierw próbujemy ruch w obu osiach
        val newX = positionX + deltaX
        val newY = positionY + deltaY
        if (canMoveTo(newX.toInt(), newY.toInt())) {
            positionX = newX
            positionY = newY
            return
        }

        // Jeśli pełny ruch jest zablokowany, próbujemy ruch tylko w osi X
        val newXOnly = positionX + deltaX
        if (canMoveTo(newXOnly.toInt(), positionY.toInt())) {
            movementSpeed = 1.25
            positionX = newXOnly
            return
        }

        // Jeśli ruch w osi X jest zablokowany, próbujemy ruch tylko w osi Y
        val newYOnly = positionY + deltaY
        if (canMoveTo(positionX.toInt(), newYOnly.toInt())) {
            movementSpeed = 1.25
            positionY = newYOnly
        }
    }

    fun w() {
        val deltaX = movementSpeed * cos(Math.toRadians(currentangle.toDouble()))
        val deltaY = movementSpeed * sin(Math.toRadians(currentangle.toDouble()))
        tryMove(deltaX, deltaY)
    }

    fun s() {
        val deltaX = -movementSpeed * cos(Math.toRadians(currentangle.toDouble()))
        val deltaY = -movementSpeed * sin(Math.toRadians(currentangle.toDouble()))
        tryMove(deltaX, deltaY)
    }

    fun a() {
        val deltaX = movementSpeed * cos(Math.toRadians(currentangle - 90.0))
        val deltaY = movementSpeed * sin(Math.toRadians(currentangle - 90.0))
        tryMove(deltaX, deltaY)
    }

    fun d() {
        val deltaX = movementSpeed * cos(Math.toRadians(currentangle + 90.0))
        val deltaY = movementSpeed * sin(Math.toRadians(currentangle + 90.0))
        tryMove(deltaX, deltaY)
    }

    fun anglea() {
        currentangle -= rotationSpeed
    }

    fun angled() {
        currentangle += rotationSpeed
    }

    fun updateAngleFromMouse() {
        if (MouseSupport == true) {
            currentangle += if (MouseInfo.getPointerInfo().location.x == 960) {
                0
            } else {
                (((MouseInfo.getPointerInfo().location.x) - 960) * sensitivity).toInt()
            }
        }
    }

    fun update(keysPressed: kotlin.collections.Map<Int, Boolean>) {
        if (keysPressed.getOrDefault(KeyEvent.VK_W, false) || keysPressed.getOrDefault(KeyEvent.VK_UP, false)) w()
        if (keysPressed.getOrDefault(KeyEvent.VK_S, false) || keysPressed.getOrDefault(KeyEvent.VK_DOWN, false)) s()
        if (keysPressed.getOrDefault(KeyEvent.VK_A, false)) a()
        if (keysPressed.getOrDefault(KeyEvent.VK_D, false)) d()
        if (keysPressed.getOrDefault(KeyEvent.VK_LEFT, false)) anglea()
        if (keysPressed.getOrDefault(KeyEvent.VK_RIGHT, false)) angled()
    }
}

class RenderCast : JPanel()     {
    private val map = Map()
    private val screenWidth = 320*2
    private val screenHeight = 200
    private val fov = 90.0
    private val textureSize = 64
    private val rayCount = screenWidth
    private val wallHeight = 32.0/1

    private var textures: Array<BufferedImage> = arrayOf()
    var enemyTextureId: BufferedImage? = null
    private var floorTexture: BufferedImage? = null
    private var ceilingTexture: BufferedImage? = null
    private val buffer: BufferedImage
    private val bufferGraphics: Graphics

    private val minBrightness = 0.25
    private val maxBrightness = 1.0
    private val shadeDistanceScale = 10.0
    private val fogColor = Color(180, 180, 180)
    private val fogDensity = 0.15

    private val rayCosines = DoubleArray(rayCount)
    private val raySines = DoubleArray(rayCount)
    private val rayAngles = DoubleArray(rayCount)

    private var lastFrameTime = System.nanoTime()
    private var frameCount = 0

    init {
        isOpaque = true
        try {
            enemyTextureId = ImageIO.read(this::class.java.classLoader.getResource("icon/icon.jpg"))
            floorTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/floor.jpg"))
            ceilingTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/ceiling.jpg"))
            textures = arrayOf(
                ImageIO.read(this::class.java.classLoader.getResource("textures/bricks.jpg")),
                ImageIO.read(this::class.java.classLoader.getResource("textures/gold.jpg")))
        }
        catch (e: Exception) {
            println("Error loading textures: ${e.message}")
            floorTexture = createTexture(Color.RED)
            ceilingTexture = createTexture(Color.BLUE)
            textures = arrayOf(
                createTexture(Color.magenta),
                createTexture(Color.YELLOW))
        }

        buffer = BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB)
        bufferGraphics = buffer.createGraphics()

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
        renderWallsToBuffer()

        // Skalowanie obrazu na pełny rozmiar panelu
        val g2d = g as Graphics2D
        val scaleX = width.toDouble() / screenWidth
        val scaleY = height.toDouble() / screenHeight
        g2d.scale(scaleX, scaleY)
        g2d.drawImage(buffer, 0, 0, null)
        g2d.scale(1.0 / scaleX, 1.0 / scaleY) // Przywraca skalę, jeśli inne elementy są rysowane
    }

    private fun renderWallsToBuffer() {
        // Obliczanie FPS
        val currentTime = System.nanoTime()
        frameCount++
        val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0 // Czas w sekundach
        if (deltaTime >= 1.0) {
            fps = (frameCount / deltaTime).toInt()
            frameCount = 0
            lastFrameTime = currentTime
        }

        val playerAngleRad = Math.toRadians(currentangle.toDouble())
        val playerHeight = wallHeight / 4.0 // Zmniejszona wysokość gracza dla bliższej perspektywy
        val horizonOffset = 0.0 // Możliwość przesunięcia linii horyzontu (dostosuj, jeśli potrzeba)
        val playerPosX = positionX / tileSize
        val playerPosY = positionY / tileSize

        // Wyczyść bufor na czarno dla debugowania
        bufferGraphics.color = Color.BLACK
        bufferGraphics.fillRect(0, 0, screenWidth, screenHeight)

        for (ray in 0 until rayCount) {
            val rayAngle = currentangle + rayAngles[ray]
            val rayAngleRad = Math.toRadians(rayAngle)
            val relativeCos = rayCosines[ray]
            val relativeSin = raySines[ray]
            val cosPlayerAngle = cos(playerAngleRad)
            val sinPlayerAngle = sin(playerAngleRad)
            val rayDirX = relativeCos * cosPlayerAngle - relativeSin * sinPlayerAngle
            val rayDirY = relativeCos * sinPlayerAngle + relativeSin * cosPlayerAngle

            var mapX = playerPosX.toInt()
            var mapY = playerPosY.toInt()
            val deltaDistX = if (rayDirX == 0.0) 1e30 else abs(1 / rayDirX)
            val deltaDistY = if (rayDirY == 0.0) 1e30 else abs(1 / rayDirY)
            var stepX: Int
            var stepY: Int
            var sideDistX: Double
            var sideDistY: Double
            var side = 0

            if (rayDirX < 0) {
                stepX = -1
                sideDistX = (playerPosX - mapX) * deltaDistX
            } else {
                stepX = 1
                sideDistX = (mapX + 1.0 - playerPosX) * deltaDistX
            }
            if (rayDirY < 0) {
                stepY = -1
                sideDistY = (playerPosY - mapY) * deltaDistY
            } else {
                stepY = 1
                sideDistY = (mapY + 1.0 - playerPosY) * deltaDistY
            }

            var hitWall = false
            var wallType = 0
            var distance: Double = 0.0
            var hitX: Double = 0.0
            var hitY: Double = 0.0

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
                if (mapY !in map.grid.indices || mapX !in map.grid[0].indices) {
                    break
                }
                if (map.grid[mapY][mapX] == 1 || map.grid[mapY][mapX] == 5) {
                    hitWall = true
                    wallType = map.grid[mapY][mapX]
                }
            }

            if (hitWall) {
                distance = if (side == 0) {
                    (mapX - playerPosX + (1 - stepX) / 2.0) / rayDirX
                } else {
                    (mapY - playerPosY + (1 - stepY) / 2.0) / rayDirY
                }
                if (distance < 0.01) distance = 0.01
                val angleDiff = abs(playerAngleRad - rayAngleRad)
                if (angleDiff < PI / 2) {
                    distance *= cos(angleDiff)
                } else {
                    hitWall = false
                }
                if (side == 0) {
                    hitX = mapX.toDouble() + (if (stepX > 0) 0.0 else 1.0)
                    hitY = playerPosY + (hitX - playerPosX) * (rayDirY / rayDirX)
                } else {
                    hitY = mapY.toDouble() + (if (stepY > 0) 0.0 else 1.0)
                    hitX = playerPosX + (hitY - playerPosY) * (rayDirX / rayDirY)
                }
            }

            val lineHeight = if (hitWall) {
                ((wallHeight * screenHeight) / (distance * tileSize)).toInt().coerceIn(0, screenHeight * 2)
            } else {
                0
            }
            val drawStart = (-lineHeight / 2 + screenHeight / 2 + horizonOffset).coerceAtLeast(0.0).toInt()
            val drawEnd = (lineHeight / 2 + screenHeight / 2 + horizonOffset).coerceAtMost(screenHeight.toDouble()).toInt()

            if (hitWall) {
                var textureX = if (side == 0) {
                    val blockY = mapY.toDouble()
                    val relativeY = hitY - blockY
                    relativeY.coerceIn(0.0, 1.0) * textureSize
                } else {
                    val blockX = mapX.toDouble()
                    val relativeX = hitX - blockX
                    relativeX.coerceIn(0.0, 1.0) * textureSize
                }
                if (side == 0 && rayDirX > 0 || side == 1 && rayDirY < 0) {
                    textureX = textureSize - textureX - 1
                }
                val texture = textures[if (wallType == 1) 0 else 1]
                for (y in drawStart until drawEnd) {
                    val wallY = (y - (screenHeight / 2.0 + horizonOffset) + lineHeight / 2.0) * wallHeight / lineHeight
                    val textureY = (wallY * textureSize / wallHeight).toInt().coerceIn(0, textureSize - 1)
                    val finalTextureX = textureX.toInt().coerceIn(0, textureSize - 1)
                    val color = texture.getRGB(finalTextureX, textureY)
                    val shadeFactor = (1.0 - (distance / shadeDistanceScale)).coerceIn(minBrightness, maxBrightness)
                    val originalColor = Color(color)
                    val shadedColor = Color(
                        (originalColor.red * shadeFactor).toInt().coerceIn(0, 255),
                        (originalColor.green * shadeFactor).toInt().coerceIn(0, 255),
                        (originalColor.blue * shadeFactor).toInt().coerceIn(0, 255)
                    )
                    val fogFactor = 1.0 - exp(-fogDensity * distance)
                    val finalColor = Color(
                        ((1.0 - fogFactor) * shadedColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * shadedColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * shadedColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                    )
                    buffer.setRGB(ray, y, finalColor.rgb)
                }
            }

            // Renderowanie podłogi i sufitu
            for (y in 0 until screenHeight) {
                if (hitWall && y in drawStart until drawEnd) continue

                val isCeiling = y < (screenHeight / 2 + horizonOffset)
                val texture = if (isCeiling) ceilingTexture else floorTexture
                if (texture == null) {
                    buffer.setRGB(ray, y, Color.GRAY.rgb)
                    continue
                }

                // Oblicz odległość do punktu na podłodze/suficie
                val rowDistance = if (isCeiling) {
                    (playerHeight * screenHeight/2) / (10.0 * ((screenHeight / 2.0 + horizonOffset) - y + 0.0)) //0.5
                } else {
                    (playerHeight * screenHeight/2) / (10.0 * (y - (screenHeight / 2.0 + horizonOffset) + 0.0)) //0.5
                }
                if (rowDistance < 0.01 || rowDistance > 50.0) continue

                // Oblicz współrzędne na mapie
                val floorX = playerPosX + rowDistance * rayDirX+100
                val floorY = playerPosY + rowDistance * rayDirY+100

                // Mapowanie tekstury (3-krotne powiększenie, korekta obrotu)
                val textureScale = 2.0
                val textureX = ((floorY / textureScale * textureSize) % textureSize).toInt().coerceIn(0, textureSize - 1)
                val textureY = ((floorX / textureScale * textureSize) % textureSize).toInt().coerceIn(0, textureSize - 1)

                // Pobierz kolor
                val color = texture.getRGB(textureX, textureY)

                // Cieniowanie i mgła
                val shadeFactor = (1.0 - (rowDistance / shadeDistanceScale)).coerceIn(minBrightness, maxBrightness)
                val originalColor = Color(color)
                val shadedColor = Color(
                    (originalColor.red * shadeFactor).toInt().coerceIn(0, 255),
                    (originalColor.green * shadeFactor).toInt().coerceIn(0, 255),
                    (originalColor.blue * shadeFactor).toInt().coerceIn(0, 255)
                )
                val fogFactor = 1.0 - exp(-fogDensity * rowDistance)
                val finalColor = Color(
                    ((1.0 - fogFactor) * shadedColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                    ((1.0 - fogFactor) * shadedColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                    ((1.0 - fogFactor) * shadedColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                )

                buffer.setRGB(ray, y, finalColor.rgb)
            }
        }
        repaint()
    }

    fun shotgun() {
        val shotAngleRad = Math.toRadians(currentangle.toDouble())
        shotx = positionX + 100 * cos(shotAngleRad)
        shoty = positionY + 100 * sin(shotAngleRad)
        repaint()
    }
}

fun main() = runBlocking {
    // Podstawa wyświetlania
    //this::class.java.classLoader.getResource(folder/plik)
    val frame = JFrame("rolada z gówna")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.iconImage = Toolkit.getDefaultToolkit().getImage(this::class.java.classLoader.getResource("icon/icon.jpg")) //src/main/resources/icon/icon.jpg
    frame.isResizable = false
    frame.setSize(1366, 768)
    frame.setLocation(((Toolkit.getDefaultToolkit().screenSize.width - frame.width) / 2), ((Toolkit.getDefaultToolkit().screenSize.height - frame.height) / 2))

    // Ukrycie kursora
    frame.cursor = frame.toolkit.createCustomCursor(
        BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
        Point(0, 0),
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

    val renderCast = RenderCast()
    renderCast.isOpaque = false
    renderCast.setSize(1366, 768)
    renderCast.setBounds(0, 0, 1366, 768)

    //frame.add(ekran)
    layeredPane.add(mapa, 1)
    layeredPane.add(renderCast, 2)

    // Inicjalizacja gracza
    val player = Player()

    // Śledzenie stanu klawiszy z jawnym typem kotlin.collections.MutableMap<Int, Boolean>
    val keysPressed: MutableMap<Int, Boolean> = mutableMapOf()

    // Środek okna
    var centerX = frame.width / 2

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
            player.updateAngleFromMouse()
            renderCast.repaint()
        }

        override fun mouseDragged(e: MouseEvent) {
            player.updateAngleFromMouse()
            renderCast.repaint()
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
    frame.addComponentListener(object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent?) {
            centerX = frame.x + frame.width / 2
        }
    })

    // Pętla czasowa do aktualizacji ruchu gracza
    fixedRateTimer(name = "player-update", initialDelay = 100, period = 16) {
        player.update(keysPressed)
        renderCast.repaint()
        Mappingmap().repaint()
    }

    // Pętla do resetowania pozycji myszy
    while (MouseSupport) {
        delay(75)
        Robot().mouseMove(MouseInfo.getPointerInfo().location.x, 0)
        Robot().mouseMove(960, 0)
        Robot().mouseMove(960, 384)
    }
}

class Map {
    // Wartości: 1-ściana, 0-pusta przestrzeń, 5-początek i koniec labiryntu
    val grid: Array<IntArray> = arrayOf(
        intArrayOf(5,5,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(5,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,1,1,0,1,1,1,0,1,0,1,1,1,1,1,1,1,0,1,0,1),
        intArrayOf(1,0,1,0,0,0,0,0,1,0,1,0,0,0,0,0,0,0,1,0,1,0,1),
        intArrayOf(1,1,1,1,1,0,1,0,1,0,1,0,1,0,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,1,0,1,0,1,0,1,0,1,0,0,0,0,0,0,0,1),
        intArrayOf(1,0,1,0,1,0,1,1,1,1,1,1,1,1,1,0,1,0,1,1,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,0,0,0,1,0,0,0,1,0,1,0,0,0,1),
        intArrayOf(1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,1,1,0,1,1,1,0,1),
        intArrayOf(1,0,1,0,0,0,1,0,0,0,0,0,0,0,0,0,1,0,1,0,1,0,1),
        intArrayOf(1,0,1,1,1,0,1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,0,1),
        intArrayOf(1,0,0,0,1,0,1,0,1,0,0,0,1,0,1,0,1,0,0,0,1,0,1),
        intArrayOf(1,1,1,0,1,1,1,1,1,1,1,0,1,1,1,0,1,0,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,1,0,1),
        intArrayOf(1,0,1,1,1,1,1,1,1,1,1,0,1,0,1,1,1,0,1,1,1,1,1),
        intArrayOf(1,0,0,0,1,0,1,0,1,0,0,0,1,0,1,0,0,0,0,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,1,1,1,1,1,1,0,1),
        intArrayOf(1,0,1,0,1,0,0,0,0,0,1,0,1,0,1,0,0,0,0,0,0,0,1),
        intArrayOf(1,1,1,1,1,0,1,1,1,0,1,1,1,1,1,1,1,1,1,0,1,0,1),
        intArrayOf(1,0,1,0,1,0,1,0,1,0,0,0,1,0,1,0,1,0,0,0,1,0,1),
        intArrayOf(1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,0,1,1,1,1,1,0,1),
        intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,5,5)
    )
}

class Mappingmap : JPanel() {
    private val map = Map()
    private val miniMapSize = 200 // Stały rozmiar minimapy (szerokość i wysokość w pikselach)
    private val offsetX = 10 // Odstęp od lewej krawędzi
    private val offsetY = 10 // Odstęp od górnej krawędzi

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        val g2 = v as Graphics2D

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Oblicz skalę na podstawie rozmiaru mapy i minimapy
        val mapWidth = map.grid[0].size
        val mapHeight = map.grid.size
        val tileScale = minOf(miniMapSize.toDouble() / mapWidth, miniMapSize.toDouble() / mapHeight)

        // Rysuj minimapę w stałym obszarze
        for (row in map.grid.indices) {
            for (col in map.grid[row].indices) {
                val x = (col * tileScale).toInt()
                val y = (row * tileScale).toInt()
                val tileSize = tileScale.toInt()+1

                when (map.grid[row][col]) {
                    1 -> {
                        g2.color = Color(0, 255, 0) // Ściana
                        g2.fillRect(x+offsetX, y+offsetY, tileSize, tileSize)
                    }
                    5 -> {
                        g2.color = Color.YELLOW // Punkt startu/końca
                        g2.fillRect(x+offsetX, y+offsetY, tileSize, tileSize)
                    }
                }
            }
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

        // Rysuj FPS
        g2.color = Color.YELLOW
        g2.font = java.awt.Font("BOLD", java.awt.Font.BOLD, 17)
        g2.drawString("FPS: $fps", 1366 - 90,  20)
        isOpaque = false
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(miniMapSize + offsetX * 2, miniMapSize + offsetY * 2)
    }
}
*/
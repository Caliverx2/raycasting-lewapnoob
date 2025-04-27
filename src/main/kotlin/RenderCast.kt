package org.example.MainKt

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JPanel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl

class RenderCast : JPanel() {
    private var map = Map()
    private val screenWidth = 320
    private val screenHeight = 200
    private val fov = 90.0
    private val textureSize = 64
    private val rayCount = screenWidth
    private val wallHeight = 32.0

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

    private var visibleEnemies = mutableListOf<Triple<Enemy, Int, Double>>() // (Enemy, screenX, distance)

    private val zBuffer = DoubleArray(rayCount) { Double.MAX_VALUE }

    private var lastShotTime = 0L // Czas ostatniego strzału w nanosekundach
    private val SHOT_COOLDOWN = 500_000_000L // 1 sekunda w nanosekundach
    private var lastLightMoveTime = 0L // Czas ostatniego ruchu lightSources[3]
    private val LIGHT_MOVE_INTERVAL = 250_000_000L/4 // 0,25 sekundy w nanosekundach
    private var lightMoveDirection = 0.0 // Kąt ruchu lightSources[3] (w stopniach)
    private var isLightMoving = false

    fun getEnemies(): List<Enemy> = enemies

    init {
        isOpaque = true
        try {
            enemyTextureId = ImageIO.read(this::class.java.classLoader.getResource("textures/boguch.jpg"))
            floorTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/floor.jpg"))
            ceilingTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/ceiling.jpg"))
            textures = arrayOf(
                ImageIO.read(this::class.java.classLoader.getResource("textures/bricks.jpg")),
                ImageIO.read(this::class.java.classLoader.getResource("textures/gold.jpg"))
            )
        } catch (e: Exception) {
            println("Error loading textures: ${e.message}")
            floorTexture = createTexture(Color.darkGray)
            ceilingTexture = createTexture(Color.lightGray)
            textures = arrayOf(
                createTexture(Color(90, 39, 15)),
                createTexture(Color(255, 215, 0))
            )
            enemyTextureId = createTexture(Color(255, 68, 68))
        }

        // Inicjalizacja przeciwników
        enemies.add(Enemy((tileSize * 6) - (tileSize / 2), (tileSize * 12) - (tileSize / 2), 100, enemyTextureId!!, this, speed = (2.0 * ((10..19).random()/10.0))))
        enemies.add(Enemy((tileSize * 16) - (tileSize / 2), (tileSize * 18) - (tileSize / 2), 100, enemyTextureId!!, this, speed = (2.0 * ((10..19).random()/10.0))))
        enemies.add(Enemy((tileSize * 2) - (tileSize / 2), (tileSize * 22) - (tileSize / 2), 100, enemyTextureId!!, this, speed = (2.0 * ((10..19).random()/10.0))))
        println(enemies[0].speed)
        println(enemies[1].speed)
        println(enemies[2].speed)

        lightSources.add(LightSource(x = (enemies[0].x/tileSize), y = enemies[0].y/tileSize, color = Color(255, 20,20), intensity = 0.4, range = 3.0, owner = "${enemies[0]}"))
        lightSources.add(LightSource(x = (enemies[1].x/tileSize), y = enemies[1].y/tileSize, color = Color(255, 255,20), intensity = 0.4, range = 3.0, owner = "${enemies[1]}"))
        lightSources.add(LightSource(x = (enemies[2].x/tileSize), y = enemies[2].y/tileSize, color = Color(255, 20,255), intensity = 0.3, range = 2.0, owner = "${enemies[2]}"))
        println(lightSources[0].owner + " " +enemies[0])
        println(lightSources[1].owner + " " +enemies[1])
        println(lightSources[2].owner + " " +enemies[2])

        lightSources.add(LightSource(x = (1/tileSize), y = 1/tileSize, color = Color(200, 200,100), intensity = 0.4, range = 0.15, owner = "nic"))
        lightSources.add(LightSource(x = (1/tileSize), y = 1/tileSize, color = Color(200, 200,100), intensity = 0.4, range = 0.15, owner = "nic"))
        lightSources.add(LightSource(x = (1/tileSize), y = 1/tileSize, color = Color(200, 200,100), intensity = 0.4, range = 0.15, owner = "nic"))


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

    override fun paintComponent(g: Graphics){
        super.paintComponent(g)
        renderWallsToBuffer()

        val g2d = g as Graphics2D
        val scaleX = width.toDouble() / screenWidth
        val scaleY = height.toDouble() / screenHeight
        g2d.scale(scaleX, scaleY)
        g2d.drawImage(buffer, 0, 0, null)
        g2d.scale(1.0 / scaleX, 1.0 / scaleY)
        if (currentangle <= -1) {
            currentangle = 360
        } else if (currentangle > 361) {
            currentangle = 0
        }
    }

    // Funkcja obliczająca wpływ światła na piksel
    private fun calculateLightContribution(worldX: Double, worldY: Double, baseColor: Color): Color {
        var totalRed = baseColor.red.toDouble()
        var totalGreen = baseColor.green.toDouble()
        var totalBlue = baseColor.blue.toDouble()

        lightSources.forEach { light ->
            // Oblicz odległość od źródła światła (w jednostkach mapy)
            val dx = worldX - light.x
            val dy = worldY - light.y
            val distance = sqrt(dx * dx + dy * dy)

            if (distance < light.range) {
                // Model zanikania światła: odwrotność kwadratu odległości
                val attenuation = light.intensity * (0.75 / (1.0 + distance * distance))
                // Skaluj kolor światła
                totalRed += light.color.red * attenuation
                totalGreen += light.color.green * attenuation
                totalBlue += light.color.blue * attenuation
            }
        }

        return Color(
            totalRed.toInt().coerceIn(0, 255),
            totalGreen.toInt().coerceIn(0, 255),
            totalBlue.toInt().coerceIn(0, 255)
        )
    }

    private fun renderWallsToBuffer() {
        enemies.forEach { it.update() }

        // Aktualizuj pozycje źródeł światła dla przeciwników
        for (i in 0..2) {
            try {
                lightSources[i].x = enemies[i].x / tileSize
                lightSources[i].y = enemies[i].y / tileSize
            } catch (e: Exception) {
                continue
            }
        }

        // Aktualizuj pozycję lightSources[3] z kolizją
        val currentTime = System.nanoTime()
        if (isLightMoving && currentTime - lastLightMoveTime >= LIGHT_MOVE_INTERVAL) {
            lastLightMoveTime = currentTime
            val angleRad = Math.toRadians(lightMoveDirection)
            val moveDistance = tileSize / 2.0 / tileSize // Pół tileSize w jednostkach mapy
            val newX = lightSources[3].x + moveDistance * cos(angleRad)
            val newY = lightSources[3].y + moveDistance * sin(angleRad)

            // Sprawdź kolizję z mapą
            val mapX = newX.toInt()
            val mapY = newY.toInt()
            if (mapY in map.grid.indices && mapX in map.grid[0].indices && map.grid[mapY][mapX] != 1) {
                // Brak kolizji, zaktualizuj pozycję
                lightSources[3].x = newX
                lightSources[3].y = newY
            } else {
                // Kolizja ze ścianą, zatrzymaj ruch
                /*
                if (map.grid[mapY][mapX] == 1) {
                    println((map.grid[mapY][mapX]))
                    map.grid[mapY][mapX] = 0
                    println("X: ${mapX},Y: ${mapY}")
                    println((map.grid[mapY][mapX]))
                }*/
                lightSources[3].intensity = 0.0
                isLightMoving = false
            }
        }

        val playerAngleRad = Math.toRadians(currentangle.toDouble())
        val playerHeight = wallHeight / 4.0
        val horizonOffset = 0.0
        val playerPosX = positionX / tileSize
        val playerPosY = positionY / tileSize

        bufferGraphics.color = Color.BLACK
        bufferGraphics.fillRect(0, 0, screenWidth, screenHeight)

        zBuffer.fill(Double.MAX_VALUE)
        visibleEnemies.clear()

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
            var distance = Double.MAX_VALUE
            var hitX = 0.0
            var hitY = 0.0

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
                    distance = Double.MAX_VALUE
                }
                if (side == 0) {
                    hitX = mapX.toDouble() + (if (stepX > 0) 0.0 else 1.0)
                    hitY = playerPosY + (hitX - playerPosX) * (rayDirY / rayDirX)
                } else {
                    hitY = mapY.toDouble() + (if (stepY > 0) 0.0 else 1.0)
                    hitX = playerPosX + (hitY - playerPosY) * (rayDirX / rayDirY)
                }
                zBuffer[ray] = distance
            }

            // Check for enemy intersections
            enemies.forEach { enemy ->
                val halfSize = enemy.size * tileSize / 2 / tileSize
                val enemyLeft = enemy.x / tileSize - halfSize
                val enemyRight = enemy.x / tileSize + halfSize
                val enemyTop = enemy.y / tileSize - halfSize
                val enemyBottom = enemy.y / tileSize + halfSize

                val closestX = clamp(enemy.x / tileSize, enemyLeft, enemyRight)
                val closestY = clamp(enemy.y / tileSize, enemyTop, enemyBottom)
                val dx = closestX - playerPosX
                val dy = closestY - playerPosY

                val rayLength = dx * rayDirX + dy * rayDirY
                if (rayLength > 0) {
                    val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
                    if (perpendicularDistance < halfSize + 0.05) {
                        val centerDx = enemy.x / tileSize - playerPosX
                        val centerDy = enemy.y / tileSize - playerPosY
                        val angleToEnemy = atan2(centerDy, centerDx)
                        val relativeAngle = normalizeAngle(Math.toDegrees(angleToEnemy) - currentangle)
                        if (abs(relativeAngle) <= fov / 2 + 10) {
                            val angleRatio = relativeAngle / (fov / 2)
                            val screenX = (screenWidth / 2 + angleRatio * screenWidth / 2).toInt()
                            if (visibleEnemies.none { it.first === enemy }) {
                                visibleEnemies.add(Triple(enemy, screenX, rayLength))
                            }
                        }
                    }
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
                    // Dodaj wpływ dynamicznych źródeł światła
                    val worldX = hitX
                    val worldY = hitY
                    val litColor = calculateLightContribution(worldX, worldY, shadedColor)
                    // Zastosuj mgłę
                    val fogFactor = 1.0 - exp(-fogDensity * distance)
                    val finalColor = Color(
                        ((1.0 - fogFactor) * litColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * litColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * litColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
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

                val rowDistance = if (isCeiling) {
                    (playerHeight * screenHeight / 2) / (10.0 * ((screenHeight / 2.0 + horizonOffset) - y + 0.0))
                } else {
                    (playerHeight * screenHeight / 2) / (10.0 * (y - (screenHeight / 2.0 + horizonOffset) + 0.0))
                }
                if (rowDistance < 0.01 || rowDistance > 50.0) continue

                val floorX = playerPosX + rowDistance * rayDirX + 100
                val floorY = playerPosY + rowDistance * rayDirY + 100

                val textureScale = 2.0
                val textureX = ((floorY / textureScale * textureSize) % textureSize).toInt().coerceIn(0, textureSize - 1)
                val textureY = ((floorX / textureScale * textureSize) % textureSize).toInt().coerceIn(0, textureSize - 1)

                val color = texture.getRGB(textureX, textureY)
                val shadeFactor = (1.0 - (rowDistance / shadeDistanceScale)).coerceIn(minBrightness, maxBrightness)
                val originalColor = Color(color)
                val shadedColor = Color(
                    (originalColor.red * shadeFactor).toInt().coerceIn(0, 255),
                    (originalColor.green * shadeFactor).toInt().coerceIn(0, 255),
                    (originalColor.blue * shadeFactor).toInt().coerceIn(0, 255)
                )
                // Dodaj wpływ dynamicznych źródeł światła
                val worldX = playerPosX + rowDistance * rayDirX
                val worldY = playerPosY + rowDistance * rayDirY
                val litColor = calculateLightContribution(worldX, worldY, shadedColor)
                // Zastosuj mgłę
                val fogFactor = 1.0 - exp(-fogDensity * rowDistance)
                val finalColor = Color(
                    ((1.0 - fogFactor) * litColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                    ((1.0 - fogFactor) * litColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                    ((1.0 - fogFactor) * litColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                )

                buffer.setRGB(ray, y, finalColor.rgb)
            }
        }

        renderEnemies()
    }

    private fun renderEnemies() {
        visibleEnemies.sortByDescending { it.third }

        visibleEnemies.forEach { (enemy, screenX, distance) ->
            val enemyHeight = wallHeight / 2
            val minSize = 0.001
            val maxSize = 128.0 * 2
            val spriteSize = ((enemyHeight * screenHeight) / (distance * tileSize)).coerceIn(minSize, maxSize).toInt()

            val floorY = (screenHeight / 2 + (wallHeight * screenHeight) / (2 * distance * tileSize)).toInt()
            val drawStartY = (floorY - spriteSize).coerceIn(0, screenHeight - 1)
            val drawEndY = floorY.coerceIn(0, screenHeight - 1)

            val fullSpriteLeftX = screenX - spriteSize / 2.0
            val fullSpriteRightX = screenX + spriteSize / 2.0
            val drawStartX = fullSpriteLeftX.coerceAtLeast(0.0).toInt()
            val drawEndX = fullSpriteRightX.coerceAtMost(screenWidth - 1.0).toInt()

            for (x in drawStartX until drawEndX) {
                if (x < 0 || x >= zBuffer.size) continue
                if (distance < zBuffer[x]) {
                    val textureFraction = (x - fullSpriteLeftX) / (fullSpriteRightX - fullSpriteLeftX)
                    val textureX = (textureFraction * enemy.texture.width).coerceIn(0.0, enemy.texture.width - 1.0)
                    for (y in drawStartY until drawEndY) {
                        val textureY = ((y - drawStartY).toDouble() * enemy.texture.height / spriteSize).coerceIn(0.0, enemy.texture.height - 1.0)
                        val color = enemy.texture.getRGB(textureX.toInt(), textureY.toInt())
                        if ((color and 0xFF000000.toInt()) != 0) {
                            val originalColor = Color(color)
                            // Dodaj wpływ dynamicznych źródeł światła
                            val worldX = enemy.x / tileSize
                            val worldY = enemy.y / tileSize
                            val litColor = calculateLightContribution(worldX, worldY, originalColor)
                            // Zastosuj mgłę
                            val fogFactor = 1.0 - exp(-fogDensity * distance)
                            val finalColor = Color(
                                ((1.0 - fogFactor) * litColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                                ((1.0 - fogFactor) * litColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                                ((1.0 - fogFactor) * litColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                            )
                            buffer.setRGB(x, y, finalColor.rgb)
                        }
                    }
                }
            }
        }
    }

    // Funkcja do odtwarzania pliku MP3 z regulacją głośności
    fun playSound(soundFile: String, volume: Float = 0.5f) {
        try {
            val resource = RenderCast::class.java.classLoader.getResource("audio/$soundFile")
                ?: throw IllegalArgumentException("Nie znaleziono pliku dźwiękowego: $soundFile")
            val clip = AudioSystem.getClip()

            // Otwórz strumień audio w osobnym wątku
            Thread {
                try {
                    clip.open(AudioSystem.getAudioInputStream(resource))

                    // Regulacja głośności (w decybelach)
                    val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    val maxGain = gainControl.maximum
                    val minGain = gainControl.minimum
                    // Przelicz liniową wartość volume (0.0 - 1.0) na skalę decybelową
                    val gainRange = maxGain - minGain
                    val gain = minGain + (gainRange * volume.coerceIn(0.0f, 1.0f))
                    gainControl.value = gain

                    clip.start()
                    clip.drain()
                } catch (e: Exception) {
                    println("Błąd podczas odtwarzania dźwięku $soundFile: ${e.message}")
                } finally {
                }
            }.start()
        } catch (e: Exception) {
            println("Błąd podczas ładowania dźwięku $soundFile: ${e.message}")
        }
    }

    fun shotgun() {
        val currentTime = System.nanoTime()
        if (currentTime - lastShotTime >= SHOT_COOLDOWN && !isShooting) {
            lastShotTime = currentTime
            isShooting = true

            val random = Random.nextFloat()
            val soundFile = when {

                random < 0.33f -> "shot1.wav"
                random < 0.66f -> "shot2.wav"
                else -> "shot3.wav"
            }

            val shotAngleRad = Math.toRadians(currentangle.toDouble())
            val playerPosX = positionX / tileSize
            val playerPosY = positionY / tileSize
            val rayDirX = cos(shotAngleRad)
            val rayDirY = sin(shotAngleRad)

            var mapX = playerPosX.toInt()
            var mapY = playerPosY.toInt()
            val deltaDistX = if (rayDirX == 0.0) 1e30 else abs(1 / rayDirX)
            val deltaDistY = if (rayDirY == 0.0) 1e30 else abs(1 / rayDirY)
            var stepX: Int
            var stepY: Int
            var sideDistX: Double
            var sideDistY: Double
            var side: Int

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
            var wallDistance = Double.MAX_VALUE

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
                    wallDistance = if (side == 0) {
                        (mapX - playerPosX + (1 - stepX) / 2.0) / rayDirX
                    } else {
                        (mapY - playerPosY + (1 - stepY) / 2.0) / rayDirY
                    }
                    if (wallDistance < 0.01) wallDistance = 0.01
                }
            }

            playSound(soundFile, volume = 0.65f)

            lightSources[3].x = positionX / tileSize
            lightSources[3].y = positionY / tileSize
            lightMoveDirection = currentangle.toDouble()
            lightSources[3].intensity = 0.4
            lastLightMoveTime = currentTime
            isLightMoving = true

            enemies.toList().forEach { enemy ->
                lightSources.toList().forEach { LightSource ->
                    val dx = enemy.x / tileSize - playerPosX
                    val dy = enemy.y / tileSize - playerPosY
                    val rayLength = dx * rayDirX + dy * rayDirY

                    if (rayLength > 0 && rayLength < wallDistance) {
                        val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
                        if ((perpendicularDistance < (enemy.size * 20) / 2 / tileSize) and (enemy.health >= 1)) {
                            val angleToEnemy = atan2(dy, dx)
                            val angleDiff = abs(angleToEnemy - shotAngleRad)
                            println("")
                            println("toenemy, enemy=${enemy}")
                            println("angleDiff: ${angleDiff}, Radians: ${Math.toRadians(15.0)}, currentAngle: ${currentangle}")

                            if (angleDiff < Math.toRadians(35.0)) {
                                enemy.health -= 10
                                println("")
                                println("trafiono, enemy health=${enemy.health}, enemy=${enemy}")
                                if (enemy.health <= 0) {
                                    playSound(when {
                                        random < 0.16f -> "scream1.wav"
                                        random < 0.32f -> "scream2.wav"
                                        random < 0.48f -> "scream3.wav"
                                        random < 0.64f -> "scream4.wav"
                                        random < 0.80f -> "scream5.wav"
                                        else -> "scream6.wav"
                                    }, volume = 0.65f)
                                    try {
                                        lightSources.find { it.owner == "${enemy}" }?.let {
                                            println("owner:${it.owner}  enemy:${enemy}")
                                            lightSources.get(index = lightSources.indexOf(it)).color = Color(0,0,0)
                                        }
                                        enemy.texture = ImageIO.read(this::class.java.classLoader.getResource("textures/boguch_bochen_chlepa.jpg"))
                                    } catch (e: Exception) {
                                        enemy.texture = createTexture(Color.black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun normalizeAngle(angle: Double): Double {
        var normalized = angle % 360.0
        if (normalized > 180.0) normalized -= 360.0
        if (normalized < -180.0) normalized += 360.0
        return normalized
    }

    private fun clamp(value: Double, min: Double, max: Double): Double {
        return maxOf(min, minOf(max, value))
    }
}

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
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow

class RenderCast(private val map: Map) : JPanel() {
    private val screenWidth = 320
    private val screenHeight = 200
    private val fov = 90.0
    private val textureSize = 64
    private val rayCount = screenWidth
    private val wallHeight = 32.0
    private val maxRayDistance = 22.0//22.0

    private val textureMap: MutableMap<Int, BufferedImage> = mutableMapOf()
    // Nowy zbiór indeksów reprezentujących ściany
    private val wallIndices: Set<Int> = setOf(1, 2, 5) // Ściany to indeksy 1, 2, 5
    var enemyTextureId: BufferedImage? = null
    private var floorTexture: BufferedImage? = null
    private var ceilingTexture: BufferedImage? = null
    private val buffer: BufferedImage
    private val bufferGraphics: Graphics
    private var renderFps = 0
    private var renderFrameCount = 0
    private var lastRenderFpsUpdate = System.nanoTime()
    private var lastRenderFrameTime = System.nanoTime()

    private val minBrightness = 0.25
    private val maxBrightness = 1.0
    private val shadeDistanceScale = 10.0
    private val fogColor = Color(180, 180, 180)
    private val fogDensity = 0.05
    private val rayCosines = DoubleArray(rayCount)
    private val raySines = DoubleArray(rayCount)
    private val rayAngles = DoubleArray(rayCount)
    private var visibleEnemies = mutableListOf<Triple<Enemy, Int, Double>>()
    private val zBuffer = DoubleArray(rayCount) { Double.MAX_VALUE }
    private var lastShotTime = 0L
    private val SHOT_COOLDOWN = 500_000_000L
    private var lastLightMoveTime = 0L
    private val LIGHT_MOVE_INTERVAL = 250_000_000L / 4
    private var lightMoveDirection = 0.0
    private var isLightMoving = false

    fun getEnemies(): List<Enemy> = enemies

    init {
        isOpaque = true
        try {
            enemyTextureId = ImageIO.read(this::class.java.classLoader.getResource("textures/boguch.jpg"))
            floorTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/floor.jpg"))
            ceilingTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/ceiling.jpg"))

            loadTexture(1, "textures/bricks.jpg")
            loadTexture(2, "textures/black_bricks.png")//Color(20, 50, 50))
            loadTexture(5, "textures/gold.jpg")
        } catch (e: Exception) {
            println("Error loading textures: ${e.message}")
            floorTexture = createTexture(Color.darkGray)
            ceilingTexture = createTexture(Color.lightGray)
            textureMap[1] = createTexture(Color(90, 39, 15))
            textureMap[2] = createTexture(Color(20, 50, 50))
            textureMap[5] = createTexture(Color(255, 215, 0))
            enemyTextureId = createTexture(Color(255, 68, 68))
        }

        lightSources.add(LightSource(0.0, 0.0, color = Color(200, 200, 100), intensity = 0.75, range = 0.15, owner = "player"))

        enemies.add(Enemy((tileSize * 2) - (tileSize / 2), (tileSize * 6) - (tileSize / 2), 100, enemyTextureId!!, this, map, speed = (2.0 * ((18..19).random() / 10.0))))
        enemies.add(Enemy((tileSize * 12) - (tileSize / 2), (tileSize * 18) - (tileSize / 2), 100, enemyTextureId!!, this, map, speed = (2.0 * ((18..19).random() / 10.0))))
        enemies.add(Enemy((tileSize * 2) - (tileSize / 2), (tileSize * 16) - (tileSize / 2), 100, enemyTextureId!!, this, map, speed = (2.0 * ((18..19).random() / 10.0))))

        lightSources.add(LightSource((enemies[0].x / tileSize), (enemies[0].y / tileSize), color = Color(20, 255, 20), intensity = 0.35, range = 1.5, owner = "${enemies[0]}"))
        lightSources.add(LightSource((enemies[1].x / tileSize), (enemies[1].y / tileSize), color = Color(255, 22, 20), intensity = 0.35, range = 1.5, owner = "${enemies[1]}"))
        lightSources.add(LightSource((enemies[2].x / tileSize), (enemies[2].y / tileSize), color = Color(22, 20, 255), intensity = 0.35, range = 1.5, owner = "${enemies[2]}"))

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

    private fun loadTexture(index: Int, resourcePath: String) {
        try {
            textureMap[index] = ImageIO.read(this::class.java.classLoader.getResource(resourcePath))
        } catch (e: Exception) {
            println("Error loading texture $resourcePath: ${e.message}")
            textureMap[index] = createTexture(Color.gray)
        }
    }

    private fun loadTexture(index: Int, color: Color) {
        textureMap[index] = createTexture(color)
    }

    private fun createTexture(color: Color): BufferedImage {
        val texture = BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_RGB)
        val g = texture.createGraphics()
        g.color = color
        g.fillRect(0, 0, textureSize, textureSize)
        g.dispose()
        return texture
    }

    fun getRenderFps(): Int = renderFps

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        renderWallsToBuffer()
        val g2d = g as Graphics2D
        val scaleX = width.toDouble() / screenWidth
        val scaleY = height.toDouble() / screenHeight
        g2d.scale(scaleX, scaleY)
        g2d.drawImage(buffer, 0, 0, null)
        g2d.scale(1.0 / scaleX, 1.0 / scaleY)
    }

    private fun calculateLightContribution(worldX: Double, worldY: Double, baseColor: Color): Color {
        var totalRed = baseColor.red.toDouble()
        var totalGreen = baseColor.green.toDouble()
        var totalBlue = baseColor.blue.toDouble()

        lightSources.forEach { light ->
            val dx = worldX - light.x
            val dy = worldY - light.y
            val distanceSquared = dx * dx + dy * dy
            val rangeSquared = light.range * light.range

            if (distanceSquared <= rangeSquared) {
                val distance = sqrt(distanceSquared)
                if (isLightVisible(light, worldX, worldY, distance)) {
                    // Smooth damping curve with smoothing, the average of the adjacent points for smoothing
                    val attenuation = light.intensity * (1.0 - (distance / light.range).pow(2)).coerceAtLeast(0.0)
                    val smoothingFactor = 0.2 // Smoothing strength (0.0 - none, 1.0 - full smoothing)
                    val smoothedAttenuation = attenuation * (1.0 - smoothingFactor) + smoothingFactor * 0.5 // Average of nominal value
                    totalRed += light.color.red * smoothedAttenuation
                    totalGreen += light.color.green * smoothedAttenuation
                    totalBlue += light.color.blue * smoothedAttenuation
                } else {
                    // Minimal ambient lighting for shadowed areas
                    val ambientFactor = 0.1
                    totalRed += light.color.red * ambientFactor * light.intensity
                    totalGreen += light.color.green * ambientFactor * light.intensity
                    totalBlue += light.color.blue * ambientFactor * light.intensity
                }
            }
        }

        return Color(
            totalRed.toInt().coerceIn(0, 255),
            totalGreen.toInt().coerceIn(0, 255),
            totalBlue.toInt().coerceIn(0, 255)
        )
    }

    private fun isLightVisible(light: LightSource, targetX: Double, targetY: Double, distance: Double): Boolean {
        val startX = light.x
        val startY = light.y
        val MIN_STEP_SIZE = 0.5
        val MAX_STEP_SIZE = 3.5

        if (distance > light.range) return false

        val dx = targetX - startX
        val dy = targetY - startY
        val stepSize = (MIN_STEP_SIZE + (MAX_STEP_SIZE - MIN_STEP_SIZE) * (distance / light.range)).coerceIn(MIN_STEP_SIZE, MAX_STEP_SIZE)
        val stepX = dx / distance * stepSize
        val stepY = dy / distance * stepSize
        val steps = (distance / stepSize).toInt() + 1

        var currentX = startX
        var currentY = startY

        for (i in 0 until steps) {
            currentX = startX + stepX * i
            currentY = startY + stepY * i
            val mapX = currentX.toInt()
            val mapY = currentY.toInt()

            if (mapX !in 0 until map.grid[0].size || mapY !in 0 until map.grid.size) {
                return false
            }

            if (wallIndices.contains(map.grid[mapY][mapX])) {
                return false
            }

            if (sqrt((currentX - targetX) * (currentX - targetX) + (currentY - targetY) * (currentY - targetY)) < stepSize) {
                return true
            }
        }

        return true
    }

    fun renderWallsToBuffer() {
        var currentTime = System.nanoTime()
        var elapsedTime = currentTime - lastRenderFrameTime

        enemies.forEach { it.update() }

        lightSources.forEach { light ->
            if (light.owner != "player") { // Pomijamy źródło gracza
                enemies.find { enemy -> light.owner == enemy.toString() }?.let { matchedEnemy ->
                    light.x = matchedEnemy.x / tileSize
                    light.y = matchedEnemy.y / tileSize
                }
            }
        }

        if (isLightMoving && currentTime - lastLightMoveTime >= LIGHT_MOVE_INTERVAL) {
            lastLightMoveTime = currentTime
            val angleRad = Math.toRadians(lightMoveDirection)
            val moveDistance = (tileSize / 2.5 / tileSize)
            val playerLight = lightSources.find { it.owner == "player" }
            playerLight?.let {
                val newX = it.x + moveDistance * cos(angleRad)
                val newY = it.y + moveDistance * sin(angleRad)

                val mapX = newX.toInt()
                val mapY = newY.toInt()
                if (mapY in map.grid.indices && mapX in map.grid[0].indices && !wallIndices.contains(map.grid[mapY][mapX])) {
                    it.x = newX
                    it.y = newY
                } else {
                    it.intensity = 0.0
                    isLightMoving = false
                }
            }
        }

        val playerAngleRad = Math.toRadians(currentangle.toDouble())
        val playerHeight = wallHeight / 4.0
        val horizonOffset = 0.0
        val playerPosX = positionX / tileSize
        val playerPosY = positionY / tileSize

        bufferGraphics.color = fogColor // Ustaw kolor tła na kolor mgły
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

            // Raycasting with maximum distance limitation
            while (!hitWall) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX
                    mapX += stepX
                    side = 0
                    distance = (mapX - playerPosX + (1 - stepX) / 2.0) / rayDirX
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                    distance = (mapY - playerPosY + (1 - stepY) / 2.0) / rayDirY
                }
                // Check if the distance has exceeded the maximum
                if (distance > maxRayDistance || mapY !in map.grid.indices || mapX !in map.grid[0].indices) {
                    distance = maxRayDistance // Limit the distance to the maximum
                    break
                }
                if (wallIndices.contains(map.grid[mapY][mapX])) {
                    hitWall = true
                    wallType = map.grid[mapY][mapX]
                }
            }

            if (hitWall && distance <= maxRayDistance) {
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
                    distance = maxRayDistance
                }
                if (side == 0) {
                    hitX = mapX.toDouble() + (if (stepX > 0) 0.0 else 1.0)
                    hitY = playerPosY + (hitX - playerPosX) * (rayDirY / rayDirX)
                } else {
                    hitY = mapY.toDouble() + (if (stepY > 0) 0.0 else 1.0)
                    hitX = playerPosX + (hitY - playerPosY) * (rayDirX / rayDirY)
                }
                zBuffer[ray] = distance
            } else {
                distance = maxRayDistance // If a wall is not hit, set the maximum distance
                zBuffer[ray] = distance
            }

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
                if (rayLength > 0 && rayLength <= maxRayDistance) {
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

            val lineHeight = if (hitWall && distance <= maxRayDistance) {
                ((wallHeight * screenHeight) / (distance * tileSize)).toInt().coerceIn(0, screenHeight * 2)
            } else {
                0
            }
            val drawStart = (-lineHeight / 2 + screenHeight / 2 + horizonOffset).coerceAtLeast(0.0).toInt()
            val drawEnd = (lineHeight / 2 + screenHeight / 2 + horizonOffset).coerceAtMost(screenHeight.toDouble()).toInt()

            if (hitWall && distance <= maxRayDistance) {
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

                val texture = textureMap[wallType] ?: createTexture(Color.gray)
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
                    val worldX = hitX
                    val worldY = hitY
                    val litColor = calculateLightContribution(worldX, worldY, shadedColor)
                    val fogFactor = 1.0 - exp(-fogDensity * distance)
                    val finalColor = Color(
                        ((1.0 - fogFactor) * litColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * litColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * litColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                    )
                    buffer.setRGB(ray, y, finalColor.rgb)
                }
            }

            for (y in 0 until screenHeight) {
                if (hitWall && distance <= maxRayDistance && y in drawStart until drawEnd) continue

                val isCeiling = y < (screenHeight / 2 + horizonOffset)
                val texture = if (isCeiling) ceilingTexture else floorTexture
                if (texture == null) {
                    buffer.setRGB(ray, y, fogColor.rgb)
                    continue
                }

                val rowDistance = if (isCeiling) {
                    (playerHeight * screenHeight / 2) / (10.0 * ((screenHeight / 2.0 + horizonOffset) - y + 0.0))
                } else {
                    (playerHeight * screenHeight / 2) / (10.0 * (y - (screenHeight / 2.0 + horizonOffset) + 0.0))
                }
                // Limit the render distance of floors and ceilings to maxRayDistance
                if (rowDistance < 0.01 || rowDistance > maxRayDistance) {
                    buffer.setRGB(ray, y, fogColor.rgb)
                    continue
                }

                val floorX = playerPosX + rowDistance * rayDirX + 100
                val floorY = playerPosY + rowDistance * rayDirY + 100

                // Check if the pixel belongs to the opponent's shadow, shadows only on the floor
                var isShadow = false
                var shadowColor = Color(50, 50, 50)
                if (!isCeiling) {
                    enemies.forEach { enemy ->
                        val shadowRadius = 0.25
                        val enemyX = enemy.x / tileSize
                        val enemyY = enemy.y / tileSize
                        val dx = floorX - enemyX - 100
                        val dy = floorY - enemyY - 100
                        val distanceToEnemy = sqrt(dx * dx + dy * dy)
                        if (distanceToEnemy <= shadowRadius) {
                            isShadow = true
                            val shadowFactor = 1.0 - (distanceToEnemy / shadowRadius) // 1.0 in the middle, 0.0 in the edge
                            shadowColor = Color(
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255)
                            )
                        }
                    }
                }

                if (isShadow) {
                    // Shadow rendering
                    val worldX = playerPosX + rowDistance * rayDirX
                    val worldY = playerPosY + rowDistance * rayDirY
                    val litColor = calculateLightContribution(worldX, worldY, shadowColor)
                    val fogFactor = 1.0 - exp(-fogDensity * rowDistance)
                    val finalColor = Color(
                        ((1.0 - fogFactor) * litColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * litColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * litColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                    )
                    buffer.setRGB(ray, y, finalColor.rgb)
                } else {
                    // Normal Floor Rendering
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
                    val worldX = playerPosX + rowDistance * rayDirX
                    val worldY = playerPosY + rowDistance * rayDirY
                    val litColor = calculateLightContribution(worldX, worldY, originalColor)
                    val fogFactor = 1.0 - exp(-fogDensity * rowDistance)
                    val finalColor = Color(
                        ((1.0 - fogFactor) * litColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * litColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * litColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                    )
                    buffer.setRGB(ray, y, finalColor.rgb)
                }
            }
        }
        renderFrameCount++
        lastRenderFrameTime = currentTime

        if (currentTime - lastRenderFpsUpdate >= 1_000_000_000L) {
            renderFps = renderFrameCount
            renderFrameCount = 0
            lastRenderFpsUpdate = currentTime
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
                            val worldX = enemy.x / tileSize
                            val worldY = enemy.y / tileSize
                            val litColor = calculateLightContribution(worldX, worldY, originalColor)
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

    fun playSound(soundFile: String, volume: Float = 0.5f) {
        try {
            val resource = RenderCast::class.java.classLoader.getResource("audio/$soundFile")
                ?: throw IllegalArgumentException("No sound file found: $soundFile")
            val clip = AudioSystem.getClip()

            Thread {
                try {
                    clip.open(AudioSystem.getAudioInputStream(resource))
                    val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    val maxGain = gainControl.maximum
                    val minGain = gainControl.minimum
                    val gainRange = maxGain - minGain
                    val gain = minGain + (gainRange * volume.coerceIn(0.0f, 1.0f))
                    gainControl.value = gain

                    clip.start()
                    clip.drain()
                } catch (e: Exception) {
                    println("Error playing audio $soundFile: ${e.message}")
                }
            }.start()
        } catch (e: Exception) {
            println("Error loading audio $soundFile: ${e.message}")
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
                // Using wallIndices to detect walls
                if (wallIndices.contains(map.grid[mapY][mapX])) {
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

            lightSources.find { it.owner == "player" }?.let {
                it.x = positionX / tileSize
                it.y = positionY / tileSize
                lightMoveDirection = currentangle.toDouble()
                it.intensity = 0.75
                lastLightMoveTime = currentTime
                isLightMoving = true
            }

            enemies.toList().forEach { enemy ->
                val dx = enemy.x / tileSize - playerPosX
                val dy = enemy.y / tileSize - playerPosY
                val rayLength = dx * rayDirX + dy * rayDirY

                if (rayLength > 0 && rayLength < wallDistance) {
                    val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
                    if ((perpendicularDistance < (enemy.size * 20) / 2 / tileSize) && (enemy.health >= 1)) {
                        val angleToEnemy = atan2(dy, dx)
                        var angleDiff = abs(angleToEnemy - shotAngleRad)
                        angleDiff = min(angleDiff, 2 * Math.PI - angleDiff)

                        if (angleDiff < Math.toRadians(35.0)) {
                            enemy.health -= 25
                            println("enemy: ${enemy} heal: ${enemy.health}")
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
                                        lightSources[lightSources.indexOf(it)].color = Color(0, 0, 0)
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
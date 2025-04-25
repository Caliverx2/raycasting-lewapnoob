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

class RenderCast : JPanel() {
    private val map = Map()
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

    private var enemies = mutableListOf<Enemy>()
    private var visibleEnemies = mutableListOf<Triple<Enemy, Int, Double>>() // (Enemy, screenX, distance)

    private val zBuffer = DoubleArray(rayCount) { Double.MAX_VALUE }

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
        enemies.add(Enemy((tileSize * 6) - (tileSize / 2), (tileSize * 12) - (tileSize / 2), 100, enemyTextureId!!, this, speed = (0.5 * ((12..19).random()/10.0))))
        enemies.add(Enemy((tileSize * 8) - (tileSize / 2), (tileSize * 8 ) - (tileSize / 2), 100, enemyTextureId!!, this, speed = (0.5 * ((12..19).random()/10.0))))
        println(enemies.get(1).speed)
        println(enemies.get(0).speed)

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

        val g2d = g as Graphics2D
        val scaleX = width.toDouble() / screenWidth
        val scaleY = height.toDouble() / screenHeight
        g2d.scale(scaleX, scaleY)
        g2d.drawImage(buffer, 0, 0, null)
        g2d.scale(1.0 / scaleX, 1.0 / scaleY)
    }

    private fun renderWallsToBuffer() {
        enemies.forEach { it.update() }

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
            var distance = Double.MAX_VALUE // Initialize to max for no wall
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

            // Check for enemy intersections, accounting for full sprite width
            enemies.forEach { enemy ->
                // Define enemy's bounding box
                val halfSize = enemy.size * tileSize / 2 / tileSize // In world units
                val enemyLeft = enemy.x / tileSize - halfSize
                val enemyRight = enemy.x / tileSize + halfSize
                val enemyTop = enemy.y / tileSize - halfSize
                val enemyBottom = enemy.y / tileSize + halfSize

                // Calculate the closest point on the enemy's bounding box to the ray
                val closestX = clamp(enemy.x / tileSize, enemyLeft, enemyRight)
                val closestY = clamp(enemy.y / tileSize, enemyTop, enemyBottom)
                val dx = closestX - playerPosX
                val dy = closestY - playerPosY

                // Project the closest point onto the ray
                val rayLength = dx * rayDirX + dy * rayDirY
                if (rayLength > 0) { // Enemy is in front of player
                    // Calculate perpendicular distance to the ray
                    val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
                    // Check if ray intersects the enemy's bounding box
                    if (perpendicularDistance < halfSize + 0.05) {
                        // Calculate the angle to the enemy's center for screenX
                        val centerDx = enemy.x / tileSize - playerPosX
                        val centerDy = enemy.y / tileSize - playerPosY
                        val angleToEnemy = atan2(centerDy, centerDx)
                        val relativeAngle = normalizeAngle(Math.toDegrees(angleToEnemy) - currentangle)
                        // Ensure the enemy is within FOV (with buffer for sprite width)
                        if (abs(relativeAngle) <= fov / 2 + 10) {
                            val angleRatio = relativeAngle / (fov / 2)
                            val screenX = (screenWidth / 2 + angleRatio * screenWidth / 2).toInt()
                            // Add to visible enemies if not occluded or partially visible
                            if (visibleEnemies.none { it.first === enemy }) {
                                // Use rayLength for distance, but allow rendering if any part is visible
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
                    val fogFactor = 1.0 - exp(-fogDensity * distance)
                    val finalColor = Color(
                        ((1.0 - fogFactor) * shadedColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * shadedColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * shadedColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                    )
                    buffer.setRGB(ray, y, finalColor.rgb)
                }
            }

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
                val fogFactor = 1.0 - exp(-fogDensity * rowDistance)
                val finalColor = Color(
                    ((1.0 - fogFactor) * shadedColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                    ((1.0 - fogFactor) * shadedColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                    ((1.0 - fogFactor) * shadedColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
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
            val minSize = 0.1
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
                            buffer.setRGB(x, y, color)
                        }
                    }
                }
            }
        }
    }

    fun shotgun(player: Player) {
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
        var side = 10

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

        enemies.toList().forEach { enemy ->
            val dx = enemy.x / tileSize - playerPosX
            val dy = enemy.y / tileSize - playerPosY
            val rayLength = dx * rayDirX + dy * rayDirY
            if (rayLength > 0 && rayLength < wallDistance) {
                val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
                if ((perpendicularDistance < (enemy.size * 200) / 2 / tileSize) && (enemy.health > 0)) {
                    val angleToEnemy = atan2(dy, dx)
                    val angleDiff = abs(angleToEnemy - shotAngleRad)
                    if (angleDiff < Math.toRadians(15.0)) {
                        enemy.health -= 25
                        println("trafiono, enemy health=${enemy.health}, enemy=${enemy}")
                        if (enemy.health <= 0) {
                            try {
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

    // Helper function to normalize angles to [-180, 180]
    private fun normalizeAngle(angle: Double): Double {
        var normalized = angle % 360.0
        if (normalized > 180.0) normalized -= 360.0
        if (normalized < -180.0) normalized += 360.0
        return normalized
    }

    // Helper function to clamp a value within a range
    private fun clamp(value: Double, min: Double, max: Double): Double {
        return maxOf(min, minOf(max, value))
    }
}
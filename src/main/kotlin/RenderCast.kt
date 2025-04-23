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

    // Declare zBuffer as a class-level property
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
        // Enemies in open spaces near player start (21.5, 21.5)
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

        // Reset zBuffer for this frame
        zBuffer.fill(Double.MAX_VALUE)
        visibleEnemies.clear() // Reset visible enemies each frame

        // Wall, floor, ceiling, and enemy visibility check
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
            var distance = 0.0
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

            // Check for enemy intersections with this ray
            enemies.forEach { enemy ->
                val dx = enemy.x / tileSize - playerPosX
                val dy = enemy.y / tileSize - playerPosY
                // Project enemy onto ray direction
                val rayLength = dx * rayDirX + dy * rayDirY
                if (rayLength > 0) { // Enemy is in front of player
                    val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
                    // Check if enemy is close enough to the ray (within half size)
                    if (perpendicularDistance < enemy.size / 2 / tileSize+0.01) {
                        // Check if enemy is closer than wall
                        if (!hitWall || rayLength < 500) {
                            // Calculate screen X based on ray index
                            val angleRatio = rayAngles[ray] / (fov / 2)
                            val screenX = (screenWidth / 2 + angleRatio * screenWidth / 2).toInt()
                            // Add to visible enemies (avoid duplicates)
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

        // Render visible enemies as 2D overlays
        renderEnemies()
    }

    private fun renderEnemies() {
        // Sort enemies by distance (farthest to nearest) to ensure correct rendering order
        visibleEnemies.sortByDescending { it.third }

        visibleEnemies.forEach { (enemy, screenX, distance) ->
            // Perspective-correct sprite size based on enemy height
            val enemyHeight = wallHeight / 2 // Enemy height is half the wall height
            val minSize = 0.5 // Minimum sprite size in pixels
            val maxSize = 128.0 // Maximum sprite size in pixels
            val spriteSize = ((enemyHeight * screenHeight) / (distance * tileSize)).coerceIn(minSize, maxSize).toInt()

            // Calculate floor position at this distance
            val floorY = (screenHeight / 2 + (wallHeight * screenHeight) / (2 * distance * tileSize)).toInt()

            // Position sprite with bottom at floorY
            val drawStartY = (floorY - spriteSize).coerceIn(0, screenHeight - 1)
            val drawEndY = floorY.coerceIn(0, screenHeight - 1)
            val drawStartX = (screenX - spriteSize / 2).coerceIn(0, screenWidth - 1)
            val drawEndX = (screenX + spriteSize / 2).coerceIn(0, screenWidth - 1)

            // Draw sprite pixel by pixel, checking z-buffer for occlusion
            for (x in drawStartX until drawEndX) {
                // Ensure x is within zBuffer bounds
                if (x < 0 || x >= zBuffer.size) continue

                // Check if enemy is closer than the wall at this column
                if (distance < zBuffer[x]) {
                    val textureX = ((x - drawStartX) * enemy.texture.width / spriteSize).coerceIn(0, enemy.texture.width - 1)
                    for (y in drawStartY until drawEndY) {
                        val textureY = ((y - drawStartY) * enemy.texture.height / spriteSize).coerceIn(0, enemy.texture.height - 1)
                        val color = enemy.texture.getRGB(textureX, textureY)
                        // Only draw if pixel is not transparent (optional, if texture has alpha)
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

        // Raycasting setup
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

        // Raycasting loop to find wall distance
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

        // Check for enemy hits
        enemies.toList().forEach { enemy ->
            val dx = enemy.x / tileSize - playerPosX
            val dy = enemy.y / tileSize - playerPosY
            // Project enemy onto ray direction
            val rayLength = dx * rayDirX + dy * rayDirY
            if (rayLength > 0 && rayLength < wallDistance) { // Enemy in front and before wall
                //player.w(-1.0)
                val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
                // Check if enemy is within ray path
                if ((perpendicularDistance < (enemy.size*200) / 2 / tileSize) and (enemy.health > 0)) {
                    // Check angle to enemy
                    val angleToEnemy = atan2(dy, dx)
                    val angleDiff = abs(angleToEnemy - shotAngleRad)
                    if (angleDiff < Math.toRadians(15.0)) { // Widened to ±30°
                        enemy.health -= 25
                        println("trafiono, enemy health=${enemy.health}, enemy=${enemy}")
                        if (enemy.health <= 0) {
                            //enemies.remove(enemy)
                            try {
                                enemy.texture = ImageIO.read(this::class.java.classLoader.getResource("textures/boguch_bochen_chlepa.jpg"))
                            }
                            catch (e: Exception) {
                                enemy.texture = createTexture(Color.black)
                            }
                        }
                    }
                }
            }
        }
    }
}
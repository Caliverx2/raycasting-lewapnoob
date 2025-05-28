package org.example.MainKt

import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.Timer
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
import kotlin.jvm.java
import kotlin.math.min
import kotlin.math.pow

class RenderCast(private val map: Map) : JPanel() {
    private val screenWidth = 320
    private val screenHeight = 200
    private val fov = 90.0
    private val textureSize = 64
    private val rayCount = screenWidth//screenWidth
    private val wallHeight = 32.0
    private val maxRayDistance = 22.0
    private var levelUp = false
    val slotSize = 39
    val perkSlots = arrayOfNulls<Perk>(3)
    var resetPerkSlot = true

    private val textureMap: MutableMap<Int, BufferedImage> = mutableMapOf()
    private val wallIndices: Set<Int> = setOf(1, 2, 5)
    var enemyTextureId: BufferedImage? = null
    var keyTextureId: BufferedImage? = null
    var medicationTextureID: BufferedImage? = null
    var chestTextureID: BufferedImage? = null
    var ammoTextureID: BufferedImage? = null
    var traderTextureID: BufferedImage? = null
    var slotMachineTextureID: BufferedImage? = null
    var coinTextureID: BufferedImage? = null
    private val accessibleTiles: Set<Int> = setOf(0, 3, 6)
    private var floorTexture: BufferedImage? = null
    private var ceilingTexture: BufferedImage? = null
    private val buffer: BufferedImage
    private val bufferGraphics: Graphics
    private var renderFps = 0
    private var renderFrameCount = 0
    private var lastRenderFpsUpdate = System.nanoTime()
    private var lastRenderFrameTime = System.nanoTime()

    private val minBrightness = 0.0
    private val maxBrightness = 1.9
    private val shadeDistanceScale = 10.0
    private val fogColor = Color(180, 180, 180)
    private val fogDensity = 0.5/4

    private val rayCosines = DoubleArray(rayCount)
    private val raySines = DoubleArray(rayCount)
    private val rayAngles = DoubleArray(rayCount)
    private var visibleEnemies = mutableListOf<Triple<Enemy, Int, Double>>()
    private var visibleKeys = mutableListOf<Triple<Key, Int, Double>>()
    private var visibleMedications = mutableListOf<Triple<Medication, Int, Double>>()
    private var visibleChests = mutableListOf<Triple<Chest, Int, Double>>()
    private var visibleAmmo = mutableListOf<Triple<Ammo, Int, Double>>()
    private var visibleTrader = mutableListOf<Triple<Trader, Int, Double>>()
    private var visibleSlotMachines = mutableListOf<Triple<SlotMachine, Int, Double>>()
    private var visibleCoins = mutableListOf<Triple<Coin, Int, Double>>()
    private val zBuffer = DoubleArray(rayCount) { Double.MAX_VALUE }
    private var lastShotTime = 0L
    private val SHOT_COOLDOWN = 500_000_000L * FastReload
    private var lastLightMoveTime = 0L
    private val LIGHT_MOVE_INTERVAL = 250_000_000L / 4
    private var lightMoveDirection = 0.0
    private var isLightMoving = false
    private var font: Font? = null
    val fontStream = this::class.java.classLoader.getResourceAsStream("font/mojangles.ttf")
        ?: throw IllegalArgumentException("Font file not found: mojangles.ttf")

    enum class Perk { HealBoost, SpeedMovement, MoreHitShot, FastReload, AmmoBoost }

    fun getEnemies(): List<Enemy> = enemies

    init {
        isOpaque = true
        font = Font.createFont(Font.TRUETYPE_FONT, fontStream)
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)
        try {
            enemyTextureId = ImageIO.read(this::class.java.classLoader.getResource("textures/boguch.jpg"))
            floorTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/floor.jpg"))
            ceilingTexture = ImageIO.read(this::class.java.classLoader.getResource("textures/ceiling.jpg"))
            keyTextureId = ImageIO.read(this::class.java.classLoader.getResource("textures/key.png"))
            medicationTextureID = ImageIO.read(this::class.java.classLoader.getResource("textures/medication.png"))
            chestTextureID = ImageIO.read(this::class.java.classLoader.getResource("textures/chest.png"))
            ammoTextureID = ImageIO.read(this::class.java.classLoader.getResource("textures/ammo.png"))
            traderTextureID = ImageIO.read(this::class.java.classLoader.getResource("textures/villager.png"))
            slotMachineTextureID = ImageIO.read(this::class.java.classLoader.getResource("textures/slotMachine.png"))
            coinTextureID = ImageIO.read(this::class.java.classLoader.getResource("textures/coin.png"))

            loadTexture(1, "textures/bricks.jpg")
            loadTexture(2, "textures/black_bricks.png")
            loadTexture(5, "textures/gold.jpg")
        } catch (e: Exception) {
            println("Error loading textures: ${e.message}")
            floorTexture = createTexture(Color.darkGray)
            ceilingTexture = createTexture(Color.lightGray)
            textureMap[1] = createTexture(Color(90, 39, 15))
            textureMap[2] = createTexture(Color(20, 50, 50))
            textureMap[5] = createTexture(Color(255, 215, 0))
            enemyTextureId = createTexture(Color(255, 68, 68))
            keyTextureId = createTexture(Color(255, 255, 0))
            coinTextureID = createTexture(Color(255, 255, 0))
            medicationTextureID = createTexture(Color(20, 255, 20))
            chestTextureID = createTexture(Color(150,75,0))
            ammoTextureID = createTexture(Color(90,90,90))
            traderTextureID = createTexture(Color(255, 68, 68))
            slotMachineTextureID = createTexture(Color(255,140,0))
        }

        lightSources.add(LightSource(0.0, 0.0, color = Color(200, 200, 100), intensity = 0.75, range = 0.15, owner = "player"))

        enemies.add(Enemy((tileSize * 2) - (tileSize / 2), (tileSize * 2) - (tileSize / 2), health = 100, enemyTextureId!!, this, map, speed = (2.0 * ((18..19).random() / 10.0))))
        enemies.add(Enemy((tileSize * 2) - (tileSize / 2), (tileSize * 20) - (tileSize / 2), health = 100, enemyTextureId!!, this, map, speed = (2.0 * ((18..19).random() / 10.0))))
        enemies.add(Enemy((tileSize * 20) - (tileSize / 2), (tileSize * 20) - (tileSize / 2), health = 100, enemyTextureId!!, this, map, speed = (2.0 * ((18..19).random() / 10.0))))
        enemies.add(Enemy((tileSize * 20) - (tileSize / 2), (tileSize * 2) - (tileSize / 2), health = 100, enemyTextureId!!, this, map, speed = (2.0 * ((18..19).random() / 10.0))))

        lightSources.add(LightSource((enemies[0].x / tileSize), (enemies[0].y / tileSize), color = Color(20, 22, 255), intensity = 0.35, range = 1.5, owner = "${enemies[0]}"))
        lightSources.add(LightSource((enemies[1].x / tileSize), (enemies[1].y / tileSize), color = Color(255, 255, 22), intensity = 0.35, range = 1.5, owner = "${enemies[1]}"))
        lightSources.add(LightSource((enemies[2].x / tileSize), (enemies[2].y / tileSize), color = Color(22, 255, 22), intensity = 0.35, range = 1.5, owner = "${enemies[2]}"))
        lightSources.add(LightSource((enemies[3].x / tileSize), (enemies[3].y / tileSize), color = Color(255, 22, 22), intensity = 0.35, range = 1.5, owner = "${enemies[3]}"))

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
        val g2d = g as Graphics2D
        var currentTime = System.nanoTime()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        update()
        try {
            renderWallsToBuffer()
        } catch (e: Exception) {
        }
        val scaleX = width.toDouble() / screenWidth
        val scaleY = height.toDouble() / screenHeight
        g2d.scale(scaleX, scaleY)
        g2d.drawImage(buffer, 0, 0, null)
        g2d.scale(1.0 / scaleX, 1.0 / scaleY)
        renderInventoryUI(g2d)
        renderPerkGUI(g2d)
        if (levelUp) {
            Mappingmap(map, this).levelUp(g2d)
        }
        renderFrameCount++
        lastRenderFrameTime = currentTime

        if (currentTime - lastRenderFpsUpdate >= 1_000_000_000L) {
            renderFps = renderFrameCount
            renderFrameCount = 0
            lastRenderFpsUpdate = currentTime
        }
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
                    val attenuation = light.intensity * (1.0 - (distance / light.range).pow(2)).coerceAtLeast(0.0)
                    val smoothingFactor = 0.2
                    val smoothedAttenuation = attenuation * (1.0 - smoothingFactor) + smoothingFactor * 0.5
                    totalRed += light.color.red * smoothedAttenuation
                    totalGreen += light.color.green * smoothedAttenuation
                    totalBlue += light.color.blue * smoothedAttenuation
                } else {
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
    //
    fun renderWallsToBuffer() {
        var currentTime = System.nanoTime()

        lightSources.forEach { light ->
            if (light.owner != "player" && !light.owner.startsWith("projectile_")) {
                enemies.find { enemy -> light.owner == enemy.toString() }?.let { matchedEnemy ->
                    if (matchedEnemy.health > 0) {
                        light.x = matchedEnemy.x / tileSize
                        light.y = matchedEnemy.y / tileSize
                    } else {
                        lightSources.remove(light)
                    }
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
                    val random = Random.nextFloat()
                    playSound(when {
                        random < 0.50f -> "8exp.wav"
                        else -> "impact.wav"
                    }, volume = 0.65f)
                    isLightMoving = false
                    it.x = 0.0
                    it.y = 0.0
                    it.intensity = 0.0
                }
            }
        }

        val playerAngleRad = Math.toRadians(currentangle.toDouble())
        val playerHeight = wallHeight / 4.0
        val horizonOffset = 0.0
        val playerPosX = positionX / tileSize
        val playerPosY = positionY / tileSize

        bufferGraphics.color = fogColor
        bufferGraphics.fillRect(0, 0, screenWidth, screenHeight)

        zBuffer.fill(Double.MAX_VALUE)
        visibleEnemies.clear()
        visibleKeys.clear()
        visibleAmmo.clear()
        visibleMedications.clear()
        visibleChests.clear()
        visibleTrader.clear()
        visibleSlotMachines.clear()
        visibleCoins.clear()
        lookchest = false
        looktrader = false
        lookslotMachine = false

        // Arrays to store raycasting results for interpolation
        val rayDistances = DoubleArray(rayCount) { Double.MAX_VALUE }
        val rayWallTypes = IntArray(rayCount) { 0 }
        val rayHitXs = DoubleArray(rayCount) { 0.0 }
        val rayHitYs = DoubleArray(rayCount) { 0.0 }
        val raySides = IntArray(rayCount) { 0 }
        val rayHitWalls = BooleanArray(rayCount) { false }

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
                    distance = (mapX - playerPosX + (1 - stepX) / 2.0) / rayDirX
                } else {
                    sideDistY += deltaDistY
                    mapY += stepY
                    side = 1
                    distance = (mapY - playerPosY + (1 - stepY) / 2.0) / rayDirY
                }
                if (distance > maxRayDistance || mapY !in map.grid.indices || mapX !in map.grid[0].indices) {
                    distance = maxRayDistance
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
                distance = maxRayDistance
                zBuffer[ray] = distance
            }

            rayDistances[ray] = distance
            rayWallTypes[ray] = wallType
            rayHitXs[ray] = hitX
            rayHitYs[ray] = hitY
            raySides[ray] = side
            rayHitWalls[ray] = hitWall

            // Entity raycasting remains unchanged

            // Precompute constants
            val halfScreenWidth = screenWidth / 2
            val halfFov = fov / 2
            val tileSizeInv = 1.0 / tileSize
            val fovCheck = halfFov + 10

            // Helper function to process entity visibility
            fun processEntityVisibility(
                entity: Any,
                x: Double, y: Double, size: Double,
                visibleList: MutableList<Triple<Any, Int, Double>>,
                rayDirX: Double, rayDirY: Double,
                playerPosX: Double, playerPosY: Double,
                currentangle: Double
            ) {
                val halfSize = size * tileSizeInv * 0.5
                val posX = x * tileSizeInv
                val posY = y * tileSizeInv
                val left = posX - halfSize
                val right = posX + halfSize
                val top = posY - halfSize
                val bottom = posY + halfSize

                val closestX = clamp(posX, left, right)
                val closestY = clamp(posY, top, bottom)
                val dx = closestX - playerPosX
                val dy = closestY - playerPosY

                val rayLength = dx * rayDirX + dy * rayDirY
                if (rayLength > 0 && rayLength <= maxRayDistance) {
                    val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
                    if (perpendicularDistance < halfSize + 0.05) {
                        val centerDx = posX - playerPosX
                        val centerDy = posY - playerPosY
                        val angleToEntity = atan2(centerDy, centerDx)
                        val relativeAngle = normalizeAngle(Math.toDegrees(angleToEntity) - currentangle)
                        if (abs(relativeAngle) <= fovCheck) {
                            val angleRatio = relativeAngle / halfFov
                            val screenX = (halfScreenWidth + angleRatio * halfScreenWidth).toInt()
                            if (visibleList.none { it.first === entity }) {
                                visibleList.add(Triple(entity, screenX, rayLength))
                            }
                        }
                    }
                }
            }

            // Process entities
            enemies.forEach { enemy ->
                processEntityVisibility(
                    entity = enemy,
                    x = enemy.x, y = enemy.y, size = enemy.size,
                    visibleList = visibleEnemies as MutableList<Triple<Any, Int, Double>>,
                    rayDirX = rayDirX, rayDirY = rayDirY,
                    playerPosX = playerPosX, playerPosY = playerPosY,
                    currentangle = currentangle.toDouble()
                )
            }

            coinsList.forEach { coin ->
                if (!coin.active) return@forEach
                processEntityVisibility(
                    entity = coin,
                    x = coin.x, y = coin.y, size = coin.size,
                    visibleList = visibleCoins as MutableList<Triple<Any, Int, Double>>,
                    rayDirX = rayDirX, rayDirY = rayDirY,
                    playerPosX = playerPosX, playerPosY = playerPosY,
                    currentangle = currentangle.toDouble()
                )
            }

            keysList.forEach { key ->
                if (!key.active) return@forEach
                processEntityVisibility(
                    entity = key,
                    x = key.x, y = key.y, size = key.size,
                    visibleList = visibleKeys as MutableList<Triple<Any, Int, Double>>,
                    rayDirX = rayDirX, rayDirY = rayDirY,
                    playerPosX = playerPosX, playerPosY = playerPosY,
                    currentangle = currentangle.toDouble()
                )
            }

            chests.forEach { chest ->
                if (!chest.active) return@forEach
                processEntityVisibility(
                    entity = chest,
                    x = chest.x, y = chest.y, size = chest.size,
                    visibleList = visibleChests as MutableList<Triple<Any, Int, Double>>,
                    rayDirX = rayDirX, rayDirY = rayDirY,
                    playerPosX = playerPosX, playerPosY = playerPosY,
                    currentangle = currentangle.toDouble()
                )
            }

            ammo.forEach { ammo ->
                if (!ammo.active) return@forEach
                processEntityVisibility(
                    entity = ammo,
                    x = ammo.x, y = ammo.y, size = ammo.size,
                    visibleList = visibleAmmo as MutableList<Triple<Any, Int, Double>>,
                    rayDirX = rayDirX, rayDirY = rayDirY,
                    playerPosX = playerPosX, playerPosY = playerPosY,
                    currentangle = currentangle.toDouble()
                )
            }

            slotMachines.forEach { slotMachine ->
                if (!slotMachine.active) return@forEach
                processEntityVisibility(
                    entity = slotMachine,
                    x = slotMachine.x, y = slotMachine.y, size = slotMachine.size,
                    visibleList = visibleSlotMachines as MutableList<Triple<Any, Int, Double>>,
                    rayDirX = rayDirX, rayDirY = rayDirY,
                    playerPosX = playerPosX, playerPosY = playerPosY,
                    currentangle = currentangle.toDouble()
                )
            }

            medications.forEach { medication ->
                if (!medication.active) return@forEach
                processEntityVisibility(
                    entity = medication,
                    x = medication.x, y = medication.y, size = medication.size,
                    visibleList = visibleMedications as MutableList<Triple<Any, Int, Double>>,
                    rayDirX = rayDirX, rayDirY = rayDirY,
                    playerPosX = playerPosX, playerPosY = playerPosY,
                    currentangle = currentangle.toDouble()
                )
            }

            traders.forEach { trader ->
                if (!trader.active) return@forEach
                processEntityVisibility(
                    entity = trader,
                    x = trader.x, y = trader.y, size = trader.size,
                    visibleList = visibleTrader as MutableList<Triple<Any, Int, Double>>,
                    rayDirX = rayDirX, rayDirY = rayDirY,
                    playerPosX = playerPosX, playerPosY = playerPosY,
                    currentangle = currentangle.toDouble()
                )
            }
        }

        // Render to screen columns
        for (x in 0 until screenWidth) {
            // Map screen column to ray index
            val rayFraction = x.toDouble() / screenWidth * (rayCount - 1)
            val rayIndex = rayFraction.toInt()
            val nextRayIndex = (rayIndex + 1).coerceAtMost(rayCount - 1)
            val interp = rayFraction - rayIndex

            val distance = if (rayIndex == nextRayIndex) {
                rayDistances[rayIndex]
            } else {
                rayDistances[rayIndex] + (rayDistances[nextRayIndex] - rayDistances[rayIndex]) * interp
            }
            val wallType = rayWallTypes[rayIndex]
            val hitX = rayHitXs[rayIndex] + (rayHitXs[nextRayIndex] - rayHitXs[rayIndex]) * interp
            val hitY = rayHitYs[rayIndex] + (rayHitYs[nextRayIndex] - rayHitYs[rayIndex]) * interp
            val side = raySides[rayIndex]
            val hitWall = rayHitWalls[rayIndex]

            val lineHeight = if (hitWall && distance <= maxRayDistance) {
                ((wallHeight * screenHeight) / (distance * tileSize)).toInt().coerceIn(0, screenHeight * 2)
            } else {
                0
            }
            val drawStart = (-lineHeight / 2 + screenHeight / 2 + horizonOffset).coerceAtLeast(0.0).toInt()
            val drawEnd = (lineHeight / 2 + screenHeight / 2 + horizonOffset).coerceAtMost(screenHeight.toDouble()).toInt()

            if (hitWall && distance <= maxRayDistance) {
                var textureX = if (side == 0) {
                    val blockY = hitY.toInt().toDouble()
                    val relativeY = hitY - blockY
                    relativeY.coerceIn(0.0, 1.0) * textureSize
                } else {
                    val blockX = hitX.toInt().toDouble()
                    val relativeX = hitX - blockX
                    relativeX.coerceIn(0.0, 1.0) * textureSize
                }
                if (side == 0 && (rayCosines[rayIndex] * cos(playerAngleRad) - raySines[rayIndex] * sin(playerAngleRad)) > 0 ||
                    side == 1 && (rayCosines[rayIndex] * sin(playerAngleRad) + raySines[rayIndex] * cos(playerAngleRad)) < 0) {
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
                    buffer.setRGB(x, y, finalColor.rgb)
                }
            }

            for (y in 0 until screenHeight) {
                if (hitWall && distance <= maxRayDistance && y in drawStart until drawEnd) continue

                val isCeiling = y < (screenHeight / 2 + horizonOffset)
                val texture = if (isCeiling) ceilingTexture else floorTexture
                if (texture == null) {
                    buffer.setRGB(x, y, fogColor.rgb)
                    continue
                }

                val rowDistance = if (isCeiling) {
                    (playerHeight * screenHeight / 2) / (10.0 * ((screenHeight / 2.0 + horizonOffset) - y + 0.0))
                } else {
                    (playerHeight * screenHeight / 2) / (10.0 * (y - (screenHeight / 2.0 + horizonOffset) + 0.0))
                }
                if (rowDistance < 0.01 || rowDistance > maxRayDistance) {
                    buffer.setRGB(x, y, fogColor.rgb)
                    continue
                }

                val rayDirX = if (rayIndex == nextRayIndex) {
                    rayCosines[rayIndex] * cos(playerAngleRad) - raySines[rayIndex] * sin(playerAngleRad)
                } else {
                    val dirX1 = rayCosines[rayIndex] * cos(playerAngleRad) - raySines[rayIndex] * sin(playerAngleRad)
                    val dirX2 = rayCosines[nextRayIndex] * cos(playerAngleRad) - raySines[nextRayIndex] * sin(playerAngleRad)
                    dirX1 + (dirX2 - dirX1) * interp
                }
                val rayDirY = if (rayIndex == nextRayIndex) {
                    rayCosines[rayIndex] * sin(playerAngleRad) + raySines[rayIndex] * cos(playerAngleRad)
                } else {
                    val dirY1 = rayCosines[rayIndex] * sin(playerAngleRad) + raySines[rayIndex] * cos(playerAngleRad)
                    val dirY2 = rayCosines[nextRayIndex] * sin(playerAngleRad) + raySines[nextRayIndex] * cos(playerAngleRad)
                    dirY1 + (dirY2 - dirY1) * interp
                }

                val floorX = playerPosX + rowDistance * rayDirX + 100
                val floorY = playerPosY + rowDistance * rayDirY + 100

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
                            val shadowFactor = 1.0 - (distanceToEnemy / shadowRadius)
                            shadowColor = Color(
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255)
                            )
                        }
                    }
                    ammo.forEach { ammo ->
                        val shadowRadius = 0.25
                        val ammoX = ammo.x / tileSize
                        val ammoY = ammo.y / tileSize
                        val dx = floorX - ammoX - 100
                        val dy = floorY - ammoY - 100
                        val distanceToEnemy = sqrt(dx * dx + dy * dy)
                        if (distanceToEnemy <= shadowRadius) {
                            isShadow = true
                            val shadowFactor = 1.0 - (distanceToEnemy / shadowRadius)
                            shadowColor = Color(
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255)
                            )
                        }
                    }
                    chests.forEach { chest ->
                        val shadowRadius = 0.25
                        val chestX = chest.x / tileSize
                        val chestY = chest.y / tileSize
                        val dx = floorX - chestX - 100
                        val dy = floorY - chestY - 100
                        val distanceToEnemy = sqrt(dx * dx + dy * dy)
                        if (distanceToEnemy <= shadowRadius) {
                            isShadow = true
                            val shadowFactor = 1.0 - (distanceToEnemy / shadowRadius)
                            shadowColor = Color(
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255)
                            )
                        }
                    }
                    keysList.forEach { key ->
                        val shadowRadius = 0.25
                        val keyX = key.x / tileSize
                        val keyY = key.y / tileSize
                        val dx = floorX - keyX - 100
                        val dy = floorY - keyY - 100
                        val distanceToEnemy = sqrt(dx * dx + dy * dy)
                        if (distanceToEnemy <= shadowRadius) {
                            isShadow = true
                            val shadowFactor = 1.0 - (distanceToEnemy / shadowRadius)
                            shadowColor = Color(
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255)
                            )
                        }
                    }
                    medications.forEach { medication ->
                        val shadowRadius = 0.25
                        val medicationX = medication.x / tileSize
                        val medicationY = medication.y / tileSize
                        val dx = floorX - medicationX - 100
                        val dy = floorY - medicationY - 100
                        val distanceToEnemy = sqrt(dx * dx + dy * dy)
                        if (distanceToEnemy <= shadowRadius) {
                            isShadow = true
                            val shadowFactor = 1.0 - (distanceToEnemy / shadowRadius)
                            shadowColor = Color(
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255)
                            )
                        }
                    }
                    traders.forEach { trader ->
                        val shadowRadius = 0.25
                        val traderX = trader.x / tileSize
                        val traderY = trader.y / tileSize
                        val dx = floorX - traderX - 100
                        val dy = floorY - traderY - 100
                        val distanceToEnemy = sqrt(dx * dx + dy * dy)
                        if (distanceToEnemy <= shadowRadius) {
                            isShadow = true
                            val shadowFactor = 1.0 - (distanceToEnemy / shadowRadius)
                            shadowColor = Color(
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255)
                            )
                        }
                    }
                    slotMachines.forEach { slotmachine ->
                        val shadowRadius = 0.1
                        val slotmachineX = slotmachine.x / tileSize
                        val slotmachineY = slotmachine.y / tileSize
                        val dx = floorX - slotmachineX - 100
                        val dy = floorY - slotmachineY - 100
                        val distanceToEnemy = sqrt(dx * dx + dy * dy)
                        if (distanceToEnemy <= shadowRadius) {
                            isShadow = true
                            val shadowFactor = 1.0 - (distanceToEnemy / shadowRadius)
                            shadowColor = Color(
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255)
                            )
                        }
                    }
                    coinsList.forEach { coin ->
                        val shadowRadius = 0.1
                        val coinX = coin.x / tileSize
                        val coinY = coin.y / tileSize
                        val dx = floorX - coinX - 100
                        val dy = floorY - coinY - 100
                        val distanceToEnemy = sqrt(dx * dx + dy * dy)
                        if (distanceToEnemy <= shadowRadius) {
                            isShadow = true
                            val shadowFactor = 1.0 - (distanceToEnemy / shadowRadius)
                            shadowColor = Color(
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255),
                                (50 * shadowFactor).toInt().coerceIn(0, 255)
                            )
                        }
                    }
                }
                if (isShadow) {
                    val worldX = playerPosX + rowDistance * rayDirX
                    val worldY = playerPosY + rowDistance * rayDirY
                    val litColor = calculateLightContribution(worldX, worldY, shadowColor)
                    val fogFactor = 1.0 - exp(-fogDensity * rowDistance)
                    val finalColor = Color(
                        ((1.0 - fogFactor) * litColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * litColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                        ((1.0 - fogFactor) * litColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                    )
                    buffer.setRGB(x, y, finalColor.rgb)
                } else {
                    val textureScale = 2.0
                    val textureX = ((floorY / textureScale * textureSize) % textureSize).toInt().coerceIn(0, textureSize - 1)
                    val textureY = ((floorX / textureScale * textureSize) % textureSize).toInt().coerceIn(0, textureSize - 1)

                    val color = texture.getRGB(textureX, textureY)
                    val shadeFactor = (1.0 - (distance / shadeDistanceScale)).coerceIn(minBrightness, maxBrightness)
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
                    buffer.setRGB(x, y, finalColor.rgb)
                }
            }
        }
        renderAllEntities()
    }
    //
    fun clickPerkGUI(mouseX: Int, mouseY: Int) {
        if (!perkGUI) return
        val scaleUI = 150
        val spacing = scaleUI/10
        val heightUI = 400

        if (mouseX in (1366/2)-(scaleUI*3+10) until (1366/2)-scaleUI-10 && mouseY in ((768/2)-(heightUI/2)) until ((768/2)-(heightUI/2))+heightUI) {
            playSound("click.wav")
            println("${perkSlots[0]}")
            selectedPerk(perkSlots[0])
            perkGUI = false
            resetPerkSlot = true
        }

        if (mouseX in (1366/2)-scaleUI until (1366/2)+scaleUI && mouseY in ((768/2)-(heightUI/2)) until ((768/2)-(heightUI/2))+heightUI) {
            playSound("click.wav")
            println("${perkSlots[1]}")
            selectedPerk(perkSlots[1])
            perkGUI = false
            resetPerkSlot = true
        }

        if (mouseX in (1366/2)+scaleUI+spacing until (1366/2)+(scaleUI*3+10) && mouseY in ((768/2)-(heightUI/2)) until ((768/2)-(heightUI/2))+heightUI) {
            playSound("click.wav")
            println("${perkSlots[2]}")
            selectedPerk(perkSlots[2])
            perkGUI = false
            resetPerkSlot = true
        }
    }

    fun selectedPerk(perk: Perk?) {
        if (perk == Perk.HealBoost) {
            println("$HealBoost HB")
            HealBoost += HealBoost * (0.5/4)
            playerHealth += (HealBoost * 100).toInt()
            println("$HealBoost HB")
        }
        if (perk == Perk.SpeedMovement) {
            println("$SpeedMovement SM")
            SpeedMovement += 0.15
            println("$SpeedMovement SM")
        }
        if (perk == Perk.MoreHitShot) {
            println("$MoreHitShot MHS")
            MoreHitShot += MoreHitShot/4
            println("$MoreHitShot MHS")
        }
        if (perk == Perk.FastReload) {
            println("$FastReload FR")
            FastReload -= -0.1
            println("$FastReload FR")
        }
        if (perk == Perk.AmmoBoost) {
            var remainingAmmo = (46 * AmmoBoost).toInt()
            val ammoSlots = playerInventory.indices.filter {
                playerInventory[it]?.type == ItemType.AMMO && playerInventory[it]!!.quantity < Item.MAX_AMMO_PER_SLOT
            }

            for (slot in ammoSlots) {
                if (remainingAmmo <= 0) break
                val currentSlot = playerInventory[slot]!!
                val spaceInSlot = Item.MAX_AMMO_PER_SLOT - currentSlot.quantity
                val ammoToAdd = minOf(remainingAmmo, spaceInSlot)
                currentSlot.quantity += ammoToAdd
                remainingAmmo -= ammoToAdd
            }

            while (remainingAmmo > 0) {
                val emptySlot = playerInventory.indexOfFirst { it == null }
                if (emptySlot == -1) break
                val ammoToAdd = minOf(remainingAmmo, Item.MAX_AMMO_PER_SLOT)
                playerInventory[emptySlot] = Item(ItemType.AMMO, ammoToAdd)
                remainingAmmo -= ammoToAdd
            }
            println("$AmmoBoost AB")
            AmmoBoost += 0.20
            println("$AmmoBoost AB")
        }
    }

    fun renderPerkGUI(g2: Graphics2D) {
        if (!perkGUI) return

        val scaleUI = 150
        val spacing = scaleUI/10
        val heightUI = 400
        val arcSize = 50

        when {
            perkSlots[0] == Perk.SpeedMovement -> g2.color = Color(150, 250, 250, 180)
            perkSlots[0] == Perk.AmmoBoost -> g2.color = Color(255, 127, 0, 180)
            perkSlots[0] == Perk.FastReload -> g2.color = Color(100, 200, 200, 180)
            perkSlots[0] == Perk.MoreHitShot -> g2.color = Color(250, 70, 70, 180)
            perkSlots[0] == Perk.HealBoost -> g2.color = Color(110, 250, 110, 180)
            else -> g2.color = Color(150, 150, 150, 180)
        }
        g2.fillRoundRect((1366/2)-(scaleUI*3+10), 768/2-heightUI/2, scaleUI*2, heightUI, arcSize, arcSize)
        g2.color = Color.WHITE
        g2.font = font?.deriveFont(Font.TYPE1_FONT, 20.toFloat()) ?: Font("Arial", Font.BOLD, 1)
        g2.drawString("${perkSlots[0]}", (1366/2)-(scaleUI*3+10)+80, 768/2+8)

        when {
            perkSlots[1] == Perk.SpeedMovement -> g2.color = Color(150, 250, 250, 180)
            perkSlots[1] == Perk.AmmoBoost -> g2.color = Color(255, 127, 0, 180)
            perkSlots[1] == Perk.FastReload -> g2.color = Color(100, 200, 200, 180)
            perkSlots[1] == Perk.MoreHitShot -> g2.color = Color(250, 70, 70, 180)
            perkSlots[1] == Perk.HealBoost -> g2.color = Color(110, 250, 110, 180)
            else -> g2.color = Color(150, 150, 150, 180)
        }
        g2.fillRoundRect((1366/2)-scaleUI, 768/2-heightUI/2, scaleUI*2, heightUI, arcSize, arcSize)
        g2.color = Color.WHITE
        g2.font = font?.deriveFont(Font.TYPE1_FONT, 20.toFloat()) ?: Font("Arial", Font.BOLD, 1)
        g2.drawString("${perkSlots[1]}", (1366/2)-scaleUI+80, 768/2+8)

        when {
            perkSlots[2] == Perk.SpeedMovement -> g2.color = Color(150, 250, 250, 180)
            perkSlots[2] == Perk.AmmoBoost -> g2.color = Color(255, 127, 0, 180)
            perkSlots[2] == Perk.FastReload -> g2.color = Color(100, 200, 200, 180)
            perkSlots[2] == Perk.MoreHitShot -> g2.color = Color(250, 70, 70, 180)
            perkSlots[2] == Perk.HealBoost -> g2.color = Color(110, 250, 110, 180)
            else -> g2.color = Color(150, 150, 150, 180)
        }
        g2.fillRoundRect((1366/2)+scaleUI+spacing, 768/2-heightUI/2, scaleUI*2, heightUI, arcSize, arcSize)
        g2.color = Color.WHITE
        g2.font = font?.deriveFont(Font.TYPE1_FONT, 20.toFloat()) ?: Font("Arial", Font.BOLD, 1)
        g2.drawString("${perkSlots[2]}", (1366/2)+scaleUI+spacing+80, 768/2+8)
    }

    fun clickInventoryGUI(mouseX: Int, mouseY: Int) {
        if (!inventoryVisible) return

        val scaleUI = 7
        val spacing = scaleUI*2
        val totalSlots = playerInventory.size
        val startX = (1366 - (slotSize + spacing) * 9 - 20)
        val startY = 600 + slotSize
        val chestY = 500 + slotSize

        openChest?.let { chest ->
            for (i in chest.loot.indices) {
                val slotX = startX + i * (slotSize + spacing)
                if (mouseX in slotX until (slotX + slotSize) && mouseY in chestY until (chestY + slotSize)) {
                    val chestItem = chest.loot[i]
                    // Szukaj slotu z tym samym typem przedmiotu w ekwipunku
                    val targetIndex = playerInventory.indexOfFirst { it?.type == chestItem.type && it.quantity < Item.getMaxQuantity(it.type) }
                    if (targetIndex != -1) {
                        // Dodaj do istniejącego slotu
                        val inventoryItem = playerInventory[targetIndex]!!
                        val spaceLeft = Item.getMaxQuantity(inventoryItem.type) - inventoryItem.quantity
                        val transferQuantity = minOf(spaceLeft, chestItem.quantity)
                        inventoryItem.quantity += transferQuantity
                        chestItem.quantity -= transferQuantity
                        if (chestItem.quantity <= 0) {
                            chest.loot.removeAt(i)
                        }
                    } else {
                        // Dodaj do pustego slotu
                        val emptyIndex = playerInventory.indexOfFirst { it == null }
                        if (emptyIndex != -1) {
                            playerInventory[emptyIndex] = Item(chestItem.type, minOf(chestItem.quantity, Item.getMaxQuantity(chestItem.type)))
                            chestItem.quantity -= playerInventory[emptyIndex]!!.quantity
                            if (chestItem.quantity <= 0) {
                                chest.loot.removeAt(i)
                            }
                        }
                    }
                    return
                }
            }
        }


        for (i in 0 until totalSlots) {
            val slotX = startX + i * (slotSize + spacing)
            if (mouseX in slotX until (slotX + slotSize) && mouseY in startY until (startY + slotSize)) {
                val inventoryItem = playerInventory[i]
                if (inventoryItem != null && openChest != null && openChest!!.loot.size < 9) {
                    // Szukaj slotu z tym samym typem przedmiotu w skrzynce
                    val chestTarget = openChest!!.loot.indexOfFirst { it.type == inventoryItem.type && it.quantity < Item.getMaxQuantity(it.type) }
                    if (chestTarget != -1) {
                        // Dodaj do istniejącego slotu w skrzynce
                        val chestItem = openChest!!.loot[chestTarget]
                        val spaceLeft = Item.getMaxQuantity(chestItem.type) - chestItem.quantity
                        val transferQuantity = minOf(spaceLeft, inventoryItem.quantity)
                        chestItem.quantity += transferQuantity
                        inventoryItem.quantity -= transferQuantity
                        if (inventoryItem.quantity <= 0) {
                            playerInventory[i] = null
                        }
                    } else {
                        // Dodaj nowy przedmiot do skrzynki
                        openChest!!.loot.add(Item(inventoryItem.type, minOf(inventoryItem.quantity, Item.getMaxQuantity(inventoryItem.type))))
                        inventoryItem.quantity -= openChest!!.loot.last().quantity
                        if (inventoryItem.quantity <= 0) {
                            playerInventory[i] = null
                        }
                    }
                }
                return
            }
        }
    }

    fun clickTrader(mouseX: Int, mouseY: Int) {
        if (!looktrader) return
        if (mouseX in 870 until 1180 && mouseY in 380 until 420) {
            selectedOfferIndex = 0
            playSound("click.wav")
        }
        if (mouseX in 870 until 1180 && mouseY in 430 until 463) {
            selectedOfferIndex = 1
            playSound("click.wav")
        }
        if (mouseX in 870 until 1180 && mouseY in 486 until 520) {
            selectedOfferIndex = 2
            playSound("click.wav")
        }
        if (mouseX in 870 until 1180 && mouseY in 537 until 574) {
            selectedOfferIndex = 3
            playSound("click.wav")
        }
    }

    fun renderInventoryUI(g2: Graphics2D) {
        val scaleUI = 7
        val spacing = scaleUI * 2
        val startX = 1366 - (slotSize + spacing) * 9 - 20
        val startY = 600
        val totalSlots = playerInventory.size

        g2.color = Color(50, 50, 50, 180)
        g2.fillRoundRect(
            startX - scaleUI,
            startY - scaleUI,
            (slotSize + spacing) * totalSlots,
            slotSize + (scaleUI * 2),
            20,
            20
        )

        g2.color = Color(150, 150, 150, 180)
        g2.fillRoundRect(
            (startX - scaleUI) + ((slotSize + spacing) * selectSlot),
            startY - scaleUI,
            (slotSize + spacing),
            slotSize + (scaleUI * 2),
            20,
            20
        )

        if (!lookchest and !looktrader and !lookslotMachine) {
            inventoryVisible = false
        }

        for (i in 0 until totalSlots) {
            val x = startX + i * (slotSize + spacing)
            val y = startY
            g2.color = Color(100, 100, 100)
            g2.fillRoundRect(x, y, slotSize, slotSize, 17, 17)
            playerInventory[i]?.let { item ->
                if ((item.type != ItemType.AMMO) and (item.type != ItemType.COIN) and (item.type != ItemType.MEDKIT)) {
                    g2.drawImage(getItemTexture(item.type), x, y, (slotSize), (slotSize), null)
                } else {
                    g2.drawImage(
                        getItemTexture(item.type),
                        x + (slotSize / 5),
                        y + (slotSize / 5),
                        (slotSize / 4) * 3,
                        (slotSize / 4) * 3,
                        null
                    )
                }
                g2.color = Color(230, 230, 230)
                g2.font = font?.deriveFont(Font.TYPE1_FONT, 16.toFloat()) ?: Font("Arial", Font.BOLD, 1)
                if (item.quantity > 9) {
                    g2.drawString("${item.quantity}", x + slotSize - 20, y + slotSize - 5)
                } else {
                    g2.drawString("  ${item.quantity}", x + slotSize - 20, y + slotSize - 5)
                }
            }
        }

        if (!inventoryVisible) return

        // GUI chest
        if (lookchest){
            openChest?.let { chest ->
                val chestY = 500
                g2.color = Color(50, 50, 50, 220)
                g2.fillRoundRect(startX - 10, chestY - 10, (slotSize + spacing) * totalSlots + 20, slotSize + 20, 20, 20)

                for (i in chest.loot.indices) {
                    val x = startX + i * (slotSize + spacing)
                    val y = chestY
                    g2.color = Color(100, 100, 100)
                    g2.fillRoundRect(x, y, slotSize, slotSize, 17, 17)
                    if ((chest.loot[i].type != ItemType.AMMO) and (chest.loot[i].type != ItemType.COIN) and (chest.loot[i].type != ItemType.MEDKIT)) {
                        g2.drawImage(getItemTexture(chest.loot[i].type), x, y, (slotSize), (slotSize), null)
                    } else {
                        g2.drawImage(
                            getItemTexture(chest.loot[i].type),
                            x + (slotSize / 5),
                            y + (slotSize / 5),
                            (slotSize / 4) * 3,
                            (slotSize / 4) * 3,
                            null
                        )
                    }
                    // Wyświetl ilość
                    g2.color = Color(230, 230, 230)
                    g2.font = font?.deriveFont(Font.TYPE1_FONT, 16.toFloat()) ?: Font("Arial", Font.BOLD, 1)
                    if (chest.loot[i].quantity > 9) {
                        g2.drawString("${chest.loot[i].quantity}", x + slotSize - 20, y + slotSize - 5)
                    } else {
                        g2.drawString("  ${chest.loot[i].quantity}", x + slotSize - 20, y + slotSize - 5)
                    }
                }
            }
        }

        // GUI trader
        if (looktrader) {
            openTrader?.let { trade ->
                val chestY = 500
                g2.color = Color(50, 50, 50, 220)
                g2.fillRoundRect(startX - 10, chestY-(slotSize + 20)*3, (slotSize + spacing) * totalSlots + 20, (slotSize + 20)*4, 20 ,20)
                g2.font = font?.deriveFont(Font.TYPE1_FONT, 16.toFloat()) ?: Font("Arial", Font.BOLD, 1)

                trade.offer.forEachIndexed { index, item ->
                    g2.color = Color.YELLOW
                    val yPos = (chestY - 3 * (slotSize + spacing)) + index * (slotSize + spacing)
                    val itemText = "${item.type} (x${item.quantity}) - ${trade.prices[index]} COINs"
                    g2.drawString(itemText, startX+100, yPos+20)
                }

                if (selectedOfferIndex in 0 until trade.offer.size) {
                    g2.color = Color.YELLOW
                    val yPos = (chestY - 3 * (slotSize + spacing))+ selectedOfferIndex * (slotSize + spacing)
                    g2.drawRect((startX - 10)+100, (yPos - 20)+20, 225, 30)
                }

                for (i in trade.offer.indices) {
                    val x = startX //+ i * (slotSize + spacing)
                    val y = (chestY - 3 * (slotSize + spacing))+ i * (slotSize + spacing)
                    g2.color = Color(100, 100, 100)
                    g2.fillRoundRect(x, y, slotSize, slotSize, 17, 17)
                    if ((trade.offer[i].type!= ItemType.AMMO) and (trade.offer[i].type!= ItemType.COIN) and (trade.offer[i].type!= ItemType.MEDKIT)){
                        g2.drawImage(getItemTexture(trade.offer[i].type), x, y, (slotSize), (slotSize), null)}
                    else {
                        g2.drawImage(getItemTexture(trade.offer[i].type), x + (slotSize/5), y + (slotSize/5), (slotSize/4)*3, (slotSize/4)*3, null)
                    }
                    g2.color = Color(230, 230, 230)
                    g2.font = font?.deriveFont(Font.TYPE1_FONT, 16.toFloat()) ?: Font("Arial", Font.BOLD, 1)
                    if (trade.offer[i].quantity > 9) {
                        g2.drawString("${trade.offer[i].quantity}", x + slotSize - 20, y + slotSize - 5)} else {
                        g2.drawString("  ${trade.offer[i].quantity}", x + slotSize - 20, y + slotSize - 5)
                    }
                }
            }
        }
    }

    fun getItemTexture(type: ItemType): BufferedImage {
        try {
            return when (type) {
                ItemType.MEDKIT -> ImageIO.read(this::class.java.classLoader.getResource("textures/medication.png"))
                ItemType.AMMO -> ImageIO.read(this::class.java.classLoader.getResource("textures/ammo.png"))
                ItemType.KEY -> ImageIO.read(this::class.java.classLoader.getResource("textures/key.png"))
                ItemType.COIN -> ImageIO.read(this::class.java.classLoader.getResource("textures/coin.png"))
            }
        } catch (e: Exception) {
            return when (type) {
                ItemType.MEDKIT -> createTexture(Color(20,220,20))
                ItemType.AMMO -> createTexture(Color(80,80,80))
                ItemType.KEY -> createTexture(Color(255, 215, 0))
                ItemType.COIN -> createTexture(Color(255, 255, 0))
            }
        }
    }

    fun updateOpenChest() {
        val playerPosX = positionX / tileSize
        val playerPosY = positionY / tileSize
        val rayDirX = cos(Math.toRadians(currentangle.toDouble()))
        val rayDirY = sin(Math.toRadians(currentangle.toDouble()))
        val shotAngleRad = atan2(rayDirY, rayDirX)

        val closestChest = chests.minByOrNull { chest ->
            val dx = chest.x / tileSize - playerPosX
            val dy = chest.y / tileSize - playerPosY
            val rayLength = dx * rayDirX + dy * rayDirY

            if (rayLength <= 0) return@minByOrNull Double.MAX_VALUE

            val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
            if (perpendicularDistance >= 0.5) return@minByOrNull Double.MAX_VALUE

            val angleToChest = atan2(dy, dx)
            var angleDiff = abs(angleToChest - shotAngleRad)
            angleDiff = min(angleDiff, 2 * Math.PI - angleDiff)

            if (angleDiff < Math.toRadians(35.0 / 3)) {
                rayLength
            } else {
                Double.MAX_VALUE
            }
        }

        openChest = closestChest?.takeIf { chest ->
            val dx = chest.x - positionX
            val dy = chest.y - positionY
            sqrt(dx * dx + dy * dy) < tileSize * chest.pickupDistance
        }
    }

    fun purchaseItem(trader: Trader, offerIndex: Int): Boolean {
        if (offerIndex !in 0 until trader.offer.size) return false

        val itemToBuy = trader.offer[offerIndex]
        val price = trader.prices[offerIndex]
        val totalCoins = getTotalCoins()

        if (totalCoins >= price) {
            if (removeCoins(price)) {
                if (addItemToInventory(Item(itemToBuy.type, itemToBuy.quantity))) {
                    return true
                } else {
                    addItemToInventory(Item(ItemType.COIN, price))
                    return false
                }
            }
        }
        return false
    }

    fun addItemToInventory(item: Item): Boolean {
        for (i in playerInventory.indices) {
            val slot = playerInventory[i]
            if (slot != null && slot.type == item.type && slot.quantity < Item.getMaxQuantity(slot.type)) {
                val spaceAvailable = Item.getMaxQuantity(slot.type) - slot.quantity
                if (spaceAvailable >= item.quantity) {
                    slot.quantity += item.quantity
                    return true
                }
            }
        }
        for (i in playerInventory.indices) {
            if (playerInventory[i] == null) {
                playerInventory[i] = Item(item.type, item.quantity)
                return true
            }
        }
        return false
    }

    fun removeCoins(quantity: Int): Boolean {
        var remaining = quantity
        for (slot in playerInventory) {
            if (slot != null && slot.type == ItemType.COIN && slot.quantity > 0) {
                if (slot.quantity >= remaining) {
                    slot.quantity -= remaining
                    if (slot.quantity == 0) {
                        val index = playerInventory.indexOf(slot)
                        playerInventory[index] = null
                    }
                    return true
                } else {
                    remaining -= slot.quantity
                    val index = playerInventory.indexOf(slot)
                    playerInventory[index] = null
                }
            }
        }
        return false
    }

    fun getTotalCoins(): Int {
        return playerInventory.filterNotNull()
            .filter { it.type == ItemType.COIN }
            .sumOf { it.quantity }
    }

    fun updateOpenTrader() {
        val playerPosX = positionX / tileSize
        val playerPosY = positionY / tileSize
        val rayDirX = cos(Math.toRadians(currentangle.toDouble()))
        val rayDirY = sin(Math.toRadians(currentangle.toDouble()))
        val shotAngleRad = atan2(rayDirY, rayDirX)

        val closestTrader = traders.minByOrNull { trader ->
            val dx = trader.x / tileSize - playerPosX
            val dy = trader.y / tileSize - playerPosY
            val rayLength = dx * rayDirX + dy * rayDirY

            if (rayLength <= 0) return@minByOrNull Double.MAX_VALUE

            val perpendicularDistance = abs(dx * rayDirY - dy * rayDirX)
            if (perpendicularDistance >= 0.5) return@minByOrNull Double.MAX_VALUE

            val angleToChest = atan2(dy, dx)
            var angleDiff = abs(angleToChest - shotAngleRad)
            angleDiff = min(angleDiff, 2 * Math.PI - angleDiff)

            if (angleDiff < Math.toRadians(35*3.0)) {
                rayLength
            } else {
                Double.MAX_VALUE
            }
        }

        openTrader = closestTrader?.takeIf { trader ->
            val dx = trader.x - positionX
            val dy = trader.y - positionY
            sqrt(dx * dx + dy * dy) < tileSize * trader.pickupDistance
        }
    }

    data class EntityRenderConfig(
        val height: Double,
        val sizeMultiplier: Double = 1.0,
        val maxSize: Double = 64.0,
        val hasInteraction: Boolean = false,
        val hasHealthBar: Boolean = false,
        val shadowScale: Double = 1.2,
        val shadowVerticalScale: Double = 0.25
    )

    private fun renderEntity(entity: Any, screenX: Double, distance: Double, texture: BufferedImage, config: EntityRenderConfig, zBuffer: DoubleArray, buffer: BufferedImage) {
        val minSize = 0.001
        val spriteSize = ((config.height * screenHeight) / (distance * tileSize) * config.sizeMultiplier)
            .coerceIn(minSize, config.maxSize).toInt()

        // Calculate floor position for shadow and sprite
        val floorY = (screenHeight / 2 + (wallHeight * screenHeight) / (2 * distance * tileSize)).toInt()
        val drawStartY = (floorY - spriteSize).coerceIn(0, screenHeight - 1)
        val drawEndY = floorY.coerceIn(0, screenHeight - 1)
        val leftX = screenX - spriteSize / 2.0
        val rightX = screenX + spriteSize / 2.0
        val drawStartX = leftX.coerceAtLeast(0.0).toInt()
        val drawEndX = rightX.coerceAtMost(screenWidth - 1.0).toInt()

        val halfSize = when (entity) {
            is Enemy -> entity.size * tileSize / 2 / tileSize
            is Coin -> entity.size * tileSize / 2 / tileSize
            is Key -> entity.size * tileSize / 2 / tileSize
            is Chest -> entity.size * tileSize / 2 / tileSize
            is Medication -> entity.size * tileSize / 2 / tileSize
            is Ammo -> entity.size * tileSize / 2 / tileSize
            is Trader -> entity.size * tileSize / 2 / tileSize
            is SlotMachine -> entity.size * tileSize / 2 / tileSize
            else -> 0.5
        }
        val entityX = when (entity) {
            is Enemy -> entity.x / tileSize
            is Coin -> entity.x / tileSize
            is Key -> entity.x / tileSize
            is Chest -> entity.x / tileSize
            is Medication -> entity.x / tileSize
            is Ammo -> entity.x / tileSize
            is Trader -> entity.x / tileSize
            is SlotMachine -> entity.x / tileSize
            else -> 0.0
        }
        val entityY = when (entity) {
            is Enemy -> entity.y / tileSize
            is Coin -> entity.y / tileSize
            is Key -> entity.y / tileSize
            is Chest -> entity.y / tileSize
            is Medication -> entity.y / tileSize
            is Ammo -> entity.y / tileSize
            is Trader -> entity.y / tileSize
            is SlotMachine -> entity.y / tileSize
            else -> 0.0
        }

        // Handle interaction (unchanged from original)
        var interactionFlag: Boolean? = null
        if (config.hasInteraction) {
            val playerPosX = positionX / tileSize
            val playerPosY = positionY / tileSize
            val (entityPosX, entityPosY, pickupDistance) = when (entity) {
                is Chest -> Triple(entity.x / tileSize, entity.y / tileSize, entity.pickupDistance)
                is Trader -> Triple(entity.x / tileSize, entity.y / tileSize, entity.pickupDistance)
                is SlotMachine -> Triple(entity.x / tileSize, entity.y / tileSize, entity.pickupDistance)
                else -> Triple(0.0, 0.0, 0.0)
            }
            val dx = entityPosX - playerPosX
            val dy = entityPosY - playerPosY
            val rayLength = sqrt(dx * dx + dy * dy)
            if (rayLength <= pickupDistance) {
                val rayDirX = dx / rayLength
                val rayDirY = dy / rayLength
                val shotAngleRad = Math.toRadians(currentangle.toDouble())
                val angleToEntity = atan2(dy, dx)
                var angleDiff = abs(angleToEntity - shotAngleRad)
                angleDiff = min(angleDiff, 2 * Math.PI - angleDiff)

                if (angleDiff < Math.toRadians(15.0)) {
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

                    if (!hitWall || rayLength < wallDistance) {
                        interactionFlag = when (entity) {
                            is Chest -> true.also { lookchest = true }
                            is Trader -> true.also { looktrader = true }
                            is SlotMachine -> true.also { lookslotMachine = true }
                            else -> false
                        }
                    }
                }
            }
        }

        // Render sprite (from renderKeys, with per-column depth)
        for (x in drawStartX until drawEndX) {
            if (x < 0 || x >= zBuffer.size) continue
            // Calculate per-column depth
            val angleRatio = (x - screenWidth / 2.0) / (screenWidth / 2.0) * (fov / 2)
            val rayAngleRad = Math.toRadians(currentangle + angleRatio)
            val rayDirX = cos(rayAngleRad)
            val rayDirY = sin(rayAngleRad)
            val entityLeft = entityX - halfSize
            val entityRight = entityX + halfSize
            val entityTop = entityY - halfSize
            val entityBottom = entityY + halfSize
            val closestX = clamp(entityX, entityLeft, entityRight)
            val closestY = clamp(entityY, entityTop, entityBottom)
            val dx = closestX - positionX/tileSize
            val dy = closestY - positionY/tileSize
            val pixelDistance = sqrt(dx * dx + dy * dy)

            if (pixelDistance < zBuffer[x]) {
                val textureFraction = (x - leftX) / (rightX - leftX)
                val textureX = (textureFraction * texture.width).coerceIn(0.0, texture.width - 1.0)
                for (y in drawStartY until drawEndY) {
                    val textureY = ((y - drawStartY).toDouble() * texture.height / spriteSize)
                        .coerceIn(0.0, texture.height - 1.0)
                    val color = texture.getRGB(textureX.toInt(), textureY.toInt())
                    if ((color and 0xFF000000.toInt()) != 0) {
                        val originalColor = Color(color)
                        val litColor = calculateLightContribution(entityX, entityY, originalColor)
                        val fogFactor = 1.0 - exp(-fogDensity * pixelDistance)
                        val finalColor = Color(
                            ((1.0 - fogFactor) * litColor.red + fogFactor * fogColor.red).toInt().coerceIn(0, 255),
                            ((1.0 - fogFactor) * litColor.green + fogFactor * fogColor.green).toInt().coerceIn(0, 255),
                            ((1.0 - fogFactor) * litColor.blue + fogFactor * fogColor.blue).toInt().coerceIn(0, 255)
                        )
                        buffer.setRGB(x, y, finalColor.rgb)
                    }
                }
                zBuffer[x] = pixelDistance // Update z-buffer with per-column depth
            }
        }

        // Render health bar (unchanged from original)
        if (config.hasHealthBar && entity is Enemy && entity.health > 0 && entity.maxHeal > entity.health) {
            val healthText = "${entity.health}"
            val textSize = (spriteSize / 3.0).coerceIn(16.0, 26.0).toInt()
            val textFont = font?.deriveFont(Font.BOLD, textSize.toFloat()) ?: Font("Arial", Font.BOLD, textSize)
            val metrics = bufferGraphics.getFontMetrics(textFont)
            val textWidth = metrics.stringWidth(healthText)
            val textHeight = metrics.height

            val textImage = BufferedImage(textWidth, textHeight, BufferedImage.TYPE_INT_ARGB)
            val textGraphics = textImage.createGraphics()
            textGraphics.font = textFont
            textGraphics.color = Color.LIGHT_GRAY
            textGraphics.setBackground(Color(0, 0, 0, 0))
            textGraphics.drawString(healthText, 0, metrics.ascent)
            textGraphics.dispose()

            val textSpriteWidth = (spriteSize / 2.0).coerceIn(4.0, maxOf(spriteSize.toDouble(), 4.0)).toInt()
            val textSpriteHeight = if (textWidth > 0) {
                (textSpriteWidth * (textHeight.toDouble() / textWidth)).toInt().coerceAtLeast(1)
            } else {
                1
            }
            val textDrawStartY = (drawStartY - textSpriteHeight - 2).coerceIn(0, screenHeight - 1)
            val textDrawEndY = (textDrawStartY + textSpriteHeight).coerceIn(0, screenHeight - 1)
            val textLeftX = screenX - textSpriteWidth / 2.0
            val textRightX = screenX + textSpriteWidth / 2.0
            val textDrawStartX = textLeftX.coerceAtLeast(0.0).toInt()
            val textDrawEndX = textRightX.coerceAtMost(screenWidth - 1.0).toInt()

            for (x in textDrawStartX until textDrawEndX) {
                if (x < 0 || x >= zBuffer.size) continue
                if (distance <= zBuffer[x]) {
                    val textTextureFraction = (x - textLeftX) / (textRightX - textLeftX)
                    val textTextureX = (textTextureFraction * textImage.width).coerceIn(0.0, textImage.width - 1.0)
                    for (y in textDrawStartY until textDrawEndY) {
                        val textTextureY = ((y - textDrawStartY).toDouble() * textImage.height / textSpriteHeight)
                            .coerceIn(0.0, textImage.height - 1.0)
                        val color = textImage.getRGB(textTextureX.toInt(), textTextureY.toInt())
                        if ((color and 0xFF000000.toInt()) != 0) {
                            val originalColor = Color(color)
                            val litColor = calculateLightContribution(entityX, entityY, originalColor)
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

    private fun renderAllEntities() {
        val configMap = mapOf(
            Chest::class to EntityRenderConfig(
                height = wallHeight / 4,
                sizeMultiplier = 1.7,
                maxSize = 64.0,
                hasInteraction = true,
                shadowScale = 1.2,
                shadowVerticalScale = 0.25
            ),
            Enemy::class to EntityRenderConfig(
                height = wallHeight / 2,
                sizeMultiplier = 1.0,
                maxSize = 128.0 * 2,
                hasHealthBar = true,
                shadowScale = 1.2,
                shadowVerticalScale = 0.25
            ),
            Key::class to EntityRenderConfig(
                height = wallHeight / 4,
                sizeMultiplier = 1.0,
                maxSize = 64.0, // From renderKeys
                shadowScale = 1.2, // From renderKeys
                shadowVerticalScale = 0.25 // From renderKeys (shadowSize / 4)
            ),
            Coin::class to EntityRenderConfig(
                height = wallHeight / 4,
                sizeMultiplier = 1.0,
                maxSize = 64.0,
                shadowScale = 1.2,
                shadowVerticalScale = 0.25
            ),
            Medication::class to EntityRenderConfig(
                height = wallHeight / 4,
                sizeMultiplier = 1.0,
                maxSize = 64.0,
                shadowScale = 1.2,
                shadowVerticalScale = 0.25
            ),
            Ammo::class to EntityRenderConfig(
                height = wallHeight / 4,
                sizeMultiplier = 1.0,
                maxSize = 64.0,
                shadowScale = 1.2,
                shadowVerticalScale = 0.25
            ),
            Trader::class to EntityRenderConfig(
                height = wallHeight / 4,
                sizeMultiplier = 3.1,
                maxSize = 64.0,
                hasInteraction = true,
                shadowScale = 1.2,
                shadowVerticalScale = 0.25
            ),
            SlotMachine::class to EntityRenderConfig(
                height = wallHeight / 4,
                sizeMultiplier = 2.2,
                maxSize = 64.0,
                hasInteraction = true,
                shadowScale = 1.2,
                shadowVerticalScale = 0.25
            )
        )

        val allEntities = mutableListOf<Triple<Any, Double, Double>>()
        allEntities.addAll(visibleChests.map { Triple(it.first, it.second.toDouble(), it.third) })
        allEntities.addAll(visibleEnemies.map { Triple(it.first, it.second.toDouble(), it.third) })
        allEntities.addAll(visibleKeys.map { Triple(it.first, it.second.toDouble(), it.third) })
        allEntities.addAll(visibleCoins.map { Triple(it.first, it.second.toDouble(), it.third) })
        allEntities.addAll(visibleMedications.map { Triple(it.first, it.second.toDouble(), it.third) })
        allEntities.addAll(visibleAmmo.map { Triple(it.first, it.second.toDouble(), it.third) })
        allEntities.addAll(visibleTrader.map { Triple(it.first, it.second.toDouble(), it.third) })
        allEntities.addAll(visibleSlotMachines.map { Triple(it.first, it.second.toDouble(), it.third) })

        allEntities.sortByDescending { it.third }

        val zBuffer = DoubleArray(screenWidth) { Double.POSITIVE_INFINITY }

        allEntities.forEach { (entity, screenX, distance) ->
            val texture = when (entity) {
                is Chest -> chestTextureID ?: return@forEach
                is Enemy -> entity.texture
                is Key -> entity.texture
                is Coin -> entity.texture
                is Medication -> entity.texture
                is Ammo -> entity.texture
                is Trader -> entity.texture
                is SlotMachine -> slotMachineTextureID ?: return@forEach
                else -> return@forEach
            }
            val config = configMap[entity::class] ?: return@forEach
            renderEntity(entity, screenX, distance, texture, config, zBuffer, buffer)
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

    private val shadowTexture: BufferedImage by lazy {
        val size = 16
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color(0, 0, 0, 128) // 50% opacity, matching enemy shadow
        g.fillOval(0, 0, size, size)
        g.dispose()
        image
    }

    fun shotgun() {
        val currentTime = System.nanoTime()
        val ammoSlot = playerInventory.indexOfFirst { it?.type == ItemType.AMMO && it.quantity > 0 }
        if (currentTime - lastShotTime >= SHOT_COOLDOWN && !isShooting && ammoSlot != -1) {
            playerInventory[ammoSlot]!!.quantity -= 1
            if (playerInventory[ammoSlot]!!.quantity <= 0) {
                playerInventory[ammoSlot] = null
            }
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

                        if (angleDiff < Math.toRadians(35.0/3)) {
                            enemy.health -= (25 * MoreHitShot).toInt()
                            println("enemy: ${enemy} heal: ${enemy.health}")
                            if (enemy.health <= 0) {
                                val spawnRadius = 1.0 * tileSize
                                val loots = when {
                                    random < 0.50f -> 1
                                    random < 0.80f -> 2
                                    else -> 3
                                }

                                for (i in 1..loots) {
                                    var itemX = enemy.x
                                    var itemY = enemy.y
                                    var validPosition = false
                                    val maxAttempts = 10

                                    for (attempt in 0 until maxAttempts) {
                                        val randomAngle = Random.nextDouble(0.0, 2 * PI)
                                        val randomDistance = Random.nextDouble(0.2 * spawnRadius, spawnRadius)
                                        itemX = enemy.x + randomDistance * cos(randomAngle)
                                        itemY = enemy.y + randomDistance * sin(randomAngle)

                                        val mapX = (itemX / tileSize).toInt()
                                        val mapY = (itemY / tileSize).toInt()

                                        if (mapY in map.grid.indices && mapX in map.grid[0].indices && accessibleTiles.contains(map.grid[mapY][mapX])) {
                                            val tooClose = keysList.any { existingKey ->
                                                if (!existingKey.active) false
                                                else {
                                                    val dx = itemX - existingKey.x
                                                    val dy = itemY - existingKey.y
                                                    sqrt(dx * dx + dy * dy) < 0.3 * tileSize
                                                }
                                            } || ammo.any { existingAmmo ->
                                                if (!existingAmmo.active) false
                                                else {
                                                    val dx = itemX - existingAmmo.x
                                                    val dy = itemY - existingAmmo.y
                                                    sqrt(dx * dx + dy * dy) < 0.3 * tileSize
                                                }
                                            }
                                            if (!tooClose) {
                                                validPosition = true
                                                break
                                            }
                                        }
                                    }
                                    if (!validPosition) {
                                        itemX = enemy.x
                                        itemY = enemy.y
                                    }

                                    val randomItem = when {
                                        random < 0.80f -> keysList.add(Key(x = itemX, y = itemY, texture = keyTextureId!!))
                                        random  < 0.90f -> medications.add(Medication(x = itemX, y = itemY, texture = medicationTextureID!!, heal = 35))
                                        else -> ammo.add(Ammo(x = itemX, y = itemY, texture = ammoTextureID!!))
                                    }
                                    randomItem
                                }
                                points = points + (100 / level)
                                if (points >= 100) {
                                    playSound("levelup.wav", volume = 0.65f)
                                    var levelUpTimer: Timer? = null
                                    val availablePerks = Perk.values().toMutableList()
                                    if (resetPerkSlot) {
                                        resetPerkSlot = false
                                        repeat(3) {
                                            val randomPerk = availablePerks.random()
                                            perkSlots[it] = randomPerk
                                            availablePerks.remove(randomPerk)
                                        }
                                    }
                                    perkGUI = true
                                    levelUp = true
                                    level += 1
                                    if (level == 2) { points = 0 } else{ points -= 100 }
                                    levelUpTimer?.stop()
                                    levelUpTimer = Timer(2500, {
                                        levelUp = false
                                        levelUpTimer?.stop()
                                    }).apply {
                                        isRepeats = false
                                        start()
                                    }
                                }
                                println("level: $level points: $points keys: $keys ammo: $currentAmmo")
                                playSound(when {
                                    random < 0.16f -> "scream1.wav"
                                    random < 0.32f -> "scream2.wav"
                                    random < 0.48f -> "scream3.wav"
                                    random < 0.64f -> "scream4.wav"
                                    random < 0.80f -> "scream5.wav"
                                    else -> "scream6.wav"
                                }, volume = 0.65f)
                                try {
                                    lightSources.removeIf { it.owner == "$enemy" }
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

    private fun update() {
        enemies.forEach { it.update() }
        enemies.forEach { it.renderProjectiles(bufferGraphics as Graphics2D) }
        keysList.forEach { key ->
            if (key.active) {
                val dx = positionX - key.x
                val dy = positionY - key.y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < key.pickupDistance) {
                    var remainingKeys = key.amount

                    val keysSlots = playerInventory.indices.filter {
                        playerInventory[it]?.type == ItemType.KEY && playerInventory[it]!!.quantity < Item.MAX_KEYS_PER_SLOT
                    }

                    for (slot in keysSlots) {
                        if (remainingKeys <= 0) break
                        val currentSlot = playerInventory[slot]!!
                        val spaceInSlot = Item.MAX_KEYS_PER_SLOT - currentSlot.quantity
                        val keyToAdd = minOf(remainingKeys, spaceInSlot)
                        currentSlot.quantity += keyToAdd
                        remainingKeys -= keyToAdd
                        key.active = false
                        playSound("8exp.wav", 0.65f)
                    }

                    while (remainingKeys > 0) {
                        val emptySlot = playerInventory.indexOfFirst { it == null }
                        if (emptySlot == -1) break
                        val keyToAdd = minOf(remainingKeys, Item.MAX_KEYS_PER_SLOT)
                        playerInventory[emptySlot] = Item(ItemType.KEY, keyToAdd)
                        remainingKeys -= keyToAdd
                        key.active = false
                        playSound("8exp.wav", 0.65f)
                    }
                }
            }
        }
        coinsList.forEach { coin ->
            if (coin.active) {
                val dx = positionX - coin.x
                val dy = positionY - coin.y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < coin.pickupDistance) {
                    var remainingCoins = coin.amount

                    val coinsSlots = playerInventory.indices.filter {
                        playerInventory[it]?.type == ItemType.COIN && playerInventory[it]!!.quantity < Item.MAX_COINS_PER_SLOT
                    }

                    for (slot in coinsSlots) {
                        if (remainingCoins <= 0) break
                        val currentSlot = playerInventory[slot]!!
                        val spaceInSlot = Item.MAX_KEYS_PER_SLOT - currentSlot.quantity
                        val coinToAdd = minOf(remainingCoins, spaceInSlot)
                        currentSlot.quantity += coinToAdd
                        remainingCoins -= coinToAdd
                        coin.active = false
                        playSound("8exp.wav", 0.65f)
                    }

                    while (remainingCoins > 0) {
                        val emptySlot = playerInventory.indexOfFirst { it == null }
                        if (emptySlot == -1) break
                        val coinToAdd = minOf(remainingCoins, Item.MAX_COINS_PER_SLOT)
                        playerInventory[emptySlot] = Item(ItemType.COIN, coinToAdd)
                        remainingCoins -= coinToAdd
                        coin.active = false
                        playSound("8exp.wav", 0.65f)
                    }
                }
            }
        }
        medications.forEach { medication ->
            if (medication.active) {
                val dx = positionX - medication.x
                val dy = positionY - medication.y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < medication.pickupDistance) {
                    medication.active = false
                    playerHealth = playerHealth + medication.heal
                    playSound("8exp.wav", 0.65f)
                }
            }
        }
        ammo.forEach { ammo ->
            if (ammo.active) {
                val dx = positionX - ammo.x
                val dy = positionY - ammo.y
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < ammo.pickupDistance) {
                    var remainingAmmo = (ammo.amount * AmmoBoost).toInt()

                    val ammoSlots = playerInventory.indices.filter {
                        playerInventory[it]?.type == ItemType.AMMO && playerInventory[it]!!.quantity < Item.MAX_AMMO_PER_SLOT
                    }

                    for (slot in ammoSlots) {
                        if (remainingAmmo <= 0) break
                        val currentSlot = playerInventory[slot]!!
                        val spaceInSlot = Item.MAX_AMMO_PER_SLOT - currentSlot.quantity
                        val ammoToAdd = minOf(remainingAmmo, spaceInSlot)
                        currentSlot.quantity += ammoToAdd
                        remainingAmmo -= ammoToAdd
                        ammo.active = false
                        playSound("8exp.wav", 0.65f)
                    }

                    while (remainingAmmo > 0) {
                        val emptySlot = playerInventory.indexOfFirst { it == null }
                        if (emptySlot == -1) break // No more empty slots
                        val ammoToAdd = minOf(remainingAmmo, Item.MAX_AMMO_PER_SLOT)
                        playerInventory[emptySlot] = Item(ItemType.AMMO, ammoToAdd)
                        remainingAmmo -= ammoToAdd
                        ammo.active = false
                        playSound("8exp.wav", 0.65f)
                    }
                }
            }
        }
        coinsList.removeIf { !it.active }
        keysList.removeIf { !it.active }
        medications.removeIf { !it.active }
        ammo.removeIf { !it.active }
    }
}
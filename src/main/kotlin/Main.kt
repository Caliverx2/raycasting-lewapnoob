package org.example.MainKt

//I announce that this week a new chapter for this project will begin. New types of enemies, weapons, level system
//./gradlew shadowJar
//0-air 1-wall 2-black_wall 3-enemy 4-ammo 5-door 6-lightSource 7-medication 8-key 9-trader 10-chest 11-slotMachine 12-closedDoor

import kotlinx.coroutines.runBlocking
import javax.swing.JFrame
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.Color
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

var playerHealth: Int = 100
var playerDamage: Int = 25
var level: Int = 1
var points: Int = 0
var coins: Int = 0
var keys: Int = 2
var selectSlot: Int = 1
var selectWeaponSlot: Int = 1
var selectedOfferIndex: Int = 0
var activateSlot: Boolean = false
var perkGUI: Boolean = false
val maxRayDistance: Double = 22.0
var shotAccuracy: Int = 5

var godMode: Boolean = false
var unlimitedAmmo: Boolean = false
var noClip: Boolean = false
var oneHitKills: Boolean = false

var map: Boolean = true
var currentangle: Int = 45
var tileSize: Double = 40.0
val mapa: Double = 0.5
var MouseSupport: Boolean = false

var HealBoost: Double = 1.0
var SpeedMovement: Double = 1.0
var MoreHitShot: Double = 1.0
var FastReload: Double = 1.0
var AmmoBoost: Double = 1.0

val TARGET_FPS: Int = 90
val FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS
var deltaTime = 1.0 / 60.0

var positionX: Double = (tileSize*11)-(tileSize/2)  //tile*positon - (half tile)
var positionY: Double = (tileSize*11)-(tileSize/2)  //tile*positon - (half tile)
var enemies = mutableListOf<Enemy>()
var lightSources = mutableListOf<LightSource>()
var keysList = mutableListOf<Key>()
var coinsList = mutableListOf<Coin>()
var projectiles = mutableListOf<Enemy.Projectile>()
var medications = mutableListOf<Medication>()
var chests = mutableListOf<Chest>()
var ammo = mutableListOf<Ammo>()
var traders = mutableListOf<Trader>()
var slotMachines = mutableListOf<SlotMachine>()
var glock34s = mutableListOf<Glock34>()
var ppsz41s = mutableListOf<PPSz41>()
var cheytacm200s = mutableListOf<CheyTacM200>()
var inventoryVisible = false
var openChest: Chest? = null
var openTrader: Trader? = null
var lookchest: Boolean = false
var lookslotMachine: Boolean = false
var looktrader: Boolean = false
var playerInventory = MutableList<Item?>(9) { null }
var isShooting: Boolean = false
var currentAmmo: Int = 46
var SHOT_COOLDOWN = 500_000_000L * FastReload

var weapon2Unlocked: Boolean = false
var weapon3Unlocked: Boolean = false
var weapon4Unlocked: Boolean = false

class Medication(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
    var heal: Int = 100,
    var amount: Int = 1
) {
    val size = 0.5 * tileSize // Medication size (radius)
    val pickupDistance = 0.7 * 2 * size // radius for pickup
}

class Key(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
    var amount: Int = 1
) {
    val size = 0.5 * tileSize // Key size (radius)
    val pickupDistance = 0.7 * 2 * size // radius for pickup
}

class Coin(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
    var amount: Int = 1
) {
    val size = 0.5 * tileSize // Key size (radius)
    val pickupDistance = 0.7 * 2 * size // radius for pickup
}

class Glock34(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
    var amount: Int = 0
) {
    val size = 0.5 * tileSize // Key size (radius)
    val pickupDistance = 0.7 * 2 * size // radius for pickup
}

class PPSz41(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
    var amount: Int = 0
) {
    val size = 0.5 * tileSize // Key size (radius)
    val pickupDistance = 0.7 * 2 * size // radius for pickup
}

class CheyTacM200(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
    var amount: Int = 0
) {
    val size = 0.5 * tileSize // Key size (radius)
    val pickupDistance = 0.7 * 2 * size // radius for pickup
}

class Ammo(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
    val amount: Int = 10
) {
    val size = 0.5 * tileSize
    val pickupDistance = 0.7 * 2 * size
}

class LightSource(
    var x: Double,
    var y: Double,
    var color: Color,
    var intensity: Double,
    var range: Double,
    var owner: String = ""
)

class Chest(
    var x: Double,
    var y: Double,
    var loot: MutableList<Item>,
    var active: Boolean = true,
) {
    val size = 1.5 * tileSize
    val pickupDistance = 1.3*2
}

class SlotMachine(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
) {
    val size = 1.5 * tileSize
    val pickupDistance = 1.3*2
}

class Trader(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var offer: MutableList<Item> = mutableListOf(Item(ItemType.MEDKIT, 1), Item(ItemType.AMMO, 10), Item(ItemType.KEY, 1), Item(ItemType.AMMO, 20)),
    var prices: MutableList<Int> = mutableListOf(15, 9, 30, 12),
    var active: Boolean = true,
) {
    val size = 1.5 * tileSize
    val pickupDistance = 1.3*2
}

data class Item(val type: ItemType, var quantity: Int = 1) {
    companion object {
        const val MAX_KEYS_PER_SLOT = 64
        const val MAX_AMMO_PER_SLOT = 46
        const val MAX_MEDKIT_PER_SLOT = 2
        const val MAX_COINS_PER_SLOT = 128
        const val MAX_WEAPON_PER_SLOT = 0

        fun getMaxQuantity(type: ItemType): Int = when (type) {
            ItemType.KEY -> MAX_KEYS_PER_SLOT
            ItemType.AMMO -> MAX_AMMO_PER_SLOT
            ItemType.MEDKIT -> MAX_MEDKIT_PER_SLOT
            ItemType.COIN -> MAX_COINS_PER_SLOT
            ItemType.GLOCK34 -> MAX_WEAPON_PER_SLOT
            ItemType.PPSH41 -> MAX_WEAPON_PER_SLOT
            ItemType.CHEYTACM200 -> MAX_WEAPON_PER_SLOT
        }
    }
}

enum class ItemType {
    MEDKIT, AMMO, KEY, COIN, GLOCK34, PPSH41, CHEYTACM200
}

enum class Perk { HealBoost, SpeedMovement, MoreHitShot, FastReload, AmmoBoost }

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

fun main() = runBlocking {
    val frame = JFrame("rolada z gówna")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.iconImage = Toolkit.getDefaultToolkit().getImage(this::class.java.classLoader.getResource("icon/icon.jpg"))
    frame.isResizable = false
    frame.setSize(1366, 768)
    frame.setLocation(((Toolkit.getDefaultToolkit().screenSize.width - frame.width) / 2), ((Toolkit.getDefaultToolkit().screenSize.height - frame.height) / 2))

    frame.isVisible = true
    val map = Map()

    val renderCast = RenderCast(map)
    map.renderCast = renderCast

    val player = Player(renderCast, map)

    val layeredPane = JLayeredPane()
    layeredPane.setSize(1366, 768)
    layeredPane.setBounds(0, 0, 1366, 768)
    frame.add(layeredPane)

    val mapa = Mappingmap(map, renderCast)
    mapa.isOpaque = false
    mapa.layout = null
    mapa.setSize(1366, 768)
    mapa.setBounds(0, 0, 1366, 768)

    renderCast.isOpaque = false
    renderCast.setSize(1366, 768)
    renderCast.setBounds(0, 0, 1366, 768)

    layeredPane.add(mapa, 1)
    layeredPane.add(renderCast, 2)

    val keysPressed: MutableMap<Int, Boolean> = mutableMapOf()
    var centerX = frame.width / 2

    ///

    glock34s.add(
        Glock34(
            x = (tileSize * (12)) - (tileSize / 2),
            y = (tileSize * (12)) - (tileSize / 2),
            texture = renderCast?.glock34TextureId!!,
            active = true
        )
    )
    ppsz41s.add(
        PPSz41(
            x = (tileSize * (10)) - (tileSize / 2),
            y = (tileSize * (10)) - (tileSize / 2),
            texture = renderCast?.ppsz41TextureId!!,
            active = true
        )
    )
    cheytacm200s.add(
        CheyTacM200(
            x = (tileSize * (14)) - (tileSize / 2),
            y = (tileSize * (14)) - (tileSize / 2),
            texture = renderCast?.cheyTacM200TextureId!!,
            active = true
        )
    )

    var remainingAmmo = 46
    var slotIndexAmmo = 0
    while (remainingAmmo > 0 && slotIndexAmmo < playerInventory.size) {
        val quantity = minOf(remainingAmmo, Item.MAX_AMMO_PER_SLOT)
        playerInventory[slotIndexAmmo] = Item(ItemType.AMMO, quantity)
        remainingAmmo -= quantity
        slotIndexAmmo++
    }

    var remainingKeys = 200
    var slotIndexKey = 1
    while (remainingKeys > 0 && slotIndexKey < playerInventory.size) {
        val quantity = minOf(remainingKeys, Item.MAX_KEYS_PER_SLOT)
        playerInventory[slotIndexKey] = Item(ItemType.KEY, quantity)
        remainingKeys -= quantity
        slotIndexKey++
    }

    var remainingCOINS = 128
    var slotIndexCOIN = 5
    while (remainingCOINS > 0 && slotIndexCOIN < playerInventory.size) {
        val quantity = minOf(remainingCOINS, Item.MAX_COINS_PER_SLOT)
        playerInventory[slotIndexCOIN] = Item(ItemType.COIN, quantity)
        remainingCOINS -= quantity
        slotIndexCOIN++
    }

    if (oneHitKills) MoreHitShot = 50000.0

    frame.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                if (inventoryVisible and lookchest) {
                    renderCast.clickInventoryGUI(event.x, event.y)
                    return
                }
                if (perkGUI) {
                    renderCast.clickPerkGUI(event.x, event.y)
                    return
                }
                if (inventoryVisible and looktrader) {
                    renderCast.clickTrader(event.x, event.y)
                    return
                }
                if (!inventoryVisible or !perkGUI) renderCast.shotgun()
            }
        }

        override fun mouseReleased(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                isShooting = false
            }
        }
    })
    /*
    frame.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseMoved(e: MouseEvent) {
            player.updateAngleFromMouse()
        }

        override fun mouseDragged(e: MouseEvent) {
            player.updateAngleFromMouse()
        }
    })*/

    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            keysPressed[event.keyCode] = true
            when (event.keyCode) {
                KeyEvent.VK_SPACE -> {
                    if (!isShooting) {
                        renderCast.shotgun()
                    }
                }
            }
        }

        override fun keyReleased(event: KeyEvent) {
            keysPressed[event.keyCode] = false
            when (event.keyCode) {
                KeyEvent.VK_SPACE -> isShooting = false
                KeyEvent.VK_E -> {
                    openTrader?.let { trader ->
                        if (looktrader) {
                            if (renderCast.purchaseItem(trader, selectedOfferIndex)) {
                                println("Purchased ${trader.offer[selectedOfferIndex].type} for ${trader.prices[selectedOfferIndex]} COINs")
                                playSound("purchase.wav")
                            } else {
                                println("Purchase failed: Not enough COINs or inventory full")
                                playSound("denied.wav")
                            }
                        }
                    } ?: run {
                        inventoryVisible = !inventoryVisible
                        if (lookslotMachine) {
                            inventoryVisible = false
                            if (coins >= 3) {
                                val coinSlot = playerInventory.indexOfFirst { it?.type == ItemType.COIN && it.quantity > 0 }
                                var currentslot = coinSlot
                                playerInventory[coinSlot]!!.quantity -= 3
                                if (playerInventory[coinSlot]!!.quantity <= 0) {
                                    playerInventory[coinSlot] = null
                                }

                                val random = Random.nextFloat()
                                val multiplier = when {
                                    random < 0.001f -> 128.0
                                    random < 0.008f -> 10.0
                                    random < 0.02f -> 5.0
                                    random < 0.07f -> 2.5
                                    random < 0.20f -> 1.0
                                    random < 0.30f -> 0.5
                                    random < 0.40f -> 0.0
                                    else -> 0.0
                                }

                                var remainingCOINS = ((coins-3) + (3*multiplier)).toInt()
                                while (remainingCOINS > 0 && currentslot < playerInventory.size) {
                                    val quantity = minOf(remainingCOINS, Item.MAX_COINS_PER_SLOT)
                                    playerInventory[currentslot] = Item(ItemType.COIN, quantity)
                                    remainingCOINS -= quantity
                                    currentslot++
                                }
                                playSound("purchase.wav")
                                inventoryVisible = false
                            }
                        }
                    }
                }
                in KeyEvent.VK_1..KeyEvent.VK_9 -> {
                    selectedOfferIndex = event.keyCode - KeyEvent.VK_0-1
                    if ((!looktrader) or (selectedOfferIndex >= 4)) {
                        selectSlot = event.keyCode - KeyEvent.VK_0-1
                        activateSlot = true
                        println(selectSlot)
                        println(playerInventory[selectSlot])
                        if (playerInventory[selectSlot] != null) {
                            if (playerInventory[selectSlot]?.type == ItemType.MEDKIT && playerInventory[selectSlot]?.quantity in 1..2) {
                                if (playerInventory[selectSlot]!!.quantity <= 0) {
                                    playerInventory[selectSlot] = null
                                }
                                val random = Random.nextFloat()
                                val healRNG = when {
                                    random < 0.33f -> 25
                                    random < 0.66f -> 35
                                    else -> 45
                                }
                                playerInventory[selectSlot]!!.quantity -= 1
                                if (playerInventory[selectSlot]!!.quantity <= 0) {
                                    playerInventory[selectSlot] = null
                                }
                                playerHealth += healRNG
                            }
                        }
                    }
                }
                KeyEvent.VK_Q -> {
                    val randomDistance = Random.nextDouble(1.2 * tileSize, 1.6 * tileSize)
                    val minWallDistance = tileSize

                    fun calculateItemPosition(): Pair<Double, Double> {
                        val itemX = positionX + randomDistance * cos(Math.toRadians(currentangle.toDouble()))
                        val itemY = positionY + randomDistance * sin(Math.toRadians(currentangle.toDouble()))
                        return Pair(itemX, itemY)
                    }

                    fun isValidPosition(x: Double, y: Double): Boolean {
                        val gridX = (x / tileSize).toInt()
                        val gridY = (y / tileSize).toInt()

                        if (gridX < 0 || gridX >= map.grid[0].size || gridY < 0 || gridY >= map.grid.size) {
                            return false
                        }
                        if (map.grid[gridY][gridX] != 0) {
                            return false
                        }
                        for (dy in -1..1) {
                            for (dx in -1..1) {
                                val checkX = gridX + dx
                                val checkY = gridY + dy
                                if (checkX >= 0 && checkX < map.grid[0].size && checkY >= 0 && checkY < map.grid.size) {
                                    if (map.grid[checkY][checkX] in listOf(1, 2, 5, 12)) {
                                        val wallX = (checkX + 0.5) * tileSize
                                        val wallY = (checkY + 0.5) * tileSize
                                        val distance = sqrt((x - wallX).pow(2) + (y - wallY).pow(2))
                                        if (distance < minWallDistance) {
                                            return false
                                        }
                                    }
                                }
                            }
                        }
                        return true
                    }

                    fun findNearestValidPosition(startX: Double, startY: Double): Pair<Double, Double>? {
                        val startGridX = (startX / tileSize).toInt()
                        val startGridY = (startY / tileSize).toInt()
                        val maxSearchRadius = 5

                        for (radius in 0..maxSearchRadius) {
                            for (dy in -radius..radius) {
                                for (dx in -radius..radius) {
                                    if (kotlin.math.abs(dx) == radius || kotlin.math.abs(dy) == radius) {
                                        val gridX = startGridX + dx
                                        val gridY = startGridY + dy
                                        if (gridX >= 0 && gridX < map.grid[0].size && gridY >= 0 && gridY < map.grid.size) {
                                            val testX = (gridX + 0.5) * tileSize
                                            val testY = (gridY + 0.5) * tileSize
                                            if (isValidPosition(testX, testY)) {
                                                return Pair(testX, testY)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return null
                    }

                    var (itemX, itemY) = calculateItemPosition()
                    if (!isValidPosition(itemX, itemY)) {
                        findNearestValidPosition(itemX, itemY)?.let { (newX, newY) ->
                            itemX = newX
                            itemY = newY
                        } ?: run {
                            return
                        }
                    }

                    when {
                        playerInventory[selectSlot]?.type!! == ItemType.KEY -> keysList.add(
                            Key(
                                x = itemX - (tileSize / 2),
                                y = itemY - (tileSize / 2),
                                texture = renderCast?.keyTextureId!!,
                                active = true,
                                amount = playerInventory[selectSlot]?.quantity!!
                            )
                        )
                        playerInventory[selectSlot]?.type!! == ItemType.AMMO -> ammo.add(
                            Ammo(
                                x = itemX - (tileSize / 2),
                                y = itemY - (tileSize / 2),
                                texture = renderCast?.ammoTextureID!!,
                                active = true,
                                amount = playerInventory[selectSlot]?.quantity!!
                            )
                        )
                        playerInventory[selectSlot]?.type!! == ItemType.MEDKIT -> medications.add(
                            Medication(
                                x = itemX - (tileSize / 2),
                                y = itemY - (tileSize / 2),
                                texture = renderCast?.medicationTextureID!!,
                                active = true,
                                amount = playerInventory[selectSlot]?.quantity!!
                            )
                        )
                        playerInventory[selectSlot]?.type!! == ItemType.COIN -> coinsList.add(
                            Coin(
                                x = itemX - (tileSize / 2),
                                y = itemY - (tileSize / 2),
                                texture = renderCast?.coinTextureID!!,
                                active = true,
                                amount = playerInventory[selectSlot]?.quantity!!
                            )
                        )
                    }
                    playerInventory[selectSlot] = null
                }
                KeyEvent.VK_R -> {
                    if (selectWeaponSlot > 1) {
                        selectWeaponSlot -= 1
                        println(selectWeaponSlot)
                        if (selectWeaponSlot == 1) {
                            SHOT_COOLDOWN = 250_000_000L * FastReload
                            shotAccuracy = 10
                            playerDamage = 20
                        }
                        if (selectWeaponSlot == 2) {
                            if (weapon2Unlocked){
                                SHOT_COOLDOWN = 500_000_000L * FastReload
                                shotAccuracy = 5
                                playerDamage = 25
                            } else { playSound("denied.wav") }
                        }
                        if (selectWeaponSlot == 3) {
                            if (weapon3Unlocked){
                                SHOT_COOLDOWN = 200_000_000L * FastReload
                                shotAccuracy = 15
                                playerDamage = 15
                            } else { playSound("denied.wav") }
                        }
                        if (selectWeaponSlot == 4) {
                            if (weapon4Unlocked){
                                SHOT_COOLDOWN = 4_000_000_000L * FastReload
                                shotAccuracy = 1
                                playerDamage = 75
                            } else { playSound("denied.wav") }
                        }
                    }
                }
                KeyEvent.VK_T -> {
                    if (selectWeaponSlot < 4) {
                        selectWeaponSlot += 1
                        println(selectWeaponSlot)
                        if (selectWeaponSlot == 1) {
                            SHOT_COOLDOWN = 250_000_000L * FastReload
                            shotAccuracy = 10
                            playerDamage = 20
                        }
                        if (selectWeaponSlot == 2) {
                            if (weapon2Unlocked){
                                SHOT_COOLDOWN = 500_000_000L * FastReload
                                shotAccuracy = 5
                                playerDamage = 25
                            } else { playSound("denied.wav") }
                        }
                        if (selectWeaponSlot == 3) {
                            if (weapon3Unlocked){
                                SHOT_COOLDOWN = 200_000_000L * FastReload
                                shotAccuracy = 15
                                playerDamage = 15
                            } else { playSound("denied.wav") }
                        }
                        if (selectWeaponSlot == 4) {
                            if (weapon4Unlocked){
                                SHOT_COOLDOWN = 4_000_000_000L * 1.0
                                shotAccuracy = 1
                                playerDamage = 75
                            } else { playSound("denied.wav") }
                        }
                    }
                }
            }
        }

    })

    frame.addComponentListener(object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent?) {
            centerX = frame.x + frame.width / 2
        }
    })

    var lastFrameTime = System.nanoTime()
    var lastFpsUpdate = System.nanoTime()
    var accumulatedTime = 0.0

    while (true) {
        val currentTime = System.nanoTime()
        val elapsedTimeNs = currentTime - lastFrameTime
        val elapsedTimeSec = elapsedTimeNs / 1_000_000_000.0
        lastFrameTime = currentTime
        accumulatedTime += elapsedTimeSec

        while (accumulatedTime >= deltaTime) {
            if (inventoryVisible) {
                renderCast.updateOpenChest()
                renderCast.updateOpenTrader()
            } else {
                openTrader = null
                openChest = null
            }
            player.update(keysPressed)
            accumulatedTime -= deltaTime
        }

        SwingUtilities.invokeLater {
            renderCast.repaint()
            mapa.repaint()
        }

        // FPS counter
        if (currentTime - lastFpsUpdate >= 1_000_000_000L) {
            lastFpsUpdate = currentTime
        }

        // Sleep to prevent busy-waiting
        val timeToNextFrame = FRAME_TIME_NS - (System.nanoTime() - lastFrameTime)
        if (timeToNextFrame > 0) {
            Thread.sleep(timeToNextFrame / 1_000_000)
        }
    }
}
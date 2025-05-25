package org.example.MainKt

//./gradlew shadowJar
//0-air 1-wall 2-black_wall 3-enemy 4-ammo 5-door 6-lightSource 7-medication 8-key 10-chest

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import javax.swing.JFrame
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.Color
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.image.BufferedImage
import javax.swing.JLayeredPane
import kotlin.random.Random

var playerHealth: Int = 100
var level: Int = 1
var points: Int = 0
var coins: Int = 0
var keys: Int = 2
var selectSlot: Int = 1
var selectedOfferIndex = 0
var activateSlot: Boolean = false
var perkGUI: Boolean = false

var map = true
var currentangle = 45
var tileSize = 40.0
val mapa = 0.5
var MouseSupport = false

var HealBoost = 1.0
var SpeedMovement = 1.0
var MoreHitShot = 1.0
var FastReload = 1.0
var AmmoBoost = 1.0

val TARGET_FPS = 90
val FRAME_TIME_NS = 1_000_000_000 / TARGET_FPS
var deltaTime = 1.0 / TARGET_FPS

var positionX = (tileSize*11)-(tileSize/2)  //tile*positon - (half tile)
var positionY = (tileSize*11)-(tileSize/2)  //tile*positon - (half tile)
var enemies = mutableListOf<Enemy>()
var lightSources = mutableListOf<LightSource>()
var keysList = mutableListOf<Key>()
var medications = mutableListOf<Medication>()
var chests = mutableListOf<Chest>()
var ammo = mutableListOf<Ammo>()
var traders = mutableListOf<Trader>()
var inventoryVisible = false
var openChest: Chest? = null
var openTrader: Trader? = null
var lookchest = false
var looktrader = false
var playerInventory = MutableList<Item?>(9) { null }
var isShooting = false
var currentAmmo = 46

class Medication(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
    var heal: Int = 100
) {
    val size = 0.5 * tileSize // Medication size (radius)
    val pickupDistance = 0.7 * 2 * size // radius for pickup
    val amount: Int = 1
}

class Key(
    var x: Double,
    var y: Double,
    var texture: BufferedImage,
    var active: Boolean = true,
) {
    val size = 0.5 * tileSize // Key size (radius)
    val pickupDistance = 0.7 * 2 * size // radius for pickup
    val amount: Int = 1
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

        fun getMaxQuantity(type: ItemType): Int = when (type) {
            ItemType.KEY -> MAX_KEYS_PER_SLOT
            ItemType.AMMO -> MAX_AMMO_PER_SLOT
            ItemType.MEDKIT -> MAX_MEDKIT_PER_SLOT
            ItemType.COIN -> MAX_COINS_PER_SLOT
        }
    }
}

enum class ItemType {
    MEDKIT, AMMO, KEY, COIN
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

    frame.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                if (inventoryVisible) {
                    renderCast.clickInventoryGUI(event.x, event.y)
                    return
                }
                if (perkGUI) {
                    renderCast.clickPerkGUI(event.x, event.y)
                    return
                }
                renderCast.shotgun()
            }
        }

        override fun mouseReleased(event: MouseEvent) {
            if (event.button == MouseEvent.BUTTON1) {
                isShooting = false
            }
        }
    })

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
                                renderCast.playSound("purchase.wav")
                            } else {
                                println("Purchase failed: Not enough COINs or inventory full")
                                renderCast.playSound("denied.wav")
                            }
                        }/* else {
                            inventoryVisible = !inventoryVisible
                        }*/
                    } ?: run {
                        inventoryVisible = !inventoryVisible
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
            }
        }

    })

    frame.addComponentListener(object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent?) {
            centerX = frame.x + frame.width / 2
        }
    })

    //fps counter
    var lastFrameTime = System.nanoTime()
    var lastFpsUpdate = System.nanoTime()

    while (true) {
        if (inventoryVisible) {
            renderCast.updateOpenChest()
            renderCast.updateOpenTrader()
        } else {
            openTrader = null
            openChest = null
        }

        val currentTime = System.nanoTime()
        val elapsedTime = currentTime - lastFrameTime

        if (elapsedTime >= (1_000_000_000 / 120)) {
            player.update(keysPressed)
            renderCast.repaint()
            mapa.repaint()

            lastFrameTime = currentTime

            if (currentTime - lastFpsUpdate >= 1_000_000_000L) {
                lastFpsUpdate = currentTime
            }
        }

        val timeToNextFrame = FRAME_TIME_NS - (System.nanoTime() - lastFrameTime)
        if (timeToNextFrame > 0) {
            delay((timeToNextFrame / 1_000_000))
        }
    }
}
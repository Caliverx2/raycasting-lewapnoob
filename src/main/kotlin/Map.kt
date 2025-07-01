package org.example.MainKt

//0-air 1-wall 2-black_wall 3-enemy 4-ammo 5-door 6-lightSource 7-medication 8-key 9-trader 10-chest 11-slotMachine 12-closedDoor 13-boss

import org.example.MainKt.Map.RoomTemplate
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO;
import kotlin.math.cos
import kotlin.math.sin
import javax.swing.JPanel
import kotlin.Int
import kotlin.random.Random

var directionForRoom = "UP"
val rooms = listOf(
    RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2, 5, 2, 2),
            intArrayOf(2, 0, 0, 0, 2),
            intArrayOf(5, 0, 6, 0, 5),
            intArrayOf(2, 0, 0, 0, 2),
            intArrayOf(2, 2, 5, 2, 2)
        ),
        scale = 5,
        weight = 0.15
    ),
    RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2, 5, 2, 2),
            intArrayOf(2, 0, 0, 0, 2),
            intArrayOf(5, 0, 10, 0, 5),
            intArrayOf(2, 0, 0, 0, 2),
            intArrayOf(2, 2, 5, 2, 2)
        ),
        scale = 5,
        weight = 0.15
    ),
    RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2, 5, 2, 2),
            intArrayOf(2, 0, 0, 0, 2),
            intArrayOf(5, 0, 4, 0, 5),
            intArrayOf(2, 0, 0, 0, 2),
            intArrayOf(2, 2, 5, 2, 2)
        ),
        scale = 5,
        weight = 0.15
    ),
    RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2, 2, 5, 2, 2, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 0, 7, 0, 4, 0, 2),
            intArrayOf(5, 0, 0, 6, 0, 0, 5),
            intArrayOf(2, 0, 4, 0, 7, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 2, 2, 5, 2, 2, 2)
        ),
        scale = 7,
        weight = 0.15
    ),
    RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2, 2, 5, 2, 2, 2),
            intArrayOf(2, 1, 1, 0, 1, 1, 2),
            intArrayOf(2, 1, 9, 0, 9, 1, 2),
            intArrayOf(5, 0, 0, 6, 0, 0, 5),
            intArrayOf(2, 1, 9, 0, 9, 1, 2),
            intArrayOf(2, 1, 1, 0, 1, 1, 2),
            intArrayOf(2, 2, 2, 5, 2, 2, 2)
        ),
        scale = 7,
        weight = 0.15
    ),
    RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2,2,5,2,2, 2),
            intArrayOf(2,2,11,0,11,2,2),
            intArrayOf(2,11,0,0,0,11,2),
            intArrayOf(5, 0,0,6,0,0, 5),
            intArrayOf(2,11,0,0,0,11,2),
            intArrayOf(2,2,11,0,11,2,2),
            intArrayOf(2, 2,2,5,2,2, 2)
        ),
        scale = 7,
        weight = 0.02
    ),
    RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2, 2, 5, 2, 2, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 0, 0, 0, 3, 0, 2),
            intArrayOf(5, 0, 0, 6, 0, 0, 5),
            intArrayOf(2, 0, 3, 0, 0, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 2, 2, 5, 2, 2, 2)
        ),
        scale = 7,
        weight = 0.15
    ),
    RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2, 2, 5, 2, 2, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 2),
            intArrayOf(5, 0, 0, 6, 0, 0, 5),
            intArrayOf(2, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 2, 2, 5, 2, 2, 2)
        ),
        scale = 7,
        weight = 0.15
    ),
    RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2, 2, 2, 5, 2, 2, 2, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 0, 6, 0, 0, 0, 6, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
            intArrayOf(5, 0, 0, 0, 10, 0, 0, 0, 5),
            intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 0, 6, 0, 0, 0, 6, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 2, 2, 2, 5, 2, 2, 2, 2)
        ),
        scale = 9,
        weight = 0.15
    ),
    RoomTemplate(
        grid = arrayOf(
            intArrayOf(2, 2, 2, 2, 2, 2, 2, 5, 2, 2, 2, 2, 2, 2, 2),
            intArrayOf(2, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 2),
            intArrayOf(2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 2),
            intArrayOf(2, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 2),
            intArrayOf(2, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 2),
            intArrayOf(5, 0, 0, 1, 0, 0, 0, 10, 0, 0, 0, 1, 0, 0, 5),
            intArrayOf(2, 0, 0, 1, 0, 0, 1, 13, 1, 0, 0, 1, 0, 0, 2),
            intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2),
            intArrayOf(2, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 2),
            intArrayOf(2, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 2),
            intArrayOf(2, 0, 3, 0, 0, 0, 0, 0, 0, 3, 0, 0, 3, 0, 2),
            intArrayOf(2, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 2),
            intArrayOf(2, 2, 2, 2, 2, 2, 2, 5, 2, 2, 2, 2, 2, 2, 2)
        ),
        scale = 15,
        weight = 0.05
    )
)

class Map(var renderCast: RenderCast? = null) {
    var grid: Array<IntArray> = arrayOf(
        intArrayOf(2,5,2,2,2,2,2,2,2,2,5,2,2,2,2,2,2,2,2,5,2),
        intArrayOf(5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5),
        intArrayOf(2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2),
        intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
        intArrayOf(2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2),
        intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
        intArrayOf(2,0,2,0,2,0,1,0,1,0,1,0,1,0,1,0,2,0,2,0,2),
        intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
        intArrayOf(2,0,2,0,2,0,1,0,1,1,0,1,1,0,1,0,2,0,2,0,2),
        intArrayOf(2,0,0,0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,0,0,2),
        intArrayOf(5,0,2,0,2,0,1,0,0,0,0,0,0,0,1,0,2,0,2,0,5),
        intArrayOf(2,0,0,0,0,0,0,0,1,0,0,0,1,0,0,0,0,0,0,0,2),
        intArrayOf(2,0,2,0,2,0,1,0,1,1,0,1,1,0,1,0,2,0,2,0,2),
        intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
        intArrayOf(2,0,2,0,2,0,1,0,1,0,1,0,1,0,1,0,2,0,2,0,2),
        intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
        intArrayOf(2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2),
        intArrayOf(2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2),
        intArrayOf(2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2,0,2),
        intArrayOf(5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5),
        intArrayOf(2,5,2,2,2,2,2,2,2,2,5,2,2,2,2,2,2,2,2,5,2)
    )

    var gridRooms: Array<IntArray> = arrayOf(
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
        intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
    )

    var gridmod = false
    var currentRooms = 0
    enum class Direction { UP, DOWN, LEFT, RIGHT }
    var clear = true
    private val roomTemplates = rooms
    private val recentRooms = mutableListOf<RoomTemplate>()

    data class RoomTemplate(
        val grid: Array<IntArray>,
        val scale: Int,
        val weight: Double = 0.15
    )

    fun randomRoom(): RoomTemplate {
        var availableRooms = roomTemplates.filter { it !in recentRooms.take(3) }
        if (availableRooms.isEmpty()) {
            recentRooms.clear()
            availableRooms = roomTemplates
        }

        val totalWeight = availableRooms.sumOf { it.weight }
        val randomValue = Random.nextDouble(totalWeight)
        var cumulativeWeight = 0.0

        val selectedRoom = availableRooms.first { room ->
            cumulativeWeight += room.weight
            randomValue <= cumulativeWeight
        }

        recentRooms.add(0, selectedRoom)
        if (recentRooms.size > 3) {
            recentRooms.removeAt(recentRooms.size - 1)
        }

        return selectedRoom
    }

    fun ensureGridCapacity(x: Int, y: Int, roomTemplate: RoomTemplate, direct: Direction): Triple<Int, Int, Pair<Int, Int>> {
        var offsetX = 0
        var offsetY = 0
        val margin = roomTemplate.scale

        if (x - margin < 0) offsetX = -((x - margin))
        if (y - margin < 0) offsetY = -((y - margin))

        val newHeight = maxOf(grid.size + offsetY, y + margin + 1)
        val newWidth = maxOf(grid[0].size + offsetX, x + margin + 1)

        if (offsetY > 0 || newHeight > grid.size || offsetX > 0 || newWidth > grid[0].size) {
            val newGrid = Array(newHeight) { IntArray(newWidth) { 1 } }
            val newGridRooms = Array(newHeight) { IntArray(newWidth) { 0 } }

            for (yOld in grid.indices) {
                for (xOld in grid[0].indices) {
                    newGrid[yOld + offsetY][xOld + offsetX] = grid[yOld][xOld]
                    newGridRooms[yOld + offsetY][xOld + offsetX] = gridRooms[yOld][xOld]
                }
            }

            gridmod = true
            grid = newGrid
            gridRooms = newGridRooms
        }

        return Triple(x + offsetX, y + offsetY, Pair(offsetX, offsetY))
    }

    fun generateRoom(x: Int = 0, y: Int = 0, enterdirection: Direction) {
        directionForRoom = enterdirection.toString()
        println("directionForRoom: $directionForRoom")
        var roomTemplate = randomRoom()

        gridmod = false
        val (newX, newY, offsets) = ensureGridCapacity(x, y, roomTemplate, enterdirection)
        val (offsetX, offsetY) = offsets

        if (gridmod && (offsetX != 0 || offsetY != 0)) {
            positionX = positionX.toInt() + offsetX*tileSize
            positionY = positionY.toInt() + offsetY*tileSize
            if ((grid[(positionY/tileSize).toInt()][(positionX/tileSize).toInt()] == 1) or (grid[(positionY/tileSize).toInt()][(positionX/tileSize).toInt()] == 2)) {
                positionX -= 0.2*tileSize
                positionY -= 0.2*tileSize
            }
        }

        if (keys > 0) {
            println("$currentRooms $enterdirection")
            clear = true
            generationCheck(x, y, enterdirection, roomTemplate, newX, newY)

            if (clear) {
                println("generation room")
                grid[newY][newX] = 0
                generation(x, y, enterdirection, roomTemplate, newX, newY, offsetX, offsetY)
                if (gridmod && (offsetX != 0 || offsetY != 0)) {
                    enemies.forEach {enemy ->
                        enemy.x += offsetX*tileSize
                        enemy.y += offsetY*tileSize
                    }
                    keysList.forEach { key ->
                        key.x += offsetX*tileSize
                        key.y += offsetY*tileSize
                    }
                    medications.forEach { medication ->
                        medication.x += offsetX*tileSize
                        medication.y += offsetY*tileSize
                    }
                    ammo.forEach { ammo ->
                        ammo.x += offsetX*tileSize
                        ammo.y += offsetY*tileSize
                    }
                    traders.forEach { trader ->
                        trader.x += offsetX*tileSize
                        trader.y += offsetY*tileSize
                    }
                    lightSources.forEach { lightSource ->
                        lightSource.x += offsetX
                        lightSource.y += offsetY
                    }
                    chests.forEach { chest ->
                        chest.x += offsetX*tileSize
                        chest.y += offsetY*tileSize
                    }
                    slotMachines.forEach { slotMachine ->
                        slotMachine.x += offsetX*tileSize
                        slotMachine.y += offsetY*tileSize
                    }
                }
                currentRooms += 1
                val keysSlot = playerInventory.indexOfFirst { it?.type == ItemType.KEY && it.quantity > 0 }
                playerInventory[keysSlot]!!.quantity -= 1
                if (playerInventory[keysSlot]!!.quantity <= 0) {
                    playerInventory[keysSlot] = null
                }
            }

            if (!clear) {
                println("not enough space to generate a room")
                if (enterdirection == Direction.UP) {
                    roomTemplate = RoomTemplate(
                        grid = arrayOf(
                            intArrayOf(2, 5, 2),
                            intArrayOf(2, 0, 2),
                            intArrayOf(2, 5, 2),
                        ),
                        scale = 3
                    )
                }
                if (enterdirection == Direction.DOWN) {
                    roomTemplate = RoomTemplate(
                        grid = arrayOf(
                            intArrayOf(2, 5, 2),
                            intArrayOf(2, 0, 2),
                            intArrayOf(2, 5, 2),
                        ),
                        scale = 3
                    )
                }
                if (enterdirection == Direction.RIGHT) {
                    roomTemplate = RoomTemplate(
                        grid = arrayOf(
                            intArrayOf(2, 2, 2),
                            intArrayOf(5, 0, 5),
                            intArrayOf(2, 2, 2),
                        ),
                        scale = 3
                    )
                }
                if (enterdirection == Direction.LEFT) {
                    roomTemplate = RoomTemplate(
                        grid = arrayOf(
                            intArrayOf(2, 2, 2),
                            intArrayOf(5, 0, 5),
                            intArrayOf(2, 2, 2),
                        ),
                        scale = 3
                    )
                }
                clear = true
                generationCheck(x, y, enterdirection, roomTemplate, newX, newY)
                if (clear == true) {
                    grid[newY][newX] = 0
                    generation(x, y, enterdirection, roomTemplate, newX, newY, offsetX, offsetY)
                }
                if (gridmod && (offsetX != 0 || offsetY != 0)) {
                    enemies.forEach {enemy ->
                        enemy.x += offsetX*tileSize
                        enemy.y += offsetY*tileSize
                    }
                    keysList.forEach { key ->
                        key.x += offsetX*tileSize
                        key.y += offsetY*tileSize
                    }
                    medications.forEach { medication ->
                        medication.x += offsetX*tileSize
                        medication.y += offsetY*tileSize
                    }
                    ammo.forEach { ammo ->
                        ammo.x += offsetX*tileSize
                        ammo.y += offsetY*tileSize
                    }
                    projectiles.forEach { projectile ->
                        projectile.x += offsetX*tileSize
                        projectile.y += offsetY*tileSize
                    }
                    lightSources.forEach { lightSource ->
                        lightSource.x += offsetX
                        lightSource.y += offsetY
                    }
                    chests.forEach { chest ->
                        chest.x += offsetX*tileSize
                        chest.y += offsetY*tileSize
                    }
                    traders.forEach { trader ->
                        trader.x += offsetX*tileSize
                        trader.y += offsetY*tileSize
                    }
                    slotMachines.forEach { slotMachine ->
                        slotMachine.x += offsetX*tileSize
                        slotMachine.y += offsetY*tileSize
                    }
                }
            }
        }
    }

    fun generationCheck(x: Int = 0, y: Int = 0, enterdirection: Direction, roomTemplate: RoomTemplate, newX: Int, newY: Int){
        for (XX in 0..(roomTemplate.scale-1)) {
            for (YY in 0..(roomTemplate.scale-1)) {
                if(clear) {
                    if (enterdirection == Direction.UP) {
                        if (gridRooms[(newY+YY)-(roomTemplate.scale-(roomTemplate.scale+1))][(newX+XX)-(roomTemplate.scale/2)] != 0) {
                            clear = false
                        }
                    }
                    if (enterdirection == Direction.DOWN) {
                        if (gridRooms[(newY+YY)-(roomTemplate.scale)][(newX+XX)-(roomTemplate.scale/2)] != 0) {
                            clear = false
                        }
                    }
                    if (enterdirection == Direction.RIGHT) {
                        if (gridRooms[(newY+YY)-(roomTemplate.scale/2)][(newX+XX)-(roomTemplate.scale)] != 0) {
                            clear = false
                        }
                    }
                    if (enterdirection == Direction.LEFT) {
                        if (gridRooms[(newY+YY)-(roomTemplate.scale/2)][(newX+XX)-(roomTemplate.scale-(roomTemplate.scale+1))] != 0) {
                            clear = false
                        }
                    }
                }
            }
        }
    }

    fun generation(x: Int = 0, y: Int = 0, enterdirection: Direction, roomTemplate: RoomTemplate, newX: Int, newY: Int, offsetX: Int, offsetY: Int) {
        for (XX in 0..(roomTemplate.scale-1)) {
            for (YY in 0..(roomTemplate.scale-1)) {
                val distGrid = when {
                    enterdirection == Direction.UP -> grid[(newY+YY)-(roomTemplate.scale-(roomTemplate.scale+1))][(newX+XX)-(roomTemplate.scale/2)] = roomTemplate.grid[YY][XX]
                    enterdirection == Direction.DOWN -> grid[(newY+YY)-(roomTemplate.scale)][(newX+XX)-(roomTemplate.scale/2)] = roomTemplate.grid[YY][XX]
                    enterdirection == Direction.RIGHT -> grid[(newY+YY)-(roomTemplate.scale/2)][(newX+XX)-roomTemplate.scale] = roomTemplate.grid[YY][XX]
                    else -> grid[(newY+YY)-(roomTemplate.scale/2)][(newX+XX)-(roomTemplate.scale-(roomTemplate.scale+1))] = roomTemplate.grid[YY][XX]
                }
                val distRooms = when {
                    enterdirection == Direction.UP -> gridRooms[(newY+YY)-(roomTemplate.scale-(roomTemplate.scale+1))][(newX+XX)-(roomTemplate.scale/2)] = roomTemplate.grid[YY][XX]
                    enterdirection == Direction.DOWN -> gridRooms[(newY+YY)-(roomTemplate.scale)][(newX+XX)-(roomTemplate.scale/2)] = roomTemplate.grid[YY][XX]
                    enterdirection == Direction.RIGHT -> gridRooms[(newY+YY)-(roomTemplate.scale/2)][(newX+XX)-roomTemplate.scale] = roomTemplate.grid[YY][XX]
                    else -> gridRooms[(newY+YY)-(roomTemplate.scale/2)][(newX+XX)-(roomTemplate.scale-(roomTemplate.scale+1))] = roomTemplate.grid[YY][XX]
                }
                distGrid
                distRooms

                val distItemX = when {
                    enterdirection == Direction.UP -> ((newX+XX)-(roomTemplate.scale/2))
                    enterdirection == Direction.DOWN -> ((newX+XX)-(roomTemplate.scale/2))
                    enterdirection == Direction.RIGHT -> ((newX+XX)-roomTemplate.scale)
                    else -> ((newX+XX)-(roomTemplate.scale-(roomTemplate.scale+1)))
                }
                val distItemY = when {
                    enterdirection == Direction.UP -> ((newY+YY)-(roomTemplate.scale-(roomTemplate.scale+1)))
                    enterdirection == Direction.DOWN -> ((newY+YY)-(roomTemplate.scale))
                    enterdirection == Direction.RIGHT -> ((newY+YY)-(roomTemplate.scale/2))
                    else -> ((newY+YY)-(roomTemplate.scale/2))
                }

                if (enterdirection == Direction.UP) {
                    grid[newY+1][newX] = 0
                    if (grid[(newY+YY)-(roomTemplate.scale-(roomTemplate.scale+1))][(newX+XX)-(roomTemplate.scale/2)] == 5) {
                        val currentY = (newY+YY)-(roomTemplate.scale-(roomTemplate.scale+1))
                        val currentX = (newX+XX)-(roomTemplate.scale/2)
                        // Check neighbors: x-1, x+1, y-1, y+1
                        if (currentX > 0 && grid[currentY][currentX-1] == 5) {
                            grid[currentY][currentX-1] = 12
                        }
                        if (currentX < grid[0].size-1 && grid[currentY][currentX+1] == 5) {
                            grid[currentY][currentX+1] = 12
                        }
                        if (currentY > 0 && grid[currentY-1][currentX] == 5) {
                            grid[currentY-1][currentX] = 12
                        }
                        if (currentY < grid.size-1 && grid[currentY+1][currentX] == 5) {
                            grid[currentY+1][currentX] = 12
                        }
                    }
                }
                if (enterdirection == Direction.DOWN) {
                    grid[newY-1][newX] = 0
                    if (grid[(newY+YY)-(roomTemplate.scale)][(newX+XX)-(roomTemplate.scale/2)] == 5) {
                        val currentY = (newY+YY)-(roomTemplate.scale)
                        val currentX = (newX+XX)-(roomTemplate.scale/2)
                        // Check neighbors: x-1, x+1, y-1, y+1
                        if (currentX > 0 && grid[currentY][currentX-1] == 5) {
                            grid[currentY][currentX-1] = 12
                        }
                        if (currentX < grid[0].size-1 && grid[currentY][currentX+1] == 5) {
                            grid[currentY][currentX+1] = 12
                        }
                        if (currentY > 0 && grid[currentY-1][currentX] == 5) {
                            grid[currentY-1][currentX] = 12
                        }
                        if (currentY < grid.size-1 && grid[currentY+1][currentX] == 5) {
                            grid[currentY+1][currentX] = 12
                        }
                    }
                }
                if (enterdirection == Direction.RIGHT) {
                    grid[newY][newX-1] = 0
                    if (grid[(newY+YY)-(roomTemplate.scale/2)][(newX+XX)-roomTemplate.scale] == 5) {
                        val currentY = (newY+YY)-(roomTemplate.scale/2)
                        val currentX = (newX+XX)-roomTemplate.scale
                        // Check neighbors: x-1, x+1, y-1, y+1
                        if (currentX > 0 && grid[currentY][currentX-1] == 5) {
                            grid[currentY][currentX-1] = 12
                        }
                        if (currentX < grid[0].size-1 && grid[currentY][currentX+1] == 5) {
                            grid[currentY][currentX+1] = 12
                        }
                        if (currentY > 0 && grid[currentY-1][currentX] == 5) {
                            grid[currentY-1][currentX] = 12
                        }
                        if (currentY < grid.size-1 && grid[currentY+1][currentX] == 5) {
                            grid[currentY+1][currentX] = 12
                        }
                    }
                }
                if (enterdirection == Direction.LEFT) {
                    grid[newY][newX+1] = 0
                    if (grid[(newY+YY)-(roomTemplate.scale/2)][(newX+XX)-(roomTemplate.scale-(roomTemplate.scale+1))] == 5) {
                        val currentY = (newY+YY)-(roomTemplate.scale/2)
                        val currentX = (newX+XX)-(roomTemplate.scale-(roomTemplate.scale+1))
                        // Check neighbors: x-1, x+1, y-1, y+1
                        if (currentX > 0 && grid[currentY][currentX-1] == 5) {
                            grid[currentY][currentX-1] = 12
                        }
                        if (currentX < grid[0].size-1 && grid[currentY][currentX+1] == 5) {
                            grid[currentY][currentX+1] = 12
                        }
                        if (currentY > 0 && grid[currentY-1][currentX] == 5) {
                            grid[currentY-1][currentX] = 12
                        }
                        if (currentY < grid.size-1 && grid[currentY+1][currentX] == 5) {
                            grid[currentY+1][currentX] = 12
                        }
                    }
                }

                if (roomTemplate.grid[XX][YY] == 3) {
                    renderCast?.let {
                        enemies.add(
                            Enemy(
                                x = (tileSize * (distItemX+1)) - (tileSize / 2),
                                y = (tileSize * (distItemY+1)) - (tileSize / 2),
                                health = (100 + (level * 7.5) * 2).toInt(),
                                texture = renderCast!!.enemyTextureId!!,
                                renderCast = it,
                                map = this,
                                speed = (2.0 * ((10..15).random() / 10.0))
                            )
                        )
                        if (gridmod && (offsetX != 0 || offsetY != 0)) {
                            enemies.get(enemies.size-1).x -= offsetX*tileSize
                            enemies.get(enemies.size-1).y -= offsetY*tileSize
                        }
                        lightSources.add(
                            LightSource(
                                (distItemX + 0.5),
                                (distItemY + 0.5),
                                color = Color(20, 20, (200 + Random.nextInt(-100, 54))),
                                intensity = 0.25,
                                range = 1.0,
                                owner = "${enemies[enemies.size - 1]}"
                            )
                        )
                    } ?: throw IllegalStateException("renderCast is null")
                }
                if (roomTemplate.grid[XX][YY] == 4) {
                    ammo.add(
                        Ammo(
                            x = (tileSize * (distItemX+1)) - (tileSize / 2),
                            y = (tileSize * (distItemY+1)) - (tileSize / 2),
                            texture = renderCast?.ammoTextureID!!,
                            active = true
                        )
                    )

                    if (gridmod && (offsetX != 0 || offsetY != 0)) {
                        ammo.get(ammo.size-1).x -= offsetX*tileSize
                        ammo.get(ammo.size-1).y -= offsetY*tileSize
                    }
                }
                if (roomTemplate.grid[XX][YY] == 6) {
                    lightSources.add(
                        LightSource(
                            ((tileSize * (distItemX+1)) - (tileSize / 2))/tileSize,
                            ((tileSize * (distItemY+1)) - (tileSize / 2))/tileSize,
                            color = Color(200, 20, 20),
                            intensity = 0.25,
                            range = 3.0,
                            owner = "skun"
                        )
                    )
                    if (gridmod && (offsetX != 0 || offsetY != 0)) {
                        lightSources.get(lightSources.size-1).x -= offsetX
                        lightSources.get(lightSources.size-1).y -= offsetY
                    }
                }
                if (roomTemplate.grid[XX][YY] == 7) {
                    val random = Random.nextFloat()
                    val healRNG = when {
                        random < 0.33f -> 15
                        random < 0.66f -> 25
                        else -> 35
                    }

                    renderCast?.let {
                        medications.add(
                            Medication(
                                x = ((tileSize * (distItemX + 1)) - (tileSize / 2)),
                                y = ((tileSize * (distItemY + 1)) - (tileSize / 2)),
                                texture = renderCast?.medicationTextureID!!,
                                heal = healRNG
                            )
                        )
                        it.repaint()
                    }
                    if (gridmod && (offsetX != 0 || offsetY != 0)) {
                        medications.get(medications.size-1).x -= offsetX*tileSize
                        medications.get(medications.size-1).y -= offsetY*tileSize
                    }
                }
                if (roomTemplate.grid[XX][YY] == 8) {
                    keysList.add(
                        Key(
                            x = (tileSize * (distItemX+1)) - (tileSize / 2),
                            y = (tileSize * (distItemY+1)) - (tileSize / 2),
                            texture = renderCast?.keyTextureId!!,
                            active = true
                        )
                    )
                    if (gridmod && (offsetX != 0 || offsetY != 0)) {
                        keysList.get(keysList.size-1).x -= offsetX*tileSize
                        keysList.get(keysList.size-1).y -= offsetY*tileSize
                    }
                }
                if (roomTemplate.grid[XX][YY] == 9) {
                    val random = Random.nextFloat()
                    var spawn = false
                    when {
                        random < 0.25 -> spawn = true
                        else -> spawn = false
                    }
                    if (spawn) {
                        val items = mutableListOf<Item>()
                        val availableItemTypes = mutableListOf(
                            ItemType.KEY,
                            ItemType.AMMO,
                            ItemType.COIN,
                            ItemType.MEDKIT
                        )
                        for (i in 0 until 4) {
                            if (availableItemTypes.isEmpty()) break
                            val randomIndex = Random.nextInt(availableItemTypes.size)
                            val itemType = availableItemTypes[randomIndex]
                            availableItemTypes.removeAt(randomIndex)

                            val quantity = when (itemType) {
                                ItemType.KEY -> Random.nextInt(1, Item.MAX_KEYS_PER_SLOT / 2)
                                ItemType.AMMO -> Random.nextInt(7, Item.MAX_AMMO_PER_SLOT / 2)
                                ItemType.COIN -> Random.nextInt(1, 8)
                                ItemType.MEDKIT -> Random.nextInt(1, 2)
                                ItemType.CROWBAR -> 0
                                ItemType.KIMBERPOLYMERPROCARRY -> 0
                                ItemType.GLOCK34 -> 0
                                ItemType.PPSH41 -> 0
                                ItemType.CHEYTACM200 -> 0
                            }
                            items.add(Item(itemType, quantity))
                        }
                        traders.add(
                            Trader(
                                x = (tileSize * (distItemX+1)) - (tileSize / 2),
                                y = (tileSize * (distItemY+1)) - (tileSize / 2),
                                offer = items,
                                texture = renderCast?.traderTextureID!!,
                                active = true
                            )
                        )
                        if (gridmod && (offsetX != 0 || offsetY != 0)) {
                            traders.get(traders.size-1).x -= offsetX*tileSize
                            traders.get(traders.size-1).y -= offsetY*tileSize
                        }
                    }
                }
                if (roomTemplate.grid[XX][YY] == 10) {
                    var item1 = 0
                    var item2 = 0
                    val items = mutableListOf<Item>()
                    var random = Random.nextFloat()
                    var itemCount = when {
                        Random.nextFloat() < 0.5f -> 1
                        Random.nextFloat() < 0.8f -> 2
                        Random.nextFloat() < 0.95f -> 3
                        else -> 5
                    }

                    for (i in 0 until itemCount) {
                        val itemType = when (Random.nextFloat()) {
                            in 0.0f..0.25f -> ItemType.KEY
                            in 0.25f..0.50f -> ItemType.AMMO
                            in 0.50f..0.75f -> ItemType.COIN
                            else -> ItemType.MEDKIT
                        }
                        val quantity = when (itemType) {
                            ItemType.KEY -> Random.nextInt(2, Item.MAX_KEYS_PER_SLOT / 4)
                            ItemType.AMMO -> Random.nextInt(7, Item.MAX_AMMO_PER_SLOT / 3)
                            ItemType.COIN -> Random.nextInt(4, 15)
                            ItemType.MEDKIT -> 1
                            ItemType.CROWBAR -> 0
                            ItemType.KIMBERPOLYMERPROCARRY -> 0
                            ItemType.GLOCK34 -> 0
                            ItemType.PPSH41 -> 0
                            ItemType.CHEYTACM200 -> 0
                        }
                        items.add(Item(itemType, quantity))
                    }
                    item1 = chests.size
                    item2 = ammo.size
                    val spawnRNG = when {
                        random < 0.5f -> chests.add(Chest((tileSize * (distItemX+1)) - (tileSize / 2), (tileSize * (distItemY+1)) - (tileSize / 2), items))
                        random < 0.75f -> ammo.add(Ammo((tileSize * (distItemX+1)) - (tileSize / 2), (tileSize * (distItemY+1)) - (tileSize / 2), texture = renderCast?.ammoTextureID!!, active = true, 6))
                        else -> keysList.add(Key((tileSize * (distItemX+1)) - (tileSize / 2), (tileSize * (distItemY+1)) - (tileSize / 2), texture = renderCast?.keyTextureId!!))
                    }
                    spawnRNG
                    if (item1 < chests.size) {
                        if (gridmod && (offsetX != 0 || offsetY != 0)) {
                            chests.get(chests.size-1).x -= offsetX*tileSize
                            chests.get(chests.size-1).y -= offsetY*tileSize
                        }
                    }
                    if (item2 < ammo.size) {
                        if (gridmod && (offsetX != 0 || offsetY != 0)) {
                            ammo.get(ammo.size-1).x -= offsetX*tileSize
                            ammo.get(ammo.size-1).y -= offsetY*tileSize
                        }
                    }
                }
                if (roomTemplate.grid[XX][YY] == 11) {
                    slotMachines.add(
                        SlotMachine(
                            x = (tileSize * (distItemX+1)) - (tileSize / 2),
                            y = (tileSize * (distItemY+1)) - (tileSize / 2),
                            texture = renderCast?.slotMachineTextureID!!,
                            active = true
                        )
                    )
                    if (gridmod && (offsetX != 0 || offsetY != 0)) {
                        slotMachines.get(slotMachines.size-1).x -= offsetX*tileSize
                        slotMachines.get(slotMachines.size-1).y -= offsetY*tileSize
                    }
                }
                if (roomTemplate.grid[XX][YY] == 13) {
                    renderCast?.let {
                        enemies.add(
                            Enemy(
                                x = (tileSize * (distItemX+1)) - (tileSize / 2),
                                y = (tileSize * (distItemY+1)) - (tileSize / 2),
                                health = ((200 * level * 2)*1.75).toInt(),
                                texture = renderCast!!.enemyBossTextureId!!,
                                renderCast = it,
                                map = this,
                                speed = (2.0 * ((10..15).random() / 10.0)),
                                maxHeal = ((200 * level * 2)*1.75).toInt(),
                                damage = ((7.5 * level)*1.5).toInt(), //2.5
                                enemyType = 1
                            )
                        )
                        if (gridmod && (offsetX != 0 || offsetY != 0)) {
                            enemies.get(enemies.size-1).x -= offsetX*tileSize
                            enemies.get(enemies.size-1).y -= offsetY*tileSize
                        }
                        lightSources.add(
                            LightSource(
                                (distItemX + 0.5),
                                (distItemY + 0.5),
                                color = Color(200, 20, 20),
                                intensity = 0.25,
                                range = 1.0,
                                owner = "${enemies[enemies.size - 1]}"
                            )
                        )
                    } ?: throw IllegalStateException("renderCast is null")
                }
            }
        }
    }
}

class Mappingmap(private val map: Map, private val renderCast: RenderCast) : JPanel() {
    private val miniMapSize = 200
    private val offsetX = 10
    private val offsetY = 10
    private var bufferedImage: BufferedImage? = null
    private var lastGrid: Array<IntArray>? = null
    private val maxRenderTiles = 20
    private val enemyPathColors = mutableMapOf<Enemy, Color>()
    var keypng: BufferedImage? = null
    var sliderpng: BufferedImage? = null
    private var font: Font? = null

    init {
        preferredSize = Dimension(miniMapSize + offsetX * 2, miniMapSize + offsetY * 2)
        isOpaque = false
        val fontStream = this::class.java.classLoader.getResourceAsStream("font/mojangles.ttf")
            ?: throw IllegalArgumentException("Font file not found: custom_font.ttf")
        font = Font.createFont(Font.TRUETYPE_FONT, fontStream)
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)

        sliderpng = ImageIO.read(this::class.java.classLoader.getResource("textures/slide.png"))
        keypng = ImageIO.read(this::class.java.classLoader.getResource("textures/key.png"))
    }

    override fun paintComponent(v: Graphics) {
        super.paintComponent(v)
        val g2 = v as Graphics2D

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val tileScale = miniMapSize.toDouble() / maxRenderTiles
        val playerMapX = miniMapSize / 2 + offsetX
        val playerMapY = miniMapSize / 2 + offsetY

        val playerGridX = positionX / tileSize
        val playerGridY = positionY / tileSize

        val totalAmmo = playerInventory.filterNotNull()
            .filter { it.type == ItemType.AMMO }
            .sumOf { it.quantity }

        val totalKeys = playerInventory.filterNotNull()
            .filter { it.type == ItemType.KEY }
            .sumOf { it.quantity }

        val totalCoins = playerInventory.filterNotNull()
            .filter { it.type == ItemType.COIN }
            .sumOf { it.quantity }

        // Cache map
        if (bufferedImage == null || !map.grid.contentDeepEquals(lastGrid)) {
            bufferedImage = BufferedImage(miniMapSize + offsetX * 2, miniMapSize + offsetY * 2, BufferedImage.TYPE_INT_ARGB)
            val bufferGraphics = bufferedImage!!.createGraphics()
            bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Clear Background
            bufferGraphics.color = Color(0, 0, 0, 0)
            bufferGraphics.fillRect(0, 0, miniMapSize + offsetX * 2, miniMapSize + offsetY * 2)

            // Calculate the rendering range around the player
            val startX = (playerGridX - maxRenderTiles / 2).toInt().coerceIn(0, map.grid[0].size - 1)
            val endX = (playerGridX + maxRenderTiles / 2).toInt().coerceIn(0, map.grid[0].size - 1)
            val startY = (playerGridY - maxRenderTiles / 2).toInt().coerceIn(0, map.grid.size - 1)
            val endY = (playerGridY + maxRenderTiles / 2).toInt().coerceIn(0, map.grid.size - 1)

            // Draw the map with the offset relative to the player, Calculate tile position relative to player position
            for (row in startY..endY) {
                for (col in startX..endX) {
                    val relativeX = col - playerGridX
                    val relativeY = row - playerGridY
                    val x = (playerMapX + relativeX * tileScale).toInt()
                    val y = (playerMapY + relativeY * tileScale).toInt()
                    val scaledTileSize = tileScale.toInt() + 1

                    // Only draw if the tile is within the boundaries of the minimap
                    if (x >= offsetX && x < miniMapSize + offsetX && y >= offsetY && y < miniMapSize + offsetY) {
                        when (map.grid[row][col]) {
                            1 -> {
                                bufferGraphics.color = Color(130, 150, 130)
                                bufferGraphics.fillRect(x, y, scaledTileSize, scaledTileSize)
                            }
                            2 -> {
                                bufferGraphics.color = Color(80, 100, 80)
                                bufferGraphics.fillRect(x, y, scaledTileSize, scaledTileSize)
                            }
                            5 -> {
                                bufferGraphics.color = Color.YELLOW
                                bufferGraphics.fillRect(x, y, scaledTileSize, scaledTileSize)
                            }
                        }
                    }
                }
            }
            bufferedImage == null
            bufferGraphics.dispose()
        }

        g2.drawImage(bufferedImage, 0, 0, null)

        //draw GUI stats
        val offsetx = 3
        val offsety = 232
        val arcSize = 20

        g2.color = Color(50, 50, 50, 180)
        g2.fillRoundRect(offsetx, offsety, 200+offsetx,200, arcSize, arcSize)
        g2.color = Color(80, 80, 80, 180)
        g2.fillRoundRect(offsetx+5, offsety+5, (200+offsetx)-10,80-5, arcSize, arcSize)

        keypng?.let {
            val offsetx = 10
            val offsety = 250
            g2.drawImage(it, offsetx, offsety, (19*3)+offsetx, (16*3)+offsety, 0, 0, it.width, it.height, null)
        }

        val enemies = renderCast.getEnemies()
        // Draw enemies and their enemy paths. Assign them a random color. Set the path color for this enemy
        enemies.forEach { enemy ->
            if (!enemyPathColors.containsKey(enemy)) {
                enemyPathColors[enemy] = Color(((72-44)..255).random(), (72..255).random(), ((72+44)..255).random(), 144)
            }
            g2.color = enemyPathColors[enemy]
            enemy.path.forEach { node ->
                val relativeX = node.x - playerGridX
                val relativeY = node.y - playerGridY
                val pointX = (playerMapX + relativeX * tileScale).toInt()
                val pointY = (playerMapY + relativeY * tileScale).toInt()
                if (pointX >= offsetX && pointX < miniMapSize + offsetX && pointY >= offsetY && pointY < miniMapSize + offsetY) {
                    g2.fillOval(pointX - 3, pointY - 3, 5, 5)
                }
            }
        }

        enemies.forEach { enemy ->
            val relativeX = (enemy.x / tileSize) - playerGridX
            val relativeY = (enemy.y / tileSize) - playerGridY
            val enemyX = (playerMapX + relativeX * tileScale).toInt()
            val enemyY = (playerMapY + relativeY * tileScale).toInt()
            if (enemyX >= offsetX && enemyX < miniMapSize + offsetX && enemyY >= offsetY && enemyY < miniMapSize + offsetY) {
                if (enemy.health > 0) {
                    if (enemy.enemyType == 0) {
                        g2.color = Color.ORANGE
                        g2.fillOval(enemyX - 3, enemyY - 3, 9, 9)
                    }
                    if (enemy.enemyType == 1) {
                        g2.color = Color.RED
                        g2.fillOval(enemyX - 3, enemyY - 3, 12, 12)
                    }
                } else {
                    g2.color = Color(0, 80, 130, 255)
                    g2.fillOval(enemyX - 3, enemyY - 3, 7, 7)
                }
            }
        }
        // draw key
        keysList.forEach { key ->
            if (key.active) {
                val relativeX = (key.x / tileSize) - playerGridX
                val relativeY = (key.y / tileSize) - playerGridY
                val keyX = (playerMapX + relativeX * tileScale).toInt()
                val keyY = (playerMapY + relativeY * tileScale).toInt()
                if (keyX >= offsetX && keyX < miniMapSize + offsetX && keyY >= offsetY && keyY < miniMapSize + offsetY) {
                    g2.color = Color.YELLOW
                    g2.fillOval(keyX - 3, keyY - 3, 6, 6)
                }
            }
        }
        // draw trader
        traders.forEach { trader ->
            if (trader.active) {
                val relativeX = (trader.x / tileSize) - playerGridX
                val relativeY = (trader.y / tileSize) - playerGridY
                val traderX = (playerMapX + relativeX * tileScale).toInt()
                val traderY = (playerMapY + relativeY * tileScale).toInt()
                if (traderX >= offsetX && traderX < miniMapSize + offsetX && traderY >= offsetY && traderY < miniMapSize + offsetY) {
                    g2.color = Color.MAGENTA
                    g2.fillOval(traderX - 3, traderY - 3, 6, 6)
                }
            }
        }
        // draw trader
        slotMachines.forEach { slotMachine ->
            if (slotMachine.active) {
                val relativeX = (slotMachine.x / tileSize) - playerGridX
                val relativeY = (slotMachine.y / tileSize) - playerGridY
                val slotMachineX = (playerMapX + relativeX * tileScale).toInt()
                val slotMachineY = (playerMapY + relativeY * tileScale).toInt()
                if (slotMachineX >= offsetX && slotMachineX < miniMapSize + offsetX && slotMachineY >= offsetY && slotMachineY < miniMapSize + offsetY) {
                    g2.color = Color.orange
                    g2.fillOval(slotMachineX - 3, slotMachineY - 3, 6, 6)
                }
            }
        }
        //draw chest
        chests.forEach { chest ->
            if (chest.active) {
                val relativeX = (chest.x / tileSize) - playerGridX
                val relativeY = (chest.y / tileSize) - playerGridY
                val chestX = (playerMapX + relativeX * tileScale).toInt()
                val chestY = (playerMapY + relativeY * tileScale).toInt()
                if (chestX >= offsetX && chestX < miniMapSize + offsetX && chestY >= offsetY && chestY < miniMapSize + offsetY) {
                    g2.color = Color(150,75,0)
                    g2.fillOval(chestX - 3, chestY - 3, 6, 6)
                }
            }
        }

        //draw medication
        medications.forEach { medication ->
            if (medication.active) {
                val relativeX = (medication.x / tileSize) - playerGridX
                val relativeY = (medication.y / tileSize) - playerGridY
                val medX = (playerMapX + relativeX * tileScale).toInt()
                val medY = (playerMapY + relativeY * tileScale).toInt()
                if (medX >= offsetX && medX < miniMapSize + offsetX && medY >= offsetY && medY < miniMapSize + offsetY) {
                    g2.color = Color.GREEN
                    g2.fillOval(medX - 3, medY - 3, 6, 6)
                }
            }
        }

        //draw ammo
        ammo.forEach { ammo ->
            if (ammo.active) {
                val relativeX = (ammo.x / tileSize) - playerGridX
                val relativeY = (ammo.y / tileSize) - playerGridY
                val ammoX = (playerMapX + relativeX * tileScale).toInt()
                val ammoY = (playerMapY + relativeY * tileScale).toInt()
                if (ammoX >= offsetX && ammoX < miniMapSize + offsetX && ammoY >= offsetY && ammoY < miniMapSize + offsetY) {
                    g2.color = Color.DARK_GRAY
                    g2.fillOval(ammoX - 3, ammoY - 3, 6, 6)
                }
            }
        }

        coinsList.forEach { coin ->
            if (coin.active) {
                val relativeX = (coin.x / tileSize) - playerGridX
                val relativeY = (coin.y / tileSize) - playerGridY
                val coinX = (playerMapX + relativeX * tileScale).toInt()
                val coinY = (playerMapY + relativeY * tileScale).toInt()
                if (coinX >= offsetX && coinX < miniMapSize + offsetX && coinY >= offsetY && coinY < miniMapSize + offsetY) {
                    g2.color = Color.YELLOW
                    g2.fillOval(coinX - 3, coinY - 3, 6, 6)
                }
            }
        }

        // draw player
        val angleRad = Math.toRadians(currentangle.toDouble())
        val lineLength = 10.0
        val playerX2 = playerMapX + (lineLength * cos(angleRad)).toInt()
        val playerY2 = playerMapY + (lineLength * sin(angleRad)).toInt()

        g2.color = Color.YELLOW
        g2.stroke = BasicStroke(2f)
        g2.drawLine(playerMapX, playerMapY, playerX2, playerY2)
        g2.color = Color.darkGray
        g2.fillRoundRect(playerMapX - 2, playerMapY - 2, 5, 5, 5, 5)

        g2.color = Color.white
        g2.fillRect(683, 384, 3, 3)

        g2.color = Color.YELLOW
        g2.font = font?.deriveFont(Font.BOLD, 17f) ?: Font("Arial", Font.BOLD, 17)

        g2.drawString("${renderCast.getRenderFps()}", 1366 - 50, 20)

        g2.drawString("HEAL: ${playerHealth}", 10, 340)
        g2.drawString("LEVEL: ${level}", 10, 360)
        g2.drawString("POINTS: ${points}", 10, 380)
        g2.drawString("AMMO: ${totalAmmo}", 10, 400)
        g2.drawString("COINS: ${coins}", 10, 420)
        if (lookchest and !inventoryVisible) {
            g2.color = Color(50, 50, 50, 180)
            g2.fillRoundRect(((1366+g2.font.size)/2)-10, ((768+g2.font.size)/2)-25, 150, 40, arcSize, arcSize)
            g2.color = Color.YELLOW
            g2.drawString("Open chest[E]", (1366+g2.font.size)/2, (768+g2.font.size)/2)
        }

        if (looktrader and !inventoryVisible) {
            g2.color = Color(50, 50, 50, 180)
            g2.fillRoundRect(((1366+g2.font.size)/2)-10, ((768+g2.font.size)/2)-25, 105, 40, arcSize, arcSize)
            g2.color = Color.YELLOW
            g2.drawString("Trade[E]", (1366+g2.font.size)/2, (768+g2.font.size)/2)
        }

        if (lookslotMachine and !inventoryVisible) {
            g2.color = Color(50, 50, 50, 180)
            g2.fillRoundRect(((1366+g2.font.size)/2)-10, ((768+g2.font.size)/2)-25, 225, 40, arcSize, arcSize)
            g2.color = Color.YELLOW
            g2.drawString("LETS GOO GAMBLING [E]", (1366+g2.font.size)/2, (768+g2.font.size)/2)
        }

        ImageIO.read(this::class.java.classLoader.getResource("textures/FPS.png"))?.let {
            val sizex = 350
            val offsetx = ((1366/2)-350/2)+60
            val offsety = 768/2
            //g2.drawImage(it, offsetx, offsety, (sizex)+offsetx, (sizex)+offsety, 0, 0, it.width, it.height, null)
        }

        g2.font = font?.deriveFont(Font.BOLD, 50f) ?: Font("Arial", Font.BOLD, 50)
        g2.drawString("${totalKeys}", 85, 290)
        keys = totalKeys
        coins = totalCoins
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(miniMapSize + offsetX * 2, miniMapSize + offsetY * 2)
    }

    fun levelUp(v: Graphics) {
        super.paintComponent(v)
        val g2 = v as Graphics2D
        g2.color = Color(80, 80, 80, 180)
        g2.fillRoundRect((1366/2)-80, (768/2)-305, 110,30, 20, 20)
        g2.color = Color.YELLOW
        g2.font = font?.deriveFont(Font.BOLD, 17f) ?: Font("Arial", Font.BOLD, 50)
        g2.drawString("level up", (1366/2)-70, 100)
        g2.color = Color.WHITE
        g2.font = Font("SansSerif", Font.PLAIN, 14)
        g2.drawString("\uD83D\uDE0E", (1366/2)+10, 100)
    }
}
package dynamicTreemaps.technique

import dynamicTreemaps.model.Entity
import dynamicTreemaps.util.Technique
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SquarifiedTreemap(root: Entity, rectangle: Rectangle, private val revision: Int, val technique: Technique) {
    private var normalizer = 0.0


    init {
        root.setRectangle(rectangle, revision)
        root.initPoint(Vector2(rectangle.x / 2.0, rectangle.y / 2.0))
        val children: List<Entity> = treemapMultidimensional(root.children, rectangle)
        for (entity in children) {
            root.addChild(entity)
        }
    }

    // Use recursion to compute single dimensional treemaps from a hierarchical dataset
    private fun treemapMultidimensional(unsortedList: MutableList<Entity>, rectangle: Rectangle): List<Entity> {

        // Sort using entities weight -- layout tends to turn out better
        val entityList = unsortedList.sortedByDescending { it.getWeight(0) }.toMutableList()

        // Make a copy of data, as the original is destroyed during treemapSingledimensional computation
        val entityCopy = entityList.toMutableList()

        treemapSingledimensional(entityList, rectangle)

        // Recursive calls for the children
        for (entity in entityCopy) {
            if (!entity.isLeaf && getNormalizedWeight(entity) > 0) {
                val newEntityList = entity.children
                treemapMultidimensional(newEntityList, entity.rectangle!!) // mmhh
            }
        }
        return entityCopy
    }

    private fun treemapSingledimensional(entityList: MutableList<Entity>, rectangle: Rectangle) {

        // Bruls' algorithm assumes that the data is normalized
        normalize(entityList, rectangle.width * rectangle.height)

        if (technique !== Technique.NMAP_ALTERNATE_CUT && technique !== Technique.NMAP_EQUAL_WEIGHTS) {
            entityList.removeIf { it.getWeight(revision) == 0.0 }
        }

        squarify(entityList, rectangle = rectangle)
    }

    private fun normalize(entityList: List<Entity>, area: Double) {
        var sum = 0.0
        if (technique === Technique.NMAP_ALTERNATE_CUT || technique === Technique.NMAP_EQUAL_WEIGHTS) { // mmhh
            for (entity in entityList) {
                var max = 0.0
                for (i in 0 until entity.numberOfRevisions) {
                    if (entity.getWeight(i) > max) {
                        max = entity.getWeight(i)
                    }
                }
                sum += max
            }
        } else {
            for (entity in entityList) {
                sum += entity.getWeight(revision)
            }
        }
        normalizer = area / sum
    }

    private fun squarify(entityList: MutableList<Entity>, currentRow: MutableList<Entity> = mutableListOf(), rectangle: Rectangle) {

        // If all elements have been placed, save coordinates into objects
        if (entityList.isEmpty()) {
            saveCoordinates(currentRow, rectangle)
            return // else?
        }

        // Test if new element should be included in current row
        val shortEdge = min(rectangle.width, rectangle.height)
        if (improvesRatio(currentRow, getNormalizedWeight(entityList[0]), shortEdge)) {

            currentRow.add(entityList[0])
            entityList.removeAt(0)

            squarify(entityList, currentRow, rectangle)

        } else {
            // New row must be created, subtract area of previous row from container
            var sum = 0.0

            for (entity in currentRow) {
                sum += getNormalizedWeight(entity)
            }

            val newRectangle = rectangle.cutArea(sum)
            saveCoordinates(currentRow, rectangle)

            val newCurrentRow = mutableListOf<Entity>()
            squarify(entityList, newCurrentRow, newRectangle)
        }
    }

    private fun saveCoordinates(currentRow: List<Entity>, rectangle: Rectangle) {

        var normalizedSum = 0.0

        for (entity in currentRow) {
            normalizedSum += getNormalizedWeight(entity)
        }

        var subxOffset = rectangle.x
        var subyOffset = rectangle.y // Offset within the container

        val areaWidth = normalizedSum / rectangle.height
        val areaHeight = normalizedSum / rectangle.width

        if (rectangle.width > rectangle.height) {
            for (entity in currentRow) {
                val x = subxOffset
                val y = subyOffset
                val height = getNormalizedWeight(entity) / areaWidth

                entity.setRectangle(Rectangle(x, y, areaWidth, height), revision)
                subyOffset += getNormalizedWeight(entity) / areaWidth

                // Save center as we'll be using it as input to the nmap algorithm  mmmhhhhhh
                entity.initPoint(Vector2(x + areaWidth / 2, y + height / 2))
            }
        } else {
            for (entity in currentRow) {
                val x = subxOffset
                val y = subyOffset
                val width = getNormalizedWeight(entity) / areaHeight

                entity.setRectangle(Rectangle(x, y, width, areaHeight), revision)
                subxOffset += getNormalizedWeight(entity) / areaHeight

                // Save center as we'll be using it as input to the nmap algorithm   mmmhhhhhh
                entity.initPoint(Vector2(x + width / 2, y + areaHeight / 2))
            }
        }
    }

    // Test if adding a new entity to row improves ratios (get closer to 1)  mmmmhhhhh
    private fun improvesRatio(currentRow: List<Entity>, nextEntity: Double, length: Double): Boolean {

        if (currentRow.isEmpty()) {
            return true
        }

        var minCurrent = Double.MAX_VALUE
        var maxCurrent = Double.MIN_VALUE

        for (entity in currentRow) {
            if (getNormalizedWeight(entity) > maxCurrent) {
                maxCurrent = getNormalizedWeight(entity)
            }
            if (getNormalizedWeight(entity) < minCurrent) {
                minCurrent = getNormalizedWeight(entity)
            }
        }
        val minNew = min(nextEntity, minCurrent)
        val maxNew = max(nextEntity, maxCurrent)

        var sumCurrent = 0.0
        for (entity in currentRow) {
            sumCurrent += getNormalizedWeight(entity)
        }
        val sumNew = sumCurrent + nextEntity
        val currentRatio = java.lang.Double.max(
            length.pow(2.0) * maxCurrent / sumCurrent.pow(2.0),
            sumCurrent.pow(2.0) / (length.pow(2.0) * minCurrent)
        )
        val newRatio = java.lang.Double.max(
            length.pow(2.0) * maxNew / sumNew.pow(2.0),
            sumNew.pow(2.0) / (length.pow(2.0) * minNew)
        )
        return currentRatio >= newRatio
    }

    private fun getNormalizedWeight(entity: Entity): Double {
        return if (technique === Technique.NMAP_ALTERNATE_CUT || technique === Technique.NMAP_EQUAL_WEIGHTS) {  // this is because the nmap starts from a squarified
            var max = 0.0
            for (i in 0 until entity.numberOfRevisions) {
                if (entity.getWeight(i) > max) {
                    max = entity.getWeight(i)
                }
            }
            max * normalizer
        } else {
            entity.getWeight(revision) * normalizer
        }
    }

}

fun Rectangle.cutArea(area: Double): Rectangle { //check what this does
    return if (width > height) {
        val areaWidth: Double = area / height
        val newWidth: Double = width - areaWidth
        Rectangle(x + areaWidth, y, newWidth, height)
    } else {
        val areaHeight: Double = area / this.width
        val newheight: Double = this.height - areaHeight
        Rectangle(x, y + areaHeight, width, newheight)
    }
}

package dynamicTreemaps.technique


import dynamicTreemaps.model.Entity
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.util.*
import kotlin.math.pow

class SpiralTreemap(root: Entity, rectangle: Rectangle, private val revision: Int) {
    private var normalizer = 0.0
    private var cutDirection = 0 // 0 WE - 1 ES - 2 SW - 3 WN

    init {
        root.setRectangle(rectangle, revision)
        root.initPoint(Vector2(rectangle.x / 2.0, rectangle.y / 2.0))
        val children: List<Entity> = treemapMultidimensional(root.children, rectangle)
        for (entity in children) {
            root.addChild(entity)
        }
    }

    // Use recursion to compute single dimensional treemaps from a hierarchical dataset
    private fun treemapMultidimensional(entityList: MutableList<Entity>, rectangle: Rectangle): List<Entity> {

        // Sort using entities weight -- layout tends to turn out better
        // entityList.sort(Comparator.comparing(o -> ((Entity) o).getWeight(0)).reversed());
        // Make a copy of data, as the original is destroyed during treemapSingledimensional computation
        val entityCopy: MutableList<Entity> = ArrayList<Entity>()
        entityCopy.addAll(entityList)
        treemapSingledimensional(entityList, rectangle)

        // Recursive calls for the children
        for (entity in entityCopy) {
            if (!entity.isLeaf && getNormalizedWeight(entity) > 0) {
                val newEntityList: MutableList<Entity> = ArrayList<Entity>()
                newEntityList.addAll(entity.children)
                treemapMultidimensional(newEntityList, entity.rectangle!!)
            }
        }
        return entityCopy
    }

    private fun treemapSingledimensional(entityList: MutableList<Entity>, rectangle: Rectangle) {
        cutDirection = 0 // 0 WE - 1 ES - 2 SW - 3 WN

        // Bruls' algorithm assumes that the data is normalized
        normalize(entityList, rectangle.width * rectangle.height)
        entityList.removeIf{ it.getWeight(revision) == 0.0 }
        val currentRow: MutableList<Entity?> = ArrayList<Entity?>()
        squarify(entityList, currentRow, rectangle)
    }

    private fun squarify(entityList: MutableList<Entity>, currentRow: MutableList<Entity?>, rectangle: Rectangle) {

        // If all elements have been placed, save coordinates into objects
        if (entityList.isEmpty()) {
            saveCoordinates(currentRow, rectangle)
            return
        }

        // Test if new element should be included in current row
        val length: Double = if (cutDirection == 0 || cutDirection == 2) rectangle.width else rectangle.height
        if (improvesRatio(currentRow, getNormalizedWeight(entityList[0]), length)) {
            currentRow.add(entityList[0])
            entityList.removeAt(0)
            squarify(entityList, currentRow, rectangle)
        } else {
            // New row must be created, subtract area of previous row from container
            var sum = 0.0
            for (entity in currentRow) {
                sum += getNormalizedWeight(entity!!)
            }
            val newRectangle = rectangle.subtractAreaFrom(cutDirection, sum)
            saveCoordinates(currentRow, rectangle)
            cutDirection++
            if (cutDirection > 3) {
                cutDirection = 0
            }
            val newCurrentRow: MutableList<Entity?> = ArrayList<Entity?>()
            squarify(entityList, newCurrentRow, newRectangle!!)
        }
    }

    private fun saveCoordinates(currentRow: List<Entity?>, rectangle: Rectangle) {
        var normalizedSum = 0.0
        for (entity in currentRow) {
            normalizedSum += getNormalizedWeight(entity!!)
        }
        var subxOffset: Double = rectangle.x
        var subyOffset: Double = rectangle.y // Offset within the container
        val areaWidth: Double = normalizedSum / rectangle.height
        val areaHeight: Double = normalizedSum / rectangle.width
        val shallowCopy: MutableList<Entity?>
        when (cutDirection) {
            0 -> for (entity in currentRow) {
                val x = subxOffset
                val y = subyOffset
                val width = getNormalizedWeight(entity!!) / areaHeight
                entity.setRectangle(Rectangle(x, y, width, areaHeight), revision)
                subxOffset += getNormalizedWeight(entity!!) / areaHeight
            }

            1 -> for (entity in currentRow) {
                val x: Double = subxOffset + rectangle.width - areaWidth
                val y = subyOffset
                val height = getNormalizedWeight(entity!!) / areaWidth
                entity.setRectangle(Rectangle(x, y, areaWidth, height), revision)
                subyOffset += getNormalizedWeight(entity!!) / areaWidth
            }

            2 -> {
                shallowCopy = currentRow.subList(0, currentRow.size).toMutableList()
                Collections.reverse(shallowCopy)
                for (entity in shallowCopy) {
                    val x = subxOffset
                    val y: Double = subyOffset + rectangle.height - areaHeight
                    val width = getNormalizedWeight(entity!!) / areaHeight
                    entity.setRectangle(Rectangle(x, y, width, areaHeight), revision)
                    subxOffset += getNormalizedWeight(entity) / areaHeight
                }
                shallowCopy.clear()
            }

            3 -> {
                shallowCopy = currentRow.subList(0, currentRow.size).toMutableList()
                Collections.reverse(shallowCopy)
                for (entity in shallowCopy) {
                    val height = getNormalizedWeight(entity!!) / areaWidth
                    val x = subxOffset
                    val y = subyOffset
                    entity.setRectangle(Rectangle(x, y, areaWidth, height), revision)
                    subyOffset += getNormalizedWeight(entity!!) / areaWidth
                }
                shallowCopy.clear()
            }
        }
    }

    // Test if adding a new entity to row improves ratios (get closer to 1)
    fun improvesRatio(currentRow: List<Entity?>, nextEntity: Double, length: Double): Boolean {
        if (currentRow.isEmpty()) {
            return true
        }
        var minCurrent = Double.MAX_VALUE
        var maxCurrent = Double.MIN_VALUE
        for (entity in currentRow) {
            if (getNormalizedWeight(entity!!) > maxCurrent) {
                maxCurrent = getNormalizedWeight(entity)
            }
            if (getNormalizedWeight(entity) < minCurrent) {
                minCurrent = getNormalizedWeight(entity)
            }
        }
        val minNew = if (nextEntity < minCurrent) nextEntity else minCurrent
        val maxNew = if (nextEntity > maxCurrent) nextEntity else maxCurrent
        var sumCurrent = 0.0
        for (entity in currentRow) {
            sumCurrent += getNormalizedWeight(entity!!)
        }
        val sumNew = sumCurrent + nextEntity
        val currentRatio = java.lang.Double.max(
            length.pow(2.0) * maxCurrent / Math.pow(sumCurrent, 2.0),
            Math.pow(sumCurrent, 2.0) / (Math.pow(length, 2.0) * minCurrent)
        )
        val newRatio = java.lang.Double.max(
            Math.pow(length, 2.0) * maxNew / Math.pow(sumNew, 2.0),
            Math.pow(sumNew, 2.0) / (Math.pow(length, 2.0) * minNew)
        )
        return currentRatio >= newRatio
    }

    private fun normalize(entityList: List<Entity>, area: Double) {
        var sum = 0.0
        for (entity in entityList) {
            sum += entity.getWeight(revision)
        }
        normalizer = area / sum
    }

    private fun getNormalizedWeight(entity: Entity): Double {
        return entity.getWeight(revision) * normalizer
    }
}

fun Rectangle.subtractAreaFrom(cutDirection: Int, area: Double): Rectangle? {
    val areaHeight: Double
    val newheight: Double
    val areaWidth: Double
    val newWidth: Double
    when (cutDirection) {
        0 -> {
            areaHeight = area / this.width
            newheight = this.height - areaHeight
            return Rectangle(x, y + areaHeight, width, newheight)
        }

        1 -> {
            areaWidth = area / height
            newWidth = width - areaWidth
            return Rectangle(x, y, newWidth, height)
        }

        2 -> {
            areaHeight = area / this.width
            newheight = this.height - areaHeight
            return Rectangle(x, y, width, newheight)
        }

        3 -> {
            areaWidth = area / height
            newWidth = width - areaWidth
            return Rectangle(x + areaWidth, y, newWidth, height)
        }
    }
    return null
}
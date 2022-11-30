package dynamicTreemaps.technique


import dynamicTreemaps.model.Entity
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
class StripTreemap(root: Entity, rectangle: Rectangle, private val revision: Int) {
    private var normalizer = 0.0

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

        // Bruls' algorithm assumes that the data is normalized
        normalize(entityList, rectangle.width * rectangle.height)
        entityList.removeIf{ it.getWeight(revision) == 0.0 }
        val currentRow: MutableList<Entity> = ArrayList<Entity>()
        strip(entityList, currentRow, rectangle)
    }

    private fun strip(entityList: MutableList<Entity>, currentRow: MutableList<Entity>, rectangle: Rectangle) {

        // If all elements have been placed, save coordinates into objects
        if (entityList.isEmpty()) {
            saveCoordinates(currentRow, rectangle)
            return
        }

        // Test if new element should be included in current row
        if (improvesRatio(currentRow, getNormalizedWeight(entityList[0]), rectangle.width)) {
            currentRow.add(entityList[0])
            entityList.removeAt(0)
            strip(entityList, currentRow, rectangle)
        } else {
            // New row must be created, subtract area of previous row from container
            var sum = 0.0
            for (entity in currentRow) {
                sum += getNormalizedWeight(entity)
            }
            val newRectangle: Rectangle = rectangle.subtractAreaFromTop(sum)
            saveCoordinates(currentRow, rectangle)
            val newCurrentRow: MutableList<Entity> = ArrayList<Entity>()
            strip(entityList, newCurrentRow, newRectangle)
        }
    }

    private fun saveCoordinates(currentRow: List<Entity>, rectangle: Rectangle) {
        var normalizedSum = 0.0
        for (entity in currentRow) {
            normalizedSum += getNormalizedWeight(entity)
        }
        var subxOffset: Double = rectangle.x
        val subyOffset: Double = rectangle.y // Offset within the container
        val areaWidth: Double = normalizedSum / rectangle.height
        val areaHeight: Double = normalizedSum / rectangle.width

        for (entity in currentRow) {
            val x = subxOffset
            val width = getNormalizedWeight(entity) / areaHeight
            entity.setRectangle(Rectangle(x, subyOffset, width, areaHeight), revision)
            subxOffset += getNormalizedWeight(entity) / areaHeight
            // Save center as we'll be using it as input to the nmap algorithm
            entity.initPoint(Vector2(x + width / 2, subyOffset + areaHeight / 2))
        }
    }

    // Test if adding a new entity to row improves ratios (get closer to 1)
    fun improvesRatio(currentRow: List<Entity>, nextEntity: Double, length: Double): Boolean {
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
        val minNew = if (nextEntity < minCurrent) nextEntity else minCurrent
        val maxNew = if (nextEntity > maxCurrent) nextEntity else maxCurrent
        var sumCurrent = 0.0
        for (entity in currentRow) {
            sumCurrent += getNormalizedWeight(entity)
        }
        val sumNew = sumCurrent + nextEntity
        val currentRatio = java.lang.Double.max(
            Math.pow(length, 2.0) * maxCurrent / Math.pow(sumCurrent, 2.0),
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

    init {
        root.setRectangle(rectangle, revision)
        root.initPoint(Vector2(rectangle.x / 2.0, rectangle.y / 2.0))
        val children: List<Entity> = treemapMultidimensional(root.children, rectangle)
        for (entity in children) {
            root.addChild(entity)
        }
    }
}

fun Rectangle.subtractAreaFromTop(area: Double): Rectangle {
    val areaHeight: Double = area / this.width
    val newheight: Double = this.height - areaHeight
    return Rectangle(x, y + areaHeight, width, newheight)
}
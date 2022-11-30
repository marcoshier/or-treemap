package dynamicTreemaps.technique

import dynamicTreemaps.model.Entity
import org.openrndr.shape.Rectangle

class SliceAndDice(root: Entity, rectangle: Rectangle, private val revision: Int) {
    private var normalizer = 0.0

    // Use recursion to compute single dimensional treemaps from a hierarchical dataset
    private fun treemapMultidimensional(entityList: List<Entity>, rectangle: Rectangle, verticalCut: Boolean) {

        // Make a copy of the data
        val entityCopy: MutableList<Entity> = ArrayList<Entity>()
        entityCopy.addAll(entityList)
        treemapSingledimensional(entityCopy, rectangle, verticalCut)

        // Recursive calls for the children
        for (entity in entityCopy) {
            if (!entity.isLeaf && getNormalizedWeight(entity) > 0) {
                val newEntityList: MutableList<Entity> = ArrayList<Entity>()
                newEntityList.addAll(entity.children)
                treemapMultidimensional(newEntityList, entity.rectangle!!, !verticalCut)
            }
        }
    }

    private fun treemapSingledimensional(entityList: MutableList<Entity>, rectangle: Rectangle, verticalCut: Boolean) {
        entityList.removeIf{it.getWeight(revision) == 0.0 }
        if (verticalCut) {
            var xOffset: Double = rectangle.x
            for (entity in entityList) {
                val width: Double = getNormalizedWeight(entity) / rectangle.height
                entity.setRectangle(Rectangle(xOffset, rectangle.y, width, rectangle.height), revision)
                xOffset += width
            }
        } else {
            var yOffset: Double = rectangle.y
            for (entity in entityList) {
                val height: Double = getNormalizedWeight(entity) / rectangle.width
                entity.setRectangle(Rectangle(rectangle.x, yOffset, rectangle.width, height), revision)
                yOffset += height
            }
        }
    }

    private fun getNormalizedWeight(entity: Entity): Double {
        return entity.getWeight(revision) * normalizer
    }

    init {
        normalizer = rectangle.width * rectangle.height / root.getWeight(revision)
        root.setRectangle(rectangle, revision)
        treemapMultidimensional(root.children, rectangle, rectangle.width > rectangle.height)
    }
}
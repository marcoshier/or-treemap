package dynamicTreemaps.technique

import dynamicTreemaps.model.Entity
import dynamicTreemaps.util.Technique
import org.openrndr.shape.Rectangle
import kotlin.math.min

class OrderedTreemap(root: Entity, rectangle: Rectangle, private val revision: Int, val technique: Technique) {
    private var normalizer = 0.0
    private fun treemapMultidimensional(entityList: List<Entity>, rectangle: Rectangle) {
        val entityCopy: MutableList<Entity> = ArrayList()
        entityCopy.addAll(entityList)
        treemapSingledimensional(entityCopy, rectangle)

        // Recursive calls for the children
        for (entity in entityCopy) {
            if (!entity.isLeaf && entity.getWeight(revision) > 0) {
                val newEntityList: MutableList<Entity> = ArrayList<Entity>()
                newEntityList.addAll(entity.children)
                treemapMultidimensional(newEntityList, entity.rectangle!!)
            }
        }
    }

    private fun treemapSingledimensional(entityList: MutableList<Entity>, rectangle: Rectangle?) {

        entityList.removeIf { it.getWeight(revision) == 0.0 }
        normalize(entityList, rectangle!!.width * rectangle.height)

        // Pivot-by-middle
        if (entityList.size == 0) {
            return
        } else if (entityList.size == 1) {
            entityList[0].setRectangle(rectangle, revision)
        } else if (entityList.size == 2) {
            val A: Entity = entityList[0]
            val B: Entity = entityList[1]
            if (rectangle.width > rectangle.height) {
                val aWidth: Double =
                    getNormalizedWeight(A) / (getNormalizedWeight(A) + getNormalizedWeight(B)) * rectangle.width
                A.setRectangle(Rectangle(rectangle.x, rectangle.y, aWidth, rectangle.height), revision)
                B.setRectangle(
                    Rectangle(rectangle.x + aWidth, rectangle.y, rectangle.width - aWidth, rectangle.height),
                    revision
                )
            } else {
                val aHeight: Double =
                    getNormalizedWeight(A) / (getNormalizedWeight(A) + getNormalizedWeight(B)) * rectangle.height
                A.setRectangle(Rectangle(rectangle.x, rectangle.y, rectangle.width, aHeight), revision)
                B.setRectangle(
                    Rectangle(rectangle.x, rectangle.y + aHeight, rectangle.width, rectangle.height - aHeight),
                    revision
                )
            }
        } else if (entityList.size == 3) {
            val A: Entity = entityList[0]
            val B: Entity = entityList[1]
            val C: Entity = entityList[2]
            if (rectangle.width > rectangle.height) {
                val aWidth: Double =
                    getNormalizedWeight(A) / (getNormalizedWeight(A) + getNormalizedWeight(B) + +getNormalizedWeight(C)) * rectangle.width
                A.setRectangle(Rectangle(rectangle.x, rectangle.y, aWidth, rectangle.height), revision)
                treemapSingledimensional(
                    mutableListOf(B, C),
                    Rectangle(rectangle.x + aWidth, rectangle.y, rectangle.width - aWidth, rectangle.height)
                )
            } else {
                val aHeight: Double =
                    getNormalizedWeight(A) / (getNormalizedWeight(A) + getNormalizedWeight(B) + getNormalizedWeight(C)) * rectangle.height
                A.setRectangle(Rectangle(rectangle.x, rectangle.y, rectangle.width, aHeight), revision)
                treemapSingledimensional(
                    mutableListOf(B, C),
                    Rectangle(rectangle.x, rectangle.y + aHeight, rectangle.width, rectangle.height - aHeight)
                )
            }
        } else {
            var pivotIndex = 0
            if (technique === Technique.ORDERED_TREEMAP_PIVOT_BY_MIDDLE) {
                pivotIndex = entityList.size / 2
            } else if (technique === Technique.ORDERED_TREEMAP_PIVOT_BY_SIZE) {
                var biggestValueIndex = 0
                var biggestValue = 0.0
                for (i in entityList.indices) {
                    if (entityList[i].getWeight(revision) > biggestValue) {
                        biggestValue = entityList[i].getWeight(revision)
                        biggestValueIndex = i
                    }
                }
                pivotIndex = biggestValueIndex
            }
            val pivot: Entity = entityList[pivotIndex]
            val l1: MutableList<Entity> = entityList.subList(0, pivotIndex)
            val l2: MutableList<Entity> = ArrayList<Entity>()
            val l3: MutableList<Entity> = entityList.subList(pivotIndex + 1, entityList.size)
            var r: Rectangle? = null
            var r1: Rectangle? = null
            var rMinusR1: Rectangle? = null
            var totalWeight = 0.0
            for (entity in entityList) {
                totalWeight += entity.getWeight(revision)
            }
            var l1Weight = 0.0
            for (entity in l1) {
                l1Weight += entity.getWeight(revision)
            }
            if (rectangle.width > rectangle.height) {
                val r1Width: Double = l1Weight / totalWeight * rectangle.width
                r1 = Rectangle(rectangle.x, rectangle.y, r1Width, rectangle.height)
                rMinusR1 = Rectangle(rectangle.x + r1Width, rectangle.y, rectangle.width - r1Width, rectangle.height)
                r = Rectangle(
                    rectangle.x + r1Width,
                    rectangle.y,
                    getNormalizedWeight(pivot) / rMinusR1.height,
                    rectangle.height
                )
            } else {
                val r1Height: Double = l1Weight / totalWeight * rectangle.height
                r1 = Rectangle(rectangle.x, rectangle.y, rectangle.width, r1Height)
                rMinusR1 = Rectangle(rectangle.x, rectangle.y + r1Height, rectangle.width, rectangle.height - r1Height)
                r = Rectangle(
                    rectangle.x,
                    rectangle.y + r1Height,
                    rectangle.width,
                    getNormalizedWeight(pivot) / rMinusR1.width
                )
            }
            var pPreviousAspectRatio: Double = min(r.width / r.height, r.height / r.width)
            var pNewWidth = 0.0
            var pNewHeight = 0.0
            var pNewAspectRatio: Double
            while (l3.size > 2) {
                l2.add(0, l3[0])
                l3.removeAt(0)
                var l2Weight = 0.0
                for (entity in l2) {
                    l2Weight += getNormalizedWeight(entity)
                }
                if (rectangle.width > rectangle.height) {
                    pNewWidth = (getNormalizedWeight(pivot) + l2Weight) / rMinusR1.height
                    pNewHeight = getNormalizedWeight(pivot) / (getNormalizedWeight(pivot) + l2Weight) * rMinusR1.height
                    pNewAspectRatio = Math.min(pNewWidth / pNewHeight, pNewHeight / pNewWidth)
                    if (pNewAspectRatio < pPreviousAspectRatio) {
                        l3.add(0, l2[l2.size - 1])
                        l2.removeAt(l2.size - 1)
                        break
                    } else {
                        r = Rectangle(rMinusR1.x, rMinusR1.y, pNewWidth, pNewHeight)
                        pPreviousAspectRatio = pNewAspectRatio
                    }
                } else {
                    pNewHeight = (getNormalizedWeight(pivot) + l2Weight) / rMinusR1.width
                    pNewWidth = getNormalizedWeight(pivot) / (getNormalizedWeight(pivot) + l2Weight) * rMinusR1.width
                    pNewAspectRatio = Math.min(pNewWidth / pNewHeight, pNewHeight / pNewWidth)
                    if (pNewAspectRatio < pPreviousAspectRatio) {
                        l3.add(0, l2[l2.size - 1])
                        l2.removeAt(l2.size - 1)
                        break
                    } else {
                        r = Rectangle(rMinusR1.x, rMinusR1.y, pNewWidth, pNewHeight)
                        pPreviousAspectRatio = pNewAspectRatio
                    }
                }
            }
            pivot.setRectangle(r, revision)
            if (rectangle.width > rectangle.height) {
                treemapSingledimensional(l1, r1)
                if (l2.size > 0) {
                    val r2 = Rectangle(rMinusR1.x, rMinusR1.y + r!!.height, r.width, rMinusR1.height - r.height)
                    treemapSingledimensional(l2, r2)
                }
                val r3 = Rectangle(rMinusR1.x + r!!.width, rMinusR1.y, rMinusR1.width - r.width, rMinusR1.height)
                treemapSingledimensional(l3, r3)
            } else {
                treemapSingledimensional(l1, r1)
                if (l2.size > 0) {
                    val r2 = Rectangle(rMinusR1.x + r!!.width, rMinusR1.y, rMinusR1.width - r.width, r.height)
                    treemapSingledimensional(l2, r2)
                }
                val r3 = Rectangle(rMinusR1.x, rMinusR1.y + r!!.height, rMinusR1.width, rMinusR1.height - r.height)
                treemapSingledimensional(l3, r3)
            }
        }
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
        treemapMultidimensional(root.children, rectangle)
    }
}
package dynamicTreemaps.technique


import dynamicTreemaps.model.Entity
import dynamicTreemaps.util.Technique
import org.openrndr.shape.Rectangle
import java.util.*
import java.util.function.Predicate
import java.util.function.ToDoubleFunction

class Nmap(root: Entity, rectangle: Rectangle, private val revision: Int, val technique: Technique) {


    init {

        // Compute Squarified Treemap to set anchors
        if (revision == 0) {
            SquarifiedTreemap(root, rectangle, revision, technique)
        }
        nmap(root.children, rectangle)
    }

    private fun nmap(entityList: List<Entity>, rectangle: Rectangle) {
        val entityCopy: MutableList<Entity> = ArrayList<Entity>()
        entityCopy.addAll(entityList)
        for (entity in entityCopy) {
            if (entity.getWeight(revision) > 0) {
                entity.setMovingPoint(entity.anchorPoint!!.x, entity.anchorPoint!!.y)
            }
        }
        if (technique === Technique.NMAP_ALTERNATE_CUT) {
            alternateCut(entityList, rectangle)
        } else if (technique === Technique.NMAP_EQUAL_WEIGHTS) {
            equalWeight(entityList, rectangle)
        }
        for (entity in entityCopy) {
            if (!entity.isLeaf && entity.getWeight(revision) > 0) {
                nmap(entity.children, entity.rectangle!!)
            }
        }
    }

    private fun alternateCut(entityList: List<Entity>, rectangle: Rectangle) {
        if (rectangle.width > rectangle.height) {
            alternateCut(entityList, rectangle, true)
        } else {
            alternateCut(entityList, rectangle, false)
        }
    }

    private fun alternateCut(entityList: List<Entity>, rectangle: Rectangle, verticalBissection: Boolean) {
        val entityCopy: MutableList<Entity> = ArrayList<Entity>()
        entityCopy.addAll(entityList)
        entityCopy.removeIf(Predicate<Entity> { entity: Entity -> entity.getWeight(revision) <= 0.0 })
        if (entityCopy.size == 0) {
            return
        } else if (entityCopy.size == 1) {
            // Done dividing
            entityCopy[0].setRectangle(rectangle, revision)
            entityCopy[0].setAnchorPoint(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height / 2)
            // System.out.println("ctx.rect(" + rectangle.x + ", " + rectangle.y + ", " + rectangle.width + ", " + rectangle.height + ");");
        } else {
            if (verticalBissection) {
                entityCopy.sortBy {
                    it.anchorPoint!!.x
                }
            } else {
                entityCopy.sortBy {
                    it.anchorPoint!!.x
                }
            }
            val cutIndex = entityCopy.size / 2
            val entityListA: List<Entity> = entityCopy.subList(0, cutIndex)
            val entityListB: List<Entity> = entityCopy.subList(cutIndex, entityCopy.size)
            val sumA = entityListA.stream().mapToDouble(ToDoubleFunction<Entity> { entity: Entity ->
                entity.getWeight(
                    revision
                )
            }).sum()
            val sumB = entityListB.stream().mapToDouble(ToDoubleFunction<Entity> { entity: Entity ->
                entity.getWeight(
                    revision
                )
            }).sum()
            val sumTotal = sumA + sumB
            val rectangleA: Rectangle
            val rectangleB: Rectangle
            if (verticalBissection) {
                val rectangleWidthA: Double = sumA / sumTotal * rectangle.width
                val rectangleWidthB: Double = sumB / sumTotal * rectangle.width
                val boundary: Double =
                    (entityListA[entityListA.size - 1].anchorPoint!!.x + entityListB[0].anchorPoint!!.x) / 2
                rectangleA = Rectangle(
                    rectangle.x, rectangle.y,
                    boundary - rectangle.x, rectangle.height
                )
                rectangleB = Rectangle(
                    rectangle.x + rectangleA.width, rectangle.y,
                    rectangle.width - rectangleA.width, rectangle.height
                )
                val affineMatrixA = doubleArrayOf(
                    rectangleWidthA / rectangleA.width,
                    0.0,
                    0.0,
                    1.0,
                    rectangle.x * (1 - rectangleWidthA / rectangleA.width),
                    0.0
                )
                val affineMatrixB = doubleArrayOf(
                    rectangleWidthB / rectangleB.width,
                    0.0,
                    0.0,
                    1.0,
                    (rectangle.x + rectangle.width) * (1 - rectangleWidthB / rectangleB.width),
                    0.0
                )
                affineTransformation(entityListA, affineMatrixA)
                affineTransformation(rectangleA, affineMatrixA)
                affineTransformation(entityListB, affineMatrixB)
                affineTransformation(rectangleB, affineMatrixB)
            } else {
                val rectangleHeightA: Double = sumA / sumTotal * rectangle.height
                val rectangleHeightB: Double = sumB / sumTotal * rectangle.height
                val boundary: Double =
                    (entityListA[entityListA.size - 1].anchorPoint!!.y + entityListB[0].anchorPoint!!.y) / 2
                rectangleA = Rectangle(
                    rectangle.x, rectangle.y,
                    rectangle.width, boundary - rectangle.y
                )
                rectangleB = Rectangle(
                    rectangle.x, rectangle.y + rectangleA.height,
                    rectangle.width, rectangle.height - rectangleA.height
                )
                val affineMatrixA = doubleArrayOf(
                    1.0,
                    0.0,
                    0.0,
                    rectangleHeightA / rectangleA.height,
                    0.0,
                    rectangle.y * (1 - rectangleHeightA / rectangleA.height)
                )
                val affineMatrixB = doubleArrayOf(
                    1.0,
                    0.0,
                    0.0,
                    rectangleHeightB / rectangleB.height,
                    0.0,
                    (rectangle.y + rectangle.height) * (1 - rectangleHeightB / rectangleB.height)
                )
                affineTransformation(entityListA, affineMatrixA)
                affineTransformation(rectangleA, affineMatrixA)
                affineTransformation(entityListB, affineMatrixB)
                affineTransformation(rectangleB, affineMatrixB)
            }
            alternateCut(entityListA, rectangleA, !verticalBissection)
            alternateCut(entityListB, rectangleB, !verticalBissection)
        }
    }

    private fun equalWeight(entityList: List<Entity>, rectangle: Rectangle) {
        val entityCopy: MutableList<Entity> = ArrayList<Entity>()
        entityCopy.addAll(entityList)
        entityCopy.removeIf(Predicate<Entity> { entity: Entity -> entity.getWeight(revision) <= 0.0 })
        if (entityCopy.size == 0) {
            return
        } else if (entityCopy.size == 1) {
            // Done dividing
            entityCopy[0].setRectangle(rectangle, revision)
            //entityCopy.get(0).setPoint(rectangle.x + rectangle.width/2, rectangle.y + rectangle.height/2);
            // System.out.println("ctx.rect(" + rectangle.x + ", " + rectangle.y + ", " + rectangle.width + ", " + rectangle.height + ");");
        } else {
            // Define if we should bisect the data vertically or horizontally and sort the data accordingly
            if (rectangle.width > rectangle.height) {
                entityCopy.sortBy {
                    it.anchorPoint!!.x
                }
            } else {
                entityCopy.sortBy {
                    it.anchorPoint!!.x
                }
            }
            val cutIndex = findEWCutElement(entityList)
            val entityListA: List<Entity> = entityCopy.subList(0, cutIndex)
            val entityListB: List<Entity> = entityCopy.subList(cutIndex, entityCopy.size)
            val sumA = entityListA.stream().mapToDouble(ToDoubleFunction<Entity> { entity: Entity ->
                entity.getWeight(
                    revision
                )
            }).sum()
            val sumB = entityListB.stream().mapToDouble(ToDoubleFunction<Entity> { entity: Entity ->
                entity.getWeight(
                    revision
                )
            }).sum()
            val sumTotal = sumA + sumB
            val rectangleA: Rectangle
            val rectangleB: Rectangle
            if (entityListA.size == 0) {
                equalWeight(entityListB, rectangle)
            } else if (entityListB.size == 0) {
                equalWeight(entityListA, rectangle)
            } else {
                if (rectangle.width > rectangle.height) {
                    val rectangleWidthA: Double = sumA / sumTotal * rectangle.width
                    val rectangleWidthB: Double = sumB / sumTotal * rectangle.width
                    val boundary: Double =
                        (entityListA[entityListA.size - 1].anchorPoint!!.x + entityListB[0].anchorPoint!!.x) / 2
                    rectangleA = Rectangle(
                        rectangle.x, rectangle.y,
                        boundary - rectangle.x, rectangle.height
                    )
                    rectangleB = Rectangle(
                        rectangle.x + rectangleA.width, rectangle.y,
                        rectangle.width - rectangleA.width, rectangle.height
                    )
                    val affineMatrixA = doubleArrayOf(
                        rectangleWidthA / rectangleA.width,
                        0.0,
                        0.0,
                        1.0,
                        rectangle.x * (1 - rectangleWidthA / rectangleA.width),
                        0.0
                    )
                    val affineMatrixB = doubleArrayOf(
                        rectangleWidthB / rectangleB.width,
                        0.0,
                        0.0,
                        1.0,
                        (rectangle.x + rectangle.width) * (1 - rectangleWidthB / rectangleB.width),
                        0.0
                    )
                    affineTransformation(entityListA, affineMatrixA)
                    affineTransformation(rectangleA, affineMatrixA)
                    affineTransformation(entityListB, affineMatrixB)
                    affineTransformation(rectangleB, affineMatrixB)
                } else {
                    val rectangleHeightA: Double = sumA / sumTotal * rectangle.height
                    val rectangleHeightB: Double = sumB / sumTotal * rectangle.height
                    val boundary: Double =
                        (entityListA[entityListA.size - 1].anchorPoint!!.y + entityListB[0].anchorPoint!!.y) / 2
                    rectangleA = Rectangle(
                        rectangle.x, rectangle.y,
                        rectangle.width, boundary - rectangle.y
                    )
                    rectangleB = Rectangle(
                        rectangle.x, rectangle.y + rectangleA.height,
                        rectangle.width, rectangle.height - rectangleA.height
                    )
                    val affineMatrixA = doubleArrayOf(
                        1.0,
                        0.0,
                        0.0,
                        rectangleHeightA / rectangleA.height,
                        0.0,
                        rectangle.y * (1 - rectangleHeightA / rectangleA.height)
                    )
                    val affineMatrixB = doubleArrayOf(
                        1.0,
                        0.0,
                        0.0,
                        rectangleHeightB / rectangleB.height,
                        0.0,
                        (rectangle.y + rectangle.height) * (1 - rectangleHeightB / rectangleB.height)
                    )
                    affineTransformation(entityListA, affineMatrixA)
                    affineTransformation(rectangleA, affineMatrixA)
                    affineTransformation(entityListB, affineMatrixB)
                    affineTransformation(rectangleB, affineMatrixB)
                }
                equalWeight(entityListA, rectangleA)
                equalWeight(entityListB, rectangleB)
            }
        }
    }

    // Transform points
    private fun affineTransformation(entityList: List<Entity>, m: DoubleArray) {
        for (entity in entityList) {
            val x: Double = entity.movingPoint!!.x * m[0] + entity.movingPoint!!.y * m[2] + m[4]
            val y: Double = entity.movingPoint!!.x * m[1] + entity.movingPoint!!.y * m[3] + m[5]
            entity.setMovingPoint(x, y)
        }
    }

    private fun affineTransformation(rectangle: Rectangle, m: DoubleArray): Rectangle {
        // Transform rectangle
        val x0: Double = rectangle.x * m[0] + rectangle.y * m[2] + m[4]
        val y0: Double = rectangle.x * m[1] + rectangle.y * m[3] + m[5]
        val x1: Double = (rectangle.x + rectangle.width) * m[0] + (rectangle.y + rectangle.height) * m[2] + m[4]
        val y1: Double = (rectangle.x + rectangle.width) * m[1] + (rectangle.y + rectangle.height) * m[3] + m[5]

        return Rectangle(x0, y0, x1 - x0, y1 - y0)
    }

    private fun findEWCutElement(entityList: List<Entity>): Int {
        var cutElement = 1
        var sumA = 0.0
        var sumB = entityList.stream().mapToDouble(ToDoubleFunction<Entity> { entity: Entity ->
            entity.getWeight(
                revision
            )
        }).sum()
        var minDiff = Double.MAX_VALUE
        for (i in entityList.indices) {
            sumA += entityList[i].getWeight(revision)
            sumB -= entityList[i].getWeight(revision)
            if (Math.abs(sumA - sumB) < minDiff) {
                minDiff = Math.abs(sumA - sumB)
                cutElement = i + 1
            } else {
                break
            }
        }
        return cutElement
    }

}
package dynamicTreemaps.model

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.color.presets.LAWN_GREEN
import org.openrndr.math.Vector2
import org.openrndr.math.clamp
import org.openrndr.math.mix
import org.openrndr.shape.Rectangle
import kotlin.math.max
import kotlin.math.min

class Entity(val id: String?, numberOfRevisions: Int) {


    private val shortId: String
    private var printId: String = ""

    var weightList = MutableList(numberOfRevisions) {0.0}
    var rectangleList = MutableList(numberOfRevisions) { Rectangle.EMPTY }

    var movingPoint: Vector2? = null
    var anchorPoint: Vector2? = null

    var rectangle: Rectangle? = null
    var pastRectangle: Rectangle? = null

    val children = mutableListOf<Entity>()
    val numberOfRevisions: Int
        get() = weightList.size


    companion object {
        var charWidth: Int = 0
    }

    init {
        val split = id!!.split("/").toTypedArray()
        shortId = split[split.size - 1]
    }


    fun getWeight(revision: Int): Double {
        return weightList[revision]
    }

    fun setWeight(weight: Double, revision: Int) {
        weightList[revision] = weight
    }

    val maximumWeight: Double
        get() = weightList.maxOrNull() ?: 0.0

    fun setAnchorPoint(x: Double, y: Double) {
        anchorPoint = Vector2(x, y)
    }

    fun initPoint(point: Vector2) {
        anchorPoint = Vector2(point.x, point.y)
        movingPoint = Vector2(point.x, point.y)
    }

    fun setMovingPoint(x: Double, y: Double) {
        //pastPoint.setValues(point.x, point.y);
        movingPoint = Vector2(x, y)
    }

    fun rectangle(progress: Double): Rectangle {

        val corner = mix(rectangle!!.corner, pastRectangle!!.corner, progress)
        val width = mix(rectangle!!.width, pastRectangle!!.width, progress)
        val height = mix(rectangle!!.height, pastRectangle!!.height, progress)

        return Rectangle(corner, width, height)
    }

    private val aspectRatio: Double
        get() {
            return min(rectangle!!.width / rectangle!!.height, rectangle!!.height / rectangle!!.width)
        }

    fun setRectangle(newRectangle: Rectangle?, revision: Int) {
        pastRectangle = if (rectangle == null) {
            newRectangle
        } else {
            rectangle
        }
        rectangle = newRectangle
        // Compute metric
        if (newRectangle != null) {
            rectangleList[revision] = Rectangle(rectangle!!.x, rectangle!!.y, rectangle!!.width, rectangle!!.height)
        }
    }

    fun addChild(entity: Entity) {
        children.add(entity)
    }

    val isLeaf: Boolean
        get() {
            return children.size == 0
        }

    fun draw(drawer: Drawer, progress: Double) {

        val f = (1 - aspectRatio).clamp(0.0, 1.0)
        val r = rectangle(progress)

        drawer.fill = ColorRGBa.LAWN_GREEN.shade(f)
        drawer.rectangle(r)

    }

    fun drawIntersection(drawer: Drawer, entityB: Entity, progress: Double) {

        val rectangle = rectangle(progress).intersection(entityB.rectangle(progress))
        val f = (((1 - aspectRatio) + (1 - entityB.aspectRatio)) / 2).clamp(0.0, 1.0)

        drawer.fill = ColorRGBa.LAWN_GREEN.shade(f)
        drawer.rectangle(rectangle)

    }

    fun drawBorder(drawer: Drawer, progress: Double) {

        val rect = rectangle(progress)
        val c = if(isLeaf) 0.3 else 1.0

        drawer.stroke = ColorRGBa.BLACK.opacify(c)
        drawer.rectangle(rect)

    }

    private fun updatePrintId(progress: Double) {

        val width = (mix(rectangle!!.width, pastRectangle!!.width, progress)).toInt() - charWidth / 2
        if (charWidth > 0) {
            printId = if (width > charWidth * shortId.length) {
                shortId
            } else {
                shortId.substring(0, width / charWidth)
            }
        }
    }

    fun drawLabel(drawer: Drawer, progress: Double) {
        if (rectangle!!.height > 20 && rectangle!!.width > 20) {
            if (progress % 0.2 < 0.01) {
                updatePrintId(progress)
            }

            val corner = mix(rectangle!!.corner, pastRectangle!!.corner, progress)

            drawer.fill = ColorRGBa.BLACK
            drawer.text(printId, corner + Vector2(4.0, 20.0))
        }
    }

    override fun toString(): String {
        return ("Entity{" +
                "id='" + id + '\'' +
                '}')
    }

    override fun equals(o: Any?): Boolean {  // ?
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val entity: Entity = o as Entity
        return if (id != null) (id == entity.id) else entity.id == null
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

}
fun Rectangle.intersection(b: Rectangle): Rectangle {

    val x0 = max(x, b.x)
    val y0 = max(y, b.y)
    val x1 = min(x + width, b.x + b.width)
    val y1 = min(y + height, b.y + b.height)
    return Rectangle(x0, y0, x1 - x0, y1 - y0)

}
package dynamicTreemaps.view

import dynamicTreemaps.model.Entity
import dynamicTreemaps.technique.*
import dynamicTreemaps.util.Technique
import org.openrndr.draw.Drawer
import org.openrndr.shape.Rectangle


class Treemap(val root: Entity, val canvas: Rectangle, val technique: Technique) {

    var entityList = mutableListOf<Entity>()
    private var revision = 0

    init {
        flattenTree(root)
        for (entity in entityList) {
            entity.setRectangle(null, 0)
        }
        root.setRectangle(canvas, 0)
    }


    private fun flattenTree(entity: Entity) {
        entityList.add(entity)
        for (child in entity.children) {
            if (child.isLeaf) {
                entityList.add(child)
            } else {
                flattenTree(child)
            }
        }
    }

    fun compute() {
        when (technique) {
            Technique.NMAP_ALTERNATE_CUT, Technique.NMAP_EQUAL_WEIGHTS -> Nmap(root, canvas, revision, technique)
            Technique.SQUARIFIED -> SquarifiedTreemap(root, canvas, revision, technique)
            Technique.ORDERED_TREEMAP_PIVOT_BY_MIDDLE, Technique.ORDERED_TREEMAP_PIVOT_BY_SIZE -> OrderedTreemap(root, canvas, revision, technique)
            Technique.SLICE_AND_DICE -> SliceAndDice(root, canvas, revision)
            Technique.STRIP -> StripTreemap(root, canvas, revision)
            Technique.SPIRAL -> SpiralTreemap(root, canvas, revision)
        }
        // computeAspectRatioAverage();
    }

    fun handleKeypress() {
        if (revision < root.numberOfRevisions - 1) {
            revision++
            compute()
        } else {
            println("popi")
        }

    }

    fun draw(drawer: Drawer, progress: Double) {

        var maxWeight = 0.0
        for (i in 0 until root.numberOfRevisions) {
            if (root.getWeight(i) > maxWeight) {
                maxWeight = root.getWeight(i)
            }
        }

        for (entity in entityList) {
            if (entity.getWeight(revision) > 0 && entity.isLeaf) {
                entity.draw(drawer, progress)
            }
            if (entity.getWeight(revision) > 0) {
                entity.drawBorder(drawer, progress)
            }
        }
    }



}
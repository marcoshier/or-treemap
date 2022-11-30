package dynamicTreemaps.util

import dynamicTreemaps.model.Entity
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.*
import kotlin.system.exitProcess

object DataHelper {
    private var numberOfRevisions = 0


    fun buildHierarchy(csvFile: String): Entity {

        val entityList = parseCSV(csvFile).sortedBy { it.id }
        val auxList = mutableListOf<Entity>()
        val root = Entity("", numberOfRevisions)

        auxList.add(root)

        // Build hierarchy
        for (entity in entityList) {

            var current = root
            val dividerIndex = entity.id!!.lastIndexOf("/")

            if (dividerIndex != -1) {
                val prefix: String = entity.id.substring(0, dividerIndex)
                val parents: Array<String> = prefix.split("/").toTypedArray()

                for (parentId in parents) {
                    current = if (contains(current.children, parentId)) {
                        find(current.children, parentId)!!
                    } else {
                        val parent = Entity(parentId, numberOfRevisions)
                        current.addChild(parent)
                        parent
                    }
                }
                current.addChild(entity)
            } else {
                root.addChild(entity)
            }
        }
        sumTree(root)
        return root
    }


    private fun parseCSV(fileName: String): List<Entity> {

        val entityList = mutableListOf<Entity>()


        try {
            val bufferedReader = BufferedReader(FileReader(fileName))
            var currentLine = bufferedReader.readLine()
            val header  = currentLine.split(",").toTypedArray()
            if (header[0] != "id" || header[1] != "weight") {
                System.err.println("Error parsing header - $fileName")
                exitProcess(-1)
            }
            while (bufferedReader.readLine().also { currentLine = it } != null) {
                val split: Array<String> = currentLine.split(",").toTypedArray()
                if (split.size != 2) {
                    System.err.println("Error parsing csv file")
                    exitProcess(-1)
                } else {
                    val id = split[0]
                    val weight: Double = split[1].toDouble()

                    if (contains(entityList, id)) {
                        val entity = find(entityList, id) // mmhh
                        entity?.setWeight(weight, 0)
                    } else {
                        val entity = Entity(id, numberOfRevisions)

                        entity.setWeight(weight, 0)
                        entityList.add(entity)
                    }
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return entityList
    }

    private fun sumTree(entity: Entity): List<Double> {
        return if (entity.isLeaf) {
            entity.weightList
        } else {
            for (child in entity.children) {
                val weightList = sumTree(child)
                for (revision in weightList.indices) {
                    entity.setWeight(entity.getWeight(revision) + weightList[revision], revision)
                }
            }
            entity.weightList
        }
    }

    private fun find(entityList: List<Entity>, entityId: String): Entity? {
        for (i in entityList.indices) {
            if (entityList[i].id.equals(entityId)) {
                return entityList[i]
            }
        }
        return null
    }

    private fun contains(entityList: List<Entity>, id: String): Boolean {
        for (entity in entityList) {
            if (entity.id.equals(id)) {
                return true
            }
        }
        return false
    }
}
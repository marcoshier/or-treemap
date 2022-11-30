package dynamicTreemaps

import dynamicTreemaps.util.DataHelper.buildHierarchy
import dynamicTreemaps.util.Technique
import dynamicTreemaps.view.Treemap
import org.openrndr.application

fun main() = application {
    configure {
        width = 1080
        height = 1080
    }

    program {

        val frame = drawer.bounds.offsetEdges(-80.0)

        val hierachy = buildHierarchy("data/names.csv")
        val treemap = Treemap(hierachy, frame, Technique.SQUARIFIED)

        keyboard.keyUp.listen {
            if (it.name == "X") {
                treemap.handleKeypress()
            }
        }

        treemap.compute()

        extend {

            treemap.draw(drawer, mouse.position.x / width)

        }
    }


}

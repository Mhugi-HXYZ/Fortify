package xyz.handshot.fortify.forts

import org.bukkit.Location
import java.util.*

class Fort {
    var id = UUID(0, 0)
    var owner = UUID(0, 0)
    var members = mutableSetOf<UUID>()
    var center: Location? = null
    var level = 1
}
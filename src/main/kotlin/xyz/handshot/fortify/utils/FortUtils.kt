package xyz.handshot.fortify.utils

import org.bukkit.Location
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.handshot.fortify.commands.FortifyCommand
import xyz.handshot.fortify.forts.Fort
import xyz.handshot.fortify.forts.FortCache
import java.util.*

object FortUtils : KoinComponent {

    private val fortCache: FortCache by inject()

    fun getFortFortification(fort: Fort): Int {
        // TODO Implement levels
        return 3
    }

    fun getClosestFort(location: Location): Pair<Fort?, Double> {
        val fort = fortCache.list().filter { it.center != null }.minByOrNull { it.center!!.distance(location) }
        var distance = -1.0

        if (fort != null) {
            val loc = location.clone().apply { y = 0.0 }
            val locFort = fort.center!!.clone().apply { y = 0.0 }
            distance = locFort.distance(loc)
        }

        return fort to distance
    }

    fun getOwningFort(location: Location): Fort? {
        fortCache.list().filter { it.center != null }.forEach {
            val center = it.center!!
            val areaSize = 100 // TODO Implement levels with differing area sizes

            val minX = center.x - areaSize
            val minZ = center.z - areaSize
            val maxX = center.x + areaSize
            val maxZ = center.z + areaSize

            if (location.x in minX..maxX && location.z in minZ..maxZ) {
                return it
            }
        }
        return null
    }

    fun getFortWithCenter(location: Location): Fort? {
        return fortCache.list().firstOrNull { location == it.center }
    }

    fun getOwnedFort(owner: UUID): Fort? {
        return fortCache.list().firstOrNull { it.owner == owner }
    }

}
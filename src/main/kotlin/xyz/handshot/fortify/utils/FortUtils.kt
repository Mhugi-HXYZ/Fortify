package xyz.handshot.fortify.utils

import org.bukkit.Location
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.handshot.fortify.forts.Fort
import xyz.handshot.fortify.forts.FortCache
import xyz.handshot.fortify.levels.LevelCache
import java.util.*

object FortUtils : KoinComponent {

    private val fortCache: FortCache by inject()
    private val levelCache: LevelCache by inject()

    fun getFortFortification(fort: Fort): Int {
        val level = levelCache.get(fort.level)!!
        return level.fortification
    }

    fun getFortRadius(fort: Fort): Int {
        val level = levelCache.get(fort.level)!!
        return level.radius
    }

    fun getDefaultFortRadius(): Int{
        return levelCache.get(1)!!.radius
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
            val areaSize = getFortRadius(it)

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
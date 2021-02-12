package xyz.handshot.fortify.forts.impl

import xyz.handshot.fortify.forts.Fort
import xyz.handshot.fortify.forts.FortCache
import java.util.*

class SimpleFortCache : FortCache {

    private val cache = mutableMapOf<UUID, Fort>()

    override fun cache(fort: Fort) {
        cache[fort.id] = fort
    }

    override fun get(id: UUID): Fort? {
        return cache[id]
    }

    override fun invalidate(fort: Fort) {
        cache.remove(fort.id)
    }

    override fun list(): Array<Fort> {
        return cache.values.toTypedArray()
    }

}
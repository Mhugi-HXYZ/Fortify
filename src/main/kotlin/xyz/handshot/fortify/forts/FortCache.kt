package xyz.handshot.fortify.forts

import java.util.*

interface FortCache {
    fun cache(fort: Fort)
    fun get(id: UUID): Fort?
    fun invalidate(fort: Fort)
    fun list(): Array<Fort>
}
package xyz.handshot.fortify.forts

import java.util.*

interface FortRepository {
    fun existsById(id: UUID): Boolean
    fun existsByOwner(id: UUID): Boolean
    fun findById(id: UUID): Fort?
    fun findByOwner(id: UUID): Fort?
    fun findAll(): Array<Fort>
    fun update(fort: Fort)
    fun delete(fort: Fort)
}
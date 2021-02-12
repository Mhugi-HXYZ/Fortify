package xyz.handshot.fortify.levels

interface LevelRepository {
    fun findAll(): Array<Level>
    fun find(id: String): Level?
    fun update(level: Level)
}
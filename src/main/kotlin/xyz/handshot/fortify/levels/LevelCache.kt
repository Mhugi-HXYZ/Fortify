package xyz.handshot.fortify.levels

interface LevelCache {
    fun list(): Array<Level>
    fun cache(level: Level)
    fun get(level: Int): Level?
    fun invalidate(level: Level)
}
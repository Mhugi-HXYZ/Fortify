package xyz.handshot.fortify.levels.impl

import xyz.handshot.fortify.levels.Level
import xyz.handshot.fortify.levels.LevelCache

class SimpleLevelCache : LevelCache {

    private val levels = mutableMapOf<Int, Level>()

    override fun list(): Array<Level> {
        return levels.values.toTypedArray()
    }

    override fun cache(level: Level) {
        levels[level.level] = level
    }

    override fun get(level: Int): Level? {
        return levels[level]
    }

    override fun invalidate(level: Level) {
        levels.remove(level.level)
    }
}
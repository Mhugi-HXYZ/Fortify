package xyz.handshot.fortify.levels.impl

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import xyz.handshot.fortify.levels.Level
import xyz.handshot.fortify.levels.LevelRepository
import java.io.File

class YmlLevelRepository(private val plugin: Plugin) : LevelRepository {

    private val file = File(plugin.dataFolder, "levels.yml")
    private var yml = YamlConfiguration.loadConfiguration(file)

    init {
        if (!file.exists()) {
            plugin.saveResource("levels.yml", false)
            yml = YamlConfiguration.loadConfiguration(file)
        }
    }

    override fun findAll(): Array<Level> {
        val levels = mutableSetOf<Level>()
        yml.root?.getKeys(false)?.forEach {
            val level = find(it)
            if (level != null) {
                levels.add(level)
            }
        }

        return levels.toTypedArray()
    }

    override fun find(id: String): Level? {
        if (!yml.contains(id) && !yml.isConfigurationSection(id)) {
            return null
        }
        return Level().apply {
            this.id = id
            this.level = yml.getInt("$id.level")
            name = yml.getString("$id.name") ?: "Untitled"
            icon = Material.matchMaterial(yml.getString("$id.icon") ?: "stone") ?: Material.STONE
            price = yml.getDouble("$id.price")
            radius = yml.getInt("$id.radius")
            fortification = yml.getInt("$id.fortification")
        }
    }

    override fun update(level: Level) {
        yml.set("${level.id}.level", level.level)
        yml.set("${level.id}.name", level.level)
        yml.set("${level.id}.icon", level.icon)
        yml.set("${level.id}.price", level.price)
        yml.set("${level.id}.radius", level.radius)
        yml.set("${level.id}.fortification", level.fortification)
        yml.save(file)
    }

}
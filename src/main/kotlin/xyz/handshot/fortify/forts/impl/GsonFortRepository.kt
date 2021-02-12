package xyz.handshot.fortify.forts.impl

import com.google.gson.GsonBuilder
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import xyz.handshot.fortify.forts.Fort
import xyz.handshot.fortify.forts.FortRepository
import xyz.handshot.fortify.gson.LocationTypeAdapter
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*

class GsonFortRepository(val folder: File) : FortRepository {

    private val gson = GsonBuilder().setPrettyPrinting()
        .registerTypeAdapter(Location::class.java, LocationTypeAdapter())
        .create()
    private val indexFile = File(folder, "_index.yml")
    private val indexYml = YamlConfiguration.loadConfiguration(indexFile)
    private val owners = mutableMapOf<UUID, UUID>()

    init {
        if (indexYml.contains("owners") && indexYml.isConfigurationSection("owners")) {
            indexYml.getConfigurationSection("owners").getKeys(false).forEach {
                val owner = UUID.fromString(it)
                val fortId = UUID.fromString(indexYml.getString("owners.$owner"))
                owners[owner] = fortId
            }
        }
    }

    override fun existsById(id: UUID): Boolean {
        return getFile(id).exists()
    }

    override fun existsByOwner(id: UUID): Boolean {
        return owners.containsKey(id)
    }

    override fun findById(id: UUID): Fort? {
        val file = getFile(id)
        if (!file.exists()) {
            return null
        }
        return BufferedReader(FileReader(file)).use {
            gson.fromJson(it, Fort::class.java)
        }
    }

    override fun findByOwner(id: UUID): Fort? {
        if (!existsByOwner(id)) {
            return null
        }
        return findById(owners[id]!!)
    }

    override fun findAll(): Array<Fort> {
        return when (folder.exists()) {
            true -> folder.listFiles().filter { it.nameWithoutExtension != "_index" }.mapNotNull { findById(UUID.fromString(it.nameWithoutExtension)) }.toTypedArray()
            false -> emptyArray()
        }
    }

    override fun update(fort: Fort) {
        val file = getFile(fort.id)

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }

        FileWriter(file).use {
            gson.toJson(fort, Fort::class.java, it)
        }

        updateOwner(fort.owner, fort.id)
    }

    override fun delete(fort: Fort) {
        val file = getFile(fort.id)
        file.delete()
    }

    private fun getFile(id: UUID): File {
        return File(folder, "$id.json")
    }

    private fun updateOwner(owner: UUID, fortId: UUID) {
        owners[owner] = fortId
        indexYml.set("owners.$owner", fortId.toString())

        if (!indexFile.exists()) {
            indexFile.parentFile.mkdirs()
            indexFile.createNewFile()
        }

        indexYml.save(indexFile)
    }

}
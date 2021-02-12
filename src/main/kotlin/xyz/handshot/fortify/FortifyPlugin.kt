package xyz.handshot.fortify

import co.aikar.commands.PaperCommandManager
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.binds
import org.koin.dsl.module
import xyz.handshot.fortify.commands.FortifyCommand
import xyz.handshot.fortify.forts.FortCache
import xyz.handshot.fortify.forts.FortListener
import xyz.handshot.fortify.forts.FortRepository
import xyz.handshot.fortify.forts.impl.GsonFortRepository
import xyz.handshot.fortify.forts.impl.SimpleFortCache
import xyz.handshot.fortify.utils.lore
import xyz.handshot.fortify.utils.name
import java.io.File

class FortifyPlugin : JavaPlugin(), KoinComponent {

    companion object {
        val ITEM_FORTS_HEART = ItemStack(Material.CAULDRON)
            .name("&cFort's Heart")
            .lore("&fPlace this to create a fort")
    }

    private val fortRepository: FortRepository by inject()
    private val fortCache: FortCache by inject()
    private val commandManager: PaperCommandManager by inject()

    override fun onEnable() {
        startKoin {
            modules(
                module {
                    single { this@FortifyPlugin } binds arrayOf(FortifyPlugin::class, JavaPlugin::class, Plugin::class)
                    single { PaperCommandManager(this@FortifyPlugin) }
                    single<FortRepository> { GsonFortRepository(File(dataFolder, "forts/")) }
                    single<FortCache> { SimpleFortCache() }
                }
            )
        }

        commandManager.registerCommand(FortifyCommand)
        server.pluginManager.registerEvents(FortListener(), this)

        fortRepository.findAll().forEach {
            fortCache.cache(it)
        }

        logger.info("Loaded ${fortCache.list().size} forts")
    }

    override fun onDisable() {
        fortCache.list()
    }

}
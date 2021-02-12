package xyz.handshot.fortify.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.handshot.fortify.FortifyPlugin
import xyz.handshot.fortify.forts.FortCache
import xyz.handshot.fortify.forts.FortRepository

@CommandAlias("fortify|fort|f")
object FortifyCommand : BaseCommand(), KoinComponent {

    private val cache: FortCache by inject()
    private val repository: FortRepository by inject()

    @Subcommand("give")
    @CommandPermission("fortify.give")
    fun give(sender: Player) {
        sender.inventory.addItem(FortifyPlugin.ITEM_FORTS_HEART)
    }

    @Subcommand("list")
    @CommandPermission("fortify.list")
    fun list(sender: Player) {
        cache.list().forEach {
            val owner = Bukkit.getOfflinePlayer(it.owner).name
            val x = it.center?.x ?: 0
            val y = it.center?.y ?: 0
            val z = it.center?.z ?: 0
            sender.sendMessage("$owner's fort at [$x, $y, $z]")
        }
    }

    @Subcommand("delete")
    @CommandPermission("fortify.delete")
    fun delete(sender: Player) {
        val ownedFort = cache.list().firstOrNull { it.owner == sender.uniqueId }
        if (ownedFort == null) {
            sender.sendMessage("${ChatColor.RED}You do not own a fort")
            return
        }

        ownedFort.center?.block?.breakNaturally()

        repository.delete(ownedFort)
        cache.invalidate(ownedFort)

        sender.sendMessage("${ChatColor.RED}Your fort has been deleted")
    }

}
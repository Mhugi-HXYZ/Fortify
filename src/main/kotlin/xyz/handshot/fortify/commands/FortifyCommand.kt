package xyz.handshot.fortify.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.handshot.fortify.FortifyPlugin
import xyz.handshot.fortify.forts.FortCache
import xyz.handshot.fortify.forts.FortRepository
import xyz.handshot.fortify.utils.FortUtils

@CommandAlias("fortify|fort|f")
object FortifyCommand : BaseCommand(), KoinComponent {

    private val cache: FortCache by inject()
    private val repository: FortRepository by inject()

    @Default
    @Subcommand("help")
    @CommandPermission("fortify.help")
    fun help(sender: CommandSender) {
        sender.sendMessage("${ChatColor.YELLOW}/fortify help - Show this menu")
        sender.sendMessage("${ChatColor.YELLOW}/fortify add <player> - Add a player to your fort")
        sender.sendMessage("${ChatColor.YELLOW}/fortify remove <player> - Remove a player from your fort")
        sender.sendMessage("${ChatColor.YELLOW}/fortify delete - Delete your fort")
    }

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
        val ownedFort = FortUtils.getOwnedFort(sender.uniqueId)
        if (ownedFort == null) {
            sender.sendMessage("${ChatColor.RED}You do not own a fort")
            return
        }

        ownedFort.center?.block?.type = Material.AIR

        repository.delete(ownedFort)
        cache.invalidate(ownedFort)

        sender.sendMessage("${ChatColor.RED}Your fort has been deleted")
    }

    @Subcommand("add")
    @CommandPermission("fortify.add")
    fun add(sender: Player, target: OfflinePlayer) {
        val ownedFort = FortUtils.getOwnedFort(sender.uniqueId)
        if (ownedFort == null) {
            sender.sendMessage("${ChatColor.RED}You do not own a fort")
            return
        }

        if (sender == target) {
            sender.sendMessage("${ChatColor.RED}You can not add yourself to the member list")
            return
        }

        if (ownedFort.members.contains(target.uniqueId)) {
            sender.sendMessage("${ChatColor.RED}${target.name} is already a member of your fort")
            return
        }

        ownedFort.members.add(target.uniqueId)
        if (target.isOnline) {
            target.player.sendMessage("${ChatColor.GREEN}${sender.name} has added you to their fort")
        }

        sender.sendMessage("${ChatColor.GREEN}You have added ${target.name} to your fort")
    }

    @Subcommand("remove")
    @CommandPermission("fortify.remove")
    fun remove(sender: Player, target: OfflinePlayer) {
        val ownedFort = FortUtils.getOwnedFort(sender.uniqueId)
        if (ownedFort == null) {
            sender.sendMessage("${ChatColor.RED}You do not own a fort")
            return
        }

        if (sender == target) {
            sender.sendMessage("${ChatColor.RED}You can not remove yourself from the member list")
            return
        }

        if (!ownedFort.members.contains(target.uniqueId)) {
            sender.sendMessage("${ChatColor.RED}${target.name} is not a member of your fort")
            return
        }

        ownedFort.members.remove(target.uniqueId)
        if (target.isOnline) {
            target.player.sendMessage("${ChatColor.GREEN}${sender.name} has removed you from their fort")
        }

        sender.sendMessage("${ChatColor.GREEN}You have removed ${target.name} from your fort")
    }

    @Subcommand("members")
    @CommandPermission("fortify.members")
    fun members(sender: Player) {
        val ownedFort = FortUtils.getOwnedFort(sender.uniqueId)
        if (ownedFort == null) {
            sender.sendMessage("${ChatColor.RED}You do not own a fort")
            return
        }

        sender.sendMessage("${ChatColor.YELLOW}Members (${ownedFort.members.size})")
        ownedFort.members.forEach { uuid ->
            val player = Bukkit.getOfflinePlayer(uuid)
            sender.sendMessage("${ChatColor.YELLOW}- ${player.name}")
        }

    }

}
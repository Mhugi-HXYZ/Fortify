package xyz.handshot.fortify.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import com.okkero.skedule.schedule
import net.milkbowl.vault.economy.Economy
import org.bukkit.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.handshot.fortify.FortifyPlugin
import xyz.handshot.fortify.forts.FortCache
import xyz.handshot.fortify.forts.FortRepository
import xyz.handshot.fortify.utils.FortUtils
import xyz.handshot.fortify.levels.LevelCache
import java.util.ArrayList

import kotlin.math.cos
import kotlin.math.sin
import org.bukkit.Material
import org.bukkit.block.data.BlockData


@CommandAlias("fortify|fort|f")
object FortifyCommand : BaseCommand(), KoinComponent {

    private val plugin: JavaPlugin by inject()
    private val economy: Economy by inject()
    private val fortCache: FortCache by inject()
    private val fortRepository: FortRepository by inject()
    private val levelCache: LevelCache by inject()

    @Default
    @Subcommand("help")
    @CommandPermission("fortify.help")
    fun help(sender: CommandSender) {
        sender.sendMessage("${ChatColor.YELLOW}/fortify help - Show this menu")
        sender.sendMessage("${ChatColor.YELLOW}/fortify add <player> - Add a player to your fort")
        sender.sendMessage("${ChatColor.YELLOW}/fortify remove <player> - Remove a player from your fort")
        sender.sendMessage("${ChatColor.YELLOW}/fortify delete - Delete your fort")
        sender.sendMessage("${ChatColor.YELLOW}/fortify upgrade - Upgrade your fort to the next level")
    }

    @Subcommand("give")
    @CommandPermission("fortify.give")
    fun give(sender: Player) {
        sender.inventory.addItem(FortifyPlugin.ITEM_FORTS_HEART)
    }

    @Subcommand("list")
    @CommandPermission("fortify.list")
    fun list(sender: Player) {
        fortCache.list().forEach {
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

        if (ownedFort.center != null) {
            ownedFort.center!!.block.type = Material.AIR
            ownedFort.center!!.world?.dropItemNaturally(ownedFort.center!!, FortifyPlugin.ITEM_FORTS_HEART)
            ownedFort.center!!.world?.playSound(ownedFort.center!!, Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.05f)
            ownedFort.center!!.world?.playSound(ownedFort.center!!, Sound.ENTITY_ENDER_EYE_DEATH, 1.0f, 0.05f)
        }

        fortRepository.delete(ownedFort)
        fortCache.invalidate(ownedFort)

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
            target.player?.sendMessage("${ChatColor.GREEN}${sender.name} has added you to their fort")
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
            target.player?.sendMessage("${ChatColor.GREEN}${sender.name} has removed you from their fort")
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

    @Subcommand("upgrade")
    @CommandPermission("fortify.upgrade")
    fun upgrade(sender: Player) {
        val ownedFort = FortUtils.getOwnedFort(sender.uniqueId)
        if (ownedFort == null) {
            sender.sendMessage("${ChatColor.RED}You do not own a fort")
            return
        }

        val nextLevel = levelCache.get(ownedFort.level + 1)

        if (nextLevel == null) {
            sender.sendMessage("${ChatColor.RED}Your fort is already at the maximum level")
            return
        }

        val closestFort = FortUtils.getClosestFort(ownedFort.center!!)
        if (closestFort.first != null) {
            val closestFortRadius = FortUtils.getFortRadius(closestFort.first!!)
            val newFortArea = nextLevel.radius
            if (closestFort.second <= closestFortRadius + newFortArea) {
                sender.sendMessage("${ChatColor.RED}You are too close to an existing fort to upgrade your fort's radius")
                return
            }
        }

        ownedFort.level += 1

        when (economy.withdrawPlayer(sender, nextLevel.price).transactionSuccess()) {
            true -> sender.sendMessage("${ChatColor.GREEN}Your fort has been upgraded to ${nextLevel.name}")
            false -> sender.sendMessage("${ChatColor.RED}You don't have ${economy.format(nextLevel.price)} to upgrade your fort to ${nextLevel.name}")
        }
    }

    @Subcommand("outline")
    @CommandPermission("fortify.outline")
    fun outline(sender: Player) {
        val ownedFort = FortUtils.getOwnedFort(sender.uniqueId)
        if (ownedFort == null) {
            sender.sendMessage("${ChatColor.RED}You do not own a fort")
            return
        }

        val fortLevel = levelCache.get(ownedFort.level)!!
        val outline = arrayListOf<Location>()

        val center = ownedFort.center!!
        val areaSize = fortLevel.radius
        val minX = center.x - areaSize
        val minZ = center.z - areaSize
        val maxX = center.x + areaSize
        val maxZ = center.z + areaSize

        outline.add(center.clone().apply { x = minX; z = minZ })
        outline.add(center.clone().apply { x = maxX; z = minZ })
        outline.add(center.clone().apply { x = minX; z = maxZ })
        outline.add(center.clone().apply { x = maxX; z = maxZ })

        outline.forEach {
            sender.sendBlockChange(it, Material.RED_WOOL.createBlockData())
        }
    }


}
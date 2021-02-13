package xyz.handshot.fortify.forts

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.handshot.fortify.FortifyPlugin
import xyz.handshot.fortify.utils.FortUtils
import java.util.*

class FortListener : Listener, KoinComponent {

    private val repository: FortRepository by inject()
    private val cache: FortCache by inject()
    private val timesBroken = mutableMapOf<Location, Int>()
    private val lastMessageSent = mutableMapOf<UUID, Long>()
    private val lastBlockBreak = mutableMapOf<UUID, Long>()

    private val lastFortPresence = mutableMapOf<UUID, UUID>()
    private val lastFortPresenceCheck = mutableMapOf<UUID, Long>()

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.itemInHand == null || !event.itemInHand.isSimilar(FortifyPlugin.ITEM_FORTS_HEART)) {
            return
        }

        if (cache.list().any { it.owner == event.player.uniqueId }) {
            event.isCancelled = true
            event.player.sendMessage("${ChatColor.RED}You already have a fort")
            return
        }

        val closestFort = FortUtils.getClosestFort(event.block.location)
        if (closestFort.first != null) {
            val closestFortRadius = FortUtils.getFortRadius(closestFort.first!!)
            val newFortArea = FortUtils.getDefaultFortRadius()
            if (closestFort.second <= closestFortRadius + newFortArea) {
                event.isCancelled = true
                event.player.sendMessage("${ChatColor.RED}You are too close to an existing fort")
                return
            }
        }

        val fort = Fort().apply {
            id = UUID.randomUUID()
            owner = event.player.uniqueId
            members.add(owner)
            center = event.block.location
        }

        repository.update(fort)
        cache.cache(fort)

        event.player.sendMessage("${ChatColor.GREEN}You have created a fort!")
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        if (!event.hasBlock()) {
            return
        }

        // TODO Implement action rule 'firewall'
        if (event.action != Action.LEFT_CLICK_BLOCK) {
            val fort = FortUtils.getOwningFort(event.clickedBlock!!.location) ?: return
            if (!fort.members.contains(event.player.uniqueId)) {
                event.isCancelled = true
                if (antiSpam(event.player)) {
                    event.player.sendMessage("${ChatColor.RED}You do not have permission to do that here")
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        if (FortUtils.getFortWithCenter(event.block.location) != null) {
            event.player.sendMessage("${ChatColor.RED}You can not break a fort's heart. To delete your fort use /fort delete")
            event.isCancelled = true
            return
        }

        val fort = FortUtils.getOwningFort(event.block.location) ?: return
        if (fort.members.contains(event.player.uniqueId)) {
            return
        }

        if (!antiSpamBlock(event.player)) {
            event.isCancelled = true
            return
        }

        val timesBlockBroken = timesBroken.getOrDefault(event.block.location, 0)

        if (timesBlockBroken < FortUtils.getFortFortification(fort)) {
            timesBroken[event.block.location] = timesBlockBroken + 1
            event.isCancelled = true
            return
        } else {
            timesBroken.remove(event.block.location)
        }
    }

    // TODO Make this less spaghetti-y
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        val lastCheck = lastFortPresenceCheck.getOrDefault(event.player.uniqueId, 0)
        if (System.currentTimeMillis() - lastCheck < 500) {
            return
        }

        lastFortPresenceCheck[event.player.uniqueId] = System.currentTimeMillis()

        val owningFort = FortUtils.getOwningFort(event.to!!)

        if (owningFort == null && lastFortPresence.containsKey(event.player.uniqueId)) {
            val lastFort = lastFortPresence[event.player.uniqueId]!!
            val fortName = Bukkit.getOfflinePlayer(lastFort).name
            event.player.spigot()
                .sendMessage(
                    ChatMessageType.ACTION_BAR,
                    *TextComponent.fromLegacyText(
                        "You have left ${fortName}'s fort",
                        net.md_5.bungee.api.ChatColor.YELLOW
                    )
                )
            lastFortPresence.remove(event.player.uniqueId)
            return
        }

        if (owningFort != null) {
            if (lastFortPresence.containsKey(event.player.uniqueId)) {
                if (owningFort.owner != lastFortPresence[event.player.uniqueId]) {
                    val fortName = Bukkit.getOfflinePlayer(owningFort.owner).name
                    event.player.spigot()
                        .sendMessage(
                            ChatMessageType.ACTION_BAR,
                            *TextComponent.fromLegacyText(
                                "You have entered ${fortName}'s fort",
                                net.md_5.bungee.api.ChatColor.YELLOW
                            )
                        )
                }
            } else {
                val fortName = Bukkit.getOfflinePlayer(owningFort.owner).name
                event.player.spigot()
                    .sendMessage(
                        ChatMessageType.ACTION_BAR,
                        *TextComponent.fromLegacyText(
                            "You have entered ${fortName}'s fort",
                            net.md_5.bungee.api.ChatColor.YELLOW
                        )
                    )
                lastFortPresence[event.player.uniqueId] = owningFort.owner
            }
        }
    }

    private fun antiSpam(player: Player): Boolean {
        val lastMessage = lastMessageSent.getOrDefault(player.uniqueId, 0)
        if (System.currentTimeMillis() - lastMessage >= 1000) {
            lastMessageSent[player.uniqueId] = System.currentTimeMillis()
            return true
        }
        return false
    }

    private fun antiSpamBlock(player: Player): Boolean {
        val lastBreak = lastBlockBreak.getOrDefault(player.uniqueId, 0)
        if (System.currentTimeMillis() - lastBreak >= 250) {
            lastBlockBreak[player.uniqueId] = System.currentTimeMillis()
            return true
        }
        return false
    }

}
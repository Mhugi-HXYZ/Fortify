package xyz.handshot.fortify.forts

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.handshot.fortify.FortifyPlugin
import xyz.handshot.fortify.utils.FortUtils
import java.util.*

class FortListener : Listener, KoinComponent {

    private val repository: FortRepository by inject()
    private val cache: FortCache by inject()
    private val timesBroken = mutableMapOf<Location, Int>()

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
            // TODO Implement levels
            val closestFortArea = 100
            val newFortArea = 100
            if (closestFort.second <= closestFortArea + newFortArea) {
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
            val fort = FortUtils.getOwningFort(event.clickedBlock.location) ?: return
            if (!fort.members.contains(event.player.uniqueId)) {
                event.isCancelled = true
                event.player.sendMessage("${ChatColor.RED}You do not have permission to do that here")
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

        val timesBlockBroken = timesBroken.getOrDefault(event.block.location, 0)

        if (timesBlockBroken + 1 < FortUtils.getFortFortification(fort)) {
            timesBroken[event.block.location] = timesBlockBroken + 1
            event.isCancelled = true
            return
        }
    }

}
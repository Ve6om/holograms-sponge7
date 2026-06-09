package de.randombyte.holograms.commands

import de.randombyte.holograms.api.HologramsService
import de.randombyte.holograms.data.HologramKeys
import de.randombyte.kosp.PlayerExecutedCommand
import de.randombyte.kosp.extensions.getServiceOrFail
import de.randombyte.kosp.extensions.green
import de.randombyte.kosp.extensions.red
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.entity.living.player.Player

/**
 * Removes every line of the named panel (tagged via [HologramKeys.PANEL_NAME]) in the player's world.
 */
class RemovePanelCommand : PlayerExecutedCommand() {

    override fun executedByPlayer(player: Player, args: CommandContext): CommandResult {
        val name = args.getOne<String>("name").get()

        val removed = HologramsService::class.getServiceOrFail()
                .getHolograms(player.location.extent)
                .filter { it.getArmorStand().get(HologramKeys.PANEL_NAME).orElse("") == name }
                .onEach { it.remove() }
                .count()

        if (removed == 0) {
            player.sendMessage("No panel named '$name' found in this world!".red())
            return CommandResult.empty()
        }

        player.sendMessage("Removed '$name' panel ($removed lines)!".green())
        return CommandResult.successCount(removed)
    }
}

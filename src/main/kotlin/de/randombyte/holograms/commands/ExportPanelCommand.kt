package de.randombyte.holograms.commands

import de.randombyte.holograms.Holograms
import de.randombyte.holograms.api.HologramsService
import de.randombyte.holograms.api.HologramsService.Hologram
import de.randombyte.holograms.data.HologramKeys
import de.randombyte.kosp.PlayerExecutedCommand
import de.randombyte.kosp.extensions.getServiceOrFail
import de.randombyte.kosp.extensions.green
import de.randombyte.kosp.extensions.orNull
import de.randombyte.kosp.extensions.red
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.serializer.TextSerializers
import java.nio.file.Files

/**
 * Writes an existing in-game panel back to `config/holograms/panels/<name>.txt` so it can be
 * edited in the web editor and re-applied with [CreateHologramFromFileCommand].
 *
 * If a panel tagged with [HologramKeys.PANEL_NAME] == name exists, those lines are exported.
 * Otherwise all Holograms within [radius] blocks of the player are exported (sorted top to bottom).
 */
class ExportPanelCommand(val pluginInstance: Holograms) : PlayerExecutedCommand() {
    companion object {
        const val DEFAULT_RADIUS = 5.0
    }

    override fun executedByPlayer(player: Player, args: CommandContext): CommandResult {
        val name = args.getOne<String>("name").get()
        val radius = args.getOne<Double>("radius").orNull() ?: DEFAULT_RADIUS

        val service = HologramsService::class.getServiceOrFail()

        val tagged = service.getHolograms(player.location.extent)
                .filter { it.getArmorStand().get(HologramKeys.PANEL_NAME).orElse("") == name }

        val holograms = if (tagged.isNotEmpty()) tagged else {
            service.getHolograms(player.location, radius).map { it.first }
        }

        if (holograms.isEmpty()) {
            player.sendMessage("No holograms found to export (looked for panel '$name', then within $radius blocks)!".red())
            return CommandResult.empty()
        }

        // Top line first, matching the order createFromFile expects.
        val sorted = holograms.sortedByDescending { it.location.position.y }

        val space = averageVerticalSpace(sorted)
        val codeLines = sorted.map { TextSerializers.FORMATTING_CODE.serialize(it.text) }

        val fileName = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val file = pluginInstance.panelsDir.resolve("$fileName.txt")

        val out = mutableListOf(
                "# Hologram panel: $name",
                "# space: ${"%.3f".format(space)}",
                "# Exported from in-game",
                ""
        )
        out += codeLines
        Files.write(file, out)

        player.sendMessage("Exported '$name' panel (${codeLines.size} lines) to panels/$fileName.txt".green())
        return CommandResult.successCount(codeLines.size)
    }

    private fun averageVerticalSpace(sortedTopToBottom: List<Hologram>): Double {
        if (sortedTopToBottom.size < 2) return CreateHologramFromFileCommand.DEFAULT_VERTICAL_SPACE
        val ys = sortedTopToBottom.map { it.location.position.y }
        val gaps = ys.zipWithNext { a, b -> a - b } // descending, so a > b
        return gaps.average()
    }
}

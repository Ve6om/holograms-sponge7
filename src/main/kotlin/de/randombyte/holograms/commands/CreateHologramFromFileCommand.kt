package de.randombyte.holograms.commands

import de.randombyte.holograms.Holograms
import de.randombyte.holograms.api.HologramsService
import de.randombyte.holograms.data.HologramData
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
 * Spawns a whole multi-line Hologram panel from a file in `config/holograms/panels/<name>.txt`.
 * Each non-empty, non-comment line becomes one Hologram line, using `&` formatting codes.
 *
 * Lines starting with `#` are comments and ignored, except a `# space: <number>` directive
 * which sets the vertical spacing between lines (overridden by the optional command argument).
 *
 * This exists because a fully formatted panel exceeds Minecraft's 256-char command limit, so it
 * can't be pasted via `/holograms cml`. The companion editor (`index.html`) exports these files.
 */
class CreateHologramFromFileCommand(val pluginInstance: Holograms) : PlayerExecutedCommand() {
    companion object {
        const val DEFAULT_VERTICAL_SPACE = 0.3
        const val SPACE_DIRECTIVE = "# space:"
    }

    override fun executedByPlayer(player: Player, args: CommandContext): CommandResult {
        val name = args.getOne<String>("name").get()
        val verticalSpaceArg = args.getOne<Double>("verticalSpace").orNull()

        val file = pluginInstance.panelsDir.resolve("$name.txt")
        if (!Files.exists(file)) {
            player.sendMessage("Panel file 'panels/$name.txt' not found!".red())
            return CommandResult.empty()
        }

        val rawLines = Files.readAllLines(file)

        // A `# space:` directive sets spacing unless the command argument overrides it.
        val directiveSpace = rawLines
                .firstOrNull { it.trim().startsWith(SPACE_DIRECTIVE) }
                ?.trim()
                ?.removePrefix(SPACE_DIRECTIVE)
                ?.trim()
                ?.toDoubleOrNull()

        val verticalSpace = verticalSpaceArg ?: directiveSpace ?: DEFAULT_VERTICAL_SPACE

        // Keep whitespace-only lines (spacer/padding lines render as empty stands), but drop
        // truly empty lines and comments.
        val texts = rawLines
                .filter { it.isNotEmpty() && !it.trim().startsWith("#") }
                .map { TextSerializers.FORMATTING_CODE.deserialize(it) }

        if (texts.isEmpty()) {
            player.sendMessage("Panel file 'panels/$name.txt' has no text lines!".red())
            return CommandResult.empty()
        }

        val service = HologramsService::class.getServiceOrFail()

        // Update-in-place: remove any existing panel with this name in the current world first.
        val replaced = service.getHolograms(player.location.extent)
                .filter { it.getArmorStand().get(HologramKeys.PANEL_NAME).orElse("") == name }
                .onEach { it.remove() }
                .count()

        val created = service.createMultilineHologram(player.location, texts, verticalSpace).orNull()
        if (created == null) {
            player.sendMessage("Couldn't spawn '$name' panel!".red())
            return CommandResult.empty()
        }

        // Tag every line with the panel name so it can be updated/removed/exported later.
        created.forEach { hologram ->
            val stand = hologram.getArmorStand()
            val data = stand.getOrCreate(HologramData::class.java).get().set(HologramKeys.PANEL_NAME, name)
            stand.offer(data)
        }

        val suffix = if (replaced > 0) " (replaced previous)" else ""
        player.sendMessage("Spawned '$name' panel (${texts.size} lines)$suffix!".green())
        return CommandResult.successCount(texts.size)
    }
}

# Holograms (fork)

A fork of [RandomByte's Holograms](https://github.com/randombyte-developer/holograms) Sponge plugin (SpongeAPI 7,
Minecraft 1.12.2), adding a file-based workflow and a browser editor for building multi-line text holograms.

## What this fork adds

- **File-based panels.** Design a multi-line hologram once, save it as a `.txt` file, and spawn the whole thing
  with a single command. This sidesteps Minecraft's 256-character command limit, which makes pasting a long,
  fully formatted panel via `/holograms cml` impossible.
- **Named panels with update-in-place.** Each panel is tagged with a name, so re-running the command replaces the
  existing panel instead of stacking a duplicate on top.
- **A browser editor** (`index.html`) for designing panels visually — see below.
- The archive-signing build step was removed so the plugin builds without a signing key.

### New commands

| Command | Description |
| --- | --- |
| `/holograms createFromFile <name> [verticalSpace]` (alias `cff`) | Spawn a panel from `config/holograms/panels/<name>.txt` at your location. Replaces an existing panel of the same name. |
| `/holograms removePanel <name>` (alias `rp`) | Remove every line of the named panel in your world. |
| `/holograms export <name> [radius]` | Write an existing in-game panel back to `panels/<name>.txt` so it can be edited and re-applied. |

Permissions: `holograms.createFromFile`, `holograms.removePanel`, `holograms.export`.

### Panel file format

Each non-empty, non-comment line becomes one hologram line, using `&` formatting codes
([legacy formatting](https://minecraft.fandom.com/wiki/Formatting_codes)). Lines beginning with `#` are comments.
A `# space: <number>` comment sets the vertical spacing (overridden by the command argument).

A blank-looking gap is a line of `&r &r` (a space wrapped in reset codes, so it survives editing).
A divider bar is `&<color>&m` followed by spaces and a closing `&r` (the trailing `&r` keeps the spaces from being
trimmed). See [`panels/welcome-example.txt`](panels/welcome-example.txt) for a small example.

## The editor

[`index.html`](index.html) is a single, self-contained, offline page — no build step, no server. Open it in a
browser (or host it; see below) to:

- Build a panel line by line with a live Minecraft-style preview.
- Click colour/format swatches to insert `&` codes at the cursor.
- Drop a screenshot in as a reference background and use the **eyedropper** to grab the nearest Minecraft colour
  from any pixel.
- Add **divider bars** (strikethrough/underline) with a colour picker and a length slider — clicking an existing
  divider line lets you resize it live.
- Centre lines and add blank spacers without typing spaces by hand.
- Export the panel as a `.txt` file ready to drop into `config/holograms/panels/`.

### Hosting it

Because it is a static file, it works with any static host. With **GitHub Pages** (repo *Settings → Pages →
Deploy from a branch → `master` / root*) the editor is served at the root URL, e.g.
`https://<user>.github.io/<repo>/`.

## Building

The build targets Java 8 (Gradle 4.5, Kotlin 1.2.50). With a JDK 8 on `JAVA_HOME`:

```
./gradlew shadowJar
```

The plugin jar is written to `build/libs/`.

## License

Licensed under the GNU General Public License v2.0 — see [`LICENSE`](LICENSE). Changes in this fork (listed above)
are released under the same license.

---

# [Ore page](https://ore.spongepowered.org/RandomByte/Holograms)

# How to use the Holograms-API

## Setup the dependency

Add [jitpack](https://jitpack.io/) as a repository and the API as a dependency.

Gradle example:

```groovy
repositories {
    maven { url "https://jitpack.io" }
}
dependencies {
    compile "com.github.randombyte-developer.holograms:holograms-api:v2.1.1"
}
```

That dependency only contains [this one file](https://github.com/randombyte-developer/holograms/blob/master/holograms-api/src/main/kotlin/de/randombyte/holograms/api/HologramsService.kt).

## How to use in code

Although everything is documented in the[`HologramService.kt`](https://github.com/randombyte-developer/holograms/blob/master/holograms-api/src/main/kotlin/de/randombyte/holograms/api/HologramsService.kt)
it may not be clear how to use that `Service` written in [Kotlin](https://kotlinlang.org/) from a plugin written in Java.
Basically, the fields prefixed with `var` like `text` and `location` have a getter and a setter method automatically generated to be used from the Java side.

Here is an example:
```java
Optional<HologramsService> hologramsServiceOptional = Sponge.getServiceManager().provide(HologramsService.class);
HologramsService hologramsService = hologramsServiceOptional.orElseThrow(
        () -> new RuntimeException("HologramsAPI not available! Is the plugin 'holograms' installed?"));

// Creating a Hologram
Optional<HologramsService.Hologram> hologramOptional = hologramsService
        .createHologram(player.getLocation(), Text.of(TextColors.GREEN, "Example text"));
if (!hologramOptional.isPresent()) {
    player.sendMessage(Text.of("Hologram couldn't be spawned!"));
    return CommandResult.success();
}

// Modifying the Hologram
Hologram hologram = hologramOptional.get();

hologram.setText(Text.of("New text"));

Location<World> newLocation = hologram.getLocation().add(0.0, 5.0, 0.0);
hologram.setLocation(newLocation);

// Finding the Hologram by UUID
// Both UUIDs are saved somewhere (e.g. in a config file)
UUID hologramUuid = hologram.getUuid();
UUID worldUuid = hologram.getWorldUuid();

Optional<World> worldOptional = Sponge.getServer().getWorld(worldUuid);
if (!worldOptional.isPresent()) {
    player.sendMessage(Text.of("Couldn't find world!"));
    return CommandResult.success();
}
World world = worldOptional.get();
Optional<? extends Hologram> loadedHologramOptional = hologramsService.getHologram(world, hologramUuid);
if (!loadedHologramOptional.isPresent()) {
    player.sendMessage(Text.of("Couldn't find Hologram!"));
    return CommandResult.success();
}
Hologram loadedHologram = loadedHologramOptional.get();
loadedHologram.setText(Text.of("This Hologram was found!"));

// Removing the Hologram
loadedHologram.remove();
```

If you have questions, please ask me!

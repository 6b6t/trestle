# Launcher service endpoints

This reference records the remote services used by the first milestone. The service clients keep these details outside Compose code.

## Minecraft metadata and files

Trestle starts with Mojang's version manifest:

```text
GET https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
```

Each manifest entry supplies a version JSON URL and optional SHA-1. The version JSON supplies the client, libraries, assets, logging, arguments, and Java requirement.

Asset objects use their SHA-1 as the path:

```text
GET https://resources.download.minecraft.net/{first-two-sha1-characters}/{sha1}
```

Legacy Maven entries use `https://libraries.minecraft.net/` when the metadata has no repository URL. Trestle uses URLs from metadata when they exist.

## Mojang Java runtimes

The version JSON names a Java runtime component and its required major version. Trestle uses this runtime index for desktop packages:

```text
GET https://piston-meta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json
```

The index groups runtime releases by component and platform. Each release contains a package manifest URL, SHA-1, version, and availability state.

The package manifest lists directories, links, executable flags, and file downloads. Trestle downloads raw files from their Mojang URLs and validates each SHA-1.

Trestle activates a runtime only after all files are complete. A completed runtime remains available for offline launches.

The [Mojang API reference](https://minecraft.wiki/w/Mojang_API) describes related profile and service APIs. These APIs are not substitutes for version metadata.

## Fabric

Trestle uses the [Fabric Meta API](https://meta.fabricmc.net/) to list loader versions and get a launch profile:

```text
GET https://meta.fabricmc.net/v2/versions/loader/{game-version}
GET https://meta.fabricmc.net/v2/versions/loader/{game-version}/{loader-version}/profile/json
```

The installer merges the Fabric profile with the matching Mojang version. Mojang remains the source for the client, assets, and base libraries.

## Resource platforms

Trestle uses one resource model for projects, versions, files, and dependencies. The model supports mods, modpacks, resource packs, and shaders.

Search results use the selected instance version and loader. Modpack searches do not use an instance filter because each pack creates an instance.

Trestle validates SHA-1 values when a platform supplies them. It also records files that belong to each installed project.

### Modrinth

The [Modrinth API](https://docs.modrinth.com/api/) returns project versions and file hashes. Trestle filters versions by Minecraft version and loader.

```text
GET https://api.modrinth.com/v2/search
GET https://api.modrinth.com/v2/project/{project-id-or-slug}/version
GET https://api.modrinth.com/v2/version/{version-id}
GET https://api.modrinth.com/v2/version_file/{sha1}?algorithm=sha1
```

The client sends the Trestle user agent. Search facets filter the project type, Minecraft version, and loader.

The client selects the primary file. It resolves required dependencies before it starts a resource download.

### CurseForge

The [CurseForge REST API](https://docs.curseforge.com/rest-api/) requires an `x-api-key` header. CurseForge must issue this key for Trestle.

Set `TRESTLE_CURSEFORGE_API_KEY` before you run a desktop build. The Android build adds the same value to its application manifest.

Do not copy the key from Prism Launcher or another application. CurseForge issues each key for one application.

```text
GET https://api.curseforge.com/v1/mods/search
GET https://api.curseforge.com/v1/mods/{mod-id}/files
GET https://api.curseforge.com/v1/mods/{mod-id}/files/{file-id}
```

CurseForge uses numeric project IDs. Trestle gets these IDs from search results, so users do not enter raw IDs.

Some authors disable third-party downloads. In this case, the API returns file metadata without a download URL.

If the file has a SHA-1, Trestle searches Modrinth for the identical file. Trestle uses the alternative only when both hashes match.

If no verified alternative exists, Trestle stops the installation and offers the official manual-download page. It does not scrape a restricted URL.

### Resource installation

Mods go in the instance `mods` directory. Resource packs and shaders go in their standard game directories.

Trestle downloads required dependencies in the same operation. The instance resource registry tracks direct projects and shared dependencies.

### Modpack installation

Trestle reads `modrinth.index.json` from Modrinth packs. It reads `manifest.json` from CurseForge packs.

The archive extractor rejects paths outside the staging directory. It also limits the number of entries and the extracted size.

Trestle downloads pack files before it creates the instance. A canceled download does not leave an incomplete instance in the library.

Trestle currently installs Vanilla and Fabric packs. Forge, NeoForge, and Quilt packs remain visible, but installation stops with a loader error.

## Microsoft authentication

Authentication is outside this milestone. `SessionProvider` reserves the boundary for a future Microsoft, Xbox, XSTS, and Minecraft Services token chain.

A current device-code flow uses these service stages:

```text
POST https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode
POST https://login.microsoftonline.com/consumers/oauth2/v2.0/token
POST https://user.auth.xboxlive.com/user/authenticate
POST https://xsts.auth.xboxlive.com/xsts/authorize
POST https://api.minecraftservices.com/launcher/login
GET  https://api.minecraftservices.com/entitlements/mcstore
GET  https://api.minecraftservices.com/minecraft/profile
```

The XSTS request uses `rp://api.minecraftservices.com/` as its relying party. The launcher login exchanges that XSTS authorization value for a Minecraft token.

The entitlements request proves product ownership. The profile request returns the player ID and name used in the launch arguments.

The future implementation must use an app registration that permits launcher authentication. It must store refresh credentials in platform-protected storage.

The implementation must validate Minecraft ownership and profile data. Logs and launch-plan diagnostics must redact all access tokens and sensitive headers.

The [MinecraftAuth project](https://github.com/RaphiMC/MinecraftAuth) shows a modern token-holder design with refresh and device-code flows. Trestle does not copy its implementation.

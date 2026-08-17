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

The [Mojang API reference](https://minecraft.wiki/w/Mojang_API) describes related profile and service APIs. These APIs are not substitutes for version metadata.

## Fabric

Trestle uses the [Fabric Meta API](https://meta.fabricmc.net/) to list loader versions and get a launch profile:

```text
GET https://meta.fabricmc.net/v2/versions/loader/{game-version}
GET https://meta.fabricmc.net/v2/versions/loader/{game-version}/{loader-version}/profile/json
```

The installer merges the Fabric profile with the matching Mojang version. Mojang remains the source for the client, assets, and base libraries.

## Modrinth

The [Modrinth API](https://docs.modrinth.com/api/) returns project versions and file hashes. Trestle filters versions by Minecraft version and loader.

```text
GET https://api.modrinth.com/v2/project/{project-id}/version
```

The client sends a Trestle user agent. It selects the primary file when the response marks one.

## CurseForge

The [CurseForge REST API](https://docs.curseforge.com/rest-api/) requires an `x-api-key` header. Trestle accepts a user-supplied key and never embeds one.

```text
GET https://api.curseforge.com/v1/mods/{mod-id}/files
```

Some authors disable third-party downloads. Trestle reports this state when the API does not return a download URL.

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

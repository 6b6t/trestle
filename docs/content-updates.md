# Update Trestle and Minecraft content

## Launcher updates

Trestle checks for a newer launcher release in the background. Successful checks occur at most once per day.

An update notice provides release notes and a download for your operating system and architecture. **Remind me tomorrow** hides the notice for 24 hours.

In **Settings → Tools**, you can disable automatic checks, include preview releases, or start a manual check.

Offline checks do not interrupt startup. Trestle retries unsuccessful automatic checks after an hour. It does not replace or execute its installer automatically.

Older releases without a download manifest link to the release page instead.

## Identify imported files

The content list reads names, versions, authors, and dependencies from Fabric, Quilt, Forge, NeoForge, and legacy mod metadata.

When metadata tracking is enabled, Trestle looks up file hashes on Modrinth and CurseForge. CurseForge identification also verifies SHA-1 after the fingerprint match.

A filename alone never establishes a project match. Renaming a file does not remove its identity. Changing its contents invalidates the cached identity.

The cache stays available offline. Unmatched files remain usable as local content. A provider failure does not prevent checks against other providers.

## Recover a restricted download

Some publishers require downloads from their own project page.

1. Open the publisher link in the download dialog.
2. Download the exact requested version.
3. Select the downloaded file in Trestle.
4. Retry the installation after checksum verification succeeds.

Trestle copies the verified file into its download cache. It does not move or delete your original download.

## Update a tracked modpack

Remote pack installs record the provider, project, version, and original file hashes. Portable Trestle exports preserve this metadata.

Older installations and ordinary local pack archives do not have a verified remote origin. Trestle does not infer that origin from the pack name.

1. Stop the instance.
2. Open the instance content page.
3. Select **Check pack updates**.
4. Review the prepared file changes.
5. Choose which modified pack files to replace.
6. Select **Back up and update**.

Trestle prepares the new pack in a separate staging instance. It preserves worlds, screenshots, logs, personal settings, and files outside the pack manifest.

Modified pack files stay unchanged unless you select their replacement. Retained custom mods and settings can still be incompatible with a newer pack.

If a file changes after the preview, the update stops. Cancel the preview and prepare it again.

Installing another version from the catalog also offers to update an existing tracked instance. You can choose a separate installation instead.

## Roll back a pack update

Select **Roll back pack update** on the content page. Trestle restores the previous pack files and runtime metadata.

Rollback stops if an updated file changed after installation. Copy those changes out of the instance before trying again.

Trestle keeps one rollback backup. The next update replaces it. Worlds are not part of this backup; use the world backup controls before playing a new version.

Interrupted file transactions recover when the instance registry opens. The same recovery applies to the graphical launcher and command-line operations.

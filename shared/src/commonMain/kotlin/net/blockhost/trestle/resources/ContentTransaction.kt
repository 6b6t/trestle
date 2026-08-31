package net.blockhost.trestle.resources

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

/** A durable, single-level undo journal. Sources must already be downloaded and verified. */
internal class ContentTransaction(
    private val fileSystem: FileSystem,
    private val root: Path,
    private val backup: Path,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val journalPath = backup / "journal.json"

    fun apply(replacements: Map<String, Path?>, expected: Map<String, String?>) {
        recover()
        require(replacements.keys.all { it in expected }) { "Every changed file needs a preview hash." }
        replacements.forEach { (name, source) ->
            require(hash(name) == expected[name]) { "$name changed after the preview. Refresh the update preview." }
            require(source == null || fileSystem.metadata(source).isRegularFile) { "$name is not a regular file." }
        }
        if (fileSystem.exists(backup)) fileSystem.deleteRecursively(backup)
        fileSystem.createDirectories(backup / "files")
        val before = replacements.keys.associateWith { name ->
            val target = checkedContentPath(fileSystem, root, name)
            hash(name)?.also { copy(target, checkedContentPath(fileSystem, backup / "files", name)) }
        }
        val after = replacements.mapValues { (_, source) -> source?.let(fileSystem::sha256) }
        val journal = Journal(before = before, after = after)
        write(journal)
        try {
            replacements.forEach { (name, source) ->
                val target = checkedContentPath(fileSystem, root, name)
                if (source == null) fileSystem.delete(target, mustExist = false) else copy(source, target)
            }
            write(journal.copy(committed = true))
        } catch (error: Exception) {
            // Leave the journal intact if restoring fails; startup will retry recovery.
            try { restore(journal) } catch (restoreError: Exception) { error.addSuppressed(restoreError) }
            throw error
        }
    }

    fun recover() {
        val journal = read() ?: return
        if (!journal.committed) restore(journal)
    }

    fun canRollback(): Boolean = read()?.committed == true

    fun rollback() {
        val journal = read() ?: error("No update backup is available.")
        require(journal.committed) { "The interrupted update must be recovered first." }
        val modified = journal.after.keys.filter { hash(it) != journal.after[it] }
        require(modified.isEmpty()) {
            "These updated files changed since installation: ${modified.joinToString()}. Copy them out of the instance before rolling back."
        }
        // Mark the journal before restoring, so interruption during rollback is recoverable too.
        write(journal.copy(committed = false))
        restore(journal)
    }

    private fun restore(journal: Journal) {
        journal.before.forEach { (name, hash) ->
            val target = checkedContentPath(fileSystem, root, name)
            if (hash == null) fileSystem.delete(target, mustExist = false)
            else {
                val source = checkedContentPath(fileSystem, backup / "files", name)
                require(fileSystem.sha256(source) == hash) { "The backup for $name is damaged." }
                copy(source, target)
            }
        }
        fileSystem.delete(journalPath)
    }

    private fun hash(name: String): String? {
        val path = checkedContentPath(fileSystem, root, name)
        val metadata = fileSystem.metadataOrNull(path) ?: return null
        require(metadata.isRegularFile) { "$name is not a regular file." }
        return fileSystem.sha256(path)
    }

    private fun copy(source: Path, target: Path) {
        fileSystem.createDirectories(requireNotNull(target.parent))
        val temporary = target.parent!! / ".${target.name}.trestle-copy"
        require(fileSystem.metadataOrNull(temporary)?.symlinkTarget == null) { "Unsafe temporary path." }
        fileSystem.copy(source, temporary)
        fileSystem.atomicMove(temporary, target)
    }

    private fun read(): Journal? = if (fileSystem.exists(journalPath))
        json.decodeFromString<Journal>(fileSystem.read(journalPath) { readUtf8() }) else null

    private fun write(journal: Journal) {
        val temporary = backup / "journal.tmp"
        fileSystem.write(temporary) { writeUtf8(json.encodeToString(Journal.serializer(), journal)) }
        fileSystem.atomicMove(temporary, journalPath)
    }

    @Serializable
    private data class Journal(val before: Map<String, String?>, val after: Map<String, String?>, val committed: Boolean = false)
}

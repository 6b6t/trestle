package net.blockhost.trestle.runtime

import net.blockhost.trestle.auth.SecretValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PreparedLaunchTest {
    @Test
    fun safeCommandRedactsCredentials() {
        val launch = PreparedLaunch(
            instanceId = "test01",
            executable = "/java/bin/java",
            arguments = listOf(
                CommandArgument.Public("net.minecraft.client.main.Main"),
                CommandArgument.Public("--accessToken"),
                CommandArgument.Secret(SecretValue("sensitive-token")),
            ),
            workingDirectory = "/instance/game",
            mainClass = "net.minecraft.client.main.Main",
            classpathEntries = listOf("client.jar"),
            nativeDirectory = "/instance/natives",
        )

        val safe = launch.safeCommand()
        assertEquals("[REDACTED]", safe.last())
        assertFalse(safe.joinToString(" ").contains("sensitive-token"))
    }
}

package net.blockhost.trestle.domain

val sampleInstances = listOf(
    GameInstance(
        id = InstanceId("fabric-main"),
        name = "Fabric main",
        gameVersion = "1.21.8",
        modLoader = ModLoader.FABRIC,
        javaVersion = 21,
        state = InstanceState.Ready,
        lastPlayed = "Today",
    ),
    GameInstance(
        id = InstanceId("vanilla-latest"),
        name = "Latest release",
        gameVersion = "1.21.8",
        modLoader = ModLoader.VANILLA,
        javaVersion = 21,
        state = InstanceState.Ready,
        lastPlayed = "4 days ago",
    ),
    GameInstance(
        id = InstanceId("forge-legacy"),
        name = "Legacy Forge",
        gameVersion = "1.12.2",
        modLoader = ModLoader.FORGE,
        javaVersion = 8,
        state = InstanceState.Installing(progress = 0.64f),
    ),
)

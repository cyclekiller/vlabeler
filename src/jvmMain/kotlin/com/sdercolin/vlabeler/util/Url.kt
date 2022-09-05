package com.sdercolin.vlabeler.util

import java.net.URI

object Url {
    const val ProjectGitHub = "https://github.com/sdercolin/vlabeler"
    const val LatestRelease = "https://github.com/sdercolin/vlabeler/releases/latest"
    const val DiscordInvitation = "https://discord.gg/yrTqG2SrRd"
    const val GithubApiRoot = "https://api.github.com/repos/sdercolin/vlabeler"
}

fun String.toUri() = URI(this)

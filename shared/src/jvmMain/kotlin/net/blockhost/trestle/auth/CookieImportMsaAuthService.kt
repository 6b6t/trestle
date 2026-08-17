package net.blockhost.trestle.auth

import net.lenni0451.commons.httpclient.HttpClient
import net.lenni0451.commons.httpclient.constants.HttpHeaders
import net.lenni0451.commons.httpclient.requests.impl.GetRequest
import net.raphimc.minecraftauth.msa.model.MsaApplicationConfig
import net.raphimc.minecraftauth.msa.model.MsaToken
import net.raphimc.minecraftauth.msa.request.MsaAuthCodeTokenRequest
import net.raphimc.minecraftauth.msa.service.MsaAuthService
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Exchanges an existing login.live.com browser session for a renewable MSA token. */
internal class CookieImportMsaAuthService(
    httpClient: HttpClient,
    applicationConfig: MsaApplicationConfig,
    private val cookieHeader: String,
) : MsaAuthService(httpClient, applicationConfig) {
    override fun acquireToken(): MsaToken {
        val cookies = normalizeMicrosoftCookieInput(cookieHeader)
        require(cookies.isNotEmpty()) { "The Microsoft cookie header is empty." }
        val parameters = applicationConfig.authCodeParameters.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
        val separator = if ('?' in applicationConfig.environment.authorizeUrl) '&' else '?'
        val request = GetRequest("${applicationConfig.environment.authorizeUrl}$separator$parameters").apply {
            setFollowRedirects(true)
            setHeader(HttpHeaders.ACCEPT, "text/html")
            setHeader(HttpHeaders.COOKIE, cookies)
        }
        val response = httpClient.execute(request)
        val callback = response.getFirstHeader(HttpHeaders.LOCATION).orElse(response.url.toString())
        val code = URI(callback).rawQuery
            ?.split('&')
            ?.mapNotNull { part ->
                val split = part.split('=', limit = 2)
                split.takeIf { it.size == 2 }?.let { it[0].urlDecode() to it[1].urlDecode() }
            }
            ?.firstOrNull { it.first == "code" }
            ?.second
            ?: error("The Microsoft cookies are expired or did not produce an authorization code.")
        return httpClient.executeAndHandle(MsaAuthCodeTokenRequest(applicationConfig, code))
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.name())
    private fun String.urlDecode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.name())
}

internal fun normalizeMicrosoftCookieInput(input: String): String {
    val trimmed = input.trim()
    require(trimmed.isNotEmpty()) { "The Microsoft cookie input is empty." }
    if (trimmed.startsWith("[")) {
        val array = com.google.gson.JsonParser.parseString(trimmed).asJsonArray
        return array.mapNotNull { item ->
            val cookie = item.asJsonObject
            val name = cookie.get("name")?.asString?.trim().orEmpty()
            val value = cookie.get("value")?.asString.orEmpty()
            val domain = cookie.get("domain")?.asString?.lowercase().orEmpty()
            "$name=$value".takeIf {
                name.isNotEmpty() && value.isNotEmpty() && (domain.isEmpty() || domain.endsWith("live.com"))
            }
        }.joinToString("; ").also {
            require(it.isNotEmpty()) { "The cookie export contains no login.live.com cookies." }
        }
    }
    if (trimmed.lineSequence().any { it.count { character -> character == '\t' } >= 6 }) {
        return trimmed.lineSequence().mapNotNull { line ->
            if (line.isBlank() || line.startsWith('#')) return@mapNotNull null
            val fields = line.split('\t')
            if (fields.size < 7 || !fields[0].lowercase().endsWith("live.com")) return@mapNotNull null
            "${fields[5]}=${fields[6]}"
        }.joinToString("; ").also {
            require(it.isNotEmpty()) { "The cookies.txt input contains no login.live.com cookies." }
        }
    }
    return trimmed.removePrefix("Cookie:").trim()
}

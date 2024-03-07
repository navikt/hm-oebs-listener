package no.nav.hjelpemidler

import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationFunction
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.Principal
import io.ktor.server.auth.UnauthorizedResponse
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.response.respond

fun AuthenticationConfig.token(
    name: String? = null,
    configure: TokenAuthenticationProvider.Config.() -> Unit,
) {
    val provider = TokenAuthenticationProvider(TokenAuthenticationProvider.Config(name).apply(configure))
    register(provider)
}

fun ApplicationRequest.tokenCredentials(): TokenCredential? {
    when (val authHeader = parseAuthorizationHeader()) {
        is HttpAuthHeader.Single -> {
            if (!authHeader.authScheme.equals(AuthScheme.Bearer, ignoreCase = true)) {
                return null
            }
            return TokenCredential(authHeader.blob)
        }

        else -> return null
    }
}

class TokenAuthenticationProvider(config: Config) : AuthenticationProvider(config) {
    val authenticationFunction = config.authenticationFunction

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val credentials = call.request.tokenCredentials()
        val principal = credentials?.let { authenticationFunction(call, it) }
        val cause =
            when {
                credentials == null -> AuthenticationFailedCause.NoCredentials
                principal == null -> AuthenticationFailedCause.InvalidCredentials
                else -> null
            }
        if (cause != null) {
            @Suppress("NAME_SHADOWING")
            context.challenge("", cause) { challenge, call ->
                call.respond(
                    UnauthorizedResponse(
                        HttpAuthHeader.Parameterized(
                            AuthScheme.Bearer,
                            mapOf("realm" to "Ktor Server"),
                        ),
                    ),
                )
                challenge.complete()
            }
        }
        if (principal != null) {
            context.principal(principal)
        }
    }

    class Config(name: String?) : AuthenticationProvider.Config(name) {
        var authenticationFunction: AuthenticationFunction<TokenCredential> = {
            throw NotImplementedError(
                "Token validate function is not specified. Use token { validate { ... } } to fix.",
            )
        }

        private fun validate(body: suspend ApplicationCall.(TokenCredential) -> Principal?) {
            authenticationFunction = body
        }

        fun validate(expectedToken: String) =
            validate {
                when (it.token) {
                    expectedToken -> TokenPrincipal(it.token)
                    else -> null
                }
            }
    }
}

data class TokenCredential(val token: String)

data class TokenPrincipal(val token: String) : Principal

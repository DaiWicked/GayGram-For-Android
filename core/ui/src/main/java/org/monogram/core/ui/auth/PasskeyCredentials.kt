package org.monogram.core.ui.auth

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import org.json.JSONObject
import org.monogram.core.models.PasskeyCredential
import java.security.MessageDigest

object PasskeyCredentials {
    class ClientDataMismatchException : Exception("Credential provider did not use the requested client data")

    private const val RELYING_PARTY = "telegram.org"
    private const val ORIGIN = "https://telegram.org"

    val supported: Boolean get() = Build.VERSION.SDK_INT >= 28

    suspend fun request(
        context: Context,
        options: String,
        registration: Boolean
    ): PasskeyCredential {
        check(supported)
        val operation = if (registration) Operation.Register else Operation.Login
        val request = CredentialRequest.parse(options, operation)
        val manager = CredentialManager.create(context)
        val response = when (operation) {
            Operation.Register -> register(context, manager, request)
            Operation.Login -> login(context, manager, request)
        }
        return CredentialResponse(response).toCredential(request)
    }

    private suspend fun register(
        context: Context,
        manager: CredentialManager,
        request: CredentialRequest,
    ): String {
        val response = manager.createCredential(
            context,
            CreatePublicKeyCredentialRequest(
                requestJson = request.publicKeyJson,
                clientDataHash = request.clientDataHash,
                origin = ORIGIN,
            ),
        ) as CreatePublicKeyCredentialResponse
        return response.registrationResponseJson
    }

    private suspend fun login(
        context: Context,
        manager: CredentialManager,
        request: CredentialRequest,
    ): String {
        val response = manager.getCredential(
            context,
            GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetPublicKeyCredentialOption(
                        request.publicKeyJson,
                        request.clientDataHash
                    )
                )
                .setOrigin(ORIGIN)
                .build(),
        )
        return (response.credential as PublicKeyCredential).authenticationResponseJson
    }

    private enum class Operation(val webAuthnType: String) {
        Register("webauthn.create"),
        Login("webauthn.get"),
    }

    private class CredentialRequest(
        val operation: Operation,
        val publicKeyJson: String,
        val clientDataJson: String,
    ) {
        val clientDataHash: ByteArray = MessageDigest.getInstance("SHA-256")
            .digest(clientDataJson.toByteArray(Charsets.UTF_8))

        companion object {
            fun parse(options: String, operation: Operation): CredentialRequest {
                val publicKey = JSONObject(options).getJSONObject("publicKey")
                val relyingParty = when (operation) {
                    Operation.Register -> publicKey.getJSONObject("rp").getString("id")
                    Operation.Login -> publicKey.getString("rpId")
                }
                require(relyingParty == RELYING_PARTY)
                val clientData = JSONObject().apply {
                    put("type", operation.webAuthnType)
                    put("challenge", publicKey.getString("challenge"))
                    put("origin", ORIGIN)
                }.toString()
                return CredentialRequest(operation, publicKey.toString(), clientData)
            }
        }
    }

    private class CredentialResponse(json: String) {
        private val credential = JSONObject(json)
        private val response = credential.getJSONObject("response")

        fun toCredential(request: CredentialRequest): PasskeyCredential {
            val id = credential.getString("id")
            val rawId = credential.getString("rawId")
            val returnedClientData = response.optString("clientDataJSON")
            if (returnedClientData.isNotEmpty() && returnedClientData != "<placeholder>") {
                val returnedBytes = Base64.decode(returnedClientData, Base64.URL_SAFE or Base64.NO_WRAP)
                if (!MessageDigest.isEqual(
                    MessageDigest.getInstance("SHA-256").digest(returnedBytes),
                    request.clientDataHash,
                )) throw ClientDataMismatchException()
            }
            return when (request.operation) {
                Operation.Register -> PasskeyCredential(
                    id = id,
                    rawId = rawId,
                    clientData = request.clientDataJson,
                    attestationData = bytes("attestationObject"),
                )

                Operation.Login -> PasskeyCredential(
                    id = id,
                    rawId = rawId,
                    clientData = request.clientDataJson,
                    authenticatorData = bytes("authenticatorData"),
                    signature = bytes("signature"),
                    userHandle = bytes("userHandle").toString(Charsets.UTF_8),
                )
            }
        }

        private fun bytes(name: String): ByteArray =
            Base64.decode(response.getString(name), Base64.URL_SAFE or Base64.NO_WRAP)
    }
}

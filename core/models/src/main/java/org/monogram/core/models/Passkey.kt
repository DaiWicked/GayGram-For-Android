package org.monogram.core.models

class PasskeyLoginOptions(val options: String, val dcId: Int, val authKeyId: Long)

class PasskeyCredential(
    val id: String,
    val rawId: String,
    val clientData: String,
    val authenticatorData: ByteArray = byteArrayOf(),
    val signature: ByteArray = byteArrayOf(),
    val userHandle: String = "",
    val attestationData: ByteArray = byteArrayOf(),
)

data class Passkey(
    val id: String,
    val name: String,
    val date: Int,
    val softwareEmojiId: Long?,
    val lastUsageDate: Int?,
)

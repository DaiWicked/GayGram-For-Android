package org.monogram.network.bridge.session

import org.monogram.core.models.Passkey
import org.monogram.core.models.PasskeyCredential
import uniffi.monogram_mtproto.PasskeyCredentialDto
import uniffi.monogram_mtproto.PasskeyDto

internal fun PasskeyCredential.toNative() = PasskeyCredentialDto(
    id, rawId, clientData, authenticatorData, signature, userHandle, attestationData,
)

internal fun PasskeyDto.toModel() = Passkey(id, name, date, softwareEmojiId, lastUsageDate)

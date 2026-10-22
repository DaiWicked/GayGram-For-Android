package org.monogram.network.bridge.session

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.monogram.core.models.PasskeyCredential
import uniffi.monogram_mtproto.PasskeyDto

class PasskeysTest {
    @Test
    fun credentialMappingPreservesSignedBytesAndClientData() {
        val data = "{\"type\":\"webauthn.get\",\"challenge\":\"fixture\"}"
        val result = PasskeyCredential(
            id = "encoded-id", rawId = "encoded-raw-id", clientData = data,
            authenticatorData = byteArrayOf(0, -1, 42), signature = byteArrayOf(-128, 127),
            userHandle = "2:1234567890123", attestationData = byteArrayOf(3, 2, 1),
        ).toNative()
        assertEquals(data, result.clientData)
        assertEquals("encoded-id", result.id)
        assertEquals("encoded-raw-id", result.rawId)
        assertEquals("2:1234567890123", result.userHandle)
        assertArrayEquals(byteArrayOf(0, -1, 42), result.authenticatorData)
        assertArrayEquals(byteArrayOf(-128, 127), result.signature)
        assertArrayEquals(byteArrayOf(3, 2, 1), result.attestationData)
    }

    @Test
    fun passkeyMappingPreservesOptionalMetadata() {
        val absent = PasskeyDto("id", "key", 123, null, null).toModel()
        assertEquals(null, absent.softwareEmojiId)
        assertEquals(null, absent.lastUsageDate)
        val present = PasskeyDto("id", "key", 123, 99999999999L, 456).toModel()
        assertEquals(99999999999L, present.softwareEmojiId)
        assertEquals(456, present.lastUsageDate)
    }
}

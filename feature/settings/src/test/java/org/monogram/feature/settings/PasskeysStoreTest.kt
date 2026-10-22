package org.monogram.feature.settings

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.monogram.core.common.Outcome
import org.monogram.core.models.*
import org.monogram.network.bridge.session.SessionOps

@OptIn(ExperimentalCoroutinesApi::class)
class PasskeysStoreTest {
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun cancellationClearsPendingCreationAndAllowsRetry() = runTest {
        val client = Client()
        val store = PasskeysStoreFactory(DefaultStoreFactory(), client).create()
        try {
            store.accept(PasskeysStore.Intent.Create)
            advanceUntilIdle()
            assertTrue(store.state.loading)
            assertEquals("options", store.state.options)
            store.accept(PasskeysStore.Intent.Created(null))
            advanceUntilIdle()
            assertFalse(store.state.loading)
            assertNull(store.state.options)
            assertFalse(store.state.providerFailed)
            assertEquals(0, client.registrations)
            store.accept(PasskeysStore.Intent.Create)
            advanceUntilIdle()
            assertEquals("options", store.state.options)
        } finally { store.dispose() }
    }

    @Test fun registrationAndDeletionRefreshServerList() = runTest {
        val client = Client()
        val store = PasskeysStoreFactory(DefaultStoreFactory(), client).create()
        try {
            store.accept(PasskeysStore.Intent.Create)
            advanceUntilIdle()
            store.accept(PasskeysStore.Intent.Created(PasskeyCredential("id", "id", "fixture")))
            advanceUntilIdle()
            assertEquals(listOf(client.key), store.state.keys)
            assertEquals(1, client.registrations)
            assertFalse(store.state.loading)
            store.accept(PasskeysStore.Intent.Delete("id"))
            advanceUntilIdle()
            assertTrue(store.state.keys.isEmpty())
            assertFalse(store.state.loading)
        } finally { store.dispose() }
    }

    @Test fun providerFailureIsVisibleAndRetryClearsIt() = runTest {
        val store = PasskeysStoreFactory(DefaultStoreFactory(), Client()).create()
        try {
            store.accept(PasskeysStore.Intent.Create)
            advanceUntilIdle()
            store.accept(PasskeysStore.Intent.Created(null, failed = true))
            advanceUntilIdle()
            assertTrue(store.state.providerFailed)
            assertFalse(store.state.loading)
            store.accept(PasskeysStore.Intent.Create)
            advanceUntilIdle()
            assertFalse(store.state.providerFailed)
        } finally { store.dispose() }
    }

    @Test fun clientDataMismatchIsDistinctAndRetryClearsIt() = runTest {
        val client = Client()
        val store = PasskeysStoreFactory(DefaultStoreFactory(), client).create()
        try {
            store.accept(PasskeysStore.Intent.Create)
            advanceUntilIdle()
            store.accept(PasskeysStore.Intent.Created(null, clientDataMismatch = true))
            advanceUntilIdle()
            assertTrue(store.state.clientDataMismatch)
            assertFalse(store.state.providerFailed)
            assertEquals(0, client.registrations)
            store.accept(PasskeysStore.Intent.Create)
            advanceUntilIdle()
            assertFalse(store.state.clientDataMismatch)
        } finally { store.dispose() }
    }

    private class Client : SessionOps {
        val key = Passkey("id", "fixture", 1, null, null)
        var keys = emptyList<Passkey>()
        var registrations = 0
        override suspend fun initPasskeyRegistration() = Outcome.Ok("options")
        override suspend fun registerPasskey(credential: PasskeyCredential): Outcome<Passkey> {
            registrations++
            keys = listOf(key)
            return Outcome.Ok(key)
        }
        override suspend fun getPasskeys() = Outcome.Ok(keys)
        override suspend fun deletePasskey(id: String): Outcome<Boolean> {
            keys = keys.filterNot { it.id == id }
            return Outcome.Ok(true)
        }
        override suspend fun connect() = Outcome.Ok(Unit)
        override suspend fun sendAuthCode(phone: String): Outcome<AuthState.AwaitingCode> = Outcome.Err("unused")
        override suspend fun signIn(phone: String, phoneCodeHash: String, code: String): Outcome<AuthState> = Outcome.Err("unused")
        override suspend fun checkPassword(password: String): Outcome<AuthState.Authorized> = Outcome.Err("unused")
        override fun libraryVersion() = "fixture"
        override suspend fun logout() = Outcome.Ok(Unit)
        override fun close() {}
    }
}

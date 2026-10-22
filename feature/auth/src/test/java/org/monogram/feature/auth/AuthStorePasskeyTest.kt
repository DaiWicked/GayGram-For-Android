package org.monogram.feature.auth

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
class AuthStorePasskeyTest {
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun cancellationReleasesLoadingAndAllowsAnotherRequest() = runTest {
        val store = AuthStoreFactory(DefaultStoreFactory(), Client()).create()
        try {
            store.accept(AuthStore.Intent.StartPasskeyLogin)
            advanceUntilIdle()
            assertNotNull(store.state.passkeyOptions)
            store.accept(AuthStore.Intent.PasskeyResult(null))
            advanceUntilIdle()
            assertFalse(store.state.loading)
            assertNull(store.state.passkeyOptions)
            assertNull(store.state.error)
            store.accept(AuthStore.Intent.StartPasskeyLogin)
            advanceUntilIdle()
            assertNotNull(store.state.passkeyOptions)
        } finally { store.dispose() }
    }

    @Test fun passwordRequiredContinuesToExistingPasswordScreen() = runTest {
        val store = AuthStoreFactory(DefaultStoreFactory(), Client()).create()
        try {
            store.accept(AuthStore.Intent.StartPasskeyLogin)
            advanceUntilIdle()
            store.accept(AuthStore.Intent.PasskeyResult(PasskeyCredential("id", "id", "fixture")))
            advanceUntilIdle()
            assertEquals(AuthStore.Phase.PasswordEntry, store.state.phase)
            assertFalse(store.state.loading)
            assertNull(store.state.passkeyOptions)
        } finally { store.dispose() }
    }

    @Test fun providerFailureIsVisibleAndBackClearsPendingRequest() = runTest {
        val store = AuthStoreFactory(DefaultStoreFactory(), Client()).create()
        try {
            store.accept(AuthStore.Intent.StartPasskeyLogin)
            advanceUntilIdle()
            store.accept(AuthStore.Intent.PasskeyResult(null, failed = true))
            advanceUntilIdle()
            assertEquals(AuthStore.Error.PasskeyFailed, store.state.error)
            assertFalse(store.state.loading)
            store.accept(AuthStore.Intent.StartPasskeyLogin)
            advanceUntilIdle()
            store.accept(AuthStore.Intent.BackToPhone)
            assertNull(store.state.passkeyOptions)
            assertFalse(store.state.loading)
        } finally { store.dispose() }
    }

    @Test fun clientDataMismatchStopsBeforeLoginAndAllowsRetry() = runTest {
        val client = Client()
        val store = AuthStoreFactory(DefaultStoreFactory(), client).create()
        try {
            store.accept(AuthStore.Intent.StartPasskeyLogin)
            advanceUntilIdle()
            store.accept(AuthStore.Intent.PasskeyResult(null, clientDataMismatch = true))
            advanceUntilIdle()
            assertEquals(AuthStore.Error.PasskeyClientDataMismatch, store.state.error)
            assertFalse(store.state.loading)
            assertNull(store.state.passkeyOptions)
            assertEquals(0, client.loginCalls)
            store.accept(AuthStore.Intent.StartPasskeyLogin)
            advanceUntilIdle()
            assertNull(store.state.error)
            assertNotNull(store.state.passkeyOptions)
        } finally { store.dispose() }
    }

    private class Client : SessionOps {
        var loginCalls = 0
        override suspend fun initPasskeyLogin() = Outcome.Ok(PasskeyLoginOptions("options", 2, 123))
        override suspend fun finishPasskeyLogin(options: PasskeyLoginOptions, credential: PasskeyCredential): Outcome<AuthState> {
            loginCalls++
            return Outcome.Ok(AuthState.AwaitingPassword)
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

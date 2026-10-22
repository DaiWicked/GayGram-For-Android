package org.monogram.feature.settings

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlinx.coroutines.launch
import org.monogram.core.common.Outcome
import org.monogram.core.common.telegram.TelegramError
import org.monogram.core.models.Passkey
import org.monogram.core.models.PasskeyCredential
import org.monogram.network.bridge.session.SessionOps

interface PasskeysStore : Store<PasskeysStore.Intent, PasskeysStore.State, Nothing> {
    sealed interface Intent {
        data object Refresh : Intent
        data object Create : Intent
        class Created(val credential: PasskeyCredential?, val failed: Boolean = false, val clientDataMismatch: Boolean = false) : Intent
        data class Delete(val id: String) : Intent
    }
    data class State(
        val keys: List<Passkey> = emptyList(),
        val providerFailed: Boolean = false,
        val clientDataMismatch: Boolean = false,
        val loading: Boolean = false,
        val error: TelegramError? = null,
        val options: String? = null,
    )
}

internal class PasskeysStoreFactory(private val factory: StoreFactory, private val client: SessionOps) {
    fun create(): PasskeysStore = object : PasskeysStore,
        Store<PasskeysStore.Intent, PasskeysStore.State, Nothing> by factory.create(
            name = "PasskeysStore",
            initialState = PasskeysStore.State(),
            executorFactory = { Executor() },
            reducer = Reducer<PasskeysStore.State, PasskeysStore.State> { it },
        ) {}

    private inner class Executor : CoroutineExecutor<PasskeysStore.Intent, Nothing, PasskeysStore.State, PasskeysStore.State, Nothing>() {
        override fun executeIntent(intent: PasskeysStore.Intent) {
            if (state().loading && intent !is PasskeysStore.Intent.Created) return
            dispatch(state().copy(loading = true, error = null, providerFailed = false, clientDataMismatch = false))
            scope.launch {
                when (intent) {
                    PasskeysStore.Intent.Refresh -> refresh()
                    PasskeysStore.Intent.Create -> when (val result = client.initPasskeyRegistration()) {
                        is Outcome.Ok -> {
                            dispatch(state().copy(options = result.value))
                            return@launch
                        }
                        is Outcome.Err -> fail(result)
                    }
                    is PasskeysStore.Intent.Created -> {
                        if (state().options == null) return@launch
                        dispatch(state().copy(options = null, providerFailed = intent.failed, clientDataMismatch = intent.clientDataMismatch))
                        intent.credential?.let { credential ->
                            when (val result = client.registerPasskey(credential)) {
                                is Outcome.Ok -> refresh()
                                is Outcome.Err -> fail(result)
                            }
                        }
                    }
                    is PasskeysStore.Intent.Delete -> when (val result = client.deletePasskey(intent.id)) {
                        is Outcome.Ok -> refresh()
                        is Outcome.Err -> fail(result)
                    }
                }
                dispatch(state().copy(loading = false))
            }
        }
        private suspend fun refresh() {
            when (val result = client.getPasskeys()) {
                is Outcome.Ok -> dispatch(state().copy(keys = result.value))
                is Outcome.Err -> fail(result)
            }
        }
        private fun fail(result: Outcome.Err) {
            dispatch(state().copy(error = result.telegramError))
        }
    }
}

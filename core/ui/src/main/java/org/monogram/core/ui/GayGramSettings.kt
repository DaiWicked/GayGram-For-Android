package org.monogram.core.ui

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The switches that make GayGram more than a reskin.
 *
 * Kept apart from [AppearanceSettings]: that object is about how the app looks and is already
 * large, while these change what the app does. One field per shipped feature.
 */
@Immutable
data class GayGramState(
    val keepRevokedMessages: Boolean = true,
)

object GayGramSettings {
    private const val PREFS = "gaygram_settings"
    private const val KEY_KEEP_REVOKED = "keep_revoked_messages"

    private val mutable = MutableStateFlow(GayGramState())
    val state: StateFlow<GayGramState> = mutable.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    fun install(context: Context) {
        val app = context.applicationContext
        appContext = app
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        mutable.value = GayGramState(
            keepRevokedMessages = prefs.getBoolean(KEY_KEEP_REVOKED, true),
        )
    }

    fun setKeepRevokedMessages(enabled: Boolean) {
        mutable.update { it.copy(keepRevokedMessages = enabled) }
        val app = appContext ?: return
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_KEEP_REVOKED, mutable.value.keepRevokedMessages)
        }
    }
}

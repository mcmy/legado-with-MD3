package io.legado.app.utils

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import io.legado.app.domain.gateway.OtherSettingsGateway
import org.koin.core.context.GlobalContext

object FirebaseManager {

    private val otherSettingsGateway
        get() = GlobalContext.get().get<OtherSettingsGateway>()

    val isEnabled: Boolean
        get() = otherSettingsGateway.currentSettings.firebaseEnable

    fun init(context: Context) {
        applyState(context, isEnabled)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        applyState(context, enabled)
    }

    private fun applyState(context: Context, enabled: Boolean) {
        if (enabled) {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true)
        } else {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(false)
                    FirebaseApp.getInstance().delete()
                }
            } catch (_: Exception) {
                // 忽略异常
            }
        }
    }
}

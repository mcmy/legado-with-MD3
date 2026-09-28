package io.legado.app.utils

import android.content.Context
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import io.legado.app.domain.gateway.ThemeSettingsGateway
import org.koin.core.context.GlobalContext

private val themeSettingsGateway
    get() = GlobalContext.get().get<ThemeSettingsGateway>()

fun loadAnimation(context: Context, @AnimRes id: Int): Animation {
    val animation = AnimationUtils.loadAnimation(context, id)
    if (themeSettingsGateway.currentSettings.appTheme == "4") {
        animation.duration = 0
    }
    return animation
}

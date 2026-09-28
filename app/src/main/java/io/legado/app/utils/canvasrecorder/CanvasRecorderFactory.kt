package io.legado.app.utils.canvasrecorder

import android.os.Build
import io.legado.app.domain.gateway.ReadSettingsGateway
import org.koin.core.context.GlobalContext

object CanvasRecorderFactory {

    private val readSettingsGateway
        get() = GlobalContext.get().get<ReadSettingsGateway>()

    private val atLeastApi24 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    private val atLeastApi29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val isSupport = atLeastApi24

    // issue 3868
    fun create(locked: Boolean = false): CanvasRecorder {
        val impl = when {
            !readSettingsGateway.currentSettings.optimizeRender -> CanvasRecorderImpl()
            atLeastApi29 -> CanvasRecorderApi29Impl()
            atLeastApi24 -> CanvasRecorderApi23Impl()
            else -> CanvasRecorderImpl()
        }
        return if (locked) {
            CanvasRecorderLocked(impl)
        } else {
            impl
        }
    }

}

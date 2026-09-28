package io.legado.app.help

import io.legado.app.domain.gateway.OtherSettingsGateway
import io.legado.app.utils.LogUtils
import org.koin.core.context.GlobalContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

object DispatchersMonitor {

    private const val TAG = "DispatchersMonitor"

    private val otherGateway by lazy { GlobalContext.get().get<OtherSettingsGateway>() }

    private val dispatcher by lazy {
        Executors.newSingleThreadExecutor {
            Thread(it, TAG)
        }.asCoroutineDispatcher()
    }

    private val scope = CoroutineScope(dispatcher)

    fun init() {
        scope.coroutineContext.cancelChildren()
        if (!otherGateway.currentSettings.recordLog) {
            return
        }
        monitor(IO)
        monitor(Default)
        monitor(Main)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitor(dispatcher: CoroutineDispatcher) {
        scope.launch {
            while (isActive) select {
                launch {
                    withContext(dispatcher) {
                        delay(3000)
                    }
                }.onJoin {}
                onTimeout(5000) {
                    LogUtils.d(TAG, "Dispatcher $dispatcher is timed out waiting for for 5000ms.")
                }
            }
        }
    }

}
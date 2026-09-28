package io.legado.app.feature.onboarding

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.help.config.ThemeConfigStore
import io.legado.app.utils.takePersistablePermissionSafely
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingRouteScreen(
    onFinish: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            uri.takePersistablePermissionSafely(context, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.onIntent(OnboardingIntent.SelectBookFolder(uri.toString()))
        }
    }
    val restoreFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onIntent(OnboardingIntent.RestoreLocalFile(uri.toString()))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                OnboardingEffect.NavigateHome -> onNavigateHome()
                OnboardingEffect.Finish -> onFinish()
                OnboardingEffect.OpenBookFolderPicker -> folderPicker.launch(null)
                OnboardingEffect.OpenRestoreFilePicker ->
                    restoreFilePicker.launch(arrayOf("application/zip"))
                OnboardingEffect.ApplyDayNight -> ThemeConfigStore.applyDayNightLive()
                is OnboardingEffect.ShowToast -> context.toastOnUi(effect.resId)
            }
        }
    }

    OnboardingScreen(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

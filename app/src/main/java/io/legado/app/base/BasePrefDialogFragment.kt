package io.legado.app.base

import androidx.lifecycle.LifecycleEventObserver
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.legado.app.domain.gateway.ThemeSettingsGateway
import org.koin.core.context.GlobalContext


abstract class BasePrefDialogFragment(
) : BottomSheetDialogFragment() {

    private val themeGateway get() = GlobalContext.get().get<ThemeSettingsGateway>()

    override fun onStart() {
        super.onStart()
        if (themeGateway.currentSettings.appTheme == "4") {
//            dialog?.window?.let {
//                it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//                val attr = it.attributes
//                attr.dimAmount = 0.0f
//                attr.windowAnimations = 0
//                it.attributes = attr
//                it.setBackgroundDrawableResource(R.color.transparent)
//            }

            // 修改gravity的时机一般在子类的onStart方法中, 因此需要在onStart之后执行.
            lifecycle.addObserver(LifecycleEventObserver { _, event ->
//                if (event == Lifecycle.Event.ON_START) {
//                    when (dialog?.window?.attributes?.gravity) {
//                        Gravity.TOP -> view?.setBackgroundResource(R.drawable.bg_eink_border_bottom)
//                        Gravity.BOTTOM -> view?.setBackgroundResource(R.drawable.bg_eink_border_top)
//                        else -> {
//                            val padding = 2.dpToPx();
//                            view?.setPadding(padding, padding, padding, padding)
//                            view?.setBackgroundResource(R.drawable.bg_eink_border_dialog)
//                        }
//                    }
//                }
            })
        }
    }
}
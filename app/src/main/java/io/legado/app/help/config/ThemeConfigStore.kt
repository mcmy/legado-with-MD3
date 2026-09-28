package io.legado.app.help.config

import android.content.Context
import android.graphics.Bitmap
import android.util.DisplayMetrics
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.toColorInt
import io.legado.app.domain.gateway.AppShellSettingsGateway
import io.legado.app.domain.gateway.ThemeSettingsGateway
import io.legado.app.ui.book.read.ConfigUpdateAction
import io.legado.app.ui.book.read.ReadConfigUpdateBus
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.constant.Theme
import io.legado.app.help.DefaultData
import io.legado.app.utils.BitmapUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.externalFiles
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getFile
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import io.legado.app.utils.hexString
import io.legado.app.utils.isNightMode
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.stackBlur
import io.legado.app.utils.sysConfiguration
import org.koin.core.context.GlobalContext
import splitties.init.appCtx
import java.io.File

@Keep
object ThemeConfigStore {
    const val configFileName = "themeConfig.json"
    val configFilePath = FileUtils.getPath(appCtx.filesDir, configFileName)

    private val themeSettingsGateway: ThemeSettingsGateway
        get() = GlobalContext.get().get()

    private val shellSettingsGateway: AppShellSettingsGateway
        get() = GlobalContext.get().get()

    val isNightTheme: Boolean
        get() = when (shellSettingsGateway.currentSettings.themeMode) {
            "1" -> false
            "2" -> true
            else -> sysConfiguration.isNightMode
        }

    val configList: ArrayList<Config> by lazy {
        val cList = getConfigs() ?: DefaultData.themeConfigs
        ArrayList(cList)
    }

    fun getTheme() = when {
        isNightTheme -> Theme.Dark
        else -> Theme.Light
    }

    fun applyDayNight(context: Context) {
        initNightMode()
        ReadConfigUpdateBus.post(setOf(ConfigUpdateAction.UpdateStyle))
    }

    /**
     * Compose 界面通过 ThemeConfig.themeMode 快照状态自动换色；旧 View 界面由
     * BaseActivity 的兼容策略决定热更新、重新绑定或受控重建。
     */
    fun applyDayNightLive() {
        initNightMode()
        ReadConfigUpdateBus.post(setOf(ConfigUpdateAction.UpdateStyle))
    }

    fun applyDayNightInit(context: Context) {
        initNightMode()
    }

    fun initNightMode() {
        val mode = when (shellSettingsGateway.currentSettings.themeMode) {
            "1" -> AppCompatDelegate.MODE_NIGHT_NO
            "2" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getBgImage(context: Context, metrics: DisplayMetrics): Bitmap? {
        val theme = themeSettingsGateway.currentSettings
        val bgCfg = when (getTheme()) {
            Theme.Light -> Pair(
                theme.backgroundImageLight,
                theme.backgroundImageBlurring
            )

            Theme.Dark -> Pair(
                theme.backgroundImageDark,
                theme.backgroundImageDarkBlurring
            )

            else -> null
        } ?: return null
        if (bgCfg.first.isNullOrBlank()) return null
        val bgImage = BitmapUtils
            .decodeBitmap(bgCfg.first!!, metrics.widthPixels, metrics.heightPixels)
        if (bgCfg.second == 0) {
            return bgImage
        }
        return bgImage?.stackBlur(bgCfg.second)
    }

    fun upConfig() {
        getConfigs()?.forEach { config ->
            addConfig(config)
        }
    }

    fun save() {
        val json = GSON.toJson(configList)
        FileUtils.writeTextAtomic(configFilePath, json)
    }

    fun addConfig(json: String): Boolean {
        GSON.fromJsonObject<Config>(json.trim { it < ' ' }).getOrNull()
            ?.let {
                if (validateConfig(it)) {
                    addConfig(it)
                    return true
                }
            }
        return false
    }

    fun addConfig(newConfig: Config) {
        if (!validateConfig(newConfig)) {
            return
        }
        configList.forEachIndexed { index, config ->
            if (newConfig.themeName == config.themeName) {
                configList[index] = newConfig
                return
            }
        }
        configList.add(newConfig)
        save()
    }

    private fun validateConfig(config: Config): Boolean {
        try {
            config.primaryColor.toColorInt()
            config.accentColor.toColorInt()
            config.backgroundColor.toColorInt()
            config.bottomBackground.toColorInt()
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun getConfigs(): List<Config>? {
        val configFile = File(configFilePath)
        if (configFile.exists()) {
            kotlin.runCatching {
                val json = configFile.readText()
                return GSON.fromJsonArray<Config>(json).getOrThrow()
            }.onFailure {
                it.printOnDebug()
            }
        }
        return null
    }

    /**
     * 清理无用背景图片
     */
    fun clearBg() {
        val theme = themeSettingsGateway.currentSettings
        val bgImagePath = theme.backgroundImageLight
        appCtx.externalFiles.getFile(PreferKey.bgImage).listFiles()?.forEach {
            if (it.absolutePath != bgImagePath) {
                it.delete()
            }
        }
        val bgImageNPath = theme.backgroundImageDark
        appCtx.externalFiles.getFile(PreferKey.bgImageN).listFiles()?.forEach {
            if (it.absolutePath != bgImageNPath) {
                it.delete()
            }
        }
    }

    fun getDurConfig(context: Context): Config {
        val isNight = isNightTheme
        val name = if (isNight) {
            "MD3-Night"
        } else {
            "MD3-Day"
        }
        return if (isNight) {
            getNightTheme(context, name)
        } else {
            getDayTheme(context, name)
        }
    }

    private fun getDayTheme(context: Context, name: String): Config {
        val primary =
            context.getPrefInt(PreferKey.cPrimary, context.getCompatColor(R.color.md_brown_500))
        val accent =
            context.getPrefInt(PreferKey.cAccent, context.getCompatColor(R.color.md_red_600))
        val background =
            context.getPrefInt(PreferKey.cBackground, context.getCompatColor(R.color.md_grey_100))
        val bBackground =
            context.getPrefInt(PreferKey.cBBackground, context.getCompatColor(R.color.md_grey_200))
        val theme = themeSettingsGateway.currentSettings
        val bgImgPath = theme.backgroundImageLight
        val bgImgBlur = theme.backgroundImageBlurring

        return Config(
            themeName = name,
            isNightTheme = false,
            primaryColor = "#${primary.hexString}",
            accentColor = "#${accent.hexString}",
            backgroundColor = "#${background.hexString}",
            bottomBackground = "#${bBackground.hexString}",
            backgroundImgPath = bgImgPath,
            backgroundImgBlur = bgImgBlur
        )
    }

    private fun getNightTheme(context: Context, name: String): Config {
        val primary =
            context.getPrefInt(
                PreferKey.cNPrimary,
                context.getCompatColor(R.color.md_blue_grey_600)
            )
        val accent =
            context.getPrefInt(
                PreferKey.cNAccent,
                context.getCompatColor(R.color.md_deep_orange_800)
            )
        val background =
            context.getPrefInt(PreferKey.cNBackground, context.getCompatColor(R.color.md_grey_900))
        val bBackground =
            context.getPrefInt(PreferKey.cNBBackground, context.getCompatColor(R.color.md_grey_850))
        val theme = themeSettingsGateway.currentSettings
        val bgImgPath = theme.backgroundImageDark
        val bgImgBlur = theme.backgroundImageDarkBlurring
        return Config(
            themeName = name,
            isNightTheme = true,
            primaryColor = "#${primary.hexString}",
            accentColor = "#${accent.hexString}",
            backgroundColor = "#${background.hexString}",
            bottomBackground = "#${bBackground.hexString}",
            backgroundImgPath = bgImgPath,
            backgroundImgBlur = bgImgBlur
        )
    }

    @Keep
    data class Config(
        var themeName: String,
        var isNightTheme: Boolean,
        var primaryColor: String,
        var accentColor: String,
        var backgroundColor: String,
        var bottomBackground: String,
        var backgroundImgPath: String?,
        var backgroundImgBlur: Int
    ) {

        override fun hashCode(): Int {
            return GSON.toJson(this).hashCode()
        }

        override fun equals(other: Any?): Boolean {
            other ?: return false
            if (other is Config) {
                return other.themeName == themeName
                        && other.isNightTheme == isNightTheme
                        && other.primaryColor == primaryColor
                        && other.accentColor == accentColor
                        && other.backgroundColor == backgroundColor
                        && other.bottomBackground == bottomBackground
                        && other.backgroundImgPath == backgroundImgPath
                        && other.backgroundImgBlur == backgroundImgBlur
            }
            return false
        }

        fun toMap() = mapOf(
            "themeName" to themeName,
            "isNightTheme" to isNightTheme,
            "primaryColor" to primaryColor,
            "accentColor" to accentColor,
            "backgroundColor" to backgroundColor,
            "bottomBackground" to bottomBackground,
            "backgroundImgPath" to backgroundImgPath,
            "backgroundImgBlur" to backgroundImgBlur
        )

    }

}

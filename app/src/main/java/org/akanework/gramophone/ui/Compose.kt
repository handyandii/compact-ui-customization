package org.akanework.gramophone.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import org.akanework.gramophone.logic.enableEdgeToEdgeProperly
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.logic.ui.AccentColors

abstract class BaseComposeActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    val pureDarkFlow by lazy {
        callbackFlow {
            val cb = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "pureDark") {
                    trySendBlocking(prefs.getBooleanStrict("pureDark", false))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(cb)
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(cb)
            }
        }.stateIn(
            lifecycleScope, WhileSubscribed(),
            prefs.getBooleanStrict("pureDark", false)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        AccentColors.applyTo(theme, resources, prefs)
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeProperly()
    }
}

@Composable
fun BaseComposeActivity.GramophoneTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val pureDark by pureDarkFlow.collectAsState()
    GramophoneTheme(useDarkTheme, pureDark, content)
}

@Composable
fun GramophoneTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    pureDark: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accentPreset = remember(context) {
        AccentColors.selected(PreferenceManager.getDefaultSharedPreferences(context.applicationContext))
    }
    MaterialTheme(
        colorScheme = (if (accentPreset != null) {
            // A color theme preset is applied to the view theme, use it instead of device colors.
            themeColorScheme(context, useDarkTheme)
        } else if (useDarkTheme) {
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                dynamicDarkColorScheme(LocalContext.current)
            else
                darkColorScheme()).let {
                if (pureDark) {
                    it.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceVariant = Color.Black,
                        surfaceContainerLowest = Color.Black,
                        surfaceContainerLow = Color.Black,
                        surfaceContainer = Color.Black,
                        surfaceContainerHigh = Color.Black,
                        surfaceContainerHighest = Color.Black,
                    )
                } else it
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                dynamicLightColorScheme(LocalContext.current)
            else
                lightColorScheme()
        }), content = {
            CompositionLocalProvider(
                LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
            ) {
                content()
            }
        }
    )
}

/** Compose color scheme from the Material color attributes of the context's (view) theme. */
private fun themeColorScheme(context: Context, dark: Boolean): ColorScheme {
    fun c(attr: Int) = Color(MaterialColors.getColor(context, attr, 0))
    return (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c(androidx.appcompat.R.attr.colorPrimary),
        onPrimary = c(com.google.android.material.R.attr.colorOnPrimary),
        primaryContainer = c(com.google.android.material.R.attr.colorPrimaryContainer),
        onPrimaryContainer = c(com.google.android.material.R.attr.colorOnPrimaryContainer),
        inversePrimary = c(com.google.android.material.R.attr.colorPrimaryInverse),
        secondary = c(com.google.android.material.R.attr.colorSecondary),
        onSecondary = c(com.google.android.material.R.attr.colorOnSecondary),
        secondaryContainer = c(com.google.android.material.R.attr.colorSecondaryContainer),
        onSecondaryContainer = c(com.google.android.material.R.attr.colorOnSecondaryContainer),
        tertiary = c(com.google.android.material.R.attr.colorTertiary),
        onTertiary = c(com.google.android.material.R.attr.colorOnTertiary),
        tertiaryContainer = c(com.google.android.material.R.attr.colorTertiaryContainer),
        onTertiaryContainer = c(com.google.android.material.R.attr.colorOnTertiaryContainer),
        background = c(android.R.attr.colorBackground),
        onBackground = c(com.google.android.material.R.attr.colorOnBackground),
        surface = c(com.google.android.material.R.attr.colorSurface),
        onSurface = c(com.google.android.material.R.attr.colorOnSurface),
        surfaceVariant = c(com.google.android.material.R.attr.colorSurfaceVariant),
        onSurfaceVariant = c(com.google.android.material.R.attr.colorOnSurfaceVariant),
        surfaceTint = c(androidx.appcompat.R.attr.colorPrimary),
        inverseSurface = c(com.google.android.material.R.attr.colorSurfaceInverse),
        inverseOnSurface = c(com.google.android.material.R.attr.colorOnSurfaceInverse),
        error = c(androidx.appcompat.R.attr.colorError),
        onError = c(com.google.android.material.R.attr.colorOnError),
        errorContainer = c(com.google.android.material.R.attr.colorErrorContainer),
        onErrorContainer = c(com.google.android.material.R.attr.colorOnErrorContainer),
        outline = c(com.google.android.material.R.attr.colorOutline),
        outlineVariant = c(com.google.android.material.R.attr.colorOutlineVariant),
        surfaceBright = c(com.google.android.material.R.attr.colorSurfaceBright),
        surfaceDim = c(com.google.android.material.R.attr.colorSurfaceDim),
        surfaceContainerLowest = c(com.google.android.material.R.attr.colorSurfaceContainerLowest),
        surfaceContainerLow = c(com.google.android.material.R.attr.colorSurfaceContainerLow),
        surfaceContainer = c(com.google.android.material.R.attr.colorSurfaceContainer),
        surfaceContainerHigh = c(com.google.android.material.R.attr.colorSurfaceContainerHigh),
        surfaceContainerHighest = c(com.google.android.material.R.attr.colorSurfaceContainerHighest),
    )
}

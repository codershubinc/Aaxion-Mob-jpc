package com.codershubinc.aaxion_mob.ui.designsystem

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.codershubinc.aaxion_mob.MainActivity
import com.codershubinc.aaxion_mob.core.data.DataStoreManager

/**
 * Traverses view hierarchy to locate the host window provider.
 */
fun findDialogWindow(view: View): Window? {
    var parent: ViewParent? = view.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) {
            return parent.window
        }
        parent = parent.parent
    }
    var ctx: Context? = view.context
    while (ctx is ContextWrapper) {
        if (ctx is Dialog) {
            return ctx.window
        }
        if (ctx is Activity) {
            return null
        }
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Synchronizes modal window insets and system bars with application preferences,
 * ensuring no status bar flicker or layout shifts occur when modals open and dismiss.
 */
@Composable
fun SyncDialogWindow(
    clearDimBehind: Boolean = false
) {
    val view = LocalView.current
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val hideStatusBar by dataStoreManager.hideStatusBar.collectAsState(initial = MainActivity.isStatusBarHiddenCached)
    val hideNavBar by dataStoreManager.hideNavigationBar.collectAsState(initial = MainActivity.isNavBarHiddenCached)

    val dialogWindow = remember(view) { findDialogWindow(view) }

    SideEffect {
        dialogWindow?.let { win ->
            if (clearDimBehind) {
                win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
            WindowCompat.setDecorFitsSystemWindows(win, false)
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            var hideTypes = 0
            var showTypes = 0
            if (hideStatusBar) {
                hideTypes = hideTypes or WindowInsetsCompat.Type.statusBars()
            } else {
                showTypes = showTypes or WindowInsetsCompat.Type.statusBars()
            }
            if (hideNavBar) {
                hideTypes = hideTypes or WindowInsetsCompat.Type.navigationBars()
            } else {
                showTypes = showTypes or WindowInsetsCompat.Type.navigationBars()
            }

            if (hideTypes != 0) insetsController.hide(hideTypes)
            if (showTypes != 0) insetsController.show(showTypes)
        }
    }
}

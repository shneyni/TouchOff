package com.touchoff.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-locks touch on every boot, unconditionally, as long as a password has
 * been set up. There is intentionally NO "reboot to recover" escape hatch
 * here — the only way out is entering the correct password inside the app.
 * This is a deliberate choice: it removes the safety net in exchange for
 * there being no way to bypass the password, ever, including via reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Nothing to protect yet if the user never finished initial setup.
        if (!PasswordManager.isPasswordSet(context)) return

        val pendingResult = goAsync()
        Thread {
            try {
                val node = RootHelper.findTouchscreenNode()
                if (node != null) {
                    val result = RootHelper.disableTouch(node)
                    if (result.success) {
                        TouchLockState.saveLocked(context, node.devicePath, node.originalPerm)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}

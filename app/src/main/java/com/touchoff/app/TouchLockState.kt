package com.touchoff.app

import android.content.Context

/**
 * Persists which touchscreen input node we disabled and what its original
 * file permissions were, so we can restore it correctly even if the app
 * process is killed and restarted between lock and unlock (the unlock
 * trigger comes from the accessibility service, which may run independently
 * of the main activity's process lifecycle).
 */
object TouchLockState {
    private const val PREFS = "touch_lock_state"
    private const val KEY_LOCKED = "is_locked"
    private const val KEY_NODE = "node_path"
    private const val KEY_ORIGINAL_PERM = "original_perm"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isLocked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCKED, false)

    fun saveLocked(context: Context, nodePath: String, originalPerm: String) {
        prefs(context).edit()
            .putBoolean(KEY_LOCKED, true)
            .putString(KEY_NODE, nodePath)
            .putString(KEY_ORIGINAL_PERM, originalPerm)
            .apply()
    }

    fun clearLocked(context: Context) {
        prefs(context).edit().putBoolean(KEY_LOCKED, false).apply()
    }

    fun getNodePath(context: Context): String? = prefs(context).getString(KEY_NODE, null)

    fun getOriginalPerm(context: Context): String? = prefs(context).getString(KEY_ORIGINAL_PERM, null)
}

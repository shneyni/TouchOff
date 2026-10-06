package com.touchoff.app

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

object PasswordManager {
    private const val PREFS = "touch_locker_prefs"
    private const val KEY_HASH = "password_hash"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isPasswordSet(context: Context): Boolean =
        prefs(context).contains(KEY_HASH)

    fun setPassword(context: Context, password: String) {
        prefs(context).edit().putString(KEY_HASH, hash(password)).apply()
    }

    fun checkPassword(context: Context, password: String): Boolean {
        val stored = prefs(context).getString(KEY_HASH, null) ?: return false
        return stored == hash(password)
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

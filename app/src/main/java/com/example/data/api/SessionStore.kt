package com.example.data.api

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the Supabase session so a logged-in user is not asked to log in again on restart.
 * Initialised once from [com.example.WudianApp].
 */
object SessionStore {
    private const val PREFS_NAME = "wudian_session"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun save(token: String, userId: String) {
        prefs?.edit()?.putString(KEY_TOKEN, token)?.putString(KEY_USER_ID, userId)?.apply()
    }

    fun load(): Pair<String, String>? {
        val token = prefs?.getString(KEY_TOKEN, null) ?: return null
        val userId = prefs?.getString(KEY_USER_ID, null) ?: return null
        if (token.isBlank() || userId.isBlank()) return null
        return token to userId
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}

package com.example.zhshop.data.api

import android.content.Context
import android.content.SharedPreferences
import com.example.zhshop.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zhshop_prefs", Context.MODE_PRIVATE)

    private val _currentUserFlow = MutableStateFlow<UserProfile?>(loadSavedUser())
    val currentUserFlow: StateFlow<UserProfile?> = _currentUserFlow.asStateFlow()

    private val _agreementAgreedFlow = MutableStateFlow(isAgreementValid())
    val agreementAgreedFlow: StateFlow<Boolean> = _agreementAgreedFlow.asStateFlow()

    init {
        // Clean up legacy dummy dev token if present
        val savedToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        if (savedToken == "default_dev_token_zhshop_2026") {
            prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
        }
    }

    fun isAgreementValid(): Boolean {
        val agreed = prefs.getBoolean(KEY_AGREED_AGREEMENT, false)
        val version = prefs.getInt(KEY_AGREEMENT_VERSION, 0)
        val timestamp = prefs.getLong(KEY_AGREED_TIMESTAMP, 0L)
        val checksum = prefs.getString(KEY_AGREED_CHECKSUM, null)

        if (!agreed || version != com.example.zhshop.data.model.AgreementContent.CURRENT_AGREEMENT_VERSION || timestamp <= 0L || checksum.isNullOrBlank()) {
            return false
        }
        val expectedChecksum = com.example.zhshop.data.model.AgreementContent.generateChecksum(version, timestamp)
        return checksum == expectedChecksum
    }

    fun saveAgreementConsent() {
        val version = com.example.zhshop.data.model.AgreementContent.CURRENT_AGREEMENT_VERSION
        val timestamp = System.currentTimeMillis()
        val checksum = com.example.zhshop.data.model.AgreementContent.generateChecksum(version, timestamp)

        prefs.edit()
            .putBoolean(KEY_AGREED_AGREEMENT, true)
            .putInt(KEY_AGREEMENT_VERSION, version)
            .putLong(KEY_AGREED_TIMESTAMP, timestamp)
            .putString(KEY_AGREED_CHECKSUM, checksum)
            .apply()

        _agreementAgreedFlow.value = true
    }

    fun revokeAgreementConsent() {
        prefs.edit()
            .remove(KEY_AGREED_AGREEMENT)
            .remove(KEY_AGREEMENT_VERSION)
            .remove(KEY_AGREED_TIMESTAMP)
            .remove(KEY_AGREED_CHECKSUM)
            .apply()

        _agreementAgreedFlow.value = false
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() && it != "default_dev_token_zhshop_2026" }
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() && it != "default_refresh_token_2026" }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun saveUser(user: UserProfile) {
        prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_AVATAR, user.avatarUrl)
            .putString(KEY_BIO, user.bio)
            .putInt(KEY_POINTS, user.points)
            .putString(KEY_LEVEL, user.level)
            .putBoolean(KEY_IS_DEV, user.isDeveloper)
            .putBoolean(KEY_IS_ADMIN, user.isAdmin)
            .apply()
        _currentUserFlow.value = user
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_EMAIL)
            .remove(KEY_AVATAR)
            .remove(KEY_BIO)
            .remove(KEY_POINTS)
            .remove(KEY_LEVEL)
            .remove(KEY_IS_DEV)
            .remove(KEY_IS_ADMIN)
            .apply()
        _currentUserFlow.value = null
    }

    private fun loadSavedUser(): UserProfile? {
        val token = getAccessToken()
        val id = prefs.getString(KEY_USER_ID, null)
        if (token.isNullOrBlank() || id.isNullOrBlank()) {
            return null
        }
        val username = prefs.getString(KEY_USERNAME, "用户") ?: "用户"
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val avatar = prefs.getString(KEY_AVATAR, "") ?: ""
        val bio = prefs.getString(KEY_BIO, "") ?: ""
        val points = prefs.getInt(KEY_POINTS, 0)
        val level = prefs.getString(KEY_LEVEL, "Lv.1 探索者") ?: "Lv.1 探索者"
        val isDev = prefs.getBoolean(KEY_IS_DEV, false)
        val isAdmin = prefs.getBoolean(KEY_IS_ADMIN, false)

        return UserProfile(
            id = id,
            username = username,
            email = email,
            avatarUrl = avatar,
            bio = bio,
            points = points,
            level = level,
            consecutiveDays = 1,
            isDeveloper = isDev,
            isAdmin = isAdmin
        )
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_AVATAR = "avatar"
        private const val KEY_BIO = "bio"
        private const val KEY_POINTS = "points"
        private const val KEY_LEVEL = "level"
        private const val KEY_IS_DEV = "is_dev"
        private const val KEY_IS_ADMIN = "is_admin"

        private const val KEY_AGREED_AGREEMENT = "agreed_agreement_consent"
        private const val KEY_AGREEMENT_VERSION = "agreed_agreement_version"
        private const val KEY_AGREED_TIMESTAMP = "agreed_agreement_timestamp"
        private const val KEY_AGREED_CHECKSUM = "agreed_agreement_checksum"
    }
}

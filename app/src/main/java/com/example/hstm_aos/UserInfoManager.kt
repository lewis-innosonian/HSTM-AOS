package com.example.hstm_aos

import android.content.Context

object UserInfoManager {

    private const val PREF = "oauth"

    fun getAccessToken(context: Context): String? {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("access_token", null)
    }

    fun getIdToken(context: Context): String? {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("id_token", null)
    }

    fun getRefreshToken(context: Context): String? {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("refresh_token", null)
    }

    fun getHStreamId(context: Context): String? {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("hstream_id", null)
    }

    fun setVoiceGuide(context: Context, name: Boolean) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isVoiceGuide", name)
            .apply()
    }

    fun getVoiceGuide(context: Context): Boolean {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean("isVoiceGuide", false)
    }

    fun getName(context: Context): String? {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("name", null)
    }

    fun getNickName(context: Context): String? {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("nickname", null)
    }

    fun setNickName(context: Context, name: String?) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString("nickname", name)
            .apply()
    }


    fun getOrganizations(context: Context): String? {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("organizations", null)
    }

    fun setOrganizations(context: Context, name: String?) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString("organizations", name)
            .apply()
    }

    fun setFirstName(context: Context, name: String?) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString("firstname", name)
            .apply()
    }

    fun setLastName(context: Context, name: String?) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString("lastname", name)
            .apply()
    }


    fun getFirstName(context: Context): String? {

        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("firstname", null)


    }

    fun getLastName(context: Context): String? {
        return context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString("lastname", null)

    }


    fun saveTokens(
        context: Context,
        accessToken: String?,
        idToken: String?,
        refreshToken: String?,
        hstreamId: String?,
//        name: String?
    ) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().apply {
                putString("access_token", accessToken)
                putString("id_token", idToken)
                putString("refresh_token", refreshToken)
                putString("hstream_id", hstreamId)
//                putString("name", name)
                apply()
            }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}

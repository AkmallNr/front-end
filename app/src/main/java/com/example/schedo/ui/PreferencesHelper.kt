package com.example.schedo.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("SchedoPrefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_USER_ID = "user_id"
        private const val KEY_SELECTED_PROJECT_IDS = "selected_project_ids"
    }

    fun saveUserId(userId: Int) {
        sharedPreferences.edit().putInt(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1) // -1 sebagai default jika belum login
    }

    fun clearUserId() {
        sharedPreferences.edit().remove(KEY_USER_ID).apply()
    }

    fun saveSelectedProjectIds(projectIds: String) {
        sharedPreferences.edit().putString(KEY_SELECTED_PROJECT_IDS, projectIds).apply()
    }

    fun getSelectedProjectIds(): List<String> {
        return sharedPreferences.getString(KEY_SELECTED_PROJECT_IDS, "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    fun clearSelectedProjectIds() {
        sharedPreferences.edit().remove(KEY_SELECTED_PROJECT_IDS).apply()
    }

    // Fungsi untuk menghapus semua data sesi saat logout
    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}
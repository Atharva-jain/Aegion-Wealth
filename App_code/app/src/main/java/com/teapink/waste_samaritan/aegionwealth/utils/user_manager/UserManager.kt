package com.teapink.waste_samaritan.aegionwealth.utils.user_manager

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.teapink.waste_samaritan.aegionwealth.data.models.UserProfile
import androidx.core.content.edit

class UserManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_USER_PROFILE = "key_user_profile"
    }

    /**
     * Serializes the UserProfile object into a JSON string and saves it.
     */
    fun saveUserProfile(user: UserProfile) {
        Log.d("FirebaseLogged", "Setting user: $user")
        val userJson = gson.toJson(user)
        prefs.edit { putString(KEY_USER_PROFILE, userJson) }
    }

    /**
     * Retrieves the JSON string and deserializes it back into a UserProfile object.
     * Returns a default empty UserProfile if no data is found.
     */
    fun getUserProfile(): UserProfile {
        val userJson = prefs.getString(KEY_USER_PROFILE, null)
        Log.d("FirebaseLogged", "Getting user: $userJson")
        return if (userJson != null) {

            gson.fromJson(userJson, UserProfile::class.java)

        } else {
            UserProfile() // Returns the default values defined in your data class
        }
    }

    /**
     * Clears user data (useful for logout).
     */
    fun clearUserData() {
        prefs.edit { remove(KEY_USER_PROFILE) }
    }
}
package com.example.worldlensai.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.worldlensai.model.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val NAME = stringPreferencesKey("user_name")
        val AGE = intPreferencesKey("user_age")
        val LOCATION = stringPreferencesKey("user_location")
        val EDUCATION_LEVEL = stringPreferencesKey("education_level")
        val LEARNING_GOAL = stringPreferencesKey("learning_goal")
    }

    val userDataFlow: Flow<UserData> = context.dataStore.data.map { preferences ->
        UserData(
            name = preferences[PreferencesKeys.NAME] ?: "",
            age = preferences[PreferencesKeys.AGE] ?: 0,
            location = preferences[PreferencesKeys.LOCATION] ?: "",
            educationLevel = preferences[PreferencesKeys.EDUCATION_LEVEL] ?: "",
            learningGoal = preferences[PreferencesKeys.LEARNING_GOAL] ?: ""
        )
    }

    suspend fun saveUserData(userData: UserData) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAME] = userData.name
            preferences[PreferencesKeys.AGE] = userData.age
            preferences[PreferencesKeys.LOCATION] = userData.location
            preferences[PreferencesKeys.EDUCATION_LEVEL] = userData.educationLevel
            preferences[PreferencesKeys.LEARNING_GOAL] = userData.learningGoal
        }
    }
}
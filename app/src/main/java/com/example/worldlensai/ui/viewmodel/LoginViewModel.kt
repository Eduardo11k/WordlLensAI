package com.example.worldlensai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldlensai.data.repository.UserPreferencesRepository
import com.example.worldlensai.model.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData.asStateFlow()

    init {
        viewModelScope.launch {
            repository.userDataFlow.collect {
                _userData.value = it
            }
        }
    }

    fun updateName(name: String) {
        _userData.value = _userData.value.copy(name = name)
    }

    fun updateAge(age: String) {
        val ageInt = age.toIntOrNull() ?: 0
        _userData.value = _userData.value.copy(age = ageInt)
    }

    fun updateLocation(location: String) {
        _userData.value = _userData.value.copy(location = location)
    }

    fun isNameValid(): Boolean = _userData.value.name.trim().length >= 2
    fun isAgeValid(): Boolean = _userData.value.age in 1..120
    fun isLocationValid(): Boolean = _userData.value.location.trim().isNotBlank()

    fun isFormValid(): Boolean = isNameValid() && isAgeValid() && isLocationValid()

    fun saveUser(): Boolean {
        if (isFormValid()) {
            viewModelScope.launch {
                repository.saveUserData(_userData.value)
            }
            return true
        }
        return false
    }
}
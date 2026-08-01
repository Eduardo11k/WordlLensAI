package com.example.worldlensai.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldlensai.data.remote.GeminiRepository
import com.example.worldlensai.data.repository.UserPreferencesRepository
import com.example.worldlensai.model.ChatMessage
import com.example.worldlensai.model.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    object Loading : AnalysisUiState()
    data class Success(val result: String) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

class CameraViewModel(
    private val geminiRepository: GeminiRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private var identifiedTopic: String = "objeto analisado"

    fun analyzeCapturedImage(bitmap: Bitmap) {
        _capturedBitmap.value = bitmap
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading
            _chatMessages.value = emptyList()
            
            val userData = userPreferencesRepository.userDataFlow.first()
            val prompt = buildPrompt(userData)
            
            try {
                val result = geminiRepository.analyzeImage(bitmap, prompt)
                if (result == "ERRO_404") {
                     _uiState.value = AnalysisUiState.Error("O modelo de IA não foi encontrado. Verifica se a tua chave API no AI Studio está ativa e se tens acesso ao modelo 'gemini-2.0-flash'.")
                } else {
                    _uiState.value = AnalysisUiState.Success(result)
                    identifiedTopic = extractTopic(result)
                }
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Error(getFriendlyErrorMessage(e.message))
            }
        }
    }

    private fun getFriendlyErrorMessage(originalMsg: String?): String {
        return when {
            originalMsg?.contains("403") == true -> "Acesso negado. A tua chave API pode estar incorreta ou sem permissões."
            originalMsg?.contains("429") == true -> "Limite de uso atingido. Aguarda um momento antes de tentar novamente."
            originalMsg?.contains("Unable to resolve host") == true -> "Não foi possível conectar à internet. Verifica a tua ligação."
            else -> "Ocorreu um problema ao conectar com a Inteligência Artificial. Verifica a tua chave API no AI Studio."
        }
    }

    fun sendFollowUpQuestion(question: String) {
        if (question.isBlank() || _isSendingChat.value) return
        
        val userMsg = ChatMessage(text = question, isUser = true)
        val currentList = _chatMessages.value + userMsg
        _chatMessages.value = currentList
        _isSendingChat.value = true

        viewModelScope.launch {
            val response = geminiRepository.sendFollowUpQuestion(
                chatHistory = currentList,
                userQuestion = question,
                currentTopic = identifiedTopic
            )
            
            _chatMessages.value = _chatMessages.value + ChatMessage(text = response, isUser = false)
            _isSendingChat.value = false
        }
    }

    fun resetState() {
        _uiState.value = AnalysisUiState.Idle
        _chatMessages.value = emptyList()
        _capturedBitmap.value = null
        identifiedTopic = "objeto analisado"
        _isSendingChat.value = false
    }

    private fun extractTopic(resultText: String): String {
        val lines = resultText.lines()
        for (line in lines) {
            if (line.contains("Objeto Identificado", ignoreCase = true) || line.contains("Identificado:", ignoreCase = true)) {
                val parts = line.split(":")
                if (parts.size > 1 && parts[1].isNotBlank()) return parts[1].trim()
            }
        }
        return "objeto analisado"
    }

    private fun buildPrompt(userData: UserData): String {
        return """
            Analise esta imagem para fins educacionais.
            Perfil: Usuário de ${userData.age} anos, vive em ${userData.location}.
            
            1. Identifique o que está na imagem.
            2. Forneça uma lição personalizada e motivadora.
            3. Crie 3 perguntas de quiz.
            4. Dê um resumo curto e sugira o próximo tópico.
            
            Responda em Português de forma clara.
        """.trimIndent()
    }
}
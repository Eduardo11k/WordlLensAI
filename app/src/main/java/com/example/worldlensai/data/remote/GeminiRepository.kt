package com.example.worldlensai.data.remote

import android.graphics.Bitmap
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class GeminiRepository(
    private val apiKey: String,
    private val modelName: String = "gemini-2.0-flash"
) {

    private val client = Client.builder().apiKey(apiKey).build()

    suspend fun analyzeImage(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey == "YOUR_GEMINI_API_KEY" || apiKey.isBlank()) {
            return@withContext "Por favor, configura a tua chave API no MainActivity."
        }

        try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val imageBytes = outputStream.toByteArray()

            // SDK Unified GenAI
            val textPart = Part.fromText(prompt)
            val imagePart = Part.fromBytes(imageBytes, "image/jpeg")
            val content = Content.fromParts(textPart, imagePart)

            val response = client.models.generateContent(modelName, content, null)
            val resultText = response.text() ?: ""
            
            if (resultText.isNotBlank()) return@withContext formatCleanText(resultText)
            
            "Não foi possível obter uma análise detalhada. Tenta capturar a imagem novamente."
        } catch (e: Exception) {
            android.util.Log.e("GeminiRepository", "ERRO GENAI: ${e.message}")
            if (e.message?.contains("404") == true) return@withContext "ERRO_404"
            throw e
        }
    }

    suspend fun sendFollowUpQuestion(
        chatHistory: List<com.example.worldlensai.model.ChatMessage>,
        userQuestion: String,
        currentTopic: String
    ): String = withContext(Dispatchers.IO) {
        val recentHistory = chatHistory.takeLast(3).joinToString("\n") { if (it.isUser) "U: ${it.text}" else "IA: ${it.text}" }
        val systemPrompt = "Responda em Português sobre: '$currentTopic'. Histórico:\n$recentHistory\nPergunta: $userQuestion"

        try {
            val content = Content.fromParts(Part.fromText(systemPrompt))
            val response = client.models.generateContent(modelName, content, null)
            formatCleanText(response.text() ?: "")
        } catch (e: Exception) {
            "Não foi possível processar agora."
        }
    }

    private fun formatCleanText(text: String): String {
        return text.replace("**", "").replace("###", "").replace("##", "").replace("#", "").trim()
    }
}
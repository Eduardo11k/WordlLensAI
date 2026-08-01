package com.example.worldlensai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.worldlensai.ui.components.WorldLensBackground
import com.example.worldlensai.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val userData by viewModel.userData.collectAsState()
    var showErrors by remember { mutableStateOf(false) }

    WorldLensBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "WorldLens AI",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Personalize sua experiência de aprendizado",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            val nameError = showErrors && !viewModel.isNameValid()
            OutlinedTextField(
                value = userData.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Nome Completo") },
                isError = nameError,
                supportingText = {
                    if (nameError) {
                        Text("Insira pelo menos 2 caracteres.", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            val ageError = showErrors && !viewModel.isAgeValid()
            OutlinedTextField(
                value = if (userData.age > 0) userData.age.toString() else "",
                onValueChange = { viewModel.updateAge(it) },
                label = { Text("Idade") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = ageError,
                supportingText = {
                    if (ageError) {
                        Text("Insira uma idade válida (entre 1 e 120).", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            val locationError = showErrors && !viewModel.isLocationValid()
            OutlinedTextField(
                value = userData.location,
                onValueChange = { viewModel.updateLocation(it) },
                label = { Text("Onde você vive? (Cidade/País)") },
                isError = locationError,
                supportingText = {
                    if (locationError) {
                        Text("Por favor, informe sua cidade/país.", color = MaterialTheme.colorScheme.error)
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    showErrors = true
                    if (viewModel.saveUser()) {
                        onLoginSuccess()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Começar a Aprender")
            }
        }
    }
}
package com.example.worldlensai.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.worldlensai.ui.viewmodel.AnalysisUiState
import com.example.worldlensai.ui.viewmodel.CameraViewModel
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onAnalysisComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        val preview = remember { Preview.Builder().build() }
        val imageCapture = remember { ImageCapture.Builder().build() }
        val cameraSelector = remember { CameraSelector.DEFAULT_BACK_CAMERA }
        val previewView = remember { PreviewView(context) }

        LaunchedEffect(capturedBitmap) {
            if (capturedBitmap == null) {
                val cameraProvider = context.getCameraProvider()
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    preview.surfaceProvider = previewView.surfaceProvider
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Binding failed", e)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (capturedBitmap != null) {
                // Congelar imagem após captura para feedback visual
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (capturedBitmap == null) {
                IconButton(
                    onClick = {
                        takePicture(context, imageCapture) { bitmap ->
                            viewModel.analyzeCapturedImage(bitmap)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                        .size(80.dp)
                ) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = androidx.compose.foundation.BorderStroke(4.dp, MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }

            if (uiState is AnalysisUiState.Loading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Analisando com IA...", modifier = Modifier.padding(8.dp))
                    }
                }
            }

            if (uiState is AnalysisUiState.Error) {
                AlertDialog(
                    onDismissRequest = { viewModel.resetState() },
                    title = { Text("Ups! Algo correu mal") },
                    text = { Text((uiState as AnalysisUiState.Error).message) },
                    confirmButton = {
                        Button(onClick = { viewModel.resetState() }) {
                            Text("Tentar Novamente")
                        }
                    }
                )
            }

            if (uiState is AnalysisUiState.Success) {
                LaunchedEffect(uiState) {
                    onAnalysisComplete()
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Precisamos de acesso à câmara para funcionar.")
        }
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({ continuation.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }
}

private fun takePicture(context: Context, imageCapture: ImageCapture, onImageCaptured: (Bitmap) -> Unit) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                image.close()
                onImageCaptured(bitmap)
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Erro na captura", exception)
            }
        }
    )
}

private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
package com.example.watermark

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1565C0))) {
                    WatermarkAppScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkAppScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var watermarkText by remember { mutableStateOf("نسخة أصلية - يمنع النسخ") }
    var opacity by remember { mutableFloatStateOf(70f) }
    var textSize by remember { mutableFloatStateOf(65f) }
    var angle by remember { mutableFloatStateOf(-45f) }
    var selectedColor by remember { mutableStateOf(Color.Red) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            generatedFile = null
            scope.launch {
                previewBitmap = PdfWatermarkEngine.getFirstPagePreview(context, uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حامي المستندات PDF", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { (context as? Activity)?.finishAffinity() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "خروج")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedPdfUri != null) "تم اختيار الملف بنجاح ✓" else "لم يتم اختيار أي ملف بعد",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختر ملف PDF")
                    }
                }
            }

            if (previewBitmap != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "معاينة حية للصفحة الأولى",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LiveWatermarkPreviewBox(
                            bitmap = previewBitmap!!,
                            watermarkText = watermarkText,
                            opacity = opacity.toInt(),
                            textSize = textSize,
                            angle = angle,
                            textColor = selectedColor.toArgb()
                        )
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "إعدادات العلامة المائية",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        label = { Text("نص العلامة المائية") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("الشفافية: ${(opacity / 255 * 100).toInt()}%")
                    Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 20f..200f)

                    Text("حجم الخط: ${textSize.toInt()}")
                    Slider(value = textSize, onValueChange = { textSize = it }, valueRange = 30f..130f)

                    Text("زاوية الميلان: ${angle.toInt()}°")
                    Slider(value = angle, onValueChange = { angle = it }, valueRange = -90f..90f)

                    Text("لون العلامة:")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val colors = listOf(Color.Red, Color.DarkGray, Color(0xFF1565C0), Color(0xFF2E7D32))
                        colors.forEach { color ->
                            Surface(
                                onClick = { selectedColor = color },
                                shape = RoundedCornerShape(50),
                                color = color,
                                modifier = Modifier.size(36.dp),
                                border = if (selectedColor == color) ButtonDefaults.outlinedButtonBorder else null
                            ) {}
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val uri = selectedPdfUri ?: return@Button
                    if (watermarkText.isBlank()) {
                        Toast.makeText(context, "يرجى إدخال نص العلامة المائية", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isProcessing = true
                    scope.launch {
                        val result = PdfWatermarkEngine.applyUnremovableWatermark(
                            context = context,
                            sourceUri = uri,
                            watermarkText = watermarkText,
                            opacity = opacity.toInt(),
                            textSize = textSize,
                            angle = angle,
                            textColor = selectedColor.toArgb()
                        )
                        isProcessing = false
                        if (result != null) {
                            generatedFile = result
                            Toast.makeText(context, "تم تأمين الملف بنجاح!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "حدث خطأ أثناء المعالجة", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = selectedPdfUri != null && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري دمج العلامة وحماية الصفحات...")
                } else {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تطبيق العلامة المائية المحمية")
                }
            }

            if (generatedFile != null) {
                OutlinedButton(
                    onClick = { sharePdfFile(context, generatedFile!!) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة أو حفظ الملف النهائي")
                }
            }
        }
    }
}

@Composable
fun LiveWatermarkPreviewBox(
    bitmap: Bitmap,
    watermarkText: String,
    opacity: Int,
    textSize: Float,
    angle: Float,
    textColor: Int
) {
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .clipToBounds()
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "معاينة الصفحة",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleFactor = size.width / (bitmap.width / 1.5f * 2f)
            val paint = Paint().apply {
                color = textColor
                alpha = opacity
                this.textSize = textSize * scaleFactor
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val stepX = size.width / 2.5f
            val stepY = size.height / 4f

            var x = -size.width
            while (x < size.width * 2) {
                var y = -size.height
                while (y < size.height * 2) {
                    drawContext.canvas.nativeCanvas.apply {
                        save()
                        rotate(angle, x, y)
                        drawText(watermarkText, x, y, paint)
                        restore()
                    }
                    y += stepY
                }
                x += stepX
            }
        }
    }
}

fun sharePdfFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "مشاركة ملف PDF المحمي"))
}

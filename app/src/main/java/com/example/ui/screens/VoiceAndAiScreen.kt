package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.ErrorSeverity
import com.example.data.model.LearnerProfile
import com.example.data.model.SpeechEvaluationResult
import com.example.data.model.TutorChatMessage
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.LocalBayanColors
import com.example.ui.viewmodel.AiActionState

@Composable
fun VoiceAndAiScreen(
    profile: LearnerProfile,
    isListening: Boolean,
    isRecordingAudio: Boolean,
    recognizedText: String,
    speechError: String?,
    speechEvalState: AiActionState<SpeechEvaluationResult>,
    chatMessages: List<TutorChatMessage>,
    chatError: String?,
    strings: AppStrings,
    onSpeakText: (String) -> Unit,
    onStartSpeechRecognition: () -> Unit,
    onStopSpeechRecognition: () -> Unit,
    onStartAudioRecording: () -> Unit,
    onStopRecordingAndAnalyze: (String, String) -> Unit,
    onAnalyzeText: (String, String) -> Unit,
    onSendTutorMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val semantic = LocalBayanColors.current
    var activeSubTab by remember { mutableIntStateOf(0) }

    val prompts = remember {
        listOf(
            "Describe a calm place where you enjoy studying and why it helps you focus.",
            "In your opinion, how can cities balance technology with green spaces?",
            "Talk about a challenge you overcame while learning English."
        )
    }
    var selectedPrompt by remember { mutableStateOf(prompts.first()) }
    var learnerInput by remember { mutableStateOf("") }
    var permissionDeniedNote by remember { mutableStateOf(false) }
    var pendingAudioAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotBlank()) {
            learnerInput = recognizedText
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = false
            when (pendingAudioAction) {
                "SPEECH" -> onStartSpeechRecognition()
                "RECORD" -> onStartAudioRecording()
            }
        } else {
            permissionDeniedNote = true
        }
        pendingAudioAction = null
    }

    fun requestMicAndRun(action: String) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            permissionDeniedNote = false
            if (action == "SPEECH") onStartSpeechRecognition() else onStartAudioRecording()
        } else {
            pendingAudioAction = action
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeSubTab) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text(strings.voiceClinicTitle) },
                icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text(strings.aiTutorTab) },
                icon = { Icon(Icons.Rounded.SupportAgent, contentDescription = null) }
            )
        }

        if (activeSubTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxWidth()
                            .border(1.dp, semantic.cardBorder, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (strings.isArabic) "اختر موضوع المحادثة الصوتية (${profile.cefrLevel}):" else "Choose Speaking Topic (${profile.cefrLevel}):",
                                style = MaterialTheme.typography.titleMedium
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(prompts) { topic ->
                                    FilterChip(
                                        selected = selectedPrompt == topic,
                                        onClick = { selectedPrompt = topic },
                                        label = { Text(topic.take(28) + "…") }
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSpeakText(selectedPrompt) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = selectedPrompt,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onSpeakText(selectedPrompt) }) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = "Hear topic",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Legend for Red vs Orange highlighting
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = semantic.criticalRedBg,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = strings.redErrorLegend,
                                        color = semantic.criticalRed,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Surface(
                                    color = semantic.minorOrangeBg,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = strings.orangeErrorLegend,
                                        color = semantic.minorOrange,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            if (permissionDeniedNote) {
                                Surface(
                                    color = semantic.minorOrangeBg,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "تم رفض إذن الميكروفون. يمكنك تفعيله من إعدادات التطبيق أو كتابة الجملة بالأسفل للتحليل الفوري.",
                                        color = semantic.minorOrange,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            if (!speechError.isNullOrBlank()) {
                                Surface(
                                    color = semantic.minorOrangeBg,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = speechError,
                                        color = semantic.minorOrange,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            // Speech Recognition & Direct Audio Recording Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (isListening) onStopSpeechRecognition()
                                        else requestMicAndRun("SPEECH")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("speech_dictation_button")
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(if (isListening) strings.stopListening else strings.speakNow)
                                }

                                Button(
                                    onClick = {
                                        if (isRecordingAudio) {
                                            onStopRecordingAndAnalyze(selectedPrompt, learnerInput)
                                        } else {
                                            requestMicAndRun("RECORD")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRecordingAudio) semantic.criticalRed else MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("record_audio_button")
                                ) {
                                    Icon(
                                        imageVector = if (isRecordingAudio) Icons.Rounded.Stop else Icons.Rounded.GraphicEq,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        if (isRecordingAudio) strings.stopRecordingAndAnalyze
                                        else strings.recordAudioClip
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = learnerInput,
                                onValueChange = { learnerInput = it },
                                label = {
                                    Text(if (strings.isArabic) "تحدث أو اكتب إجابتك بالإنكليزية هنا" else "Speak or type your English response")
                                },
                                placeholder = { Text("Example: I am agree that quiet places helps me focus...") },
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_transcript_input")
                            )

                            Button(
                                onClick = { onAnalyzeText(selectedPrompt, learnerInput) },
                                enabled = learnerInput.isNotBlank() && speechEvalState !is AiActionState.Loading,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("analyze_speech_button")
                            ) {
                                if (speechEvalState is AiActionState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(strings.analyzeWrittenOrSpoken)
                                }
                            }
                        }
                    }
                }

                // Evaluation Result Card with Red & Orange Color Highlighting
                item {
                    when (speechEvalState) {
                        is AiActionState.Error -> {
                            Surface(
                                color = semantic.criticalRedBg,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .widthIn(max = 760.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = speechEvalState.message,
                                    color = semantic.criticalRed,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        is AiActionState.Success -> {
                            val res = speechEvalState.data
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .widthIn(max = 760.dp)
                                    .fillMaxWidth()
                                    .border(1.dp, semantic.cardBorder, RoundedCornerShape(22.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (strings.isArabic) "نتيجة التقييم الصوتي واللغوي" else "Evaluation Result",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "${res.overallScore}%",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (strings.isArabic) "النطق: ${res.pronunciationScore}%" else "Pronunciation: ${res.pronunciationScore}%",
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (strings.isArabic) "القواعد: ${res.grammarScore}%" else "Grammar: ${res.grammarScore}%",
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }

                                    // Color-coded Annotated Sentence
                                    val annotated = buildAnnotatedString {
                                        res.segments.forEachIndexed { idx, seg ->
                                            when (seg.severity) {
                                                ErrorSeverity.RED_CRITICAL -> {
                                                    withStyle(
                                                        SpanStyle(
                                                            color = semantic.criticalRed,
                                                            background = semantic.criticalRedBg,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    ) {
                                                        append(seg.text)
                                                    }
                                                }

                                                ErrorSeverity.ORANGE_MINOR -> {
                                                    withStyle(
                                                        SpanStyle(
                                                            color = semantic.minorOrange,
                                                            background = semantic.minorOrangeBg,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    ) {
                                                        append(seg.text)
                                                    }
                                                }

                                                ErrorSeverity.NONE -> {
                                                    append(seg.text)
                                                }
                                            }
                                            if (idx < res.segments.lastIndex && !seg.text.endsWith(" ")) {
                                                append(" ")
                                            }
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = annotated,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }

                                    // Corrected Version + Hear Correct Pronunciation
                                    Surface(
                                        color = semantic.masteryGreenBg,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSpeakText(res.correctedFullText) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (strings.isArabic) "الصياغة السليمة:" else "Corrected Version:",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = semantic.masteryGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = res.correctedFullText,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                            IconButton(onClick = { onSpeakText(res.correctedFullText) }) {
                                                Icon(
                                                    Icons.AutoMirrored.Rounded.VolumeUp,
                                                    contentDescription = "Hear corrected version",
                                                    tint = semantic.masteryGreen
                                                )
                                            }
                                        }
                                    }

                                    // Detailed Red/Orange Error Cards
                                    res.segments.filter { it.severity != ErrorSeverity.NONE }.forEach { seg ->
                                        val isRed = seg.severity == ErrorSeverity.RED_CRITICAL
                                        Surface(
                                            color = if (isRed) semantic.criticalRedBg else semantic.minorOrangeBg,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "${if (isRed) "● خطأ جوهري (أحمر)" else "● ملاحظة طفيفة (برتقالي)"}: \"${seg.text}\" → \"${seg.correction}\"",
                                                    color = if (isRed) semantic.criticalRed else semantic.minorOrange,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (strings.isArabic) seg.explanationAr else seg.explanationEn,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = if (strings.isArabic) "${res.praiseAr}\nنصيحة: ${res.nextTipAr}" else "${res.praiseEn}\nTip: ${res.nextTipEn}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        else -> Unit
                    }
                }
            }
        } else {
            // 24/7 AI Tutor & Technical Support Chat
            AiTutorChatView(
                chatMessages = chatMessages,
                chatError = chatError,
                strings = strings,
                onSend = onSendTutorMessage
            )
        }
    }
}

@Composable
private fun AiTutorChatView(
    chatMessages: List<TutorChatMessage>,
    chatError: String?,
    strings: AppStrings,
    onSend: (String) -> Unit
) {
    val semantic = LocalBayanColors.current
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, semantic.cardBorder, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ai_tutor),
                        contentDescription = "AI Tutor alcove",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Text(
                        text = strings.aiTutorDisclaimer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quickPrompts = listOf(
                    "اشرح لي الفرق بين Although و Despite في B2",
                    "كيف أحسن نطق الأصوات الصعبة مثل th و r؟",
                    "مساعدة تقنية: كيف أشارك تقريري مع المعلم؟"
                )
                items(quickPrompts) { qp ->
                    FilterChip(
                        selected = false,
                        onClick = { onSend(qp) },
                        label = { Text(qp) }
                    )
                }
            }

            if (!chatError.isNullOrBlank()) {
                Surface(color = semantic.criticalRedBg, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = chatError,
                        color = semantic.criticalRed,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            color = if (msg.isUser) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .widthIn(max = 520.dp)
                                .border(
                                    1.dp,
                                    if (msg.isUser) MaterialTheme.colorScheme.primary else semantic.cardBorder,
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text(
                                text = msg.text.ifBlank { if (msg.isStreaming) "…" else "" },
                                color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = {
                        Text(if (strings.isArabic) "اسأل المعلم الذكي أو اطلب دعماً تقنياً..." else "Ask AI tutor or request technical help...")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_tutor_input")
                )
                Button(
                    onClick = {
                        val toSend = draft
                        draft = ""
                        onSend(toSend)
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("send_ai_tutor_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
                }
            }
        }
    }
}

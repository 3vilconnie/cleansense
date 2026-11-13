package com.cleansense.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleansense.app.sensor.AirReading
import com.cleansense.app.sensor.AirStatus
import com.cleansense.app.ui.components.BreathingText
import com.cleansense.app.ui.components.CleanSenseLayout
import com.cleansense.app.ui.components.StatusChip
import com.cleansense.app.ui.theme.CsNormalBg
import com.cleansense.app.ui.theme.CsNormalBorder
import com.cleansense.app.ui.theme.CsNormalLabel
import com.cleansense.app.ui.theme.CsNormalText
import com.cleansense.app.ui.theme.CsNormalTitle
import com.cleansense.app.ui.theme.CsRecoverBg
import com.cleansense.app.ui.theme.CsRecoverBorder
import com.cleansense.app.ui.theme.CsRecoverLabel
import com.cleansense.app.ui.theme.CsRecoverText
import com.cleansense.app.ui.theme.CsRecoverTitle
import com.cleansense.app.ui.theme.CsTextMuted
import com.cleansense.app.ui.theme.CsTextSecondary
import com.cleansense.app.ui.theme.CsVocBg
import com.cleansense.app.ui.theme.CsVocBorder
import com.cleansense.app.ui.theme.CsVocLabel
import com.cleansense.app.ui.theme.CsVocText
import com.cleansense.app.ui.theme.CsVocTitle

/** Pantallas 03 · Sensor activo (aire normal / Mandar a lavar / Volviendo a niveles base). */
@Composable
fun SensorScreen(
    deviceName: String,
    reading: AirReading,
    onExit: () -> Unit,
) {
    CleanSenseLayout(
        onExit = onExit,
        header = {
            Spacer(Modifier.height(12.dp))
            StatusChip("Dispositivo conectado")
            Spacer(Modifier.height(20.dp))
            BreathingText(
                text = "Respirando…",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AirQualityCard(reading)
            Spacer(Modifier.height(20.dp))
            Text(
                text = descriptionFor(reading.status),
                color = CsTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    append(deviceName)
                    reading.raw?.let { append(" · lectura $it") }
                    reading.baseline?.let { append(" · base $it") }
                },
                color = CsTextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class CardPalette(
    val background: Color,
    val border: Color,
    val label: Color,
    val title: Color,
    val text: Color,
)

private val NormalPalette = CardPalette(CsNormalBg, CsNormalBorder, CsNormalLabel, CsNormalTitle, CsNormalText)
private val VocPalette = CardPalette(CsVocBg, CsVocBorder, CsVocLabel, CsVocTitle, CsVocText)
private val RecoverPalette = CardPalette(CsRecoverBg, CsRecoverBorder, CsRecoverLabel, CsRecoverTitle, CsRecoverText)

private fun paletteFor(status: AirStatus) = when (status) {
    AirStatus.CALIBRATING, AirStatus.NORMAL -> NormalPalette
    AirStatus.VOC -> VocPalette
    AirStatus.RECOVERING -> RecoverPalette
}

private fun titleFor(status: AirStatus) = when (status) {
    AirStatus.CALIBRATING -> "Calibrando"
    AirStatus.NORMAL -> "aire normal"
    AirStatus.VOC -> "Mandar a lavar"
    AirStatus.RECOVERING -> "Volviendo a niveles base"
}

private fun subtitleFor(status: AirStatus) = when (status) {
    AirStatus.CALIBRATING -> "Midiendo el aire de referencia."
    AirStatus.NORMAL -> "Todo está bien."
    AirStatus.VOC -> "Uno menos por revisar"
    AirStatus.RECOVERING -> "Espere un momento."
}

private fun descriptionFor(status: AirStatus) = when (status) {
    AirStatus.CALIBRATING -> "Mantén el sensor en aire limpio unos segundos."
    AirStatus.NORMAL -> "No se detectan olores cerca del sensor."
    AirStatus.VOC -> "Se detectan olores: compuestos orgánicos volátiles (COV)."
    AirStatus.RECOVERING -> "Los olores se están disipando, el aire vuelve a la normalidad."
}

@Composable
private fun AirQualityCard(reading: AirReading) {
    val palette = paletteFor(reading.status)
    val spec = tween<Color>(durationMillis = 700)
    val background by animateColorAsState(palette.background, spec, label = "cardBg")
    val border by animateColorAsState(palette.border, spec, label = "cardBorder")
    val label by animateColorAsState(palette.label, spec, label = "cardLabel")
    val title by animateColorAsState(palette.title, spec, label = "cardTitle")
    val text by animateColorAsState(palette.text, spec, label = "cardText")
    val shape = RoundedCornerShape(20.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .padding(horizontal = 20.dp, vertical = 30.dp)
    ) {
        Text(
            text = "CALIDAD DEL AIRE",
            color = label,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = reading.status,
            transitionSpec = {
                (fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.92f)) togetherWith
                    fadeOut(tween(250))
            },
            label = "cardTitleText"
        ) { status ->
            Text(
                text = titleFor(status),
                color = title,
                fontSize = 34.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = reading.status,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(250)) },
            label = "cardSubtitleText"
        ) { status ->
            Text(
                text = subtitleFor(status),
                color = text,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (reading.status == AirStatus.CALIBRATING) {
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { reading.calibrationProgress },
                color = title,
                trackColor = border,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

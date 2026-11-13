package com.cleansense.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleansense.app.ui.theme.CsBackground
import com.cleansense.app.ui.theme.CsChipBg
import com.cleansense.app.ui.theme.CsField
import com.cleansense.app.ui.theme.CsFieldBorder
import com.cleansense.app.ui.theme.CsMint
import com.cleansense.app.ui.theme.CsOnMint
import com.cleansense.app.ui.theme.CsOutline
import com.cleansense.app.ui.theme.CsTextMuted
import com.cleansense.app.ui.theme.CsTextPrimary
import com.cleansense.app.ui.theme.CsTextSecondary

const val APP_VERSION = "v0.1"
private val ButtonShape = RoundedCornerShape(12.dp)

/**
 * Estructura común de todas las pantallas del diseño:
 * título "CLeanSense" arriba, contenido al centro, botón "Salir" y versión abajo.
 */
@Composable
fun CleanSenseLayout(
    onExit: () -> Unit,
    exitLabel: String = "Salir",
    header: @Composable ColumnScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CsBackground)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = "CLeanSense",
            color = CsTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 20.dp)
        )
        header()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
            content = content
        )
        SecondaryButton(text = exitLabel, onClick = onExit)
        Text(
            text = APP_VERSION,
            color = CsTextMuted,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 12.dp)
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CsMint,
            contentColor = CsOnMint,
            disabledContainerColor = CsMint.copy(alpha = 0.4f),
            disabledContentColor = CsOnMint.copy(alpha = 0.6f),
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = ButtonShape,
        border = BorderStroke(1.dp, CsOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CsTextPrimary),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text, fontSize = 15.sp)
    }
}

@Composable
fun CsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Column(modifier.fillMaxWidth()) {
        Text(label, color = CsTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, color = CsTextMuted) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
                imeAction = imeAction,
            ),
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CsField,
                unfocusedContainerColor = CsField,
                focusedBorderColor = CsMint,
                unfocusedBorderColor = CsFieldBorder,
                focusedTextColor = CsTextPrimary,
                unfocusedTextColor = CsTextPrimary,
                cursorColor = CsMint,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Etiqueta pequeña "Dispositivo conectado". */
@Composable
fun StatusChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = CsMint,
        fontSize = 12.sp,
        modifier = modifier
            .background(CsChipBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

/** Texto que "respira" (sube y baja su opacidad) para indicar lectura en vivo. */
@Composable
fun BreathingText(text: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "breathing")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )
    Text(
        text = text,
        color = CsTextSecondary,
        fontSize = 14.sp,
        modifier = modifier.alpha(alpha)
    )
}

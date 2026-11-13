package com.cleansense.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleansense.app.ui.components.CleanSenseLayout
import com.cleansense.app.ui.components.CsTextField
import com.cleansense.app.ui.components.PrimaryButton
import com.cleansense.app.ui.components.SecondaryButton
import com.cleansense.app.ui.theme.CsError
import com.cleansense.app.ui.theme.CsTextSecondary

/**
 * Pantalla 01 · Inicio de sesión.
 * El botón "Registrarse" cambia el formulario a modo "crear cuenta local".
 * onLogin / onRegister devuelven un mensaje de error, o null si todo salió bien.
 */
@Composable
fun LoginScreen(
    onLogin: (username: String, password: String) -> String?,
    onRegister: (username: String, password: String, confirm: String) -> String?,
    onExit: () -> Unit,
) {
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val submit: () -> Unit = {
        focusManager.clearFocus()
        error = if (registerMode) onRegister(username, password, confirm) else onLogin(username, password)
    }
    val nextField = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val done = KeyboardActions(onDone = { submit() })

    CleanSenseLayout(onExit = onExit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (registerMode) "Crea una cuenta local." else "Inicia sesión.",
                color = CsTextSecondary,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(24.dp))

            CsTextField(
                label = "Nombre de usuario",
                value = username,
                onValueChange = { username = it; error = null },
                placeholder = "Conejo blanco",
                keyboardActions = nextField,
            )
            Spacer(Modifier.height(16.dp))
            CsTextField(
                label = "Contraseña",
                value = password,
                onValueChange = { password = it; error = null },
                placeholder = "••••••••",
                isPassword = true,
                imeAction = if (registerMode) ImeAction.Next else ImeAction.Done,
                keyboardActions = if (registerMode) nextField else done,
            )
            AnimatedVisibility(visible = registerMode) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    CsTextField(
                        label = "Confirmar contraseña",
                        value = confirm,
                        onValueChange = { confirm = it; error = null },
                        placeholder = "••••••••",
                        isPassword = true,
                        imeAction = ImeAction.Done,
                        keyboardActions = done,
                    )
                }
            }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = CsError, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = if (registerMode) "Crear cuenta" else "Iniciar sesión",
                onClick = submit
            )
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                text = if (registerMode) "Ya tengo cuenta" else "Registrarse",
                onClick = {
                    registerMode = !registerMode
                    error = null
                    confirm = ""
                }
            )
        }
    }
}

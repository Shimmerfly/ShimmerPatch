package moe.shimmerfly.shimmerpatch.ui.component.m3

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

@Composable
fun CustomValueDialog(
    show: Boolean,
    title: String,
    label: String,
    value: String,
    errorText: String,
    description: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onDismissRequest: () -> Unit,
    validate: suspend (String) -> Boolean,
    onConfirm: (String) -> Unit,
) {
    if (!show) return

    var draft by rememberSaveable { mutableStateOf(value) }
    var hasError by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SettingsDialog(
        show = true,
        title = title,
        onDismissRequest = { if (!saving) onDismissRequest() },
        dismissOnBackPress = !saving,
        dismissOnClickOutside = !saving,
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismissRequest) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    if (!saving) {
                        saving = true
                        scope.launch {
                            try {
                                val candidate = draft.trim()
                                hasError = !validate(candidate)
                                if (!hasError) {
                                    onConfirm(candidate)
                                    onDismissRequest()
                                }
                            } finally {
                                saving = false
                            }
                        }
                    }
                },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        description?.let { Text(it) }
        OutlinedTextField(
            value = draft,
            onValueChange = {
                draft = it
                hasError = false
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            label = { Text(label) },
            isError = hasError,
            supportingText = if (hasError) ({ Text(errorText) }) else null,
            keyboardOptions = keyboardOptions,
            singleLine = true,
        )
    }
}

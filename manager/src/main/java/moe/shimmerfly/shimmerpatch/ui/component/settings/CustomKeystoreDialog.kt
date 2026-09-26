package moe.shimmerfly.shimmerpatch.ui.component.settings

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.config.MyKeyStore
import moe.shimmerfly.shimmerpatch.ui.component.m3.SettingsDialog
import moe.shimmerfly.shimmerpatch.ui.component.m3.SettingsErrorText

private const val TAG = "CustomKeystoreDialog"

/**
 * Picks a BKS keystore, stages it in [stageFile] and checks that its password, alias and alias
 * password really open it before handing everything to [onConfirm].
 *
 * The caller owns the result: Settings stores it as the default, while the patch sheet keeps it
 * for the one patch being configured.
 */
@Composable
fun CustomKeystoreDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    stageFile: File = MyKeyStore.tmpFile,
    onConfirm: suspend (file: File, name: String, password: String, alias: String, aliasPassword: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var path by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var alias by rememberSaveable { mutableStateOf("") }
    var aliasPassword by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    var previouslyShown by rememberSaveable { mutableStateOf(show) }
    LaunchedEffect(show) {
        if (show && !previouslyShown) {
            path = ""
            password = ""
            alias = ""
            aliasPassword = ""
            error = null
        }
        previouslyShown = show
    }
    // Keep the result launcher registered while its external picker is on screen.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("No keystore input")
                    input.use { source -> stageFile.outputStream().use { source.copyTo(it) } }
                }
                path = uri.lastPathSegment.orEmpty()
                error = null
            } catch (failure: Exception) {
                Log.e(TAG, "Failed to read keystore", failure)
                error = R.string.settings_keystore_wrong_keystore
            } finally { busy = false }
        }
    }
    SettingsDialog(
        show = show,
        title = stringResource(R.string.settings_keystore_dialog_title),
        onDismissRequest = { if (!busy) onDismiss() },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                error = null
                if (path.isBlank()) {
                    error = R.string.settings_keystore_wrong_keystore
                    return@TextButton
                }
                busy = true
                scope.launch {
                    try {
                        error = withContext(Dispatchers.IO) { validateKeystore(stageFile, password, alias, aliasPassword) }
                        if (error == null) {
                            onConfirm(stageFile, path, password, alias, aliasPassword)
                            onDismiss()
                        }
                    } catch (failure: Exception) {
                        Log.e(TAG, "Failed to import keystore", failure)
                        error = R.string.settings_keystore_wrong_keystore
                    } finally { busy = false }
                }
            }) { Text(stringResource(android.R.string.ok)) }
        },
    ) {
        Text(stringResource(R.string.settings_keystore_desc), style = MaterialTheme.typography.bodyMedium)
        error?.let { SettingsErrorText(stringResource(it)) }
        // A real accessible button replaces the old read-only field / PressInteraction interception.
        OutlinedButton(enabled = !busy, onClick = { launcher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(path.ifBlank { stringResource(R.string.settings_keystore_file) })
        }
        OutlinedTextField(
            value = password, onValueChange = { password = it; error = null },
            label = { Text(stringResource(R.string.settings_keystore_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = alias, onValueChange = { alias = it; error = null },
            label = { Text(stringResource(R.string.settings_keystore_alias)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = aliasPassword, onValueChange = { aliasPassword = it; error = null },
            label = { Text(stringResource(R.string.settings_keystore_alias_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

private fun validateKeystore(stageFile: File, password: String, alias: String, aliasPassword: String): Int? {
    val keystore = KeyStore.getInstance("BKS")
    try {
        stageFile.inputStream().use { keystore.load(it, password.toCharArray()) }
    } catch (error: IOException) {
        return if (error.message == "KeyStore integrity check failed.") R.string.settings_keystore_wrong_password
        else R.string.settings_keystore_wrong_keystore
    }
    if (!keystore.containsAlias(alias)) return R.string.settings_keystore_wrong_alias
    try {
        if (keystore.getKey(alias, aliasPassword.toCharArray()) == null) return R.string.settings_keystore_wrong_alias
    } catch (_: GeneralSecurityException) {
        return R.string.settings_keystore_wrong_alias_password
    }
    return null
}

package com.matchora.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matchora.app.data.*
import com.matchora.app.ui.theme.MColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/** Publication keys and drafts share one account-scoped record, including after a lost reply. */
@Stable
class CreationFormState(
    private val backend: BackendRepository?,
    private val owner: String?,
    private val store: ListingDraftStore,
    private val scope: CoroutineScope
) {
    val restored = store.restore() ?: apiPayload()
    var fields = restored
    var fieldProvider: (() -> JsonObject)? = null
    var initialFields: JsonObject? = null
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var leaveDialog by mutableStateOf(false)
    var completed = false
    val dirty get() = restored.isNotEmpty() || (fieldProvider?.invoke() ?: fields) != initialFields
    val hasAttempt get() = store.hasPublicationAttempt

    fun save(): Boolean = try {
        fields = fieldProvider?.invoke() ?: fields
        store.save(fields)
        true
    } catch (failure: Exception) {
        error = failure.message ?: "Taslak kaydedilemedi."
        false
    }

    fun requestDismiss(onDismiss: () -> Unit) {
        if (busy) return
        if (dirty && !completed) leaveDialog = true else onDismiss()
    }

    fun confirmDismiss(onDismiss: () -> Unit): Boolean {
        if (busy) return false
        if (dirty && !completed) { requestDismiss(onDismiss); return false }
        return true
    }

    fun discard(onDismiss: () -> Unit) {
        try {
            store.clear()
            completed = true
            leaveDialog = false
            onDismiss()
        } catch (failure: Exception) { error = failure.message ?: "Taslak silinemedi." }
    }

    fun submit(operation: String, payload: JsonObject, onPreview: () -> Unit, onPublished: (String) -> Unit, onDismiss: () -> Unit) {
        if (busy || !save()) return
        val key = try { store.publicationKey(payload) }
        catch (failure: Exception) { error = failure.message ?: "İşlem kaydedilemedi."; return }
        busy = true
        error = null
        scope.launch {
            try {
                val result = if (backend != null) backend.command(operation, payload, key, expectedOwner = owner) else null
                store.clear()
                completed = true
                if (result == null) onPreview() else onPublished(result.s("id"))
                onDismiss()
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { error = failure.message ?: "Kaydedilemedi. Taslağın korunuyor; tekrar deneyebilirsin." }
            finally { busy = false }
        }
    }
}

@Composable
fun rememberCreationForm(kind: String): CreationFormState {
    val backend = LocalBackendRepository.current
    val owner = backend?.userId
    val scope = rememberCoroutineScope()
    return remember(kind, owner, backend) {
        CreationFormState(backend, owner, ListingDraftStore("form:$kind:${owner ?: "preview"}"), scope)
    }
}

@Composable
fun CreationDraftEffect(form: CreationFormState, readFields: () -> JsonObject) {
    val fields = readFields()
    form.fieldProvider = readFields
    if (form.initialFields == null) form.initialFields = fields
    form.fields = fields
    LaunchedEffect(form, fields) {
        if (form.dirty && !form.completed) { delay(250); form.save() }
    }
    DisposableEffect(form) {
        onDispose { if (form.dirty && !form.completed) form.save() }
    }
}

@Composable
fun CreationFormBody(form: CreationFormState, content: @Composable () -> Unit) {
    Box {
        content()
        if (form.busy) {
            Box(Modifier.matchParentSize().clickable(
                indication = null, interactionSource = remember { MutableInteractionSource() }
            ) {})
            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter), color = MColors.Brand)
        }
    }
}

@Composable
fun CreationFormNotice(form: CreationFormState) {
    form.error?.let { Text(it, color = MColors.Rose, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp)) }
}

@Composable
fun CreationFormDialogs(form: CreationFormState, onDismiss: () -> Unit) {
    if (form.leaveDialog) AlertDialog(
        onDismissRequest = { form.leaveDialog = false }, title = { Text("Taslağı saklamak ister misin?") },
        text = { Text("Bilgilerine daha sonra kaldığın yerden devam edebilirsin.") },
        confirmButton = { TextButton(onClick = { if (form.save()) { form.leaveDialog = false; onDismiss() } }) { Text("Taslağı sakla") } },
        dismissButton = { Row {
            TextButton(onClick = { form.leaveDialog = false }) { Text("Devam et") }
            TextButton(onClick = { form.discard(onDismiss) }) { Text("Sil", color = MColors.Rose) }
        } }
    )
}

internal inline fun <reified T : Enum<T>> JsonObject.formEnum(key: String, default: T): T =
    runCatching { enumValueOf<T>(s(key)) }.getOrDefault(default)

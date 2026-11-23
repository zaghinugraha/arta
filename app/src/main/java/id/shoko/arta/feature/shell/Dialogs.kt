package id.shoko.arta.feature.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.shoko.arta.R
import id.shoko.arta.core.ui.Arta

@Composable
fun artaFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Arta.colors.accent,
    unfocusedBorderColor = Arta.colors.hairStrong,
    focusedLabelColor = Arta.colors.accent,
    cursorColor = Arta.colors.accent,
    focusedContainerColor = Arta.colors.surf,
    unfocusedContainerColor = Arta.colors.surf,
)

@Composable
fun ArtaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    supporting: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = Arta.type.label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        isError = isError,
        textStyle = Arta.type.bodyLarge.copy(color = Arta.colors.on),
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        supportingText = supporting?.let { { Text(it, style = Arta.type.small) } },
        shape = RoundedCornerShape(16.dp),
        colors = artaFieldColors(),
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Arta.colors.bg,
        title = { Text(title, style = Arta.type.section) },
        text = { Text(body, style = Arta.type.body, color = Arta.colors.muted) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = if (danger) Arta.colors.down else Arta.colors.accent, style = Arta.type.labelStrong) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Arta.colors.muted, style = Arta.type.labelStrong) } },
    )
}

@Composable
fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    footer: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Arta.colors.bg,
        title = { Text(title, style = Arta.type.section) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { o ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.RadioButton) { onSelect(o); onDismiss() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(selected = o == selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Arta.colors.accent))
                        Text(label(o), style = Arta.type.bodyLarge, color = Arta.colors.on)
                    }
                }
                footer?.let { Text(it, style = Arta.type.caption, color = Arta.colors.muted, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = Arta.colors.accent) } },
    )
}

/** Dialog input satu/lebih field. [validate] mengembalikan pesan error atau null. */
@Composable
fun InputDialog(
    title: String,
    fields: List<Pair<String, String>>,
    numeric: List<Boolean> = fields.map { false },
    confirm: String,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    note: String? = null,
    validate: (List<String>) -> String? = { null },
) {
    var values by remember { mutableStateOf(fields.map { it.second }) }
    val error = validate(values)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Arta.colors.bg,
        title = { Text(title, style = Arta.type.section) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                note?.let { Text(it, style = Arta.type.caption, color = Arta.colors.muted) }
                fields.forEachIndexed { i, (label, _) ->
                    ArtaTextField(values[i], { nv -> values = values.toMutableList().also { it[i] = nv } }, label, numeric = numeric.getOrElse(i) { false })
                }
                error?.let { Text(it, style = Arta.type.caption, color = Arta.colors.down) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(values); onDismiss() }, enabled = error == null) {
                Text(confirm, color = if (error == null) Arta.colors.accent else Arta.colors.muted, style = Arta.type.labelStrong)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Arta.colors.muted) } },
    )
}


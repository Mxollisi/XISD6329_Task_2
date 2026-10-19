package za.co.giantautoupholstery.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import za.co.giantautoupholstery.core.formatRand
import za.co.giantautoupholstery.domain.BookingStatus
import za.co.giantautoupholstery.domain.Service

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading" })
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(Spacing.l), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState("Something went wrong", message, modifier) { Button(onClick = onRetry) { Text("Retry") } }
}

@Composable
fun SignInRequired(message: String, onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState("Sign in required", message) {
            Button(onClick = onSignIn, modifier = Modifier.heightIn(min = 48.dp)) { Text("Sign in") }
        }
    }
}

@Composable
fun FormField(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    error: String? = null, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true,
    minLines: Int = 1, enabled: Boolean = true, supporting: String? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(),
        isError = error != null, enabled = enabled, singleLine = singleLine, minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = { (error ?: supporting)?.let { Text(it) } },
    )
}

@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, error: String? = null, enabled: Boolean = true) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(),
        isError = error != null, enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (visible) "Hide password" else "Show password")
            }
        },
        supportingText = { error?.let { Text(it) } },
    )
}

@Composable
fun BusyButton(text: String, busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        else Text(text)
    }
}

@Composable
fun StatusChip(status: BookingStatus) {
    val (bg, fg) = when (status) {
        BookingStatus.Pending -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        BookingStatus.Confirmed -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        BookingStatus.Completed -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        BookingStatus.Cancelled -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.small) {
        Text(status.name, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Avatar(initials: String, modifier: Modifier = Modifier) {
    Box(modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, CircleShape).semantics { contentDescription = "Profile avatar $initials" }, contentAlignment = Alignment.Center) {
        Text(initials, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun ServiceCard(service: Service, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            ServiceImage(service)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(service.name, style = MaterialTheme.typography.titleMedium)
                Text(service.category + " · " + service.duration, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(service.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Text("From " + formatRand(service.priceCents), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ServiceImage(service: Service, modifier: Modifier = Modifier) {
    val size = modifier.size(64.dp)
    if (service.imageRef != null) {
        AsyncImage(model = service.imageRef, contentDescription = service.name + " photo", modifier = size)
    } else { // accessible fallback when no genuine image has been supplied
        Box(size.background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Chair, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier, style = MaterialTheme.typography.titleLarge)

/** Simple confirm dialog used for destructive actions. */
@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep") } },
    )
}

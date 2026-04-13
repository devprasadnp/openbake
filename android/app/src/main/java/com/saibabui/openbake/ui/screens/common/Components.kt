package com.saibabui.openbake.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.saibabui.openbake.ui.theme.Primary
import com.saibabui.openbake.ui.theme.PrimaryContainer
import com.saibabui.openbake.ui.theme.OpenBakeShapes
import com.saibabui.openbake.ui.theme.openBakeSpacing

enum class OpenBakeButtonStyle {
    Primary,
    Secondary,
    Ghost,
    Danger,
}

enum class OpenBakeCardStyle {
    Elevated,
    Filled,
    Outlined,
}

val CrustGradient = Brush.linearGradient(
    colors = listOf(Primary, PrimaryContainer)
)

@Composable
fun OpenBakeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    style: OpenBakeButtonStyle = OpenBakeButtonStyle.Primary,
) {
    val spacing = MaterialTheme.openBakeSpacing
    val colorScheme = MaterialTheme.colorScheme
    val buttonColors = when (style) {
        OpenBakeButtonStyle.Primary -> ButtonDefaults.buttonColors(
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
            disabledContainerColor = colorScheme.primary.copy(alpha = 0.4f),
        )

        OpenBakeButtonStyle.Secondary -> ButtonDefaults.buttonColors(
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer,
            disabledContainerColor = colorScheme.secondaryContainer.copy(alpha = 0.45f),
        )

        OpenBakeButtonStyle.Ghost -> ButtonDefaults.buttonColors(
            containerColor = colorScheme.surfaceContainerLow,
            contentColor = colorScheme.onSurface,
            disabledContainerColor = colorScheme.surfaceContainerLow.copy(alpha = 0.45f),
        )

        OpenBakeButtonStyle.Danger -> ButtonDefaults.buttonColors(
            containerColor = colorScheme.error,
            contentColor = colorScheme.onError,
            disabledContainerColor = colorScheme.error.copy(alpha = 0.45f),
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = OpenBakeShapes.pill,
        colors = buttonColors,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(spacing.xs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(spacing.xs))
            trailingIcon()
        }
    }
}

@Composable
fun OpenBakeOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val spacing = MaterialTheme.openBakeSpacing
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        enabled = enabled,
        shape = OpenBakeShapes.small,
        border = ButtonDefaults.outlinedButtonBorder(enabled = enabled),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(spacing.xs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun OpenBakeCard(
    modifier: Modifier = Modifier,
    style: OpenBakeCardStyle = OpenBakeCardStyle.Elevated,
    shape: Shape = OpenBakeShapes.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardColors = when (style) {
        OpenBakeCardStyle.Elevated -> CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest)
        OpenBakeCardStyle.Filled -> CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
        OpenBakeCardStyle.Outlined -> CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLowest)
    }

    Card(
        modifier = modifier.then(
            if (style == OpenBakeCardStyle.Outlined) {
                Modifier.border(1.dp, colorScheme.outlineVariant, shape)
            } else {
                Modifier
            }
        ),
        shape = shape,
        colors = cardColors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (style == OpenBakeCardStyle.Elevated) 2.dp else 0.dp,
        ),
        content = content,
    )
}

@Composable
fun OpenBakeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
    ),
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OpenBakeShapes.small,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary,
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorLabelColor = MaterialTheme.colorScheme.error,
        errorTrailingIconColor = MaterialTheme.colorScheme.error,
    ),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors,
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OpenBakeButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        style = OpenBakeButtonStyle.Primary,
    )
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val spacing = MaterialTheme.openBakeSpacing
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(spacing.xxl)
        ) {
            Text(emoji, style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(spacing.lg))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(spacing.xl))
                GradientButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

/**
 * Image composable with built-in placeholder and error fallback.
 * Shows a cake emoji placeholder while loading and on error.
 */
@Composable
fun OpenBakeImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = OpenBakeShapes.medium,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderEmoji: String = "🎂",
    emojiFontSize: Int = 32
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = contentScale,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Text(placeholderEmoji, fontSize = emojiFontSize.sp)
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Text(placeholderEmoji, fontSize = emojiFontSize.sp)
            }
        }
    )
}

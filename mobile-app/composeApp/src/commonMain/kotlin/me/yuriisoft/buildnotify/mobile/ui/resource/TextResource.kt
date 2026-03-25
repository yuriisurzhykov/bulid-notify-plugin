package me.yuriisoft.buildnotify.mobile.ui.resource

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Stable
interface TextResource {
    @Composable
    fun resolve(): CharSequence
}

@Immutable
data class RawText(val value: String) : TextResource {
    @Composable
    override fun resolve(): String = value
}

@Immutable
data class ResText(
    val resource: StringResource,
    val args: List<Any> = emptyList(),
) : TextResource {
    @Composable
    override fun resolve(): String = stringResource(resource, *args.toTypedArray())
}

@Immutable
data class StyledText(val value: AnnotatedString) : TextResource {
    @Composable
    override fun resolve(): AnnotatedString = value
}

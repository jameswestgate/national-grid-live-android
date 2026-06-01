package com.crainiate.nationalgridlive.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crainiate.nationalgridlive.data.model.GridSnapshot
import com.crainiate.nationalgridlive.ui.GridUiState
import java.util.Locale

/** Picks a legible on-colour for text/icons drawn over a fixed brand colour. */
fun onColor(background: Color): Color =
    if (background.luminance() > 0.55f) Color(0xFF1A1A1A) else Color.White

fun gw(value: Double): String = String.format(Locale.UK, "%.2f", value)
fun gw1(value: Double): String = String.format(Locale.UK, "%.1f", value)
fun percent(fraction: Double): String = String.format(Locale.UK, "%.1f%%", fraction * 100)

/** Large screen title that scrolls with the content (no pinned app bar). */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

/**
 * Standard scrolling screen body: pull-to-refresh, a large [title] that scrolls
 * off the top, an [OfflineBanner] when [online] is false, optional always-visible
 * [topContent] (e.g. the Historic period selector) and state-driven [readyContent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreenContainer(
    title: String,
    state: GridUiState,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    online: Boolean,
    modifier: Modifier = Modifier,
    topContent: @Composable ColumnScope.() -> Unit = {},
    readyContent: @Composable ColumnScope.(GridSnapshot) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ScreenTitle(title)
            if (!online) OfflineBanner(onRetry = onRefresh)
            topContent()
            when (state) {
                is GridUiState.Loading -> LoadingBox()
                is GridUiState.Failed -> ErrorBox(state.message)
                is GridUiState.Ready -> readyContent(state.snapshot)
            }
        }
    }
}

/** "Couldn't refresh" banner with a Retry action (shown while offline). */
@Composable
fun OfflineBanner(
    onRetry: () -> Unit,
    message: String = "Showing the latest available data.",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Couldn't refresh", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(message: String) {
    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

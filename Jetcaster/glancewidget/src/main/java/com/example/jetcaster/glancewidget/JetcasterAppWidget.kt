package com.example.jetcaster.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.SquareIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal val TAG = "JetcasterAppWidegt"

/**
 * Implementation of App Widget functionality.
 */
class JetcasterAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = JetcasterAppWidget()


}

data class JetcasterAppWidgetViewState(
    val trackTitle: String,
    val trackSubtitle: String,
    val isPlaying: Boolean,
    val albumArtUri: String
)

private object Sizes {
    val minWidth = 140.dp
    val smallBucketCutoffWidth = 250.dp // anything from minWidth to this will have no title

    val imageNormal = 80.dp
    val imageCondensed = 60.dp
}

private enum class SizeBucket { Invalid, Narrow, Normal }

@Composable
private fun calculateSizeBucket(): SizeBucket {
    val size: DpSize = LocalSize.current
    val width = size.width

    return when {
        width < Sizes.minWidth -> SizeBucket.Invalid
        width <= Sizes.smallBucketCutoffWidth -> SizeBucket.Narrow
        else -> SizeBucket.Normal
    }
}

class JetcasterAppWidget() : GlanceAppWidget() {
    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val testState = JetcasterAppWidgetViewState(
            trackTitle = "Serifs afoot",
            trackSubtitle =  "Roboto Beach",
            isPlaying = false,
            albumArtUri = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Longsbear.JPG/320px-Longsbear.JPG"
        )


        provideContent {
            val sizeBucket = calculateSizeBucket()
            val playPauseIcon = if (testState.isPlaying) PlayPauseIcon.Pause else PlayPauseIcon.Play
            val artUri = Uri.parse(testState.albumArtUri)

            when (sizeBucket) {
                SizeBucket.Invalid -> WidgetUiInvalidSize()
                SizeBucket.Narrow -> WidgetUiNarrow(
                    imageUri = artUri ,
                    playPauseIcon = playPauseIcon
                )
                SizeBucket.Normal -> WidgetUiNormal(
                    title = testState.trackTitle,
                    subtitle = testState.trackSubtitle,
                    imageUri = artUri,
                    playPauseIcon = playPauseIcon
                )
            }
        }
    }
}

@Composable
private fun WidgetUiNormal(
    title: String,
    subtitle: String,
    imageUri: Uri,
    playPauseIcon: PlayPauseIcon,
) {
    Scaffold(titleBar = {} /* title bar will be optional starting in glance 1.1.0-beta3*/) {
        Row(
            GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            AlbumArt(imageUri, GlanceModifier.size(Sizes.imageNormal))
            PodcastText(title, subtitle, modifier = GlanceModifier.padding(16.dp).defaultWeight())
            PlayPauseButton(playPauseIcon, {})
        }
    }
}

@Composable
private fun WidgetUiNarrow(
    imageUri: Uri,
    playPauseIcon: PlayPauseIcon,
) {
    Scaffold(titleBar = {} /* title bar will be optional in scaffold in glance 1.1.0-beta3*/) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            AlbumArt(imageUri, GlanceModifier.size(Sizes.imageCondensed))
            Spacer(GlanceModifier.defaultWeight())
            PlayPauseButton(playPauseIcon, {})
        }
    }
}

@Composable
private fun WidgetUiInvalidSize() {
    Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color.Magenta))) {
        Text("invalid size")
    }
}

@Composable
private fun AlbumArt(
    imageUri: Uri,
    modifier: GlanceModifier = GlanceModifier
) {
    WidgetAsyncImage(uri = imageUri, contentDescription = null, modifier = modifier)
}

@Composable
fun PodcastText(title: String, subtitle: String, modifier: GlanceModifier = GlanceModifier) {
    val fgColor = GlanceTheme.colors.onPrimaryContainer
    Column(modifier) {
        Text(
            text = title,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = fgColor),
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )
        Text(
            text = subtitle,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fgColor),
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun PlayPauseButton(state: PlayPauseIcon, onClick: () -> Unit) {
    val (iconRes: Int, description: Int) = when (state) {
        PlayPauseIcon.Play -> R.drawable.outline_play_arrow_24 to R.string.content_description_play
        PlayPauseIcon.Pause -> R.drawable.outline_pause_24 to R.string.content_description_pause
    }

    val provider = ImageProvider(iconRes)
    val contentDescription = LocalContext.current.getString(description)

    SquareIconButton(
        provider,
        contentDescription = contentDescription,
        onClick = onClick
    )
}

enum class PlayPauseIcon { Play, Pause }


/**
 * Uses Coil to load images.
 */
@Composable
private fun WidgetAsyncImage(
    uri: Uri,
    contentDescription: String?,
    modifier: GlanceModifier = GlanceModifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = uri) {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(200, 200)
            .target { data: Drawable ->
                bitmap = (data as BitmapDrawable).bitmap
            }
            .build()

        scope.launch(Dispatchers.IO) {
            val result = ImageLoader(context).execute(request)
            if (result is ErrorResult) {
                val t = result.throwable
                Log.e(TAG, "Image request error:", t)
            }
        }
    }

    bitmap?.let { bitmap ->
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = modifier.cornerRadius(12.dp)// TODO: confirm radius with design
        )
    }
}
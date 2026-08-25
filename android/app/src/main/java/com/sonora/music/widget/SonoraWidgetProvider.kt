package com.sonora.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.sonora.app.R
import com.sonora.music.SonoraNativeActivity
import com.sonora.music.data.model.Song
import com.sonora.music.service.SonoraAudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SonoraWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.sonora.music.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.sonora.music.widget.ACTION_NEXT"
        const val ACTION_PREV = "com.sonora.music.widget.ACTION_PREV"

        fun updateAllWidgets(context: Context, song: Song?, isPlaying: Boolean) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SonoraWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                val views = RemoteViews(context.packageName, R.layout.widget_sonora_capsule)

                // Title & Artist
                views.setTextViewText(R.id.widget_song_title, song?.title ?: "Sonora Music")
                views.setTextViewText(R.id.widget_song_artist, song?.artist ?: "Toca para reproducir")

                // Play / Pause Icon
                val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

                // Load artwork if available
                var artBitmap: Bitmap? = null
                song?.coverUri?.let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            artBitmap = BitmapFactory.decodeStream(stream)
                        }
                    } catch (_: Exception) {}
                }

                if (artBitmap != null) {
                    views.setImageViewBitmap(R.id.widget_song_art, artBitmap)
                } else {
                    views.setImageViewResource(R.id.widget_song_art, R.mipmap.ic_launcher)
                }

                // Pending Intent: Open App
                val openAppIntent = Intent(context, SonoraNativeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root_layout, openAppPendingIntent)

                // Pending Intent: Play / Pause
                val playPauseIntent = Intent(context, SonoraWidgetProvider::class.java).apply {
                    action = ACTION_PLAY_PAUSE
                }
                val playPausePending = PendingIntent.getBroadcast(
                    context,
                    1,
                    playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePending)

                // Pending Intent: Next
                val nextIntent = Intent(context, SonoraWidgetProvider::class.java).apply {
                    action = ACTION_NEXT
                }
                val nextPending = PendingIntent.getBroadcast(
                    context,
                    2,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending)

                // Pending Intent: Previous
                val prevIntent = Intent(context, SonoraWidgetProvider::class.java).apply {
                    action = ACTION_PREV
                }
                val prevPending = PendingIntent.getBroadcast(
                    context,
                    3,
                    prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPending)

                withContext(Dispatchers.Main) {
                    for (widgetId in appWidgetIds) {
                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val player = SonoraAudioPlayer.getInstance(context)
        updateAllWidgets(context, player.currentSong.value, player.isPlaying.value)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val player = SonoraAudioPlayer.getInstance(context)

        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                player.togglePlay()
                updateAllWidgets(context, player.currentSong.value, player.isPlaying.value)
            }
            ACTION_NEXT -> {
                player.nextTrack()
                updateAllWidgets(context, player.currentSong.value, player.isPlaying.value)
            }
            ACTION_PREV -> {
                player.prevTrack()
                updateAllWidgets(context, player.currentSong.value, player.isPlaying.value)
            }
        }
    }
}

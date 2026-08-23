package com.luxtune.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import java.io.File;

public class MusicPlayerService extends Service {

    public static final String ACTION_PLAY = "com.luxtune.app.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.luxtune.app.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.luxtune.app.ACTION_RESUME";
    public static final String ACTION_PREV = "com.luxtune.app.ACTION_PREV";
    public static final String ACTION_NEXT = "com.luxtune.app.ACTION_NEXT";
    public static final String ACTION_STOP = "com.luxtune.app.ACTION_STOP";

    private static final String CHANNEL_ID = "luxtune_music_channel";
    private static final int NOTIFICATION_ID = 8801;

    // Dual MediaPlayers for real overlapping DJ crossfade
    private MediaPlayer playerA;
    private MediaPlayer playerB;
    private MediaPlayer activePlayer;
    private MediaPlayer nextPlayer;

    private MediaSessionCompat mediaSession;
    private final IBinder binder = new MusicBinder();

    private String currentTitle = "luxTune";
    private String currentArtist = "Reproductor Local";
    private String currentPath = "";
    private String currentCoverPath = "";

    // Pre-loaded next track information for crossfade
    private String nextTitle = "";
    private String nextArtist = "";
    private String nextPath = "";
    private String nextCoverPath = "";

    private boolean isActivePrepared = false;
    private boolean isCrossfading = false;
    private int crossfadeSeconds = 2;
    private float targetVolume = 0.85f;
    private float currentSpeed = 1.0f;

    private ValueAnimator crossfadeAnimator;
    private final Handler crossfadeCheckHandler = new Handler(Looper.getMainLooper());
    private final Runnable crossfadeCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkAndTriggerOverlappingCrossfade();
            if (activePlayer != null && activePlayer.isPlaying()) {
                crossfadeCheckHandler.postDelayed(this, 250);
            }
        }
    };

    public interface ServiceCallback {
        void onTrackEnded();
        void onPlayStateChanged(boolean isPlaying);
        void onNextRequested();
        void onPrevRequested();
        void onTrackAutoSwapped(String newTrackPath);
    }

    private static ServiceCallback callback;

    public static void setCallback(ServiceCallback cb) {
        callback = cb;
    }

    public class MusicBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayers();
        initMediaSession();
        createNotificationChannel();
    }

    private MediaPlayer createConfiguredMediaPlayer() {
        MediaPlayer mp = new MediaPlayer();
        mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mp.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
        return mp;
    }

    private void initMediaPlayers() {
        playerA = createConfiguredMediaPlayer();
        playerB = createConfiguredMediaPlayer();
        activePlayer = playerA;
        nextPlayer = playerB;
        setupCompletionListener(playerA);
        setupCompletionListener(playerB);
    }

    private void setupCompletionListener(final MediaPlayer mp) {
        mp.setOnCompletionListener(mediaPlayer -> {
            if (mediaPlayer == activePlayer && !isCrossfading) {
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                updateNotification(false);
                if (callback != null) {
                    callback.onTrackEnded();
                }
            }
        });

        mp.setOnErrorListener((mediaPlayer, what, extra) -> {
            if (mediaPlayer == activePlayer) {
                isActivePrepared = false;
            }
            return false;
        });
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "luxTuneMediaSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resume();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                if (callback != null) callback.onNextRequested();
            }

            @Override
            public void onSkipToPrevious() {
                if (callback != null) callback.onPrevRequested();
            }

            @Override
            public void onSeekTo(long pos) {
                seek((int) pos);
            }
        });
        mediaSession.setActive(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reproducción de Música luxTune",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controles de reproducción y notificación multimedia");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                String path = intent.getStringExtra("path");
                String title = intent.getStringExtra("title");
                String artist = intent.getStringExtra("artist");
                String cover = intent.getStringExtra("cover");
                play(path, title, artist, cover);
            } else if (ACTION_PAUSE.equals(action)) {
                pause();
            } else if (ACTION_RESUME.equals(action)) {
                resume();
            } else if (ACTION_NEXT.equals(action)) {
                if (callback != null) callback.onNextRequested();
            } else if (ACTION_PREV.equals(action)) {
                if (callback != null) callback.onPrevRequested();
            } else if (ACTION_STOP.equals(action)) {
                stopForeground(true);
                stopSelf();
            }
        }
        return START_STICKY;
    }

    public void setCrossfadeSeconds(int sec) {
        this.crossfadeSeconds = Math.max(0, sec);
    }

    public void setNextTrack(String path, String title, String artist, String cover) {
        this.nextPath = path != null ? path : "";
        this.nextTitle = title != null ? title : "";
        this.nextArtist = artist != null ? artist : "";
        this.nextCoverPath = cover != null ? cover : "";
    }

    /**
     * Checks if the active track is nearing its end and triggers overlapping DJ crossfade
     */
    private void checkAndTriggerOverlappingCrossfade() {
        if (isCrossfading || crossfadeSeconds <= 0 || activePlayer == null || !activePlayer.isPlaying()) {
            return;
        }

        if (nextPath == null || nextPath.isEmpty()) {
            return;
        }

        try {
            int pos = activePlayer.getCurrentPosition();
            int dur = activePlayer.getDuration();
            int crossfadeMs = crossfadeSeconds * 1000;

            if (dur > 6000 && pos >= (dur - crossfadeMs)) {
                int remainingMs = Math.max(1000, dur - pos);
                executeDualCrossfade(nextPath, nextTitle, nextArtist, nextCoverPath, remainingMs);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Executes the simultaneous overlapping blend between activePlayer and nextPlayer
     */
    private void executeDualCrossfade(final String newPath, final String newTitle, final String newArtist, final String newCover, final long transitionDurationMs) {
        if (isCrossfading) return;
        isCrossfading = true;

        try {
            if (nextPlayer == null) {
                nextPlayer = createConfiguredMediaPlayer();
                setupCompletionListener(nextPlayer);
            } else {
                nextPlayer.reset();
            }

            if (newPath.startsWith("content://")) {
                nextPlayer.setDataSource(this, Uri.parse(newPath));
            } else {
                nextPlayer.setDataSource(newPath);
            }

            nextPlayer.setOnPreparedListener(np -> {
                try {
                    np.setVolume(0f, 0f);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed != 1.0f) {
                        try {
                            np.setPlaybackParams(np.getPlaybackParams().setSpeed(currentSpeed));
                        } catch (Exception ignored) {}
                    }
                    np.start();

                    if (crossfadeAnimator != null) {
                        crossfadeAnimator.cancel();
                    }

                    crossfadeAnimator = ValueAnimator.ofFloat(0f, 1f);
                    crossfadeAnimator.setDuration(transitionDurationMs);
                    crossfadeAnimator.addUpdateListener(anim -> {
                        float fraction = (float) anim.getAnimatedValue();
                        float outVol = (1f - fraction) * targetVolume;
                        float inVol = fraction * targetVolume;
                        try {
                            if (activePlayer != null) activePlayer.setVolume(outVol, outVol);
                        } catch (Exception ignored) {}
                        try {
                            if (np != null) np.setVolume(inVol, inVol);
                        } catch (Exception ignored) {}
                    });

                    crossfadeAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            try {
                                if (activePlayer != null) {
                                    activePlayer.stop();
                                    activePlayer.reset();
                                }
                            } catch (Exception ignored) {}

                            // Swap references
                            MediaPlayer temp = activePlayer;
                            activePlayer = nextPlayer;
                            nextPlayer = temp;

                            currentPath = newPath;
                            currentTitle = (newTitle != null && !newTitle.isEmpty()) ? newTitle : "Canción";
                            currentArtist = (newArtist != null && !newArtist.isEmpty()) ? newArtist : "Artista";
                            currentCoverPath = newCover;
                            isActivePrepared = true;
                            isCrossfading = false;
                            nextPath = "";

                            if (activePlayer != null) {
                                activePlayer.setVolume(targetVolume, targetVolume);
                            }

                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                            updateMediaMetadata();
                            updateNotification(true);

                            if (callback != null) {
                                callback.onTrackAutoSwapped(currentPath);
                            }
                        }
                    });

                    crossfadeAnimator.start();
                } catch (Exception e) {
                    isCrossfading = false;
                }
            });

            nextPlayer.prepareAsync();

        } catch (Exception e) {
            isCrossfading = false;
            e.printStackTrace();
        }
    }

    public void play(String path, String title, String artist, String cover) {
        // If already playing and crossfade is enabled, do a quick DJ overlap transition (1.2s)
        if (activePlayer != null && activePlayer.isPlaying() && crossfadeSeconds > 0 && !path.equals(currentPath)) {
            executeDualCrossfade(path, title, artist, cover, Math.min(2000, crossfadeSeconds * 400L));
            return;
        }

        // Direct playback
        if (crossfadeAnimator != null) {
            crossfadeAnimator.cancel();
        }
        isCrossfading = false;

        this.currentPath = path;
        this.currentTitle = (title != null && !title.isEmpty()) ? title : "Canción";
        this.currentArtist = (artist != null && !artist.isEmpty()) ? artist : "Artista";
        this.currentCoverPath = cover;

        try {
            if (activePlayer == null) {
                activePlayer = createConfiguredMediaPlayer();
                setupCompletionListener(activePlayer);
            }
            activePlayer.reset();
            isActivePrepared = false;

            if (path.startsWith("content://")) {
                activePlayer.setDataSource(this, Uri.parse(path));
            } else {
                activePlayer.setDataSource(path);
            }

            activePlayer.setOnPreparedListener(mp -> {
                isActivePrepared = true;
                mp.setVolume(targetVolume, targetVolume);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentSpeed != 1.0f) {
                    try {
                        mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(currentSpeed));
                    } catch (Exception ignored) {}
                }
                mp.start();
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                updateMediaMetadata();
                updateNotification(true);
                if (callback != null) callback.onPlayStateChanged(true);

                crossfadeCheckHandler.removeCallbacks(crossfadeCheckRunnable);
                crossfadeCheckHandler.postDelayed(crossfadeCheckRunnable, 500);
            });

            activePlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        if (activePlayer != null && activePlayer.isPlaying()) {
            activePlayer.pause();
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
            updateNotification(false);
            if (callback != null) callback.onPlayStateChanged(false);
            crossfadeCheckHandler.removeCallbacks(crossfadeCheckRunnable);
        }
    }

    public void resume() {
        if (activePlayer != null && !activePlayer.isPlaying()) {
            activePlayer.setVolume(targetVolume, targetVolume);
            activePlayer.start();
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
            updateNotification(true);
            if (callback != null) callback.onPlayStateChanged(true);
            crossfadeCheckHandler.removeCallbacks(crossfadeCheckRunnable);
            crossfadeCheckHandler.postDelayed(crossfadeCheckRunnable, 500);
        }
    }

    public void seek(int msec) {
        if (activePlayer != null && isActivePrepared) {
            activePlayer.seekTo(msec);
        }
    }

    public void setSpeed(float speed) {
        this.currentSpeed = speed;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activePlayer != null) {
            try {
                activePlayer.setPlaybackParams(activePlayer.getPlaybackParams().setSpeed(speed));
            } catch (Exception ignored) {}
        }
    }

    public void setVolume(float vol) {
        this.targetVolume = vol;
        if (!isCrossfading && activePlayer != null) {
            activePlayer.setVolume(vol, vol);
        }
    }

    public int getCurrentPosition() {
        return (activePlayer != null && isActivePrepared) ? activePlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return (activePlayer != null && isActivePrepared) ? activePlayer.getDuration() : 0;
    }

    public boolean isPlaying() {
        return activePlayer != null && activePlayer.isPlaying();
    }

    private void updatePlaybackState(int state) {
        long position = (activePlayer != null && isActivePrepared) ? activePlayer.getCurrentPosition() : 0;
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                            PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_PLAY_PAUSE |
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                            PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(state, position, currentSpeed);
        mediaSession.setPlaybackState(stateBuilder.build());
    }

    private void updateMediaMetadata() {
        MediaMetadataCompat.Builder metaBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "luxTune")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration());

        Bitmap coverBitmap = loadCoverBitmap(currentCoverPath);
        if (coverBitmap != null) {
            metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, coverBitmap);
        }

        mediaSession.setMetadata(metaBuilder.build());
    }

    private Bitmap loadCoverBitmap(String path) {
        if (path != null && !path.isEmpty()) {
            try {
                String cleanPath = path;
                if (cleanPath.startsWith("https://localhost/_capacitor_file_")) {
                    cleanPath = cleanPath.replace("https://localhost/_capacitor_file_", "");
                }
                File f = new File(cleanPath);
                if (f.exists() && f.canRead()) {
                    return BitmapFactory.decodeFile(f.getAbsolutePath());
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void updateNotification(boolean isPlaying) {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent prevIntent = new Intent(this, MusicPlayerService.class).setAction(ACTION_PREV);
        PendingIntent prevPendingIntent = PendingIntent.getService(
                this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent playPauseIntent = new Intent(this, MusicPlayerService.class)
                .setAction(isPlaying ? ACTION_PAUSE : ACTION_RESUME);
        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent nextIntent = new Intent(this, MusicPlayerService.class).setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(
                this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Bitmap coverBitmap = loadCoverBitmap(currentCoverPath);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setContentIntent(contentPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying)
                .setSilent(true)
                .addAction(android.R.drawable.ic_media_previous, "Anterior", prevPendingIntent)
                .addAction(
                        isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        isPlaying ? "Pausa" : "Reproducir",
                        playPausePendingIntent
                )
                .addAction(android.R.drawable.ic_media_next, "Siguiente", nextPendingIntent)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        if (coverBitmap != null) {
            builder.setLargeIcon(coverBitmap);
        }

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        crossfadeCheckHandler.removeCallbacks(crossfadeCheckRunnable);
        if (crossfadeAnimator != null) {
            crossfadeAnimator.cancel();
        }
        if (playerA != null) {
            playerA.release();
            playerA = null;
        }
        if (playerB != null) {
            playerB.release();
            playerB = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        stopForeground(true);
        super.onDestroy();
    }
}

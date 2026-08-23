package com.luxtune.app;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class MainActivity extends BridgeActivity {

    private static MusicPlayerService musicService;
    private static boolean isServiceBound = false;
    private static NativeAudioPlugin audioPluginInstance;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicPlayerService.MusicBinder b = (MusicPlayerService.MusicBinder) binder;
            musicService = b.getService();
            isServiceBound = true;
            setupServiceCallbacks();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isServiceBound = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(MediaStorePlugin.class);
        registerPlugin(NativeAudioPlugin.class);
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, MusicPlayerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupServiceCallbacks() {
        MusicPlayerService.setCallback(new MusicPlayerService.ServiceCallback() {
            @Override
            public void onTrackEnded() {
                if (audioPluginInstance != null) {
                    JSObject data = new JSObject();
                    data.put("event", "ended");
                    audioPluginInstance.sendTrackEnded(data);
                }
            }

            @Override
            public void onPlayStateChanged(boolean isPlaying) {
                if (audioPluginInstance != null) {
                    JSObject data = new JSObject();
                    data.put("isPlaying", isPlaying);
                    audioPluginInstance.sendPlayStateChanged(data);
                }
            }

            @Override
            public void onNextRequested() {
                if (audioPluginInstance != null) {
                    JSObject data = new JSObject();
                    audioPluginInstance.sendNextRequested(data);
                }
            }

            @Override
            public void onPrevRequested() {
                if (audioPluginInstance != null) {
                    JSObject data = new JSObject();
                    audioPluginInstance.sendPrevRequested(data);
                }
            }

            @Override
            public void onTrackAutoSwapped(String newTrackPath) {
                if (audioPluginInstance != null) {
                    JSObject data = new JSObject();
                    data.put("path", newTrackPath);
                    audioPluginInstance.sendTrackAutoSwapped(data);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        super.onDestroy();
    }

    @CapacitorPlugin(name = "NativeAudioPlugin")
    public static class NativeAudioPlugin extends Plugin {

        @Override
        public void load() {
            audioPluginInstance = this;
        }

        public void sendTrackEnded(JSObject data) {
            notifyListeners("trackEnded", data);
        }

        public void sendPlayStateChanged(JSObject data) {
            notifyListeners("playStateChanged", data);
        }

        public void sendNextRequested(JSObject data) {
            notifyListeners("nextRequested", data);
        }

        public void sendPrevRequested(JSObject data) {
            notifyListeners("prevRequested", data);
        }

        public void sendTrackAutoSwapped(JSObject data) {
            notifyListeners("trackAutoSwapped", data);
        }


        @PluginMethod
        public void play(PluginCall call) {
            String path = call.getString("path", "");
            String title = call.getString("title", "");
            String artist = call.getString("artist", "");
            String cover = call.getString("cover", "");

            if (musicService != null) {
                musicService.play(path, title, artist, cover);
                JSObject ret = new JSObject();
                ret.put("success", true);
                call.resolve(ret);
            } else {
                Intent intent = new Intent(getContext(), MusicPlayerService.class);
                intent.setAction(MusicPlayerService.ACTION_PLAY);
                intent.putExtra("path", path);
                intent.putExtra("title", title);
                intent.putExtra("artist", artist);
                intent.putExtra("cover", cover);
                getContext().startService(intent);
                JSObject ret = new JSObject();
                ret.put("success", true);
                call.resolve(ret);
            }
        }

        @PluginMethod
        public void pause(PluginCall call) {
            if (musicService != null) {
                musicService.pause();
            }
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        }

        @PluginMethod
        public void resume(PluginCall call) {
            if (musicService != null) {
                musicService.resume();
            }
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        }

        @PluginMethod
        public void seek(PluginCall call) {
            int positionMs = call.getInt("positionMs", 0);
            if (musicService != null) {
                musicService.seek(positionMs);
            }
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        }

        @PluginMethod
        public void setSpeed(PluginCall call) {
            double speed = call.getDouble("speed", 1.0);
            if (musicService != null) {
                musicService.setSpeed((float) speed);
            }
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        }

        @PluginMethod
        public void setVolume(PluginCall call) {
            double vol = call.getDouble("volume", 1.0);
            if (musicService != null) {
                musicService.setVolume((float) vol);
            }
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        }

        @PluginMethod
        public void setCrossfade(PluginCall call) {
            int seconds = call.getInt("seconds", 2);
            if (musicService != null) {
                musicService.setCrossfadeSeconds(seconds);
            }
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        }

        @PluginMethod
        public void setNextTrack(PluginCall call) {
            String path = call.getString("path", "");
            String title = call.getString("title", "");
            String artist = call.getString("artist", "");
            String cover = call.getString("cover", "");
            if (musicService != null) {
                musicService.setNextTrack(path, title, artist, cover);
            }
            JSObject ret = new JSObject();
            ret.put("success", true);
            call.resolve(ret);
        }



        @PluginMethod
        public void getPosition(PluginCall call) {
            JSObject ret = new JSObject();
            if (musicService != null) {
                ret.put("currentPositionMs", musicService.getCurrentPosition());
                ret.put("durationMs", musicService.getDuration());
                ret.put("isPlaying", musicService.isPlaying());
            } else {
                ret.put("currentPositionMs", 0);
                ret.put("durationMs", 0);
                ret.put("isPlaying", false);
            }
            call.resolve(ret);
        }
    }

    @CapacitorPlugin(name = "MediaStorePlugin")
    public static class MediaStorePlugin extends Plugin {

        @PluginMethod
        public void checkPermissions(PluginCall call) {
            boolean hasPermission = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasPermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
            } else {
                hasPermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            }
            JSObject ret = new JSObject();
            ret.put("granted", hasPermission);
            call.resolve(ret);
        }

        @PluginMethod
        public void requestMusicPermissions(PluginCall call) {
            String perm = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
            
            if (ContextCompat.checkSelfPermission(getContext(), perm) == PackageManager.PERMISSION_GRANTED) {
                JSObject ret = new JSObject();
                ret.put("granted", true);
                call.resolve(ret);
                return;
            }

            ActivityCompat.requestPermissions(getActivity(), new String[]{perm}, 1001);
            JSObject ret = new JSObject();
            ret.put("requested", true);
            call.resolve(ret);
        }

        @PluginMethod
        public void getLocalAudioFiles(PluginCall call) {
            JSArray songsArray = new JSArray();
            String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.YEAR
            };

            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + MediaStore.Audio.Media.DURATION + " >= 20000";
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

            File coversDir = new File(getContext().getCacheDir(), "album_covers");
            if (!coversDir.exists()) {
                coversDir.mkdirs();
            }

            try (Cursor cursor = getContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                    int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                    int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
                    int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                    int yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR);

                    do {
                        long id = cursor.getLong(idCol);
                        String title = cursor.getString(titleCol);
                        String artist = cursor.getString(artistCol);
                        String album = cursor.getString(albumCol);
                        long duration = cursor.getLong(durationCol);
                        String path = cursor.getString(dataCol);
                        long size = cursor.getLong(sizeCol);
                        long albumId = cursor.getLong(albumIdCol);
                        int year = cursor.getInt(yearCol);

                        // Extract and cache real embedded album cover art
                        String coverUrl = "";
                        File coverFile = new File(coversDir, "alb_" + albumId + ".jpg");
                        if (!coverFile.exists() && path != null && new File(path).exists()) {
                            try {
                                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                                mmr.setDataSource(path);
                                byte[] art = mmr.getEmbeddedPicture();
                                if (art != null && art.length > 0) {
                                    FileOutputStream fos = new FileOutputStream(coverFile);
                                    fos.write(art);
                                    fos.flush();
                                    fos.close();
                                }
                                mmr.release();
                            } catch (Exception ignored) {}
                        }

                        if (coverFile.exists()) {
                            coverUrl = "https://localhost/_capacitor_file_" + coverFile.getAbsolutePath();
                        }

                        // 1. Check embedded lyrics from audio metadata (FLAC / MP3 / M4A)
                        String lrcContent = "";
                        if (path != null && new File(path).exists()) {
                            try {
                                MediaMetadataRetriever mmrLyrics = new MediaMetadataRetriever();
                                mmrLyrics.setDataSource(path);
                                // Try extract lyrics tag (code 1000 / METADATA_KEY_LYRICS)
                                try {
                                    String embedded = mmrLyrics.extractMetadata(1000);
                                    if (embedded != null && !embedded.trim().isEmpty()) {
                                        lrcContent = embedded.trim();
                                    }
                                } catch (Exception ignored) {}
                                mmrLyrics.release();
                            } catch (Exception ignored) {}
                        }

                        // 2. Check if an external .lrc file exists alongside
                        if ((lrcContent == null || lrcContent.isEmpty()) && path != null && path.contains(".")) {
                            try {
                                String lrcPath = path.substring(0, path.lastIndexOf('.')) + ".lrc";
                                File lrcFile = new File(lrcPath);
                                if (lrcFile.exists() && lrcFile.canRead()) {
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(lrcFile)));
                                    StringBuilder sb = new StringBuilder();
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        sb.append(line).append("\n");
                                    }
                                    reader.close();
                                    lrcContent = sb.toString();
                                }
                            } catch (Exception ignored) {}
                        }

                        // Audio URI
                        String audioUrl = (path != null && new File(path).exists())
                            ? "https://localhost/_capacitor_file_" + path
                            : "content://media/external/audio/media/" + id;

                        JSObject songObj = new JSObject();
                        songObj.put("id", String.valueOf(id));
                        songObj.put("title", (title != null && !title.isEmpty()) ? title : "Canción sin título");
                        songObj.put("artist", (artist != null && !artist.equals("<unknown>")) ? artist : "Artista desconocido");
                        songObj.put("album", (album != null && !album.equals("<unknown>")) ? album : "Álbum desconocido");
                        songObj.put("duration", duration / 1000);
                        songObj.put("audioUrl", audioUrl);
                        songObj.put("filePath", path != null ? path : "");
                        songObj.put("coverUrl", coverUrl);
                        songObj.put("size", size);
                        songObj.put("year", year);
                        songObj.put("lyrics", lrcContent);

                        songsArray.put(songObj);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                call.reject("Error scanning audio files: " + e.getMessage());
                return;
            }

            JSObject result = new JSObject();
            result.put("songs", songsArray);
            result.put("count", songsArray.length());
            call.resolve(result);
        }
    }
}

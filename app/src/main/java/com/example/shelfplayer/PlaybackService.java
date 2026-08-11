package com.example.shelfplayer;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class PlaybackService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession mediaSession;
    private SharedPreferences positions;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable saveLoop = new Runnable() {
        @Override
        public void run() {
            saveCurrentPosition();
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        positions = getSharedPreferences("positions", MODE_PRIVATE);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build();

        player = new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(audioAttributes, true);
        player.setHandleAudioBecomingNoisy(true);

        mediaSession = new MediaSession.Builder(this, player).build();
        handler.post(saveLoop);
    }

    private void saveCurrentPosition() {
        if (player == null) return;
        MediaItem item = player.getCurrentMediaItem();
        if (item == null || item.mediaId == null || item.mediaId.isEmpty()) return;
        long position = Math.max(0, player.getCurrentPosition());
        positions.edit().putLong(item.mediaId, position).apply();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(saveLoop);
        saveCurrentPosition();
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}

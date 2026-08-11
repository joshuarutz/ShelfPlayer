package com.example.shelfplayer;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity {
    private static final int OPEN_AUDIO_REQUEST = 1001;
    private static final int NOTIFICATION_REQUEST = 1002;

    private final List<Book> books = new ArrayList<>();
    private SharedPreferences prefs;
    private SharedPreferences positions;

    private LinearLayout libraryContainer;
    private TextView nowPlayingTitle;
    private TextView timeText;
    private Button playPauseButton;
    private SeekBar seekBar;

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController controller;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean userSeeking = false;

    private final Runnable uiLoop = new Runnable() {
        @Override
        public void run() {
            refreshPlayerUi();
            uiHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("library", MODE_PRIVATE);
        positions = getSharedPreferences("positions", MODE_PRIVATE);

        buildUi();
        loadBooks();
        renderLibrary();
        connectController();
        requestNotificationPermissionIfNeeded();
        uiHandler.post(uiLoop);
    }

    private void buildUi() {
        int pad = dp(18);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xFFF7F7F5);

        TextView title = new TextView(this);
        title.setText("ShelfPlayer");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(0xFF202124);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText("Your audiobooks, remembered exactly where you stopped.");
        subtitle.setTextSize(15);
        subtitle.setTextColor(0xFF5F6368);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(4);
        root.addView(subtitle, subParams);

        Button addButton = new Button(this);
        addButton.setText("+ Add audiobook");
        addButton.setAllCaps(false);
        addButton.setOnClickListener(v -> openAudioPicker());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addParams.topMargin = dp(14);
        root.addView(addButton, addParams);

        ScrollView scroll = new ScrollView(this);
        libraryContainer = new LinearLayout(this);
        libraryContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(libraryContainer);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = dp(8);
        root.addView(scroll, scrollParams);

        LinearLayout playerPanel = new LinearLayout(this);
        playerPanel.setOrientation(LinearLayout.VERTICAL);
        playerPanel.setPadding(dp(14), dp(12), dp(14), dp(12));
        playerPanel.setBackgroundColor(0xFFFFFFFF);

        nowPlayingTitle = new TextView(this);
        nowPlayingTitle.setText("Nothing playing");
        nowPlayingTitle.setTextSize(16);
        nowPlayingTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nowPlayingTitle.setTextColor(0xFF202124);
        nowPlayingTitle.setSingleLine(true);
        playerPanel.addView(nowPlayingTitle);

        seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) { userSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                userSeeking = false;
                if (controller != null && controller.getDuration() > 0) {
                    long newPos = (controller.getDuration() * bar.getProgress()) / 1000L;
                    controller.seekTo(newPos);
                    saveCurrentPosition();
                }
            }
        });
        playerPanel.addView(seekBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        timeText = new TextView(this);
        timeText.setText("0:00 / 0:00");
        timeText.setTextColor(0xFF5F6368);
        timeText.setGravity(Gravity.END);
        playerPanel.addView(timeText);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setOrientation(LinearLayout.HORIZONTAL);

        Button backButton = new Button(this);
        backButton.setText("-15s");
        backButton.setAllCaps(false);
        backButton.setOnClickListener(v -> skipBy(-15000));
        controls.addView(backButton);

        playPauseButton = new Button(this);
        playPauseButton.setText("Play");
        playPauseButton.setAllCaps(false);
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT);
        playParams.leftMargin = dp(8);
        playParams.rightMargin = dp(8);
        controls.addView(playPauseButton, playParams);

        Button forwardButton = new Button(this);
        forwardButton.setText("+30s");
        forwardButton.setAllCaps(false);
        forwardButton.setOnClickListener(v -> skipBy(30000));
        controls.addView(forwardButton);

        playerPanel.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(playerPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }

    private void connectController() {
        SessionToken token = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                controller.addListener(new Player.Listener() {
                    @Override public void onIsPlayingChanged(boolean isPlaying) { refreshPlayerUi(); }
                    @Override public void onMediaItemTransition(MediaItem mediaItem, int reason) { refreshPlayerUi(); }
                });
                runOnUiThread(this::refreshPlayerUi);
            } catch (ExecutionException | InterruptedException e) {
                runOnUiThread(() -> Toast.makeText(this, "Could not start audio player", Toast.LENGTH_LONG).show());
            }
        }, Runnable::run);
    }

    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, OPEN_AUDIO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != OPEN_AUDIO_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }

        String title = getDisplayName(uri);
        if (title == null || title.trim().isEmpty()) title = "Audiobook";

        boolean exists = false;
        for (Book book : books) {
            if (book.uri.equals(uri.toString())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            books.add(new Book(uri.toString(), title));
            saveBooks();
            renderLibrary();
        }
        playBook(uri.toString(), title);
    }

    private String getDisplayName(Uri uri) {
        ContentResolver resolver = getContentResolver();
        try (Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Exception ignored) {
        }
        return uri.getLastPathSegment();
    }

    private void playBook(String uriString, String title) {
        if (controller == null) {
            Toast.makeText(this, "Audio player is starting. Tap the book again.", Toast.LENGTH_SHORT).show();
            return;
        }
        saveCurrentPosition();
        long resumePosition = positions.getLong(uriString, 0L);

        MediaItem item = new MediaItem.Builder()
                .setMediaId(uriString)
                .setUri(Uri.parse(uriString))
                .setMediaMetadata(new MediaMetadata.Builder().setTitle(title).build())
                .build();

        controller.setMediaItem(item, resumePosition);
        controller.prepare();
        controller.play();
        refreshPlayerUi();
    }

    private void togglePlayPause() {
        if (controller == null || controller.getCurrentMediaItem() == null) return;
        if (controller.isPlaying()) controller.pause(); else controller.play();
        saveCurrentPosition();
        refreshPlayerUi();
    }

    private void skipBy(long millis) {
        if (controller == null || controller.getCurrentMediaItem() == null) return;
        long duration = controller.getDuration();
        long target = Math.max(0, controller.getCurrentPosition() + millis);
        if (duration > 0) target = Math.min(duration, target);
        controller.seekTo(target);
        saveCurrentPosition();
    }

    private void saveCurrentPosition() {
        if (controller == null) return;
        MediaItem item = controller.getCurrentMediaItem();
        if (item == null || item.mediaId == null || item.mediaId.isEmpty()) return;
        positions.edit().putLong(item.mediaId, Math.max(0, controller.getCurrentPosition())).apply();
    }

    private void refreshPlayerUi() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(this::refreshPlayerUi);
            return;
        }
        if (controller == null || controller.getCurrentMediaItem() == null) {
            nowPlayingTitle.setText("Nothing playing");
            playPauseButton.setText("Play");
            if (!userSeeking) seekBar.setProgress(0);
            timeText.setText("0:00 / 0:00");
            return;
        }

        MediaItem item = controller.getCurrentMediaItem();
        CharSequence title = item.mediaMetadata.title;
        nowPlayingTitle.setText(title == null ? "Audiobook" : title);
        playPauseButton.setText(controller.isPlaying() ? "Pause" : "Play");

        long pos = Math.max(0, controller.getCurrentPosition());
        long dur = Math.max(0, controller.getDuration());
        if (!userSeeking && dur > 0) seekBar.setProgress((int) Math.min(1000, (pos * 1000L) / dur));
        timeText.setText(formatTime(pos) + " / " + formatTime(dur));
    }

    private void renderLibrary() {
        libraryContainer.removeAllViews();
        if (books.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No audiobooks yet. Tap “Add audiobook” and choose an MP3, M4A, M4B, FLAC, OGG, WAV, or other audio file.");
            empty.setTextColor(0xFF5F6368);
            empty.setTextSize(15);
            empty.setPadding(0, dp(16), 0, dp(16));
            libraryContainer.addView(empty);
            return;
        }

        for (Book book : books) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackgroundColor(0xFFFFFFFF);

            Button bookButton = new Button(this);
            long saved = positions.getLong(book.uri, 0L);
            bookButton.setText(book.title + (saved > 0 ? "\nContinue at " + formatTime(saved) : "\nStart"));
            bookButton.setAllCaps(false);
            bookButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            bookButton.setOnClickListener(v -> playBook(book.uri, book.title));
            row.addView(bookButton, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(8);
            libraryContainer.addView(row, rowParams);
        }
    }

    private void loadBooks() {
        books.clear();
        String raw = prefs.getString("books", "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                books.add(new Book(obj.getString("uri"), obj.getString("title")));
            }
        } catch (JSONException ignored) {
        }
    }

    private void saveBooks() {
        JSONArray array = new JSONArray();
        try {
            for (Book book : books) {
                JSONObject obj = new JSONObject();
                obj.put("uri", book.uri);
                obj.put("title", book.title);
                array.put(obj);
            }
        } catch (JSONException ignored) {
        }
        prefs.edit().putString("books", array.toString()).apply();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        }
    }

    private String formatTime(long millis) {
        long totalSeconds = Math.max(0, millis / 1000);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
        return String.format("%d:%02d", minutes, seconds);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onPause() {
        saveCurrentPosition();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderLibrary();
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(uiLoop);
        saveCurrentPosition();
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        controller = null;
        super.onDestroy();
    }

    private static class Book {
        final String uri;
        final String title;
        Book(String uri, String title) {
            this.uri = uri;
            this.title = title;
        }
    }
}

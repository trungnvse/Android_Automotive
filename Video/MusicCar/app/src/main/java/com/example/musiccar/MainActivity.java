package com.example.musiccar;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    private Button btnPlayPause, btnNext, btnPrev;
    private Button btnTabMusic, btnTabVideo;
    private Button btnVolumeUp, btnVolumeDown;
    private TextView tvSongTitle, tvArtist, tvCurrentTime, tvTotalTime, tvVolumePercent;
    private SeekBar seekBar, seekBarVolume;

    // Volume lưu lại khi chuyển tab, không reset về 0.8 mỗi lần
    private float currentVolume = 0.8f;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isSeekBarTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Kết nối lại mỗi khi quay về từ VideoActivity
        if (mediaController == null) {
            connectToService();
        }
    }

    private void initViews() {
        btnPlayPause    = findViewById(R.id.btnPlayPause);
        btnNext         = findViewById(R.id.btnNext);
        btnPrev         = findViewById(R.id.btnPrev);
        btnTabMusic     = findViewById(R.id.btnTabMusic);
        btnTabVideo     = findViewById(R.id.btnTabVideo);
        btnVolumeUp     = findViewById(R.id.btnVolumeUp);
        btnVolumeDown   = findViewById(R.id.btnVolumeDown);
        tvSongTitle     = findViewById(R.id.tvSongTitle);
        tvArtist        = findViewById(R.id.tvArtist);
        tvCurrentTime   = findViewById(R.id.tvCurrentTime);
        tvTotalTime     = findViewById(R.id.tvTotalTime);
        tvVolumePercent = findViewById(R.id.tvVolumePercent);
        seekBar         = findViewById(R.id.seekBar);
        seekBarVolume   = findViewById(R.id.seekBarVolume);

        // Tab navigation
        btnTabMusic.setOnClickListener(v -> { /* đang ở đây rồi */ });
        btnTabVideo.setOnClickListener(v -> {
            // Pause nhạc trước khi sang tab Video
            if (mediaController != null && mediaController.isPlaying()) {
                mediaController.pause();
                btnPlayPause.setText("▶");
            }
            startActivity(new Intent(this, VideoActivity.class));
        });

        // Playback controls
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> {
            if (mediaController != null) mediaController.seekToNextMediaItem();
        });
        btnPrev.setOnClickListener(v -> {
            if (mediaController != null) mediaController.seekToPreviousMediaItem();
        });

        // SeekBar tiến trình nhạc
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar s) { isSeekBarTracking = true; }
            @Override
            public void onStopTrackingTouch(SeekBar s) {
                isSeekBarTracking = false;
                if (mediaController != null) mediaController.seekTo(s.getProgress());
            }
        });

        // Nút tăng/giảm volume
        btnVolumeUp.setOnClickListener(v -> adjustVolume(0.1f));
        btnVolumeDown.setOnClickListener(v -> adjustVolume(-0.1f));

        // SeekBar volume
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    currentVolume = progress / 100f;
                    if (mediaController != null) mediaController.setVolume(currentVolume);
                    tvVolumePercent.setText(progress + "%");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Hiển thị volume ban đầu
        syncVolumeUI();
    }

    /**
     * Đồng bộ UI volume với giá trị currentVolume hiện tại
     */
    private void syncVolumeUI() {
        int percent = (int) (currentVolume * 100);
        seekBarVolume.setProgress(percent);
        tvVolumePercent.setText(percent + "%");
    }

    private void adjustVolume(float delta) {
        currentVolume = Math.max(0f, Math.min(1f, currentVolume + delta));
        if (mediaController != null) mediaController.setVolume(currentVolume);
        syncVolumeUI();
    }

    private void connectToService() {
        SessionToken token = new SessionToken(
                this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                // Áp dụng volume đã lưu
                mediaController.setVolume(currentVolume);
                setupPlayerListener();
                updateUI();
                startProgressUpdater();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private void setupPlayerListener() {
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setText(isPlaying ? "⏸" : "▶");
            }
            @Override
            public void onMediaMetadataChanged(@NonNull MediaMetadata metadata) {
                updateUI();
            }
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                updateUI();
            }
        });
    }

    private void updateUI() {
        if (mediaController == null) return;
        MediaMetadata metadata = mediaController.getMediaMetadata();
        tvSongTitle.setText(metadata.title  != null ? metadata.title  : "Unknown Title");
        tvArtist.setText(metadata.artist != null ? metadata.artist : "Unknown Artist");
        long duration = mediaController.getDuration();
        if (duration > 0) {
            seekBar.setMax((int) duration);
            tvTotalTime.setText(formatTime((int) duration));
        }
        btnPlayPause.setText(mediaController.isPlaying() ? "⏸" : "▶");
        // Đồng bộ lại volume UI khi kết nối xong
        syncVolumeUI();
    }

    private void startProgressUpdater() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaController != null && !isSeekBarTracking) {
                    int pos = (int) mediaController.getCurrentPosition();
                    seekBar.setProgress(pos);
                    tvCurrentTime.setText(formatTime(pos));
                }
                handler.postDelayed(this, 500);
            }
        });
    }

    private void togglePlayPause() {
        if (mediaController == null) return;
        if (mediaController.isPlaying()) mediaController.pause();
        else mediaController.play();
    }

    private String formatTime(int millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
        mediaController = null;
    }
}
package com.example.musiccar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class VideoActivity extends AppCompatActivity
        implements VoiceCommandManager.VoiceCommandListener,
        WakeWordManager.WakeWordListener {

    // ── Player ────────────────────────────────────────────────────────────────
    private ExoPlayer videoPlayer;
    private PlayerView playerView;

    // ── Views ─────────────────────────────────────────────────────────────────
    private Button btnVideoPlayPause, btnVideoNext, btnVideoPrev;
    private Button btnTabMusic, btnTabVideo, btnMic;
    private Button btnVideoVolumeUp, btnVideoVolumeDown;
    private TextView tvVideoTitle, tvVideoArtist;
    private TextView tvVideoCurrentTime, tvVideoTotalTime, tvVideoVolumePercent;
    private TextView tvVoiceStatus;
    private SeekBar seekBarVideo, seekBarVideoVolume;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isSeekBarTracking = false;

    // ── Voice ─────────────────────────────────────────────────────────────────
    private VoiceCommandManager voiceManager;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        initViews();
        initVideoPlayer();
        initVolumeControl();
        initVoiceFeatures();
        startProgressUpdater();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Đăng ký listener — VideoActivity đang foreground
        WakeWordManager.getInstance().setListener(this);
        WakeWordManager.getInstance().resume();
        showStatus("Nói \"Hey MusicCar\" để điều khiển 🎙");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Không pause WakeWordManager — MainActivity sẽ setListener và tiếp tục
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
        if (voiceManager != null) voiceManager.stopListening();
        VideoPlayerManager.getInstance().pause();
        playerView.setPlayer(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceManager != null) voiceManager.destroy();
    }

    // ── Init views ────────────────────────────────────────────────────────────
    private void initViews() {
        playerView           = findViewById(R.id.playerView);
        btnVideoPlayPause    = findViewById(R.id.btnVideoPlayPause);
        btnVideoNext         = findViewById(R.id.btnVideoNext);
        btnVideoPrev         = findViewById(R.id.btnVideoPrev);
        btnTabMusic          = findViewById(R.id.btnTabMusic);
        btnTabVideo          = findViewById(R.id.btnTabVideo);
        btnVideoVolumeUp     = findViewById(R.id.btnVideoVolumeUp);
        btnVideoVolumeDown   = findViewById(R.id.btnVideoVolumeDown);
        btnMic               = findViewById(R.id.btnMic);
        tvVideoTitle         = findViewById(R.id.tvVideoTitle);
        tvVideoArtist        = findViewById(R.id.tvVideoArtist);
        tvVideoCurrentTime   = findViewById(R.id.tvVideoCurrentTime);
        tvVideoTotalTime     = findViewById(R.id.tvVideoTotalTime);
        tvVideoVolumePercent = findViewById(R.id.tvVideoVolumePercent);
        tvVoiceStatus        = findViewById(R.id.tvVoiceStatus);
        seekBarVideo         = findViewById(R.id.seekBarVideo);
        seekBarVideoVolume   = findViewById(R.id.seekBarVideoVolume);

        btnTabMusic.setOnClickListener(v -> finish());
        btnTabVideo.setOnClickListener(v -> { /* đang ở đây */ });

        btnVideoPlayPause.setOnClickListener(v -> togglePlayPause());
        btnVideoNext.setOnClickListener(v -> {
            if (videoPlayer.hasNextMediaItem()) videoPlayer.seekToNextMediaItem();
        });
        btnVideoPrev.setOnClickListener(v -> {
            if (videoPlayer.hasPreviousMediaItem()) videoPlayer.seekToPreviousMediaItem();
        });

        seekBarVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) tvVideoCurrentTime.setText(formatTime(p));
            }
            @Override public void onStartTrackingTouch(SeekBar s) { isSeekBarTracking = true; }
            @Override public void onStopTrackingTouch(SeekBar s) {
                isSeekBarTracking = false;
                if (videoPlayer.getDuration() > 0) videoPlayer.seekTo(s.getProgress());
            }
        });

        btnMic.setOnClickListener(v -> onMicClicked());
    }

    // ── Video player ──────────────────────────────────────────────────────────
    private void initVideoPlayer() {
        videoPlayer = VideoPlayerManager.getInstance().getPlayer(this);
        playerView.setPlayer(videoPlayer);
        playerView.setUseController(false);

        videoPlayer.addListener(new Player.Listener() {
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                btnVideoPlayPause.setText(isPlaying ? "⏸" : "▶");
            }
            @Override public void onMediaMetadataChanged(@NonNull MediaMetadata m) { updateVideoInfo(); }
            @Override public void onMediaItemTransition(MediaItem item, int reason) {
                seekBarVideo.setProgress(0);
                seekBarVideo.setMax(0);
                tvVideoCurrentTime.setText("0:00");
                tvVideoTotalTime.setText("0:00");
                updateVideoInfo();
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    long dur = videoPlayer.getDuration();
                    if (dur > 0) {
                        seekBarVideo.setMax((int) dur);
                        tvVideoTotalTime.setText(formatTime((int) dur));
                    }
                }
            }
            @Override public void onPlayerError(@NonNull PlaybackException e) {
                tvVideoTitle.setText("Lỗi phát video");
            }
        });

        updateVideoInfo();
        syncSeekBarWithCurrentPosition();
    }

    private void syncSeekBarWithCurrentPosition() {
        long dur = videoPlayer.getDuration();
        long pos = videoPlayer.getCurrentPosition();
        if (dur > 0) {
            seekBarVideo.setMax((int) dur);
            seekBarVideo.setProgress((int) pos);
            tvVideoTotalTime.setText(formatTime((int) dur));
            tvVideoCurrentTime.setText(formatTime((int) pos));
        }
    }

    private void updateVideoInfo() {
        MediaMetadata m = videoPlayer.getMediaMetadata();
        tvVideoTitle.setText(m.title  != null ? m.title  : "Unknown Title");
        tvVideoArtist.setText(m.artist != null ? m.artist : "Unknown Artist");
        long dur = videoPlayer.getDuration();
        if (dur > 0) { seekBarVideo.setMax((int) dur); tvVideoTotalTime.setText(formatTime((int) dur)); }
        btnVideoPlayPause.setText(videoPlayer.isPlaying() ? "⏸" : "▶");
    }

    // ── Volume ────────────────────────────────────────────────────────────────
    private void initVolumeControl() {
        int pct = (int)(videoPlayer.getVolume() * 100);
        seekBarVideoVolume.setProgress(pct);
        tvVideoVolumePercent.setText(pct + "%");

        btnVideoVolumeUp.setOnClickListener(v -> adjustVolume(0.1f));
        btnVideoVolumeDown.setOnClickListener(v -> adjustVolume(-0.1f));
        seekBarVideoVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) { videoPlayer.setVolume(p / 100f); tvVideoVolumePercent.setText(p + "%"); }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void adjustVolume(float delta) {
        float v = Math.max(0f, Math.min(1f, videoPlayer.getVolume() + delta));
        videoPlayer.setVolume(v);
        int p = (int)(v * 100);
        seekBarVideoVolume.setProgress(p);
        tvVideoVolumePercent.setText(p + "%");
    }

    // ── Voice features ────────────────────────────────────────────────────────
    private void initVoiceFeatures() {
        boolean hasPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        if (!hasPermission) {
            btnMic.setEnabled(false);
            showStatus("Chưa cấp quyền mic");
            return;
        }
        if (VoiceCommandManager.isAvailable(this)) {
            voiceManager = new VoiceCommandManager(this, this);
        }
        // Dùng Singleton — không tạo mới, chỉ setListener
        WakeWordManager.getInstance().setListener(this);
    }

    // ── Nút MIC thủ công ─────────────────────────────────────────────────────
    private void onMicClicked() {
        if (voiceManager == null) return;
        if (voiceManager.isListening()) {
            voiceManager.stopListening();
            return;
        }
        WakeWordManager.getInstance().pause();
        voiceManager.startListening();
    }

    // ── WakeWordListener ──────────────────────────────────────────────────────
    @Override
    public void onWakeWordDetected() {
        runOnUiThread(() -> {
            showStatus("👂 Hey MusicCar! Đang nghe lệnh...");
            WakeWordManager.getInstance().pause();
            if (voiceManager != null) voiceManager.startListening();
        });
    }

    @Override
    public void onWakeWordError(String error) {
        runOnUiThread(() -> showStatus("⚠ WakeWord: " + error));
    }

    // ── VoiceCommandListener ──────────────────────────────────────────────────
    @Override
    public void onListeningStarted() {
        btnMic.setText("⏹");
        btnMic.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFE94560));
        showStatus("🎙 Đang nghe lệnh...");
    }

    @Override
    public void onListeningStopped() {
        btnMic.setText("MIC");
        btnMic.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.argb(0x44, 0xFF, 0xFF, 0xFF)));
        handler.postDelayed(() -> {
            WakeWordManager.getInstance().resume();
            showStatus("Nói \"Hey MusicCar\" để điều khiển 🎙");
        }, 800);
    }

    @Override
    public void onRawTextReceived(String rawText) { showStatus(rawText); }

    @Override
    public void onCommandReceived(VoiceCommandManager.Command command) {
        switch (command) {
            case PLAY:
                videoPlayer.play();
                showStatusTimed("▶ Phát video");
                break;
            case PAUSE:
                videoPlayer.pause();
                showStatusTimed("⏸ Tạm dừng");
                break;
            case NEXT:
                if (videoPlayer.hasNextMediaItem()) videoPlayer.seekToNextMediaItem();
                showStatusTimed("⏭ Video tiếp theo");
                break;
            case PREVIOUS:
                if (videoPlayer.hasPreviousMediaItem()) videoPlayer.seekToPreviousMediaItem();
                showStatusTimed("⏮ Video trước");
                break;
            case VOLUME_UP:
                adjustVolume(0.2f);
                showStatusTimed("🔊 Âm lượng: " + (int)(videoPlayer.getVolume() * 100) + "%");
                break;
            case VOLUME_DOWN:
                adjustVolume(-0.2f);
                showStatusTimed("🔉 Âm lượng: " + (int)(videoPlayer.getVolume() * 100) + "%");
                break;
            case OPEN_MUSIC:
                showStatusTimed("🎵 Về tab Nhạc...");
                handler.postDelayed(this::finish, 500);
                break;
            case OPEN_VIDEO:
                showStatusTimed("🎬 Đang ở tab Video rồi");
                break;
            case UNKNOWN:
            default:
                showStatusTimed("❓ Không hiểu — thử: play, dừng, next");
                break;
        }
    }

    @Override
    public void onError(String errorMessage) {
        showStatusTimed("⚠ " + errorMessage);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void startProgressUpdater() {
        handler.post(new Runnable() {
            @Override public void run() {
                if (videoPlayer != null && !isSeekBarTracking) {
                    long dur = videoPlayer.getDuration();
                    int pos = (int) videoPlayer.getCurrentPosition();
                    if (dur > 0 && seekBarVideo.getMax() == 0) {
                        seekBarVideo.setMax((int) dur);
                        tvVideoTotalTime.setText(formatTime((int) dur));
                    }
                    seekBarVideo.setProgress(pos);
                    tvVideoCurrentTime.setText(formatTime(pos));
                }
                handler.postDelayed(this, 500);
            }
        });
    }

    private void togglePlayPause() {
        if (videoPlayer.isPlaying()) videoPlayer.pause();
        else videoPlayer.play();
    }

    private void showStatus(String msg) { tvVoiceStatus.setText(msg); }

    private void showStatusTimed(String msg) {
        tvVoiceStatus.setText(msg);
        handler.postDelayed(
                () -> showStatus("Nói \"Hey MusicCar\" để điều khiển 🎙"), 3000);
    }

    private String formatTime(int ms) {
        return String.format(Locale.getDefault(), "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(ms),
                TimeUnit.MILLISECONDS.toSeconds(ms) % 60);
    }
}
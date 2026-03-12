package com.example.musiccar;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements VoiceCommandManager.VoiceCommandListener,
        WakeWordManager.WakeWordListener {

    private static final int REQUEST_RECORD_AUDIO = 101;

    // ── Media ─────────────────────────────────────────────────────────────────
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    // ── Views ─────────────────────────────────────────────────────────────────
    private Button btnPlayPause, btnNext, btnPrev;
    private Button btnTabMusic, btnTabVideo, btnMic;
    private Button btnVolumeUp, btnVolumeDown;
    private TextView tvSongTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private TextView tvVolumePercent, tvVoiceStatus;
    private SeekBar seekBar, seekBarVolume;

    // ── State ─────────────────────────────────────────────────────────────────
    private float currentVolume = 0.8f;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isSeekBarTracking = false;

    // ── Voice ─────────────────────────────────────────────────────────────────
    private VoiceCommandManager voiceManager;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        checkPermissionAndInit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mediaController == null) connectToService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Đăng ký listener — MainActivity đang foreground
        WakeWordManager.getInstance().setListener(this);
        WakeWordManager.getInstance().resume();
        showStatus("Nói \"Hey MusicCar\" để điều khiển 🎙");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Không pause WakeWordManager ở đây!
        // VideoActivity sẽ setListener và tiếp tục dùng chung instance
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
        if (voiceManager != null) voiceManager.stopListening();
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        mediaController = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceManager != null) voiceManager.destroy();
        // Chỉ destroy WakeWordManager khi app đóng hẳn (isFinishing)
        if (isFinishing()) WakeWordManager.getInstance().destroy();
    }

    // ── Init views ────────────────────────────────────────────────────────────
    private void initViews() {
        btnPlayPause    = findViewById(R.id.btnPlayPause);
        btnNext         = findViewById(R.id.btnNext);
        btnPrev         = findViewById(R.id.btnPrev);
        btnTabMusic     = findViewById(R.id.btnTabMusic);
        btnTabVideo     = findViewById(R.id.btnTabVideo);
        btnVolumeUp     = findViewById(R.id.btnVolumeUp);
        btnVolumeDown   = findViewById(R.id.btnVolumeDown);
        btnMic          = findViewById(R.id.btnMic);
        tvSongTitle     = findViewById(R.id.tvSongTitle);
        tvArtist        = findViewById(R.id.tvArtist);
        tvCurrentTime   = findViewById(R.id.tvCurrentTime);
        tvTotalTime     = findViewById(R.id.tvTotalTime);
        tvVolumePercent = findViewById(R.id.tvVolumePercent);
        tvVoiceStatus   = findViewById(R.id.tvVoiceStatus);
        seekBar         = findViewById(R.id.seekBar);
        seekBarVolume   = findViewById(R.id.seekBarVolume);

        btnTabMusic.setOnClickListener(v -> { /* đang ở đây */ });
        btnTabVideo.setOnClickListener(v -> switchToVideo());

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> {
            if (mediaController != null) mediaController.seekToNextMediaItem();
        });
        btnPrev.setOnClickListener(v -> {
            if (mediaController != null) mediaController.seekToPreviousMediaItem();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) tvCurrentTime.setText(formatTime(p));
            }
            @Override public void onStartTrackingTouch(SeekBar s) { isSeekBarTracking = true; }
            @Override public void onStopTrackingTouch(SeekBar s) {
                isSeekBarTracking = false;
                if (mediaController != null) mediaController.seekTo(s.getProgress());
            }
        });

        btnVolumeUp.setOnClickListener(v -> adjustVolume(0.1f));
        btnVolumeDown.setOnClickListener(v -> adjustVolume(-0.1f));
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) {
                    currentVolume = p / 100f;
                    if (mediaController != null) mediaController.setVolume(currentVolume);
                    tvVolumePercent.setText(p + "%");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        syncVolumeUI();

        btnMic.setOnClickListener(v -> onMicClicked());
    }

    // ── Permission ────────────────────────────────────────────────────────────
    private void checkPermissionAndInit() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            initVoiceFeatures();
        } else {
            showStatus("Cần quyền microphone — đang xin...");
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initVoiceFeatures();
            } else {
                btnMic.setEnabled(false);
                showStatus("Chưa cấp quyền mic — tính năng giọng nói bị tắt");
                Toast.makeText(this,
                        "Vào Settings > Apps > MusicCar để cấp quyền Microphone",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initVoiceFeatures() {
        // SpeechRecognizer
        if (VoiceCommandManager.isAvailable(this)) {
            voiceManager = new VoiceCommandManager(this, this);
        } else {
            btnMic.setEnabled(false);
        }

        // WakeWordManager Singleton — init 1 lần duy nhất ở đây
        WakeWordManager wm = WakeWordManager.getInstance();
        wm.init(this);
        wm.setListener(this);
        wm.start();

        showStatus("Nói \"Hey MusicCar\" để điều khiển 🎙");
    }

    // ── Chuyển sang Video ─────────────────────────────────────────────────────
    private void switchToVideo() {
        // Dừng nhạc trước khi chuyển
        if (mediaController != null) {
            mediaController.pause();
            btnPlayPause.setText("▶");
        }
        startActivity(new Intent(this, VideoActivity.class));
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
                android.content.res.ColorStateList.valueOf(0xFF16213E));
        handler.postDelayed(() -> {
            WakeWordManager.getInstance().resume();
            showStatus("Nói \"Hey MusicCar\" để điều khiển 🎙");
        }, 800);
    }

    @Override
    public void onRawTextReceived(String rawText) {
        showStatus(rawText);
    }

    @Override
    public void onCommandReceived(VoiceCommandManager.Command command) {
        switch (command) {
            case PLAY:
                if (mediaController != null) mediaController.play();
                showStatusTimed("▶ Phát nhạc");
                break;
            case PAUSE:
                if (mediaController != null) mediaController.pause();
                showStatusTimed("⏸ Tạm dừng");
                break;
            case NEXT:
                if (mediaController != null) mediaController.seekToNextMediaItem();
                showStatusTimed("⏭ Bài tiếp theo");
                break;
            case PREVIOUS:
                if (mediaController != null) mediaController.seekToPreviousMediaItem();
                showStatusTimed("⏮ Bài trước");
                break;
            case VOLUME_UP:
                adjustVolume(0.2f);
                showStatusTimed("🔊 Âm lượng: " + (int)(currentVolume * 100) + "%");
                break;
            case VOLUME_DOWN:
                adjustVolume(-0.2f);
                showStatusTimed("🔉 Âm lượng: " + (int)(currentVolume * 100) + "%");
                break;
            case OPEN_VIDEO:
                showStatusTimed("🎬 Mở Video...");
                // Dừng nhạc TRƯỚC khi chuyển tab
                if (mediaController != null) mediaController.pause();
                handler.postDelayed(this::switchToVideo, 500);
                break;
            case OPEN_MUSIC:
                showStatusTimed("🎵 Đang ở tab Nhạc rồi");
                break;
            case UNKNOWN:
            default:
                showStatusTimed("❓ Không hiểu — thử: play, dừng, next, tăng âm lượng");
                break;
        }
    }

    @Override
    public void onError(String errorMessage) {
        showStatusTimed("⚠ " + errorMessage);
    }

    // ── Status helpers ────────────────────────────────────────────────────────
    private void showStatus(String msg) { tvVoiceStatus.setText(msg); }

    private void showStatusTimed(String msg) {
        tvVoiceStatus.setText(msg);
        handler.postDelayed(
                () -> showStatus("Nói \"Hey MusicCar\" để điều khiển 🎙"), 3000);
    }

    // ── Media helpers ─────────────────────────────────────────────────────────
    private void connectToService() {
        SessionToken token = new SessionToken(
                this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
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
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setText(isPlaying ? "⏸" : "▶");
            }
            @Override public void onMediaMetadataChanged(@NonNull MediaMetadata m) { updateUI(); }
            @Override public void onMediaItemTransition(MediaItem item, int reason) { updateUI(); }
        });
    }

    private void updateUI() {
        if (mediaController == null) return;
        MediaMetadata m = mediaController.getMediaMetadata();
        tvSongTitle.setText(m.title  != null ? m.title  : "Unknown Title");
        tvArtist.setText(m.artist != null ? m.artist : "Unknown Artist");
        long dur = mediaController.getDuration();
        if (dur > 0) { seekBar.setMax((int) dur); tvTotalTime.setText(formatTime((int) dur)); }
        btnPlayPause.setText(mediaController.isPlaying() ? "⏸" : "▶");
        syncVolumeUI();
    }

    private void startProgressUpdater() {
        handler.post(new Runnable() {
            @Override public void run() {
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

    private void adjustVolume(float delta) {
        currentVolume = Math.max(0f, Math.min(1f, currentVolume + delta));
        if (mediaController != null) mediaController.setVolume(currentVolume);
        syncVolumeUI();
    }

    private void syncVolumeUI() {
        int p = (int)(currentVolume * 100);
        seekBarVolume.setProgress(p);
        tvVolumePercent.setText(p + "%");
    }

    private String formatTime(int ms) {
        return String.format(Locale.getDefault(), "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(ms),
                TimeUnit.MILLISECONDS.toSeconds(ms) % 60);
    }
}
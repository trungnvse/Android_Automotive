package com.example.musiccar;

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
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class VideoActivity extends AppCompatActivity {

    private ExoPlayer videoPlayer;
    private PlayerView playerView;

    private Button btnVideoPlayPause, btnVideoNext, btnVideoPrev;
    private Button btnTabMusic, btnTabVideo;
    private Button btnVideoVolumeUp, btnVideoVolumeDown;
    private TextView tvVideoTitle, tvVideoArtist;
    private TextView tvVideoCurrentTime, tvVideoTotalTime, tvVideoVolumePercent;
    private SeekBar seekBarVideo, seekBarVideoVolume;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isSeekBarTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        initViews();
        initVideoPlayer();
        initVolumeControl();
        startProgressUpdater();
    }

    private void initViews() {
        playerView           = findViewById(R.id.playerView);
        btnVideoPlayPause    = findViewById(R.id.btnVideoPlayPause);
        btnVideoNext         = findViewById(R.id.btnVideoNext);
        btnVideoPrev         = findViewById(R.id.btnVideoPrev);
        btnTabMusic          = findViewById(R.id.btnTabMusic);
        btnTabVideo          = findViewById(R.id.btnTabVideo);
        btnVideoVolumeUp     = findViewById(R.id.btnVideoVolumeUp);
        btnVideoVolumeDown   = findViewById(R.id.btnVideoVolumeDown);
        tvVideoTitle         = findViewById(R.id.tvVideoTitle);
        tvVideoArtist        = findViewById(R.id.tvVideoArtist);
        tvVideoCurrentTime   = findViewById(R.id.tvVideoCurrentTime);
        tvVideoTotalTime     = findViewById(R.id.tvVideoTotalTime);
        tvVideoVolumePercent = findViewById(R.id.tvVideoVolumePercent);
        seekBarVideo         = findViewById(R.id.seekBarVideo);
        seekBarVideoVolume   = findViewById(R.id.seekBarVideoVolume);

        // Tab navigation
        btnTabMusic.setOnClickListener(v -> finish());
        btnTabVideo.setOnClickListener(v -> { /* đang ở đây rồi */ });

        // Playback controls
        btnVideoPlayPause.setOnClickListener(v -> togglePlayPause());
        btnVideoNext.setOnClickListener(v -> {
            if (videoPlayer.hasNextMediaItem()) videoPlayer.seekToNextMediaItem();
        });
        btnVideoPrev.setOnClickListener(v -> {
            if (videoPlayer.hasPreviousMediaItem()) videoPlayer.seekToPreviousMediaItem();
        });

        // SeekBar tiến trình video
        seekBarVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) tvVideoCurrentTime.setText(formatTime(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar s) { isSeekBarTracking = true; }
            @Override
            public void onStopTrackingTouch(SeekBar s) {
                isSeekBarTracking = false;
                if (videoPlayer.getDuration() > 0) {
                    videoPlayer.seekTo(s.getProgress());
                }
            }
        });
    }

    private void initVideoPlayer() {
        // Lấy player từ Singleton — giữ nguyên trạng thái từ lần trước
        videoPlayer = VideoPlayerManager.getInstance().getPlayer(this);

        // Gắn vào PlayerView để hiển thị video
        playerView.setPlayer(videoPlayer);
        playerView.setUseController(false);

        // Lắng nghe sự kiện
        videoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnVideoPlayPause.setText(isPlaying ? "⏸" : "▶");
            }
            @Override
            public void onMediaMetadataChanged(@NonNull MediaMetadata metadata) {
                updateVideoInfo();
            }
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                seekBarVideo.setProgress(0);
                seekBarVideo.setMax(0);
                tvVideoCurrentTime.setText("0:00");
                tvVideoTotalTime.setText("0:00");
                updateVideoInfo();
            }
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long duration = videoPlayer.getDuration();
                    if (duration > 0) {
                        seekBarVideo.setMax((int) duration);
                        tvVideoTotalTime.setText(formatTime((int) duration));
                    }
                }
            }
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                tvVideoTitle.setText("Lỗi phát video");
            }
        });

        // Cập nhật UI ngay với trạng thái hiện tại (giữ từ lần trước)
        updateVideoInfo();
        syncSeekBarWithCurrentPosition();
    }

    /**
     * Đồng bộ SeekBar với vị trí hiện tại của player
     * (quan trọng khi quay lại từ tab nhạc)
     */
    private void syncSeekBarWithCurrentPosition() {
        long duration = videoPlayer.getDuration();
        long position = videoPlayer.getCurrentPosition();
        if (duration > 0) {
            seekBarVideo.setMax((int) duration);
            seekBarVideo.setProgress((int) position);
            tvVideoTotalTime.setText(formatTime((int) duration));
            tvVideoCurrentTime.setText(formatTime((int) position));
        }
    }

    private void updateVideoInfo() {
        MediaMetadata meta = videoPlayer.getMediaMetadata();
        tvVideoTitle.setText(meta.title  != null ? meta.title  : "Unknown Title");
        tvVideoArtist.setText(meta.artist != null ? meta.artist : "Unknown Artist");
        long duration = videoPlayer.getDuration();
        if (duration > 0) {
            seekBarVideo.setMax((int) duration);
            tvVideoTotalTime.setText(formatTime((int) duration));
        }
        btnVideoPlayPause.setText(videoPlayer.isPlaying() ? "⏸" : "▶");
    }

    private void initVolumeControl() {
        // Lấy volume hiện tại từ player (giữ từ lần trước)
        float vol = videoPlayer.getVolume();
        int initPercent = (int) (vol * 100);
        seekBarVideoVolume.setProgress(initPercent);
        tvVideoVolumePercent.setText(initPercent + "%");

        btnVideoVolumeUp.setOnClickListener(v -> adjustVolume(0.1f));
        btnVideoVolumeDown.setOnClickListener(v -> adjustVolume(-0.1f));

        seekBarVideoVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    float newVol = progress / 100f;
                    videoPlayer.setVolume(newVol);
                    tvVideoVolumePercent.setText(progress + "%");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void adjustVolume(float delta) {
        float current = videoPlayer.getVolume();
        float newVol  = Math.max(0f, Math.min(1f, current + delta));
        videoPlayer.setVolume(newVol);
        int percent = (int) (newVol * 100);
        seekBarVideoVolume.setProgress(percent);
        tvVideoVolumePercent.setText(percent + "%");
    }

    private void startProgressUpdater() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (videoPlayer != null && !isSeekBarTracking) {
                    long duration = videoPlayer.getDuration();
                    int pos = (int) videoPlayer.getCurrentPosition();
                    if (duration > 0 && seekBarVideo.getMax() == 0) {
                        seekBarVideo.setMax((int) duration);
                        tvVideoTotalTime.setText(formatTime((int) duration));
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

    private String formatTime(int millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
        // Chỉ tạm dừng, KHÔNG release — giữ trạng thái cho lần quay lại
        VideoPlayerManager.getInstance().pause();
        // Gỡ player khỏi PlayerView để tránh memory leak
        playerView.setPlayer(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Không release player ở đây — Singleton quản lý vòng đời
    }
}
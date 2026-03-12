package com.example.musiccar;

import android.content.Context;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton giữ ExoPlayer sống trong suốt vòng đời app.
 * Giúp video không bị reset về đầu khi chuyển tab rồi quay lại.
 */
public class VideoPlayerManager {

    private static VideoPlayerManager instance;
    private ExoPlayer player;

    private VideoPlayerManager() {}

    public static VideoPlayerManager getInstance() {
        if (instance == null) {
            instance = new VideoPlayerManager();
        }
        return instance;
    }

    /**
     * Lấy player hiện tại. Nếu chưa có hoặc đã bị release thì tạo mới.
     */
    public ExoPlayer getPlayer(Context context) {
        if (player == null) {
            player = new ExoPlayer.Builder(context.getApplicationContext()).build();
            player.setMediaItems(buildVideoItems(context));
            player.prepare();
            player.setVolume(0.8f);
        }
        return player;
    }

    /**
     * Tạm dừng video (gọi khi rời VideoActivity).
     */
    public void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    /**
     * Giải phóng hoàn toàn (gọi khi app bị đóng hoàn toàn).
     */
    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private List<MediaItem> buildVideoItems(Context context) {
        List<MediaItem> items = new ArrayList<>();
        int[] rawIds   = { R.raw.castle, R.raw.giu_anh };
        String[] titles  = { "Castle on the Hill", "Giữ Anh Cho Ngày Hôm Qua" };
        String[] artists = { "Ed Sheeran", "Hoàng Dũng" };

        for (int i = 0; i < rawIds.length; i++) {
            String uri = "android.resource://" + context.getPackageName() + "/" + rawIds[i];
            items.add(new MediaItem.Builder()
                    .setMediaId("video_" + i)
                    .setUri(uri)
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setTitle(titles[i])
                            .setArtist(artists[i])
                            .setIsPlayable(true)
                            .build())
                    .build());
        }
        return items;
    }
}

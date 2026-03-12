package com.example.musiccar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends MediaLibraryService {

    private ExoPlayer player;
    private MediaLibrarySession mediaLibrarySession;

    @Override
    public void onCreate() {
        super.onCreate();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build();

        player.setMediaItems(buildMediaItems());
        player.prepare();

        mediaLibrarySession = new MediaLibrarySession.Builder(
                this, player, new CustomCallback())
                .build();
    }

    private List<MediaItem> buildMediaItems() {
        List<MediaItem> items = new ArrayList<>();

        int[] rawIds = { R.raw.canon, R.raw.dongthoai };
        String[] titles = { "Canon In D Major", "Đồng Thoại" };
        String[] artists = { "Various Artists", "Quang Lương" };

        for (int i = 0; i < rawIds.length; i++) {
            String uri = "android.resource://" + getPackageName() + "/" + rawIds[i];
            MediaItem item = new MediaItem.Builder()
                    .setMediaId("song_" + i)
                    .setUri(uri)
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setTitle(titles[i])
                            .setArtist(artists[i])
                            .setIsPlayable(true)
                            .build())
                    .build();
            items.add(item);
        }
        return items;
    }

    @Nullable
    @Override
    public MediaLibrarySession onGetSession(@NonNull MediaSession.ControllerInfo info) {
        return mediaLibrarySession;
    }

    @Override
    public void onDestroy() {
        mediaLibrarySession.release();
        player.release();
        super.onDestroy();
    }

    private class CustomCallback implements MediaLibrarySession.Callback {

        @NonNull
        @Override
        public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo browser,
                @Nullable LibraryParams params) {
            MediaItem root = new MediaItem.Builder()
                    .setMediaId("root")
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setIsPlayable(false)
                            .setIsBrowsable(true)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setTitle("Root")
                            .build())
                    .build();
            return Futures.immediateFuture(LibraryResult.ofItem(root, null));
        }

        @NonNull
        @Override
        public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
                @NonNull MediaLibrarySession session,
                @NonNull MediaSession.ControllerInfo browser,
                @NonNull String parentId,
                int page,
                int pageSize,
                @Nullable LibraryParams params) {
            return Futures.immediateFuture(
                    LibraryResult.ofItemList(
                            ImmutableList.copyOf(buildMediaItems()), null));
        }
    }
}

# 🚗 MusicCar v2.0 — Android Automotive Media Player

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20Automotive-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Min%20SDK-API%2035-4285F4?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Media3-1.5.1-A142F4?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Version-2.0-E94560?style=for-the-badge"/>
</p>

<p align="center">
  Ứng dụng media đầy đủ cho Android Automotive OS — phát <strong>nhạc</strong> và <strong>video</strong><br/>
  với điều khiển <strong>âm lượng</strong>, chuyển tab thông minh và <strong>giữ trạng thái video</strong>.
</p>

---

## 📋 Mục lục
- [Nâng cấp từ v1.0](#-nâng-cấp-từ-v10)
- [Tính năng đầy đủ](#-tính-năng-đầy-đủ)
- [Giao diện](#-giao-diện)
- [Kiến trúc](#-kiến-trúc)
- [Cấu trúc thư mục](#-cấu-trúc-thư-mục)
- [Giải thích chi tiết từng file](#-giải-thích-chi-tiết-từng-file)
- [Luồng chuyển tab](#-luồng-chuyển-tab-thông-minh)
- [Các vấn đề đã giải quyết](#-các-vấn-đề-đã-giải-quyết)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)

---

## 🆙 Nâng cấp từ v1.0

| Tính năng | v1.0 | v2.0 |
|-----------|------|------|
| Phát nhạc | ✅ | ✅ |
| Phát video | ❌ | ✅ |
| Tab chuyển đổi Nhạc/Video | ❌ | ✅ |
| Điều chỉnh âm lượng | ❌ | ✅ |
| Giữ trạng thái video khi đổi tab | ❌ | ✅ |
| Nhạc tự pause khi sang Video | ❌ | ✅ |
| Video tự pause khi sang Nhạc | ❌ | ✅ |
| Layout 2 cột (không cần cuộn) | ❌ | ✅ |
| Video full screen overlay | ❌ | ✅ |
| Singleton VideoPlayerManager | ❌ | ✅ |

---

## ✨ Tính năng đầy đủ

### 🎵 Tab Nhạc
| Tính năng | Chi tiết |
|-----------|----------|
| ▶ / ⏸ Play / Pause | Phát hoặc tạm dừng |
| ⏮ ⏭ Chuyển bài | Bài trước / bài tiếp theo |
| 📊 SeekBar tiến trình | Tua nhạc, cập nhật mỗi 500ms |
| ⏱ Thời gian | Vị trí hiện tại / tổng thời lượng |
| 🎤 Metadata | Tên bài & nghệ sĩ tự cập nhật |
| 🔊 Volume SeekBar | Kéo để điều chỉnh 0–100% |
| ➕➖ Nút Volume | Tăng/giảm 10% mỗi lần nhấn |
| 📺 Chuyển Video | Nhạc tự pause trước khi sang tab Video |
| 🔇 Phát nền | Tiếp tục khi đóng app |

### 🎬 Tab Video
| Tính năng | Chi tiết |
|-----------|----------|
| 📺 Phát video full screen | PlayerView chiếm toàn màn hình |
| ▶ / ⏸ Play / Pause | Nút overlay trong suốt |
| ⏮ ⏭ Chuyển video | Bài trước / bài tiếp theo |
| 📊 SeekBar tiến trình | Hoạt động sau khi `STATE_READY` |
| ⏱ Thời gian | Vị trí hiện tại / tổng thời lượng |
| 🔊 Volume riêng | SeekBar + nút -/+ độc lập với tab Nhạc |
| 💾 Giữ trạng thái | Quay lại tiếp tục từ vị trí cũ |
| 🎵 Chuyển Nhạc | Video tự pause khi quay về tab Nhạc |

### 🔄 Chuyển tab thông minh
| Hành động | Kết quả |
|-----------|---------|
| Nhạc đang phát → sang Video | Nhạc **pause**, Video sẵn sàng |
| Video đang phát → về Nhạc | Video **pause**, giữ vị trí |
| Quay lại Video | Tiếp tục từ **đúng điểm** đã dừng |
| Quay lại Nhạc | Nhạc sẵn sàng để play lại |

---

## 🖥 Giao diện

### Tab Nhạc — Layout 2 cột landscape

```
┌─────────────────────────────────────────────────────────────┐
│  🎵 NHẠC  [active]              🎬 VIDEO                    │  48dp
├──────────────────────────────┬──────────────────────────────┤
│                              │                              │
│  Canon In D Major            │  🔊  ÂM LƯỢNG               │
│  Various Artists             │                              │
│                              │  ━━━━━●━━━━━━━━━━━━━━━━━    │
│  ━━━━━━━━━━●━━━━━━━━━━━━━    │                              │
│  0:21                 3:40   │    [ - ]    80%    [ + ]     │
│                              │                              │
│  [ ⏮ ]   [ ▶ ]   [ ⏭ ]     │                              │
│                              │                              │
└──────────────────────────────┴──────────────────────────────┘
    Cột trái: weight=1              Cột phải: weight=0.6
```

### Tab Video — Full screen overlay

```
┌─────────────────────────────────────────────────────────────┐
│  🎵 NHẠC          🎬 VIDEO [active]        ← overlay #AA   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                                                             │
│              🎬  Video đang phát                            │
│           (PlayerView — match_parent)                       │
│                                                             │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  Castle on the Hill                        Ed Sheeran       │
│  ━━━━━●━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│  0:04                                               4:12   │
│  [⏮]  [▶]  [⏭]   VOL  [-] ━━━●━━━━━━ 80% [+]            │
└─────────────────────────────────────────────────────────────┘
                    ← overlay #CC000000 →
```

**Màu sắc:**
- Background: `#1A1A2E` / `#000000`
- Tab bar overlay: `#AA000000` (67% opacity)
- Control panel overlay: `#CC000000` (80% opacity)
- Accent: `#E94560`
- Nút phụ: `#44FFFFFF` (27% opacity)

---

## 🏗 Kiến trúc

### Tổng quan 4 thành phần

```
┌─────────────────────────────────────────────────────────────────┐
│  PROCESS: com.example.musiccar                                   │
│                                                                  │
│  ┌─────────────────┐  Binder IPC  ┌──────────────────────────┐  │
│  │  MainActivity   │◄────────────►│      MusicService        │  │
│  │  (Music Client) │              │      (Music Server)      │  │
│  │                 │              │  ExoPlayer (audio)       │  │
│  │  MediaController│              │  MediaLibrarySession     │  │
│  └────────┬────────┘              └──────────────────────────┘  │
│           │ startActivity()                                      │
│           ▼                                                      │
│  ┌─────────────────┐  getPlayer() ┌──────────────────────────┐  │
│  │  VideoActivity  │◄────────────►│   VideoPlayerManager     │  │
│  │  (Video Client) │              │   (Singleton)            │  │
│  │                 │              │   ExoPlayer (video)      │  │
│  │  PlayerView     │              │   Giữ trạng thái mãi mãi │  │
│  └─────────────────┘              └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Singleton Pattern cho VideoPlayerManager

```
Lần 1: VideoActivity mở
VideoPlayerManager.getInstance()
    │ instance == null
    ▼
Tạo mới ExoPlayer → load videos → prepare()
    │
    ▼
Phát video tại position = 0

Lần 2: Chuyển về tab Nhạc
VideoActivity.onStop()
    │ VideoPlayerManager.pause()   ← chỉ pause, KHÔNG release
    │ playerView.setPlayer(null)   ← gỡ View, giữ Player
    ▼
ExoPlayer vẫn sống, position = 1:23 (đã được lưu ngầm)

Lần 3: Quay lại tab Video
VideoPlayerManager.getInstance()
    │ instance != null  ← CÙNG INSTANCE CŨ
    ▼
playerView.setPlayer(videoPlayer)  ← gắn lại View
syncSeekBarWithCurrentPosition()   ← SeekBar = 1:23
    │
    ▼
Tiếp tục từ 1:23 ✓
```

---

## 📁 Cấu trúc thư mục

```
MusicCar/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/musiccar/
│   │   │   ├── MainActivity.java          # Tab Nhạc — Client + Volume
│   │   │   ├── MusicService.java          # Server phát nhạc (không đổi từ v1)
│   │   │   ├── VideoActivity.java         # Tab Video — phát video
│   │   │   └── VideoPlayerManager.java    # Singleton giữ ExoPlayer video
│   │   │
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml      # Layout 2 cột ngang
│   │   │   │   └── activity_video.xml     # Layout full screen overlay
│   │   │   ├── raw/
│   │   │   │   ├── canon.mp3
│   │   │   │   ├── dongthoai.mp3
│   │   │   │   ├── castle.mp4             # Castle on the Hill - Ed Sheeran
│   │   │   │   └── giu_anh.mp4            # Giữ Anh Cho Ngày Hôm Qua - Hoàng Dũng
│   │   │   └── values/
│   │   │       └── strings.xml
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   └── build.gradle.kts
│
└── README.md
```

---

## 🔍 Giải thích chi tiết từng file

### 📄 `AndroidManifest.xml`

```xml
<!-- Theme NoActionBar → ẩn thanh tiêu đề "MusicCar" -->
<application android:theme="@style/Theme.AppCompat.DayNight.NoActionBar">

    <!-- MainActivity: Màn hình chính -->
    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN"/>
            <category android:name="android.intent.category.LAUNCHER"/>
        </intent-filter>
    </activity>

    <!-- VideoActivity: Màn hình video, cố định landscape -->
    <activity
        android:name=".VideoActivity"
        android:exported="false"
        android:screenOrientation="landscape"/>

    <!-- MusicService: exported=true để Automotive OS kết nối -->
    <service android:name=".MusicService"
             android:exported="true"
             android:foregroundServiceType="mediaPlayback">
        <intent-filter>
            <action android:name="androidx.media3.session.MediaSessionService"/>
            <action android:name="android.media.browse.MediaBrowserService"/>
        </intent-filter>
    </service>
</application>
```

---

### 📄 `build.gradle.kts`

```kotlin
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    // Engine phát audio + video: MP3/MP4 → decode → AudioTrack/Surface

    implementation("androidx.media3:media3-session:1.5.1")
    // MediaSession: giao thức chuẩn, bắt buộc cho Automotive OS

    implementation("androidx.media3:media3-common:1.5.1")
    // MediaItem, MediaMetadata, Player interface dùng chung

    implementation("androidx.media3:media3-ui:1.5.1")
    // PlayerView: Surface hiển thị video, thêm ở v2.0

    implementation("com.google.android.material:material:1.12.0")
}
```

---

### 📄 `MusicService.java` — Không thay đổi từ v1.0

Service này ổn định từ v1.0 và không cần sửa đổi. Đây là minh chứng cho kiến trúc tốt — khi thêm tính năng video, phần nhạc không bị ảnh hưởng.

```
MusicService (Foreground Service — chạy độc lập với UI)
├── ExoPlayer
│   ├── AudioAttributes: USAGE_MEDIA + CONTENT_TYPE_MUSIC
│   ├── setAudioAttributes(..., true) → tự quản lý audio focus
│   └── setHandleAudioBecomingNoisy(true) → pause khi rút tai nghe
├── Playlist: canon.mp3, dongthoai.mp3
└── MediaLibrarySession
    ├── onGetLibraryRoot() → Root folder
    └── onGetChildren()   → Danh sách 2 bài nhạc
```

---

### 📄 `VideoPlayerManager.java` — Singleton mới ở v2.0

```java
public class VideoPlayerManager {
    private static VideoPlayerManager instance;  // Chỉ 1 instance duy nhất
    private ExoPlayer player;

    public static VideoPlayerManager getInstance() {
        if (instance == null) {
            instance = new VideoPlayerManager();
        }
        return instance;  // Luôn trả về cùng 1 object
    }

    public ExoPlayer getPlayer(Context context) {
        if (player == null) {
            // Chỉ tạo ExoPlayer 1 lần duy nhất
            player = new ExoPlayer.Builder(context.getApplicationContext()).build();
            player.setMediaItems(buildVideoItems(context));
            player.prepare();
            player.setVolume(0.8f);
        }
        return player;  // Lần sau trả về player cũ với trạng thái cũ
    }

    public void pause() {
        if (player != null && player.isPlaying()) player.pause();
        // Không release — giữ nguyên position, playlist, trạng thái
    }
}
```

**Tại sao dùng `context.getApplicationContext()`?**

```
Activity context:              Application context:
────────────────────           ────────────────────────────
Gắn với vòng đời Activity      Gắn với vòng đời Application
Activity destroy → leak        Application destroy → an toàn
❌ Dùng cho Singleton          ✅ Dùng cho Singleton
```

---

### 📄 `VideoActivity.java`

```
VideoActivity
│
├── onCreate()
│   ├── initViews() → ánh xạ 14 views
│   ├── initVideoPlayer()
│   │   ├── videoPlayer = VideoPlayerManager.getInstance().getPlayer(this)
│   │   │   └── Lấy player cũ nếu đã có, tạo mới nếu chưa có
│   │   ├── playerView.setPlayer(videoPlayer) → gắn vào Surface
│   │   ├── playerView.setUseController(false) → dùng controls tự làm
│   │   └── addListener()
│   │       ├── onPlaybackStateChanged(STATE_READY) → set seekBar.max = duration
│   │       └── onMediaItemTransition() → reset SeekBar khi đổi video
│   │
│   ├── initVolumeControl()
│   │   ├── Đọc volume từ videoPlayer.getVolume() ← giữ từ lần trước
│   │   ├── btnVolumeUp/Down → adjustVolume(±0.1f)
│   │   └── seekBarVideoVolume → videoPlayer.setVolume(progress/100f)
│   │
│   └── startProgressUpdater()
│       └── Handler 500ms: setProgress(getCurrentPosition())
│
└── onStop()
    ├── handler.removeCallbacksAndMessages(null)
    ├── VideoPlayerManager.getInstance().pause() ← chỉ pause
    └── playerView.setPlayer(null) ← gỡ View, tránh memory leak
    // KHÔNG gọi videoPlayer.release() — Singleton quản lý
```

**Tại sao `playerView.setPlayer(null)` trong `onStop()`?**

```
Surface (màn hình render video) gắn với Activity.
Khi Activity vào nền nhưng player vẫn giữ Surface:
→ Memory leak: Surface không được giải phóng
→ Khi Activity quay lại, Surface mới tạo xung đột với Surface cũ

Giải pháp:
onStop():  playerView.setPlayer(null)  → gỡ Surface, giữ Player
onStart(): playerView.setPlayer(player) → gắn Surface mới
```

---

### 📄 `MainActivity.java` — Nâng cấp từ v1.0

**Thêm mới so với v1.0:**

```java
// 1. Pause nhạc trước khi chuyển sang Video
btnTabVideo.setOnClickListener(v -> {
    if (mediaController != null && mediaController.isPlaying()) {
        mediaController.pause();    // ← THÊM MỚI
        btnPlayPause.setText("▶");
    }
    startActivity(new Intent(this, VideoActivity.class));
});

// 2. Tái kết nối khi quay từ VideoActivity về
@Override
protected void onStart() {
    super.onStart();
    if (mediaController == null) {  // ← THÊM MỚI
        connectToService();
    }
}

// 3. Volume control bằng ExoPlayer (không dùng AudioManager)
private float currentVolume = 0.8f;

private void adjustVolume(float delta) {
    currentVolume = Math.max(0f, Math.min(1f, currentVolume + delta));
    if (mediaController != null) mediaController.setVolume(currentVolume);
    syncVolumeUI();
}
```

**Tại sao dùng `ExoPlayer.setVolume()` thay vì `AudioManager`?**

```
AudioManager.setStreamVolume() trên Automotive emulator:
→ Bị restrict bởi Car Audio Service
→ Không có tác dụng trong nhiều trường hợp
→ Cần permission đặc biệt của system

ExoPlayer.setVolume(float):
→ Điều chỉnh software gain trực tiếp
→ Hoạt động 100% trên mọi môi trường
→ Range: 0.0f (câm) → 1.0f (tối đa)
→ Không cần permission
```

---

### 📄 `activity_main.xml` — Layout 2 cột

```
LinearLayout (vertical)
├── Tab bar (48dp)
│   ├── Button btnTabMusic  [active: #E94560]
│   └── Button btnTabVideo  [inactive: #AAAAAA]
│
└── LinearLayout (horizontal, layout_weight=1)
    ├── LinearLayout CỘT TRÁI (weight=1) — Nhạc
    │   ├── TextView tvSongTitle
    │   ├── TextView tvArtist
    │   ├── SeekBar seekBar
    │   ├── LinearLayout → tvCurrentTime + tvTotalTime
    │   └── LinearLayout → btnPrev + btnPlayPause + btnNext
    │
    ├── View (divider 1dp)
    │
    └── LinearLayout CỘT PHẢI (weight=0.6) — Volume
        ├── TextView "🔊 ÂM LƯỢNG"
        ├── SeekBar seekBarVolume
        └── LinearLayout → btnVolumeDown + tvVolumePercent + btnVolumeUp
```

**Tại sao `weight=0.6` cho cột phải?**

```
Tổng width = match_parent
Cột trái:  weight=1   → 1/(1+0.6) = 62.5% màn hình
Cột phải:  weight=0.6 → 0.6/(1+0.6) = 37.5% màn hình
→ Nhạc có nhiều không gian hơn, volume gọn ở bên phải
```

---

### 📄 `activity_video.xml` — Full screen overlay

```
RelativeLayout (match_parent, #000000)
│
├── PlayerView (match_parent)                    ← LỚP ĐÁY: video full screen
│
├── Tab bar (alignParentTop, #AA000000)          ← LỚP GIỮA: overlay 67% opacity
│   ├── Button btnTabMusic
│   └── Button btnTabVideo [active]
│
└── Control panel (alignParentBottom, #CC000000) ← LỚP TRÊN: overlay 80% opacity
    ├── tvVideoTitle + tvVideoArtist (hàng ngang)
    ├── SeekBar seekBarVideo
    ├── tvVideoCurrentTime + tvVideoTotalTime
    └── LinearLayout (horizontal)
        ├── [⏮] [▶] [⏭]
        ├── [Divider]
        └── VOL [-] ━━●━━ 80% [+]
```

**Tại sao dùng `RelativeLayout` thay vì `LinearLayout`?**

```
LinearLayout: xếp lần lượt, không thể xếp chồng
→ PlayerView + TabBar + Controls phải xếp dọc
→ Video chỉ chiếm 1/3 màn hình ❌

RelativeLayout: định vị tương đối, CÓ THỂ xếp chồng
→ PlayerView: match_parent (full screen)
→ TabBar: alignParentTop (đè lên video phía trên)
→ Controls: alignParentBottom (đè lên video phía dưới)
→ Video full màn hình với controls overlay ✓
```

---

## 🔄 Luồng chuyển tab thông minh

### Nhạc → Video
```
User nhấn tab VIDEO
        │
        ▼
btnTabVideo.onClick()
        │
        ├── mediaController.pause()     ← nhạc dừng ngay
        ├── btnPlayPause.setText("▶")   ← cập nhật UI
        └── startActivity(VideoActivity)
                │
                ▼
        VideoActivity.onCreate()
                │
                └── VideoPlayerManager.getPlayer()
                        │
                        ├── [Lần đầu] → tạo mới ExoPlayer, load videos
                        └── [Lần sau] → trả về instance cũ với position cũ
```

### Video → Nhạc
```
User nhấn tab NHẠC
        │
        ▼
btnTabMusic.onClick() → finish()
        │
        ▼
VideoActivity.onStop()
        ├── VideoPlayerManager.pause()     ← video dừng, GIỮ POSITION
        └── playerView.setPlayer(null)     ← gỡ Surface tránh leak
        │
        ▼
MainActivity.onStart()
        └── if (mediaController == null)
                └── connectToService()    ← kết nối lại Binder
                        └── setupPlayerListener() → updateUI() → startProgressUpdater()
```

### Quay lại Video
```
User nhấn tab VIDEO lần 2
        │
        ▼
VideoActivity.onCreate() (Activity mới)
        │
        └── VideoPlayerManager.getInstance()
                │ instance != null → CÙNG PLAYER CŨ
                │ position = 1:23 (giữ nguyên)
                │ playlist = giữ nguyên
                ▼
        playerView.setPlayer(videoPlayer)  ← gắn Surface mới
        syncSeekBarWithCurrentPosition()   ← SeekBar hiện 1:23
        updateVideoInfo()                  ← tên video, nghệ sĩ
```

---

## 🐛 Các vấn đề đã giải quyết

### 1. Video reset về đầu khi quay lại
```
Nguyên nhân: onDestroy() → videoPlayer.release() → mất trạng thái
Giải pháp:  VideoPlayerManager Singleton
            → ExoPlayer không bao giờ bị release khi chuyển tab
            → Chỉ release khi app bị đóng hoàn toàn
```

### 2. SeekBar video không điều khiển được lần đầu
```
Nguyên nhân: videoPlayer.getDuration() = -1 khi chưa STATE_READY
             → seekBarVideo.setMax(0) → không thể seekTo()
Giải pháp:  onPlaybackStateChanged(STATE_READY)
             → chỉ setMax() khi duration > 0
             + Vòng lặp 500ms kiểm tra lại liên tục
```

### 3. Nhạc không dừng khi chuyển sang Video
```
Nguyên nhân: startActivity() không tự pause MusicService
Giải pháp:  mediaController.pause() trước startActivity()
```

### 4. Nút nhạc không hoạt động khi quay lại
```
Nguyên nhân: onStop() → mediaController = null
             onRestart() không gọi lại connectToService()
Giải pháp:  onStart() kiểm tra mediaController == null
             → connectToService() nếu cần
```

### 5. Volume không hoạt động trên Automotive emulator
```
Nguyên nhân: AudioManager.setStreamVolume() bị Car Audio Service restrict
Giải pháp:  ExoPlayer.setVolume(float) — software gain
             Hoạt động 100% không cần permission
```

### 6. Phần volume bị ẩn dưới màn hình ngang
```
Nguyên nhân: LinearLayout dọc + ScrollView
             Màn hình landscape thiếu chiều cao
Giải pháp:  Layout 2 cột LinearLayout horizontal
             Nhạc bên trái, Volume bên phải
             Tất cả hiển thị không cần cuộn
```

### 7. ActionBar "MusicCar" chiếm 56dp
```
Nguyên nhân: Theme mặc định có ActionBar
Giải pháp:  Theme.AppCompat.DayNight.NoActionBar
             Trong AndroidManifest.xml
```

### 8. Emoji 🔉🔊 render thành icon lạ
```
Nguyên nhân: Automotive emulator không hỗ trợ emoji font đầy đủ
Giải pháp:  Dùng ký tự text "-" và "+" thay cho emoji
```

---

## 🚀 Hướng dẫn cài đặt

### Bước 1: Chuẩn bị file media
```
Tên file yêu cầu (chữ thường + số + dấu gạch dưới):
app/src/main/res/raw/
├── canon.mp3         ← Canon In D Major
├── dongthoai.mp3     ← Đồng Thoại
├── castle.mp4        ← Castle on the Hill
└── giu_anh.mp4       ← Giữ Anh Cho Ngày Hôm Qua
```

### Bước 2: Thứ tự tạo các file
```
1. app/build.gradle.kts      → Sync Now (chờ xong)
2. AndroidManifest.xml
3. res/layout/activity_main.xml
4. res/layout/activity_video.xml
5. MusicService.java
6. VideoPlayerManager.java   ← File mới
7. MainActivity.java
8. VideoActivity.java        ← File mới
9. res/values/strings.xml
```

### Bước 3: Build & Run
```bash
Build > Clean Project
Build > Rebuild Project   # Kiểm tra BUILD SUCCESSFUL
Run > Run 'app'           # Chọn emulator 1408p Landscape
```

---

## ⚙️ Thông số kỹ thuật

| Thông số | Giá trị |
|----------|---------|
| Min SDK | API 35 (Android 15) |
| Target SDK | API 36 (Android 16) |
| Compile SDK | API 36 |
| Java | 17 |
| Media3 | 1.5.1 |
| Gradle DSL | Kotlin `.kts` |
| Theme | `Theme.AppCompat.DayNight.NoActionBar` |
| Emulator | 1408p Landscape with Google Play |
| Files Java | 4 (MainActivity, MusicService, VideoActivity, VideoPlayerManager) |
| Files Layout | 2 (activity_main, activity_video) |
| Files Media | 4 (2 MP3 + 2 MP4) |

---

## 📚 Design Patterns được áp dụng

| Pattern | File | Mục đích |
|---------|------|----------|
| **Client-Server** | MainActivity ↔ MusicService | Tách UI khỏi logic phát nhạc |
| **Singleton** | VideoPlayerManager | Giữ ExoPlayer sống qua nhiều Activity |
| **Observer** | Player.Listener | Lắng nghe sự kiện player không đồng bộ |
| **Facade** | MediaController | Đơn giản hóa Binder IPC phức tạp |

---

<p align="center">
  <strong>MusicCar v2.0</strong> — Nhạc 🎵 · Video 🎬 · Volume 🔊 · Android Automotive 🚗
</p>

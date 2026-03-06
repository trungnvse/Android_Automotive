# 🎵 MusicCar v1.0 — Android Automotive Music Player

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20Automotive-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Min%20SDK-API%2035-4285F4?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Media3-1.5.1-A142F4?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Version-1.0-brightgreen?style=for-the-badge"/>
</p>

<p align="center">
  Ứng dụng phát nhạc cơ bản đầu tiên trên nền tảng <strong>Android Automotive OS</strong>.<br/>
  Phiên bản khởi đầu — tập trung vào kiến trúc nền tảng và luồng phát nhạc chuẩn.
</p>

---

## 📋 Mục lục
- [Giới thiệu](#-giới-thiệu)
- [Tính năng](#-tính-năng)
- [Giao diện](#-giao-diện)
- [Kiến trúc](#-kiến-trúc)
- [Cấu trúc thư mục](#-cấu-trúc-thư-mục)
- [Giải thích chi tiết từng file](#-giải-thích-chi-tiết-từng-file)
- [Luồng hoạt động](#-luồng-hoạt-động)
- [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)

---

## 🎯 Giới thiệu

**MusicCar v1.0** là phiên bản đầu tiên của ứng dụng phát nhạc cho Android Automotive OS. Project này được xây dựng với mục tiêu:

- Hiểu rõ mô hình **Client-Server** trong Android (Activity ↔ Service)
- Nắm vững kiến trúc **MediaSession** theo chuẩn Google
- Hiểu cơ chế **Binder IPC** — giao tiếp giữa các component
- Xây dựng **Foreground Service** phát nhạc liên tục khi tắt màn hình

> **Đây là nền tảng kiến trúc** — mọi tính năng nâng cao ở phiên bản sau đều được xây dựng trên cơ sở này.

---

## ✨ Tính năng

| Tính năng | Mô tả |
|-----------|-------|
| ▶ / ⏸ Play / Pause | Phát hoặc tạm dừng bài hát hiện tại |
| ⏮ Bài trước | Chuyển về bài hát trước trong playlist |
| ⏭ Bài tiếp theo | Chuyển sang bài hát tiếp theo trong playlist |
| 📊 Thanh tiến trình | SeekBar hiển thị vị trí hiện tại, có thể kéo để tua |
| ⏱ Đồng hồ thời gian | Hiển thị thời gian hiện tại và tổng thời lượng |
| 🎤 Tên bài & nghệ sĩ | Tự động cập nhật khi chuyển bài |
| 🔇 Phát nền | Nhạc tiếp tục khi đóng app hoặc tắt màn hình |
| 📞 Audio Focus | Tự động pause khi có cuộc gọi, resume sau khi kết thúc |
| 🎧 Noisy Handling | Tự động pause khi rút tai nghe |
| 🚗 Automotive Ready | Tích hợp MediaBrowserService cho Automotive OS |

---

## 🖥 Giao diện

```
┌─────────────────────────────────────────────────────────────┐
│  MusicCar                                          4:09  🔔 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    Canon In D Major                         │
│                     Various Artists                         │
│                                                             │
│         ━━━━━━━━━━━━━━━━━━━━━●━━━━━━━━━━━━━━━━━           │
│         0:21                                 3:40           │
│                                                             │
│                  [ ⏮ ]   [ ▶ ]   [ ⏭ ]                    │
│                                                             │
│                                                             │
│              🔊  ÂM LƯỢNG                                   │
│         ━━━━━━━━━━━━━━●━━━━━━━━━━━━━━━━━━━━━               │
│              [ 🔉 ]       80%       [ 🔊 ]                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Màu sắc chủ đạo:**
- Nền: `#1A1A2E` — xanh đêm đậm
- Accent: `#E94560` — đỏ hồng nổi bật
- Panel phụ: `#16213E` — xanh đêm nhạt hơn
- Text phụ: `#AAAAAA` — xám nhạt

---

## 🏗 Kiến trúc

### Mô hình Client-Server

```
┌──────────────────────────────────────────────────────────────┐
│  PROCESS: com.example.musiccar                               │
│                                                              │
│  ┌─────────────────────┐    Binder IPC   ┌────────────────┐ │
│  │    MainActivity     │◄───────────────►│  MusicService  │ │
│  │    (CLIENT)         │                 │  (SERVER)      │ │
│  │                     │                 │                │ │
│  │  ┌───────────────┐  │                 │  ┌──────────┐  │ │
│  │  │ MediaController│  │                 │  │ExoPlayer │  │ │
│  │  │ (Remote)       │  │                 │  │(Engine)  │  │ │
│  │  └───────────────┘  │                 │  └──────────┘  │ │
│  │                     │                 │                │ │
│  │  ┌───────────────┐  │                 │  ┌──────────┐  │ │
│  │  │ UI Components │  │                 │  │MediaLib  │  │ │
│  │  │ Button/SeekBar│  │                 │  │Session   │  │ │
│  │  └───────────────┘  │                 │  └──────────┘  │ │
│  └─────────────────────┘                 └────────────────┘ │
└──────────────────────────────────────────────────────────────┘
           ▲
           │ Binder IPC
           ▼
┌──────────────────────┐
│  Android Automotive  │
│  OS (System)         │
│  Duyệt thư viện nhạc │
│  Điều khiển từ xe    │
└──────────────────────┘
```

### Tại sao cần tách Service khỏi Activity?

```
❌ KHÔNG DÙNG SERVICE:          ✅ DÙNG SERVICE:
─────────────────────────       ────────────────────────────
Activity chứa ExoPlayer         MusicService chứa ExoPlayer

User nhấn Home                  User nhấn Home
→ onPause() → onStop()          → onPause() → onStop()
→ Activity có thể bị kill       → Activity bị kill ✓
→ ExoPlayer bị destroy          → MusicService VẪN CHẠY
→ Nhạc DỪNG ✗                  → Nhạc TIẾP TỤC ✓
```

---

## 📁 Cấu trúc thư mục

```
MusicCar/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/musiccar/
│   │   │   ├── MainActivity.java      ← CLIENT: Giao diện người dùng
│   │   │   └── MusicService.java      ← SERVER: Engine phát nhạc
│   │   │
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml  ← Thiết kế giao diện
│   │   │   ├── raw/
│   │   │   │   ├── canon.mp3          ← Canon In D Major
│   │   │   │   └── dongthoai.mp3      ← Đồng Thoại - Quang Lương
│   │   │   └── values/
│   │   │       └── strings.xml
│   │   │
│   │   └── AndroidManifest.xml        ← Khai báo toàn bộ component
│   │
│   └── build.gradle.kts               ← Dependencies & cấu hình build
│
└── README.md
```

---

## 🔍 Giải thích chi tiết từng file

### 📄 `AndroidManifest.xml`

File Android OS đọc **đầu tiên** khi cài app. Khai báo mọi thứ app cần tồn tại.

```xml
<!-- Quyền chạy nhạc nền liên tục -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/>

<!-- Service phát nhạc — exported=true để Automotive OS kết nối vào -->
<service
    android:name=".MusicService"
    android:exported="true"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <!-- Địa chỉ để Media3 tìm thấy service -->
        <action android:name="androidx.media3.session.MediaSessionService"/>
        <!-- Địa chỉ để Automotive OS duyệt thư viện nhạc -->
        <action android:name="android.media.browse.MediaBrowserService"/>
    </intent-filter>
</service>
```

**Lý do `exported="true"`:** Android Automotive OS chạy trong process riêng của hệ thống. Để OS có thể kết nối vào service của app và duyệt thư viện nhạc, service phải được `exported`.

---

### 📄 `build.gradle.kts`

```kotlin
android {
    namespace = "com.example.musiccar"
    compileSdk = 36   // API dùng để COMPILE code
    defaultConfig {
        minSdk = 35   // Android 15 — thấp nhất app chạy được
        targetSdk = 36 // Android 16 — đã test và tối ưu cho version này
    }
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    // ExoPlayer: decode MP3 → PCM → AudioFlinger → DAC → Loa
    // Thay thế MediaPlayer cũ, linh hoạt và ít bug hơn

    implementation("androidx.media3:media3-session:1.5.1")
    // MediaSession: giao thức chuẩn giao tiếp UI ↔ Player
    // Bắt buộc để Automotive OS điều khiển app

    implementation("androidx.media3:media3-common:1.5.1")
    // Các class dùng chung: MediaItem, MediaMetadata, Player interface
}
```

---

### 📄 `MusicService.java` — Trái tim của app

```
MusicService (extends MediaLibraryService)
│
├── onCreate()
│   ├── Tạo ExoPlayer với AudioAttributes
│   │   ├── setUsage(USAGE_MEDIA) → AudioFlinger biết đây là nhạc
│   │   ├── setContentType(AUDIO_CONTENT_TYPE_MUSIC)
│   │   └── setHandleAudioBecomingNoisy(true) → tự pause khi rút tai nghe
│   │
│   ├── buildMediaItems() → tạo playlist từ res/raw/
│   │   └── URI: "android.resource://com.example.musiccar/R.raw.canon"
│   │
│   ├── player.prepare() → ExoPlayer bắt đầu buffer dữ liệu
│   │
│   └── Tạo MediaLibrarySession
│
├── onGetLibraryRoot() → Automotive OS hỏi "thư mục gốc?"
│   └── Trả về MediaItem root với MEDIA_TYPE_FOLDER_MIXED
│
├── onGetChildren() → Automotive OS hỏi "danh sách bài?"
│   └── Trả về ImmutableList<MediaItem> toàn bộ playlist
│
└── onDestroy()
    ├── mediaLibrarySession.release()
    └── player.release()
```

**Hành trình âm thanh từ file đến loa:**
```
res/raw/canon.mp3
    │ URI: android.resource://...
    ▼
ExoPlayer
    │ ExtractorMediaSource đọc bytes MP3
    │ MP3Decoder decode → PCM (raw audio samples)
    ▼
AudioTrack API
    │ Đẩy PCM vào buffer
    ▼
AudioFlinger (System Service)
    │ Mix nhiều audio stream
    │ Apply volume, effects
    ▼
ALSA Driver (Linux Kernel)
    │ /dev/snd/pcmC0D0p
    ▼
DAC (Digital-to-Analog Converter)
    │ Số → Tín hiệu điện analog
    ▼
🔊 Loa / Tai nghe
```

---

### 📄 `MainActivity.java` — Điều phối viên UI

```
MainActivity (extends AppCompatActivity)
│
├── onCreate()
│   ├── setContentView(R.layout.activity_main)
│   │   └── Android inflate XML → tạo View objects trong RAM
│   ├── initViews() → findViewById() map View → biến Java
│   └── connectToService()
│       └── SessionToken + MediaController.Builder.buildAsync()
│           └── [Binder IPC] kết nối tới MusicService
│
├── connectToService() — BẤT ĐỒNG BỘ
│   ├── Tạo SessionToken trỏ tới MusicService
│   ├── buildAsync() → không block UI Thread
│   └── addListener() → callback khi kết nối xong
│       ├── mediaController = controllerFuture.get()
│       ├── setupPlayerListener() → lắng nghe sự kiện player
│       ├── updateUI() → hiển thị tên bài, trạng thái
│       └── startProgressUpdater() → vòng lặp 500ms
│
├── startProgressUpdater() — VÒNG LẶP UI
│   └── handler.post(Runnable {
│           seekBar.setProgress(mediaController.getCurrentPosition())
│           handler.postDelayed(this, 500) ← tự gọi lại
│       })
│
└── onStop()
    ├── handler.removeCallbacksAndMessages(null) ← dừng vòng lặp
    ├── MediaController.releaseFuture() ← ngắt Binder connection
    └── mediaController = null
```

**Tại sao dùng Handler thay vì Thread?**

```java
// ❌ SAI — crash ngay lập tức:
new Thread(() -> {
    seekBar.setProgress(100); // CalledFromWrongThreadException!
}).start();

// ✅ ĐÚNG — chạy trên UI Thread:
handler.post(() -> {
    seekBar.setProgress(100); // An toàn
});
```

Android có quy tắc tuyệt đối: **chỉ UI Thread được cập nhật UI**. `Handler(Looper.getMainLooper())` đảm bảo code chạy trên đúng thread.

---

### 📄 `activity_main.xml` — Bản thiết kế giao diện

```
LinearLayout (vertical, #1A1A2E)
├── TextView  id=tvSongTitle   → Tên bài hát
├── TextView  id=tvArtist      → Nghệ sĩ
├── SeekBar   id=seekBar       → Tiến trình nhạc
├── LinearLayout (horizontal)
│   ├── TextView id=tvCurrentTime → "0:21"
│   └── TextView id=tvTotalTime   → "3:40"
├── LinearLayout (horizontal)
│   ├── Button id=btnPrev      → ⏮
│   ├── Button id=btnPlayPause → ▶ / ⏸
│   └── Button id=btnNext      → ⏭
├── [Divider]
├── TextView "🔊 ÂM LƯỢNG"
├── SeekBar   id=seekBarVolume → Điều chỉnh âm lượng
└── LinearLayout (horizontal)
    ├── Button id=btnVolumeDown → 🔉
    ├── TextView id=tvVolumePercent → "80%"
    └── Button id=btnVolumeUp   → 🔊
```

**Cách Android render XML thành UI:**
```
XML (text)
    │ aapt2 compile (lúc build)
    ▼
Binary XML (trong APK)
    │ LayoutInflater.inflate() (lúc runtime)
    ▼
View Objects trong RAM
    │ Measure → Layout → Draw
    ▼
Display List (GPU commands)
    │ OpenGL ES / Vulkan
    ▼
Frame Buffer → 🖥 Màn hình
```

**R.java — Cầu nối XML và Java:**
```java
// Auto-generated — KHÔNG sửa file này
public final class R {
    public static final class id {
        public static final int btnPlayPause = 0x7f080045;
        public static final int seekBar      = 0x7f080089;
        // ...
    }
    public static final class raw {
        public static final int canon     = 0x7f0c0001;
        public static final int dongthoai = 0x7f0c0002;
    }
}
```

---

## 🔄 Luồng hoạt động

```
[1] User mở app
         │
         ▼
[2] Android OS đọc AndroidManifest.xml
    → Tìm LAUNCHER Activity → khởi động MainActivity
         │
         ▼
[3] MainActivity.onCreate()
    → setContentView(R.layout.activity_main)
    → initViews() → findViewById() × 9 views
    → connectToService()
         │
         ▼
[4] Android OS khởi động MusicService (nếu chưa chạy)
    → MusicService.onCreate()
    → Tạo ExoPlayer với AudioAttributes
    → buildMediaItems() → 2 MediaItem từ res/raw/
    → player.prepare() → bắt đầu buffer
    → Tạo MediaLibrarySession
         │
         ▼
[5] Binder connection thành công
    → controllerFuture callback
    → mediaController = controllerFuture.get()
    → setupPlayerListener()
    → updateUI() → tvSongTitle = "Canon In D Major"
    → startProgressUpdater() → handler loop mỗi 500ms
         │
         ▼
[6] User nhấn ▶
    → btnPlayPause.onClick()
    → mediaController.play()
    → [Binder IPC → MusicService]
    → player.play()
    → ExoPlayer: đọc MP3 → decode → AudioTrack
    → AudioFlinger → ALSA → DAC → 🔊
         │
         ▼
[7] Mỗi 500ms (UI Thread)
    → handler Runnable chạy
    → getCurrentPosition() → [Binder IPC]
    → seekBar.setProgress(currentPosition)
    → tvCurrentTime.setText("0:21")
         │
         ▼
[8] User nhấn Home
    → onPause() → onStop()
    → handler.removeCallbacksAndMessages(null)
    → MediaController.releaseFuture()
    → mediaController = null
    → MusicService VẪN CHẠY → nhạc TIẾP TỤC 🎵
```

---

## 🚀 Hướng dẫn cài đặt

### Bước 1: Tạo project
```
Android Studio → New Project → No Activity
Name:        MusicCar
Package:     com.example.musiccar
Language:    Java
Minimum SDK: API 35
```

### Bước 2: Thêm file nhạc
```
Tạo thư mục: app/src/main/res/raw/
Thêm file:   canon.mp3
             dongthoai.mp3
```
> ⚠️ Tên file: **chữ thường + số + dấu gạch dưới** — không dùng dấu cách hay gạch ngang.

### Bước 3: Cấu hình các file
Theo thứ tự:
1. `app/build.gradle.kts` → Sync Now
2. `AndroidManifest.xml`
3. `res/layout/activity_main.xml`
4. `MusicService.java`
5. `MainActivity.java`
6. `res/values/strings.xml`

### Bước 4: Build & Run
```
Build > Clean Project
Build > Rebuild Project   → Kiểm tra BUILD SUCCESSFUL
Run > Run 'app'           → Chọn emulator 1408p Landscape
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

---

## 📚 Kiến thức cần nắm để hiểu project

| Chủ đề | Tầm quan trọng |
|--------|----------------|
| Android Activity Lifecycle | ⭐⭐⭐⭐⭐ |
| Android Service & Foreground Service | ⭐⭐⭐⭐⭐ |
| Binder IPC | ⭐⭐⭐⭐ |
| MediaSession Architecture | ⭐⭐⭐⭐⭐ |
| Handler / Looper / UI Thread | ⭐⭐⭐⭐ |
| ExoPlayer pipeline | ⭐⭐⭐⭐ |
| Android Resource System (R.java) | ⭐⭐⭐ |
| Gradle & Build System | ⭐⭐⭐ |

---

<p align="center">
 Ứng dụng cơ bản 🎵 - nền tảng để giúp phát triển những ứng dụng khác.
</p>

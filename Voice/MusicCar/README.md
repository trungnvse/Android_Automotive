# 🚗 MusicCar — Android Automotive Music & Video Player

> Ứng dụng phát nhạc và video tích hợp điều khiển giọng nói cho Android Automotive OS  
> **Đóng góp bởi:** Trung
> **Phiên bản:** 2.0  

---

## 📋 Mục lục

- [Giới thiệu](#-giới-thiệu)
- [Tính năng](#-tính-năng)
- [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
- [Cấu trúc project](#-cấu-trúc-project)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Điều khiển giọng nói](#-điều-khiển-giọng-nói)
- [Cài đặt & Chạy thử](#-cài-đặt--chạy-thử)
- [Các vấn đề đã giải quyết](#-các-vấn-đề-đã-giải-quyết)
- [Hướng phát triển tiếp theo](#-hướng-phát-triển-tiếp-theo)

---

## 🎯 Giới thiệu

**MusicCar** là ứng dụng phát nhạc và video được thiết kế đặc biệt cho **Android Automotive OS** — hệ điều hành chạy trực tiếp trên màn hình xe hơi. Ứng dụng hỗ trợ điều khiển bằng giọng nói thông qua wake word **"Hey MusicCar"**, cho phép người lái tương tác hoàn toàn rảnh tay trong khi lái xe.

### Thông tin dự án

| Mục | Chi tiết |
|-----|---------|
| Package | `com.example.musiccar` |
| Ngôn ngữ | Java |
| Min SDK | 35 |
| Target SDK | 36 |
| Compile SDK | 36 |
| Build System | Kotlin DSL (`build.gradle.kts`) |
| IDE | Android Studio Panda 2025.3.1 |
| Emulator | Automotive 1408p landscape, API 35 |

---

## ✨ Tính năng

### 🎵 Tab Nhạc
- Phát / Tạm dừng / Bài tiếp / Bài trước
- Thanh tiến trình có thể kéo thả
- Hiển thị tên bài, nghệ sĩ, thời gian
- Điều chỉnh âm lượng bằng nút hoặc thanh kéo
- Giao diện 2 cột tối ưu cho màn hình landscape xe hơi

### 🎬 Tab Video
- Phát video toàn màn hình
- Controls overlay trong suốt phía dưới màn hình
- Nhớ vị trí phát khi chuyển tab và quay lại
- Điều chỉnh âm lượng riêng cho video

### 🎙 Điều khiển giọng nói
- Wake word **"Hey MusicCar"** nhận diện offline (không cần internet)
- Hỗ trợ lệnh tiếng Việt và tiếng Anh
- Nút MIC thủ công làm backup
- Hiển thị trạng thái và text nhận diện trực tiếp trên màn hình

---

## 🛠 Công nghệ sử dụng

| Thư viện | Phiên bản | Mục đích |
|---------|----------|---------|
| AndroidX AppCompat | 1.7.0 | UI cơ bản |
| Material Components | 1.12.0 | Giao diện Material Design |
| Media3 ExoPlayer | 1.5.1 | Engine phát nhạc & video |
| Media3 Session | 1.5.1 | MediaSession cho Automotive OS |
| Media3 UI | 1.5.1 | PlayerView cho video |
| Porcupine Android | 4.0.0 | Wake word "Hey MusicCar" offline |
| Android SpeechRecognizer | built-in | Nhận diện lệnh giọng nói |

---

## 📁 Cấu trúc project

```
MusicCar/
├── app/
│   ├── build.gradle.kts                    # Dependencies & build config
│   └── src/main/
│       ├── java/com/example/musiccar/
│       │   ├── MainActivity.java           # Tab Nhạc — màn hình chính
│       │   ├── VideoActivity.java          # Tab Video
│       │   ├── MusicService.java           # Background service phát nhạc
│       │   ├── VideoPlayerManager.java     # Singleton quản lý ExoPlayer video
│       │   ├── VoiceCommandManager.java    # Nhận diện & xử lý lệnh giọng nói
│       │   └── WakeWordManager.java        # Singleton wake word Porcupine
│       ├── res/
│       │   ├── layout/
│       │   │   ├── activity_main.xml       # Layout 2 cột tab Nhạc
│       │   │   └── activity_video.xml      # Layout overlay tab Video
│       │   ├── raw/
│       │   │   ├── canon.mp3               # Canon In D Major
│       │   │   ├── dongthoai.mp3           # Đồng Thoại — Quang Lương
│       │   │   ├── castle.mp4              # Castle on the Hill — Ed Sheeran
│       │   │   └── giu_anh.mp4             # Giữ Anh — Hoàng Dũng
│       │   └── values/strings.xml
│       ├── assets/
│       │   └── Hey-Music-Car_en_android_v4_0_0.ppn   # Model wake word Porcupine
│       └── AndroidManifest.xml
└── build.gradle.kts                        # Root build config
```

---

## 🏗 Kiến trúc hệ thống

### Tổng quan

```
┌─────────────────────────────────────────────────────┐
│                  Android Automotive OS               │
├──────────────────────┬──────────────────────────────┤
│   MainActivity       │      VideoActivity            │
│   (Tab Nhạc)         │      (Tab Video)              │
│                      │                              │
│  MediaController ────┼──→  MusicService             │
│  (Client)            │     (Server - Background)     │
│                      │                              │
│  VoiceCommandManager ├──── VoiceCommandManager      │
│  WakeWordManager ────┴──── WakeWordManager           │
│  (Singleton, dùng chung)                            │
│                      │                              │
│                      │  VideoPlayerManager          │
│                      │  (Singleton)                 │
└─────────────────────────────────────────────────────┘
```

### Client-Server Pattern (Phát nhạc)

```
MainActivity (Client)
    │  MediaController — giao tiếp qua Binder IPC
    ▼
MusicService (Server)
    │  ExoPlayer + MediaLibrarySession
    │  AudioAttributes: USAGE_MEDIA
    │  setHandleAudioBecomingNoisy(true)
    ▼
Playlist: canon.mp3 → dongthoai.mp3
```

### Singleton Pattern (Video & Wake Word)

```
VideoPlayerManager.getInstance()
    └── ExoPlayer (1 instance duy nhất)
        └── Giữ trạng thái khi chuyển tab
        └── pause() khi rời VideoActivity
        └── destroy() chỉ khi app đóng hẳn

WakeWordManager.getInstance()
    └── Porcupine + AudioRecord (1 instance duy nhất)
        └── setListener(activity) khi Activity lên foreground
        └── pause() khi nhường mic cho SpeechRecognizer
        └── resume() sau khi SpeechRecognizer xong
```

---

## 🎙 Điều khiển giọng nói

### Luồng hoạt động

```
Porcupine chạy ngầm liên tục (offline, ~1% CPU)
    │
    │  Phát hiện "Hey MusicCar"
    ▼
WakeWordManager.onWakeWordDetected()
    │  pause() Porcupine — nhường mic
    ▼
VoiceCommandManager.startListening()
    │  Android SpeechRecognizer (cần internet)
    │  Nhận diện giọng nói → text
    ▼
onCommandReceived(Command)
    │  Thực thi lệnh
    ▼
WakeWordManager.resume()
    └── Porcupine lắng nghe trở lại
```

### Danh sách lệnh hỗ trợ

| Lệnh tiếng Việt | Lệnh tiếng Anh | Hành động |
|----------------|----------------|----------|
| "phát nhạc", "phát" | "play", "resume" | Phát |
| "dừng", "tạm dừng" | "pause", "stop" | Tạm dừng |
| "bài tiếp theo" | "next" | Bài/video tiếp |
| "bài trước" | "previous" | Bài/video trước |
| "tăng âm lượng" | "volume up" | Tăng 20% |
| "giảm âm lượng" | "volume down" | Giảm 20% |
| "mở video", "xem video" | "open video" | Chuyển tab Video |
| "về nhạc", "mở nhạc" | "open music" | Về tab Nhạc |

### Thông tin Porcupine

- **AccessKey:** Đăng ký tại [console.picovoice.ai](https://console.picovoice.ai)
- **File model:** `Hey-Music-Car_en_android_v4_0_0.ppn` (đặt trong `assets/`)
- **Sensitivity:** 0.7 (0.0 = ít nhạy → 1.0 = rất nhạy)
- **Hoạt động:** Hoàn toàn offline, không gửi dữ liệu lên server

---

## 🚀 Cài đặt & Chạy thử

### Yêu cầu

- Android Studio Panda 2025.3.1 trở lên
- JDK 17
- Android SDK 35+
- Emulator: Automotive 1408p landscape với Google Play (API 35)

### Các bước

**1. Clone project**
```bash
git clone https://github.com/<your-repo>/MusicCar.git
cd MusicCar
```

**2. Thêm file media vào `res/raw/`**
```
canon.mp3
dongthoai.mp3
castle.mp4
giu_anh.mp4
```

**3. Thêm file wake word vào `assets/`**
```
Hey-Music-Car_en_android_v4_0_0.ppn
```
> Tạo wake word tại: https://console.picovoice.ai → Porcupine → Train custom wake word

**4. Cập nhật AccessKey trong `WakeWordManager.java`**
```java
private static final String ACCESS_KEY = "your_access_key_here";
```

**5. Sync Gradle & Build**
```
Android Studio → File → Sync Project with Gradle Files
→ Run 'app'
```

**6. Cấu hình emulator microphone**
```
Emulator → "..." (Extended Controls) → Microphone
→ Bật "Enable Host Microphone Access"
```

**7. Cấp quyền microphone**

Lần đầu chạy app sẽ tự hỏi quyền → chọn **Allow**

---

## 🐛 Các vấn đề đã giải quyết

| Vấn đề | Nguyên nhân | Giải pháp |
|--------|------------|----------|
| Video reset về đầu khi quay lại | `onDestroy()` gọi `release()` | `VideoPlayerManager` Singleton |
| SeekBar video không điều khiển được lần đầu | `getDuration() = -1` trước `STATE_READY` | Set max trong `onPlaybackStateChanged(STATE_READY)` |
| Nhạc không dừng khi chuyển Video | Thiếu `pause()` trước `startActivity()` | Gọi `mediaController.pause()` trong `switchToVideo()` |
| Nút nhạc chết sau khi về từ Video | `onStop()` null controller, không reconnect | `onStart()` kiểm tra null + `connectToService()` |
| Âm lượng không hoạt động trên Automotive | `AudioManager.setStreamVolume()` bị giới hạn | `ExoPlayer.setVolume(float)` |
| Wake word conflict mic khi chuyển tab | 2 `WakeWordManager` tranh mic | `WakeWordManager` Singleton + `setListener()` |
| Nhạc vẫn phát khi sang tab Video qua giọng nói | Thiếu `pause()` trong `OPEN_VIDEO` command | Thêm `mediaController.pause()` trước khi switch |
| `integer number too large` | Hex literal 7 chữ số vượt int | Dùng `Color.argb()` |
| `EXTRA_ALSO_RECOGNIZE_SPEECH` không tồn tại | API không có trong Android SDK | Xoá dòng đó |
| `method does not override` | Interface cũ thiếu `onRawTextReceived()` | Cập nhật đồng bộ interface và implementation |
| Porcupine `Could Not Resolve 4.0.1` | Version không tồn tại trên Maven | Dùng `4.0.0` |

---

## 🔮 Hướng phát triển tiếp theo

- **Google Assistant integration** — implement `onPlayFromSearch()` trong `MusicService` để tích hợp "Hey Google" chuẩn Automotive OS
- **Thêm playlist động** — cho phép thêm/xóa bài hát từ bộ nhớ thiết bị
- **Equalizer** — điều chỉnh âm thanh theo sở thích
- **CarInputManager** — hỗ trợ nút PTT (Push-to-Talk) trên vô lăng xe thật
- **Bluetooth HFP** — điều khiển qua nút vật lý trên xe

---

---

<div align="center">
  <strong>MusicCar v2.0</strong> — Made with ❤️ - keep learning together
</div>

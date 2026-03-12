# 🚗 Android Automotive OS — Tổng quan toàn diện

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%20Automotive%20OS-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Powered%20by-Google-4285F4?style=for-the-badge&logo=google&logoColor=white"/>
  <img src="https://img.shields.io/badge/Since-2019-orange?style=for-the-badge"/>
</p>

<p align="center">
  <em>Hệ điều hành Android được nhúng trực tiếp vào xe hơi — không cần điện thoại, không cần kết nối.</em>
</p>

---

## 📋 Mục lục

- [Android Automotive là gì?](#-android-automotive-là-gì)
- [So sánh với Android Auto](#-so-sánh-android-automotive-vs-android-auto)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Các thành phần cốt lõi](#-các-thành-phần-cốt-lõi)
- [Vòng đời ứng dụng](#-vòng-đời-ứng-dụng)
- [Các loại ứng dụng hỗ trợ](#-các-loại-ứng-dụng-hỗ-trợ)
- [Yêu cầu phát triển](#-yêu-cầu-phát-triển)
- [Các hãng xe đang dùng](#-các-hãng-xe-đang-dùng)
- [Tương lai của Android Automotive](#-tương-lai)

---

## 🤔 Android Automotive là gì?

**Android Automotive OS (AAOS)** là phiên bản Android được thiết kế đặc biệt để chạy **nhúng trực tiếp** vào hệ thống infotainment của xe hơi. Không giống Android thông thường chạy trên điện thoại, AAOS chạy như **hệ điều hành chính của xe** — điều khiển màn hình trung tâm, điều hòa không khí, điều hướng, và toàn bộ trải nghiệm giải trí.

```
Điện thoại Android thông thường:
┌─────────────────────────────────┐
│  Android OS                     │
│  ├── Apps (Camera, Maps...)     │
│  ├── Phone features             │
│  └── Sensors (GPS, IMU...)      │
└─────────────────────────────────┘
        ↕ Kết nối qua USB/Bluetooth
┌─────────────────────────────────┐
│  Màn hình xe (Android Auto)     │
│  (Chỉ là màn hình phụ)         │
└─────────────────────────────────┘

Android Automotive OS (tích hợp vào xe):
┌──────────────────────────────────────────────┐
│  XE HƠI                                      │
│  ┌────────────────────────────────────────┐  │
│  │  Android Automotive OS                 │  │
│  │  ├── Media (Nhạc, Video, Podcast)      │  │
│  │  ├── Navigation (Google Maps tích hợp) │  │
│  │  ├── Climate Control (Điều hòa)        │  │
│  │  ├── Vehicle APIs (Tốc độ, nhiên liệu) │  │
│  │  ├── Google Assistant                  │  │
│  │  └── 3rd Party Apps                   │  │
│  └────────────────────────────────────────┘  │
│  Không cần điện thoại                        │
└──────────────────────────────────────────────┘
```

---

## ⚖️ So sánh: Android Automotive vs Android Auto

| Tiêu chí | Android Auto | Android Automotive OS |
|----------|-------------|----------------------|
| **Cần điện thoại?** | ✅ Bắt buộc | ❌ Không cần |
| **Chạy ở đâu?** | Trên điện thoại, phản chiếu lên màn hình xe | Chạy thẳng trên phần cứng xe |
| **Cài app?** | Qua điện thoại | Qua Play Store tích hợp trên xe |
| **Offline?** | Phụ thuộc điện thoại | Hoàn toàn độc lập |
| **Tùy chỉnh UI?** | Rất hạn chế | Nhà sản xuất xe có thể tùy chỉnh sâu |
| **Truy cập vehicle API?** | Không | Có (tốc độ, nhiên liệu, HVAC...) |
| **Ra mắt?** | 2015 | 2019 (Polestar 2) |
| **Phát triển app?** | Android Auto Library | Android Automotive OS SDK |

---

## 🏛 Kiến trúc hệ thống

Android Automotive OS được xây dựng theo kiến trúc nhiều tầng:

```
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATIONS LAYER                        │
│   Google Maps · Spotify · YouTube Music · 3rd Party Apps    │
├─────────────────────────────────────────────────────────────┤
│                  ANDROID FRAMEWORK LAYER                     │
│   Activity Manager · Media Session · Package Manager        │
│   Window Manager · Content Providers · Notification System  │
├─────────────────────────────────────────────────────────────┤
│                   CAR SERVICE LAYER                          │
│   ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│   │ CarAudio     │  │ CarHVAC      │  │ CarProperty      │ │
│   │ Service      │  │ Service      │  │ Service          │ │
│   │ (Âm thanh)   │  │ (Điều hòa)   │  │ (Tốc độ, xăng)  │ │
│   └──────────────┘  └──────────────┘  └──────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    HAL LAYER (Hardware Abstraction)          │
│   Audio HAL · Vehicle HAL · Graphics HAL · Camera HAL       │
├─────────────────────────────────────────────────────────────┤
│                    LINUX KERNEL                              │
│   ALSA Audio Driver · CAN Bus Driver · GPU Driver           │
│   Binder IPC · File System · Power Management               │
├─────────────────────────────────────────────────────────────┤
│                    HARDWARE                                  │
│   Infotainment ECU · CAN Bus · Audio Amplifier              │
│   Display · Touch Screen · GPS · IMU Sensors                │
└─────────────────────────────────────────────────────────────┘
```

### Vehicle HAL — Cầu nối với xe

`Vehicle HAL (Hardware Abstraction Layer)` là tầng quan trọng nhất trong AAOS. Nó cung cấp giao diện chuẩn để Android truy cập các thông số xe:

```java
// Ví dụ đọc tốc độ xe qua Vehicle HAL:
CarPropertyManager carPropertyManager =
    (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);

float speed = carPropertyManager.getFloatProperty(
    VehiclePropertyIds.PERF_VEHICLE_SPEED,
    VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
);
// speed = tốc độ xe tính bằng m/s
```

### Binder IPC trong AAOS

Tất cả giao tiếp giữa các thành phần trong AAOS đều qua **Binder IPC** — driver kernel tại `/dev/binder`:

```
App (Media Player)
      │ mediaController.play()
      │ → Binder call
      ▼
CarAudioService (System Process)
      │ → Binder call
      ▼
AudioFlinger (MediaServer Process)
      │ → ALSA syscall
      ▼
Audio HAL → DSP/Amplifier → Loa xe
```

---

## 🧩 Các thành phần cốt lõi

### 1. Media Session Architecture
AAOS yêu cầu mọi ứng dụng media phải implement `MediaBrowserService` hoặc `MediaLibraryService` để:
- Automotive OS có thể **duyệt thư viện** nhạc/video của app
- Điều khiển phát nhạc từ **vô lăng, nút cứng** trên xe
- Hiển thị thông tin bài hát trên **instrument cluster** (đồng hồ xe)

```
Android Automotive OS
        │ Kết nối qua MediaBrowser protocol
        ▼
MediaLibraryService (app của bạn)
        │
        ├── onGetLibraryRoot()  → Trả về thư mục gốc
        ├── onGetChildren()     → Trả về danh sách bài hát
        └── MediaSession        → Nhận lệnh play/pause/next
```

### 2. Distraction Optimization (Chống phân tâm)
AAOS có cơ chế bảo vệ an toàn — ứng dụng **không được hiển thị nội dung phức tạp** khi xe đang chạy:

```java
// Kiểm tra xe có đang di chuyển không:
UXRestrictionManager uxManager =
    (UXRestrictionManager) car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE);

CarUxRestrictions restrictions = uxManager.getCurrentCarUxRestrictions();

if (restrictions.isRequiresDistractionOptimization()) {
    // Xe đang chạy → ẩn keyboard, giới hạn thao tác
    hideComplexUI();
}
```

### 3. CarAudioService — Quản lý âm thanh đa vùng
Xe hơi có nhiều vùng âm thanh (zone): ghế lái, ghế sau, loa ngoài. AAOS quản lý tất cả:

```
┌──────────────────────────────────────────┐
│  CarAudioService                         │
│  ┌──────────────┐  ┌───────────────────┐ │
│  │  Zone 0      │  │  Zone 1           │ │
│  │  (Lái xe)    │  │  (Hành khách)     │ │
│  │  Navigation  │  │  Entertainment    │ │
│  │  Music       │  │  Gaming           │ │
│  └──────────────┘  └───────────────────┘ │
└──────────────────────────────────────────┘
```

### 4. Google Assistant tích hợp
AAOS tích hợp sẵn Google Assistant cho điều khiển bằng giọng nói:
- "Hey Google, phát nhạc Ed Sheeran"
- "Hey Google, tăng nhiệt độ lên 22 độ"
- "Hey Google, dẫn đường về nhà"

---

## 🔄 Vòng đời ứng dụng

Ứng dụng AAOS có vòng đời tương tự Android thông thường nhưng có thêm các trạng thái đặc biệt:

```
App khởi động
      │
      ▼
onCreate() → onStart() → onResume()
      │              [App đang hiển thị, xe đỗ]
      │
      │ Xe bắt đầu di chuyển
      ▼
onUxRestrictionsChanged()
      │ [Giới hạn UI phức tạp]
      │
      │ Người dùng chuyển app
      ▼
onPause() → onStop()
      │
      │ [Background: Service vẫn chạy]
      │ [Media vẫn phát, Navigation vẫn dẫn đường]
      │
      │ Xe tắt máy
      ▼
onStop() → onDestroy()
      │ [Lưu trạng thái để khôi phục khi mở lại]
```

---

## 📦 Các loại ứng dụng hỗ trợ

### ✅ Media Apps (Nhạc, Podcast, Audiobook)
Yêu cầu implement `MediaLibraryService` — đây là loại app phổ biến nhất trên AAOS.

```xml
<!-- AndroidManifest.xml bắt buộc -->
<service android:name=".MyMusicService"
         android:exported="true">
  <intent-filter>
    <action android:name="androidx.media3.session.MediaSessionService"/>
    <action android:name="android.media.browse.MediaBrowserService"/>
  </intent-filter>
</service>
```

### ✅ Navigation Apps (Dẫn đường)
Tích hợp với `CarNavigationStatusManager` để hiển thị trên instrument cluster.

### ✅ Video Apps (Khi xe đỗ)
Chỉ được phép phát video khi `DRIVING_STATE = PARKED`. Tự động ẩn khi xe di chuyển.

### ✅ Messaging Apps
Yêu cầu implement `NotificationCompat.CarExtender` để hiển thị tin nhắn an toàn.

### ✅ Point of Interest Apps
Tìm kiếm địa điểm, tích hợp với hệ thống navigation của xe.

### ❌ Không hỗ trợ
- App yêu cầu camera (chụp ảnh selfie)
- App game phức tạp khi xe đang chạy
- App có nhiều text input khi di chuyển

---

## 💻 Yêu cầu phát triển

### Thiết lập môi trường

```
Android Studio → SDK Manager → Cài đặt:
✅ Android Automotive APIs (API 28+)
✅ Android Automotive with Play Store system image
✅ Google Play Services for Automotive
```

### Emulator được hỗ trợ
| Emulator | Độ phân giải | API | Ghi chú |
|----------|-------------|-----|---------|
| Automotive 1024p landscape | 1024x768 | 28+ | Cơ bản |
| Automotive 1080p landscape | 1920x1080 | 30+ | Phổ biến |
| **1408p landscape with Play** | 1408x792 | 33+ | **Khuyến nghị** |

### Dependencies cần thiết

```kotlin
// build.gradle.kts
dependencies {
    // Media3 — phát nhạc/video theo chuẩn AAOS
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    // Car App Library — UI components cho AAOS
    implementation("androidx.car.app:app:1.4.0")
    implementation("androidx.car.app:app-automotive:1.4.0")
}
```

### Permissions phổ biến

```xml
<!-- Foreground Service để nhạc phát khi màn hình tắt -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/>

<!-- Đọc trạng thái xe -->
<uses-permission android:name="android.car.permission.CAR_SPEED"/>
<uses-permission android:name="android.car.permission.CAR_ENERGY"/>

<!-- Nhận diện giọng nói -->
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```

---

## 🏎 Các hãng xe đang dùng

| Hãng xe | Model tiêu biểu | Năm ra mắt |
|---------|----------------|-----------|
| **Polestar** | Polestar 2 | 2019 — xe đầu tiên dùng AAOS |
| **Volvo** | XC40 Recharge, C40 | 2020 |
| **General Motors** | Chevy Bolt EUV, Cadillac Lyriq | 2021 |
| **Renault** | Megane E-Tech | 2022 |
| **Honda** | Honda e:Ny1 | 2023 |
| **Jeep / RAM** | Wagoneer S | 2024 |
| **Hyundai / Kia** | Ioniq 6, EV6 | 2023+ |
| **Sony Honda** | Afeela 1 | 2026 (dự kiến) |

---

## 🔭 Tương lai

### Android Automotive + AI
Google đang tích hợp **Gemini AI** vào AAOS:
- Trợ lý AI hiểu ngữ cảnh phức tạp hơn Google Assistant
- Tổng hợp thông tin từ calendar, email để gợi ý lịch trình
- Điều khiển xe bằng giọng nói tự nhiên hơn

### Vehicle-to-Everything (V2X)
AAOS đang phát triển hỗ trợ V2X — xe giao tiếp với:
- Đèn giao thông (biết khi nào đèn đỏ/xanh)
- Xe khác (tránh va chạm)
- Hạ tầng đường bộ (cảnh báo nguy hiểm)

### Over-the-Air (OTA) Updates
Như điện thoại, xe dùng AAOS có thể nhận cập nhật phần mềm qua mạng — thêm tính năng, vá lỗi mà không cần ra đại lý.

---

## 📚 Tài liệu chính thức

- [Android Automotive OS Overview](https://source.android.com/docs/devices/automotive)
- [Build media apps for cars](https://developer.android.com/training/cars/media)
- [Car App Library](https://developer.android.com/training/cars/apps)
- [Vehicle Properties API](https://developer.android.com/reference/android/car/VehiclePropertyIds)
- [AAOS Design Guidelines](https://developer.android.com/training/cars/design)
- [AndroidX Media3](https://developer.android.com/media/media3)

---

<p align="center">
  Được tổng hợp bởi Trung với niềm đam mê - keep learning...
</p>

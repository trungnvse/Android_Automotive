package com.example.musiccar;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;

/**
 * Singleton — toàn app chỉ có 1 instance duy nhất.
 * Cả MainActivity và VideoActivity dùng chung,
 * tránh conflict mic khi chuyển tab.
 */
public class WakeWordManager {

    private static final String TAG            = "WakeWordManager";
    private static final String ACCESS_KEY     = "X7fOmHdsOX2P1gsoDkEKtKISvT/kBnnMsW92znC4EvZTek8RQC2wfw==";
    private static final String WAKE_WORD_FILE = "Hey-Music-Car_en_android_v4_0_0.ppn";
    private static final int    SAMPLE_RATE    = 16000;

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static WakeWordManager instance;

    public static WakeWordManager getInstance() {
        if (instance == null) instance = new WakeWordManager();
        return instance;
    }

    private WakeWordManager() {}

    // ── Interface ─────────────────────────────────────────────────────────────
    public interface WakeWordListener {
        void onWakeWordDetected();
        void onWakeWordError(String error);
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private WakeWordListener listener;
    private Porcupine        porcupine;
    private AudioRecord      audioRecord;
    private Thread           listeningThread;
    private volatile boolean isRunning     = false;
    private boolean          isInitialized = false;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Khởi tạo Porcupine + AudioRecord.
     * Chỉ gọi 1 lần duy nhất từ MainActivity sau khi có RECORD_AUDIO permission.
     */
    public void init(Context context) {
        if (isInitialized) return;
        try {
            // Khởi tạo Porcupine với file .ppn từ assets/
            porcupine = new Porcupine.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPath(WAKE_WORD_FILE)
                    .setSensitivity(0.7f)
                    .build(context.getApplicationContext());

            int minBuf = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            // SecurityException ném ra nếu chưa có RECORD_AUDIO permission
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                if (listener != null) listener.onWakeWordError("Không thể khởi tạo microphone");
                return;
            }

            isInitialized = true;
            Log.d(TAG, "Initialized successfully");

        } catch (PorcupineException e) {
            Log.e(TAG, "Porcupine error: " + e.getMessage());
            if (listener != null) listener.onWakeWordError("Lỗi Porcupine: " + e.getMessage());
        } catch (SecurityException e) {
            // Android yêu cầu bắt exception này khi tạo AudioRecord
            Log.e(TAG, "Permission denied: " + e.getMessage());
            if (listener != null) listener.onWakeWordError("Chưa cấp quyền microphone");
        }
    }

    /**
     * Đặt listener — Activity nào foreground thì đăng ký.
     * Khi chuyển tab, Activity mới setListener đè lên Activity cũ.
     */
    public void setListener(WakeWordListener listener) {
        this.listener = listener;
    }

    /** Bắt đầu lắng nghe wake word. */
    public void start() {
        if (!isInitialized || isRunning) return;
        try {
            // SecurityException ném ra nếu quyền bị thu hồi sau khi init
            audioRecord.startRecording();
            isRunning = true;
            listeningThread = new Thread(this::runLoop, "WakeWordThread");
            listeningThread.setDaemon(true);
            listeningThread.start();
            Log.d(TAG, "Started — listening for 'Hey MusicCar'");
        } catch (SecurityException e) {
            Log.e(TAG, "startRecording permission denied: " + e.getMessage());
            if (listener != null) listener.onWakeWordError("Mất quyền microphone");
        } catch (Exception e) {
            Log.e(TAG, "Start error: " + e.getMessage());
        }
    }

    /** Tạm dừng — nhường mic cho SpeechRecognizer. */
    public void pause() {
        if (!isRunning) return;
        isRunning = false;
        if (listeningThread != null) {
            listeningThread.interrupt();
            listeningThread = null;
        }
        if (audioRecord != null) {
            try { audioRecord.stop(); } catch (Exception ignored) {}
        }
        Log.d(TAG, "Paused");
    }

    /** Tiếp tục sau khi SpeechRecognizer xong. */
    public void resume() {
        if (isRunning || !isInitialized) return;
        start();
    }

    /** Giải phóng hoàn toàn — chỉ gọi khi app đóng hẳn. */
    public void destroy() {
        pause();
        if (audioRecord != null) {
            try { audioRecord.release(); } catch (Exception ignored) {}
            audioRecord = null;
        }
        if (porcupine != null) {
            porcupine.delete();
            porcupine = null;
        }
        isInitialized = false;
        instance = null;
        Log.d(TAG, "Destroyed");
    }

    public boolean isRunning()     { return isRunning; }
    public boolean isInitialized() { return isInitialized; }

    // ── Background loop ───────────────────────────────────────────────────────
    private void runLoop() {
        short[] pcm = new short[porcupine.getFrameLength()];
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            int read = audioRecord.read(pcm, 0, pcm.length);
            if (read != pcm.length) continue;
            try {
                if (porcupine.process(pcm) >= 0) {
                    Log.d(TAG, "Wake word detected!");
                    if (listener != null) listener.onWakeWordDetected();
                }
            } catch (PorcupineException e) {
                Log.e(TAG, "Process error: " + e.getMessage());
            }
        }
    }
}
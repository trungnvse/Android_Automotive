package com.example.musiccar;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.Locale;


public class VoiceCommandManager {

    // ── Interface ─────────────────────────────────────────────────────────────
    public interface VoiceCommandListener {
        void onListeningStarted();
        void onListeningStopped();
        void onRawTextReceived(String rawText);
        void onCommandReceived(Command command);
        void onError(String errorMessage);
    }

    // ── Commands ──────────────────────────────────────────────────────────────
    public enum Command {
        PLAY, PAUSE, NEXT, PREVIOUS,
        VOLUME_UP, VOLUME_DOWN,
        OPEN_VIDEO, OPEN_MUSIC,
        UNKNOWN
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private final Context context;
    private final VoiceCommandListener listener;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    // ── Constructor ───────────────────────────────────────────────────────────
    public VoiceCommandManager(Context context, VoiceCommandListener listener) {
        this.context  = context.getApplicationContext();
        this.listener = listener;
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public static boolean isAvailable(Context context) {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    public boolean isListening() {
        return isListening;
    }

    public void startListening() {
        if (isListening) return;

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(buildRecognitionListener());

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());

        speechRecognizer.startListening(intent);
        isListening = true;
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        isListening = false;
    }

    // ── Recognition listener ──────────────────────────────────────────────────
    private RecognitionListener buildRecognitionListener() {
        return new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                if (listener != null) listener.onListeningStarted();
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                if (listener != null) listener.onListeningStopped();

                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    String rawText = String.join(" | ", matches);
                    if (listener != null) listener.onRawTextReceived("Nghe được: " + rawText);
                    Command command = parseCommand(matches);
                    if (listener != null) listener.onCommandReceived(command);
                } else {
                    if (listener != null) listener.onRawTextReceived("Không nhận được text nào");
                    if (listener != null) listener.onCommandReceived(Command.UNKNOWN);
                }
            }

            @Override
            public void onError(int errorCode) {
                isListening = false;
                if (listener != null) listener.onListeningStopped();
                if (listener != null) listener.onError(errorMessage(errorCode));
            }

            // Không set isListening=false ở đây — tránh race với onResults
            @Override public void onEndOfSpeech() {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        };
    }

    // ── Command parser ────────────────────────────────────────────────────────
    private Command parseCommand(ArrayList<String> matches) {
        for (String text : matches) {
            Command cmd = matchCommand(text.toLowerCase(Locale.ROOT).trim());
            if (cmd != Command.UNKNOWN) return cmd;
        }
        return Command.UNKNOWN;
    }

    private Command matchCommand(String text) {
        if (containsAny(text, "phát nhạc", "phát", "play", "bắt đầu", "tiếp tục", "resume", "chạy"))
            return Command.PLAY;
        if (containsAny(text, "dừng", "tạm dừng", "pause", "stop", "ngừng"))
            return Command.PAUSE;
        if (containsAny(text, "bài tiếp", "tiếp theo", "next", "bài kế", "video tiếp"))
            return Command.NEXT;
        if (containsAny(text, "bài trước", "quay lại", "previous", "prev", "video trước"))
            return Command.PREVIOUS;
        if (containsAny(text, "tăng âm lượng", "tăng âm", "to hơn", "lớn hơn", "volume up", "tăng tiếng"))
            return Command.VOLUME_UP;
        if (containsAny(text, "giảm âm lượng", "giảm âm", "nhỏ hơn", "nhẹ hơn", "volume down", "giảm tiếng"))
            return Command.VOLUME_DOWN;
        if (containsAny(text, "mở video", "xem video", "sang video", "tab video", "open video"))
            return Command.OPEN_VIDEO;
        if (containsAny(text, "về nhạc", "mở nhạc", "sang nhạc", "tab nhạc", "open music", "nghe nhạc"))
            return Command.OPEN_MUSIC;
        return Command.UNKNOWN;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    private String errorMessage(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_AUDIO:                    return "Lỗi thu âm";
            case SpeechRecognizer.ERROR_CLIENT:                   return "Lỗi ứng dụng";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Chưa cấp quyền mic";
            case SpeechRecognizer.ERROR_NETWORK:                  return "Mất mạng";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:          return "Hết thời gian mạng";
            case SpeechRecognizer.ERROR_NO_MATCH:                 return "Không khớp lệnh nào";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:          return "Đang bận, thử lại";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:           return "Không nghe thấy giọng nói";
            default:                                              return "Lỗi #" + code;
        }
    }
}
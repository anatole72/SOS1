package com.sos.alert;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERM_RECORD  = 1001;
    private static final int PERM_SMS     = 1002;
    private static final String TAG       = "SOSAlert";

    private SharedPreferences prefs;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

    private Button  sosButton;
    private Button  voiceButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs      = getSharedPreferences("sos_prefs", MODE_PRIVATE);
        sosButton  = findViewById(R.id.sos_button);
        voiceButton= findViewById(R.id.voice_button);
        statusText = findViewById(R.id.status_text);

        // Кнопка SOS
        sosButton.setOnClickListener(v -> sendAlert());

        // Кнопка голоса
        voiceButton.setOnClickListener(v -> {
            if (isListening) stopListening();
            else startListening();
        });

        // Кнопка настроек
        findViewById(R.id.settings_button).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class))
        );

        updatePhrasLabel();
    }

    // ─── Голосовое распознавание ───────────────────────────────

    private void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, PERM_RECORD);
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override public void onReadyForSpeech(Bundle p) {
                isListening = true;
                runOnUiThread(() -> {
                    voiceButton.setText("🔴 Слушаю...");
                    voiceButton.setBackgroundColor(Color.parseColor("#1a3a1a"));
                    statusText.setText("🎤 Говорите кодовое слово");
                });
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) checkPhrase(matches);
                // Перезапустить
                if (isListening) restartListening();
            }

            @Override public void onPartialResults(Bundle partial) {
                ArrayList<String> matches =
                    partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) checkPhrase(matches);
            }

            @Override public void onError(int error) {
                if (isListening) restartListening();
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int t, Bundle b) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        speechRecognizer.startListening(intent);
    }

    private void restartListening() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        // Небольшая задержка перед перезапуском
        sosButton.postDelayed(this::startListening, 500);
    }

    private void stopListening() {
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        voiceButton.setText("🎤 Голос");
        voiceButton.setBackgroundColor(Color.parseColor("#2a2a2a"));
        statusText.setText("Готово к отправке");
    }

    private void checkPhrase(ArrayList<String> matches) {
        String keyword = prefs.getString("voice_phrase", "тревога").toLowerCase(Locale.ROOT);
        for (String s : matches) {
            if (s.toLowerCase(Locale.ROOT).contains(keyword)) {
                stopListening();
                runOnUiThread(this::sendAlert);
                return;
            }
        }
    }

    // ─── Отправка сигнала ──────────────────────────────────────

    private void sendAlert() {
        String token  = prefs.getString("bot_token", "");
        String chatId = prefs.getString("chat_id", "");
        String name   = prefs.getString("my_name", "");
        String msg    = prefs.getString("message",
                        "🚨 ТРЕВОГА! Помогите! Меня забрали. Звоните срочно!");
        String smsNum = prefs.getString("sms_number", "");

        if (token.isEmpty() || chatId.isEmpty()) {
            Toast.makeText(this, "Сначала заполните настройки!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        // Анимация кнопки
        sosButton.setBackgroundColor(Color.parseColor("#cc5500"));
        sosButton.setText("...");
        statusText.setText("Отправляю...");

        // Вибрация
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createWaveform(
                new long[]{0, 200, 100, 200}, -1));
        }

        String now  = new SimpleDateFormat("HH:mm dd.MM.yyyy", Locale.ROOT).format(new Date());
        String text = (name.isEmpty() ? "" : "👤 " + name + "\n")
                    + msg + "\n\n🕐 " + now;

        // SMS если указан номер
        if (!smsNum.isEmpty()) {
            sendSMS(smsNum, text);
        }

        // Telegram в фоне
        String finalText  = text;
        String finalToken = token;
        String finalChat  = chatId;
        new Thread(() -> {
            boolean ok = sendTelegram(finalToken, finalChat, finalText);
            runOnUiThread(() -> {
                if (ok) {
                    sosButton.setBackgroundColor(Color.parseColor("#226622"));
                    sosButton.setText("✓");
                    statusText.setText("✅ Сообщение отправлено!");
                } else {
                    sosButton.setBackgroundColor(Color.parseColor("#cc0000"));
                    sosButton.setText("SOS");
                    statusText.setText("❌ Ошибка отправки — проверьте интернет");
                }
                // Вернуть кнопку через 4 сек
                sosButton.postDelayed(() -> {
                    sosButton.setBackgroundColor(Color.parseColor("#cc0000"));
                    sosButton.setText("SOS");
                    statusText.setText("Готово к отправке");
                }, 4000);
            });
        }).start();
    }

    private boolean sendTelegram(String token, String chatId, String text) {
        try {
            URL url = new URL("https://api.telegram.org/bot" + token + "/sendMessage");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            JSONObject body = new JSONObject();
            body.put("chat_id", chatId);
            body.put("text", text);

            byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(out);
            os.close();

            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            Log.e(TAG, "Telegram error: " + e.getMessage());
            return false;
        }
    }

    private void sendSMS(String number, String text) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS}, PERM_SMS);
            return;
        }
        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(number, null,
                text.length() > 160 ? text.substring(0, 160) : text,
                null, null);
        } catch (Exception e) {
            Log.e(TAG, "SMS error: " + e.getMessage());
        }
    }

    // ─── Вспомогательное ──────────────────────────────────────

    private void updatePhrasLabel() {
        TextView t = findViewById(R.id.phrase_label);
        String phrase = prefs.getString("voice_phrase", "тревога");
        t.setText("Кодовое слово: \"" + phrase + "\"");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePhrasLabel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == PERM_RECORD && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }
}

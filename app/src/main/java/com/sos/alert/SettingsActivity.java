package com.sos.alert;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private EditText etToken, etChatId, etName, etMessage, etSmsNumber, etVoicePhrase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs        = getSharedPreferences("sos_prefs", MODE_PRIVATE);
        etToken      = findViewById(R.id.et_token);
        etChatId     = findViewById(R.id.et_chat_id);
        etName       = findViewById(R.id.et_name);
        etMessage    = findViewById(R.id.et_message);
        etSmsNumber  = findViewById(R.id.et_sms_number);
        etVoicePhrase= findViewById(R.id.et_voice_phrase);

        // Загрузить сохранённые значения
        etToken.setText(prefs.getString("bot_token", ""));
        etChatId.setText(prefs.getString("chat_id", ""));
        etName.setText(prefs.getString("my_name", ""));
        etMessage.setText(prefs.getString("message",
                "🚨 ТРЕВОГА! Помогите! Меня забрали. Звоните срочно!"));
        etSmsNumber.setText(prefs.getString("sms_number", ""));
        etVoicePhrase.setText(prefs.getString("voice_phrase", "тревога"));

        Button saveBtn = findViewById(R.id.btn_save);
        saveBtn.setOnClickListener(v -> saveAndClose());
    }

    private void saveAndClose() {
        String token  = etToken.getText().toString().trim();
        String chatId = etChatId.getText().toString().trim();

        if (token.isEmpty() || chatId.isEmpty()) {
            Toast.makeText(this, "Токен и Chat ID обязательны!", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit()
            .putString("bot_token",    token)
            .putString("chat_id",      chatId)
            .putString("my_name",      etName.getText().toString().trim())
            .putString("message",      etMessage.getText().toString().trim())
            .putString("sms_number",   etSmsNumber.getText().toString().trim())
            .putString("voice_phrase", etVoicePhrase.getText().toString().trim().toLowerCase())
            .apply();

        Toast.makeText(this, "Сохранено ✓", Toast.LENGTH_SHORT).show();
        finish();
    }
}

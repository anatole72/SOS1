package com.sos.alert;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private EditText etToken, etChatId, etName, etMessage, etSmsNumber, etVoicePhrase;

    // Готовые шаблоны ситуаций
    private static final String[][] TEMPLATES = {
        {
            "⚠️ ТЦК",
            "тцк",
            "🚨 ТРЕВОГА! Меня забирает ТЦК! Нахожусь по адресу — срочно звоните! Нужна юридическая помощь!"
        },
        {
            "🚔 Полиция",
            "полиция",
            "🚨 ТРЕВОГА! Меня задержала полиция! Нужна помощь — звоните срочно!"
        },
        {
            "🆘 Похитили",
            "помогите",
            "🚨 SOS! Меня похитили! Звоните в полицию 102 и срочно мне на телефон!"
        },
        {
            "🏥 Скорая",
            "скорая",
            "🚨 Нужна помощь! Мне плохо, вызовите скорую 103! Позвоните мне!"
        },
        {
            "🔥 Опасность",
            "опасность",
            "🚨 ТРЕВОГА! Я в опасности! Срочно звоните и приезжайте!"
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs         = getSharedPreferences("sos_prefs", MODE_PRIVATE);
        etToken       = findViewById(R.id.et_token);
        etChatId      = findViewById(R.id.et_chat_id);
        etName        = findViewById(R.id.et_name);
        etMessage     = findViewById(R.id.et_message);
        etSmsNumber   = findViewById(R.id.et_sms_number);
        etVoicePhrase = findViewById(R.id.et_voice_phrase);

        // Загрузить сохранённые значения
        etToken.setText(prefs.getString("bot_token", ""));
        etChatId.setText(prefs.getString("chat_id", ""));
        etName.setText(prefs.getString("my_name", ""));
        etMessage.setText(prefs.getString("message",
                "🚨 ТРЕВОГА! Помогите! Меня забрали. Звоните срочно!"));
        etSmsNumber.setText(prefs.getString("sms_number", ""));
        etVoicePhrase.setText(prefs.getString("voice_phrase", "тревога"));

        // Добавить кнопки шаблонов
        buildTemplateButtons();

        findViewById(R.id.btn_save).setOnClickListener(v -> saveAndClose());
    }

    private void buildTemplateButtons() {
        LinearLayout container = findViewById(R.id.template_container);
        container.removeAllViews();

        for (String[] tpl : TEMPLATES) {
            String label   = tpl[0];
            String phrase  = tpl[1];
            String message = tpl[2];

            // Кнопка шаблона
            Button btn = new Button(this);
            btn.setText(label);
            btn.setTextSize(14f);
            btn.setTextColor(0xFFFFFFFF);
            btn.setBackgroundColor(0xFF2a2a2a);
            btn.setStateListAnimator(null);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 8);
            btn.setLayoutParams(lp);

            // Подсказка под кнопкой
            TextView hint = new TextView(this);
            hint.setText("  голос: «" + phrase + "»  |  " + message.substring(0, Math.min(50, message.length())) + "...");
            hint.setTextSize(11f);
            hint.setTextColor(0xFF555555);
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
            hlp.setMargins(0, 0, 0, 16);
            hint.setLayoutParams(hlp);

            btn.setOnClickListener(v -> {
                etVoicePhrase.setText(phrase);
                etMessage.setText(message);
                Toast.makeText(this, "Шаблон «" + label + "» выбран", Toast.LENGTH_SHORT).show();
            });

            container.addView(btn);
            container.addView(hint);
        }
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

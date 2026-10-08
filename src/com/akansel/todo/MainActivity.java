/*
 * Yapılacaklar
 * © 2026 by AKANSEL
 */
package com.akansel.todo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int PRIMARY = 0xFF3F51B5;
    private static final String PREFS = "todo_prefs";
    private static final String KEY_ITEMS = "items";

    private static class Task {
        String text;
        boolean done;
        Task(String text, boolean done) { this.text = text; this.done = done; }
    }

    private final List<Task> tasks = new ArrayList<>();
    private LinearLayout listContainer;
    private LinearLayout inputRow;
    private EditText input;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadTasks();

        final LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF2F3F7);

        // Üst çubuk: başlık + artı + çöp kutusu
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(PRIMARY);
        topBar.setPadding(dp(16), dp(8), dp(8), dp(8));

        TextView title = new TextView(this);
        title.setText("Yapılacaklar");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        topBar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ImageButton addBtn = iconButton(R.drawable.ic_add, "Ekle");
        addBtn.setOnClickListener(v -> toggleInput());
        topBar.addView(addBtn);

        ImageButton deleteBtn = iconButton(R.drawable.ic_delete, "Sil");
        deleteBtn.setOnClickListener(v -> deleteCompleted());
        topBar.addView(deleteBtn);

        root.addView(topBar);

        // Yazı girme satırı (artıya basınca açılır)
        inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setBackgroundColor(Color.WHITE);
        inputRow.setPadding(dp(16), dp(8), dp(8), dp(8));
        inputRow.setElevation(dp(2));
        inputRow.setVisibility(View.GONE);

        input = new EditText(this);
        input.setHint("Yapılacak işi yaz...");
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                saveInput();
                return true;
            }
            return false;
        });
        inputRow.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ImageButton saveBtn = new ImageButton(this);
        saveBtn.setImageResource(R.drawable.ic_check);
        saveBtn.setContentDescription("Kaydet");
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFF4CAF50);
        saveBtn.setBackground(circle);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        saveLp.leftMargin = dp(8);
        saveBtn.setOnClickListener(v -> saveInput());
        inputRow.addView(saveBtn, saveLp);

        root.addView(inputRow);

        // Liste
        ScrollView scroll = new ScrollView(this);
        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(dp(12), dp(12), dp(12), dp(12));

        emptyView = new TextView(this);
        emptyView.setText("Henüz yapılacak bir şey yok.\nEklemek için üstteki + işaretine dokun.");
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setTextColor(0xFF888888);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        emptyView.setPadding(dp(16), dp(48), dp(16), dp(16));
        scrollContent.addView(emptyView);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollContent.addView(listContainer);

        scroll.addView(scrollContent);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        TextView footer = new TextView(this);
        footer.setText("© 2026 by AKANSEL");
        footer.setGravity(Gravity.CENTER);
        footer.setTextColor(0xFFAAAAAA);
        footer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        footer.setPadding(0, dp(6), 0, dp(8));
        root.addView(footer);

        // Durum çubuğu / klavye boşlukları (Android 15 kenardan kenara ekran)
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            topBar.setPadding(dp(16), dp(8) + insets.getSystemWindowInsetTop(), dp(8), dp(8));
            v.setPadding(insets.getSystemWindowInsetLeft(), 0,
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });

        setContentView(root);
        refreshList();
    }

    private void toggleInput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputRow.getVisibility() == View.VISIBLE) {
            inputRow.setVisibility(View.GONE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        } else {
            inputRow.setVisibility(View.VISIBLE);
            input.requestFocus();
            input.post(() -> imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT));
        }
    }

    private void saveInput() {
        String text = input.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Önce bir şey yaz", Toast.LENGTH_SHORT).show();
            return;
        }
        tasks.add(0, new Task(text, false));
        input.setText("");
        persist();
        refreshList();
        inputRow.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    private void deleteCompleted() {
        int count = 0;
        for (Task t : tasks) if (t.done) count++;
        if (count == 0) {
            Toast.makeText(this, "Silmek için önce tamamlananları işaretle", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Sil")
                .setMessage(count + " tamamlanan iş silinsin mi?")
                .setPositiveButton("Sil", (d, w) -> {
                    for (int i = tasks.size() - 1; i >= 0; i--) {
                        if (tasks.get(i).done) tasks.remove(i);
                    }
                    persist();
                    refreshList();
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void refreshList() {
        listContainer.removeAllViews();
        emptyView.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        for (final Task task : tasks) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(16), dp(12), dp(8), dp(12));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(10));
            card.setBackground(bg);
            card.setElevation(dp(1));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dp(8);

            final TextView text = new TextView(this);
            text.setText(task.text);
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            styleText(text, task.done);
            card.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            final CheckBox check = new CheckBox(this);
            check.setChecked(task.done);
            check.setOnCheckedChangeListener((b, isChecked) -> {
                task.done = isChecked;
                styleText(text, isChecked);
                persist();
            });
            card.addView(check);

            card.setOnClickListener(v -> check.toggle());
            listContainer.addView(card, cardLp);
        }
    }

    private void styleText(TextView tv, boolean done) {
        if (done) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setTextColor(0xFF9E9E9E);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setTextColor(0xFF212121);
        }
    }

    private ImageButton iconButton(int res, String desc) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(res);
        b.setContentDescription(desc);
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
        b.setBackgroundResource(tv.resourceId);
        b.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        return b;
    }

    private void loadTasks() {
        tasks.clear();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_ITEMS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                tasks.add(new Task(o.getString("t"), o.optBoolean("d", false)));
            }
        } catch (Exception ignored) {
        }
    }

    private void persist() {
        JSONArray arr = new JSONArray();
        try {
            for (Task t : tasks) {
                JSONObject o = new JSONObject();
                o.put("t", t.text);
                o.put("d", t.done);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_ITEMS, arr.toString()).apply();
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }
}

package com.davidik.files;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;

import java.io.*;
import java.util.zip.*;

public class MainActivity extends Activity {

    LinearLayout root, content;
    TextView status;

    static final int PICK_FILE = 100;
    static final int PICK_FOLDER = 101;

    Uri archiveUri;
    String archiveName;

    int green = Color.rgb(0, 220, 120);
    int bg = Color.rgb(10, 12, 12);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUI();
    }

    int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density);
    }

    TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        return t;
    }

    Button button(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(28, 65, 48)
            )
        );
        return b;
    }

    void buildUI() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(10));
        root.setBackgroundColor(bg);

        TextView logo = text("📁 DaviFiles", 27, green);
        logo.setTypeface(null, Typeface.BOLD);
        root.addView(logo);

        root.addView(text(
            "File Manager • Archive Viewer",
            13, Color.LTGRAY
        ));

        View line = new View(this);
        line.setBackgroundColor(Color.rgb(40, 70, 55));
        root.addView(line, new LinearLayout.LayoutParams(
            -1, dp(1)
        ));

        root.addView(text("Мои файлы", 21, Color.WHITE));

        addButton("📂 Открыть файл", () -> openFile());

        addButton("📄 Просмотреть TXT", () -> openFile());

        addButton("📦 Открыть ZIP", () -> openFile());

        addButton("🗜️ Извлечь ZIP", () -> openFile());

        addButton("📚 Открыть RAR", () -> {
            new AlertDialog.Builder(this)
                .setTitle("RAR")
                .setMessage("Поддержку RAR добавим следующим этапом.")
                .setPositiveButton("OK", null)
                .show();
        });

        status = text(
            "Выбери действие, чтобы начать.",
            14, Color.LTGRAY
        );

        root.addView(status);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);

        root.addView(scroll, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        setContentView(root);
    }

    void addButton(String title, Runnable action) {
        Button b = button(title);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, dp(55));

        p.setMargins(0, dp(7), 0, dp(7));

        root.addView(b, p);
        b.setOnClickListener(v -> action.run());
    }

    void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, PICK_FILE);
    }

    void openFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, PICK_FOLDER);
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) return;

        Uri uri = data.getData();

        if (requestCode == PICK_FILE) {
            getContentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            String name = getName(uri);

            if (name.toLowerCase().endsWith(".txt")) {
                readTxt(uri);
            } else if (name.toLowerCase().endsWith(".zip")) {
                archiveUri = uri;
                archiveName = name;
                showZip(uri);
            } else if (name.toLowerCase().endsWith(".rar")) {
                status.setText(
                    "RAR выбран: поддержку распаковки добавим отдельно."
                );
            } else {
                status.setText("Формат пока не поддерживается: " + name);
            }
        }

        if (requestCode == PICK_FOLDER && archiveUri != null) {
            extractZip(archiveUri, uri);
        }
    }

    String getName(Uri uri) {
        String result = "file";

        android.database.Cursor cursor =
            getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    );

                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        return result;
    }

    void readTxt(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int n;

            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }

            in.close();

            String contentText = out.toString("UTF-8");

            content.removeAllViews();
            content.addView(text(contentText, 14, Color.WHITE));

            status.setText("Открыт TXT: " + getName(uri));

        } catch (Exception e) {
            status.setText("Ошибка чтения: " + e.getMessage());
        }
    }

    void showZip(Uri uri) {
        try {
            ZipInputStream zip = new ZipInputStream(
                getContentResolver().openInputStream(uri)
            );

            content.removeAllViews();

            content.addView(text(
                "Содержимое архива: " + getName(uri),
                17, green
            ));

            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();

                content.addView(text(
                    (entry.isDirectory() ? "📁 " : "📄 ") + name,
                    14, Color.WHITE
                ));

                zip.closeEntry();
            }

            zip.close();

            status.setText("ZIP открыт. Можно извлечь файлы.");

            addExtractButton();

        } catch (Exception e) {
            status.setText("Ошибка ZIP: " + e.getMessage());
        }
    }

    void addExtractButton() {
        Button extract = button("📂 Выбрать папку и извлечь");

        content.addView(extract);

        extract.setOnClickListener(v -> openFolder());
    }

    void extractZip(Uri source, Uri folderUri) {
        try {
            DocumentFile folder =
                DocumentFile.fromTreeUri(this, folderUri);

            if (folder == null || !folder.canWrite()) {
                status.setText("Нет доступа к папке.");
                return;
            }

            InputStream input =
                getContentResolver().openInputStream(source);

            ZipInputStream zip = new ZipInputStream(input);

            ZipEntry entry;
            int count = 0;

            while ((entry = zip.getNextEntry()) != null) {
                String path = entry.getName();

                if (path.startsWith("/") ||
                    path.contains("\\") ||
                    path.contains("../") ||
                    path.equals("..") ||
                    path.contains("/..")) {
                    zip.closeEntry();
                    continue;
                }

                String[] parts = path.split("/");
                DocumentFile current = folder;

                boolean valid = true;

                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];

                    if (part.isEmpty() || part.equals(".")) continue;

                    boolean last = i == parts.length - 1;

                    if (last && !entry.isDirectory()) {
                        DocumentFile file =
                            current.createFile(
                                "application/octet-stream",
                                part
                            );

                        if (file != null) {
                            OutputStream output =
                                getContentResolver().openOutputStream(
                                    file.getUri()
                                );

                            if (output != null) {
                                byte[] buffer = new byte[8192];
                                int len;

                                while ((len = zip.read(buffer)) != -1) {
                                    output.write(buffer, 0, len);
                                }

                                output.close();
                                count++;
                            }
                        }
                    } else {
                        DocumentFile next = current.findFile(part);

                        if (next == null) {
                            next = current.createDirectory(part);
                        }

                        if (next == null) {
                            valid = false;
                            break;
                        }

                        current = next;
                    }
                }

                zip.closeEntry();
            }

            zip.close();

            status.setText("Готово! Извлечено файлов: " + count);

            new AlertDialog.Builder(this)
                .setTitle("Распаковка завершена")
                .setMessage("Извлечено файлов: " + count)
                .setPositiveButton("Отлично", null)
                .show();

        } catch (Exception e) {
            status.setText("Ошибка распаковки: " + e.getMessage());
        }
    }
}

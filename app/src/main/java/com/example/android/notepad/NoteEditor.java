/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.OutputStream;


public class NoteEditor extends Activity {
    // For logging and debugging purposes
    private static final String TAG = "NoteEditor";


    private static final String[] PROJECTION =
        new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
    };

    // A label for the saved state of the activity
    private static final String ORIGINAL_CONTENT = "origContent";


    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    // Global mutable variables
    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private String mOriginalContent;

    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        // This constructor is used by LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }


        @Override
        protected void onDraw(Canvas canvas) {

            // Gets the number of lines of text in the View.
            int count = getLineCount();

            // Gets the global Rect and Paint objects
            Rect r = mRect;
            Paint paint = mPaint;

            for (int i = 0; i < count; i++) {

                // Gets the baseline coordinates for the current line of text
                int baseline = getLineBounds(i, r);

                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            // Finishes up by calling the parent method
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_EDIT.equals(action)) {
            mState = STATE_EDIT;
            mUri = intent.getData();
        } else if (Intent.ACTION_INSERT.equals(action) || Intent.ACTION_PASTE.equals(action)) {
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);
            if (mUri == null) {
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
                finish();
                return;
            }
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
        } else {
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        // Initialize the cursor to query the note data
        mCursor = managedQuery(
                mUri,
                PROJECTION,
                null,
                null,
                null
        );

        if (Intent.ACTION_PASTE.equals(action)) {
            performPaste();
            mState = STATE_EDIT;
        }

        setContentView(R.layout.note_editor);
        mText = (EditText) findViewById(R.id.note);

        // 恢复背景资源
        SharedPreferences preferences = getSharedPreferences("NoteEditorPrefs", MODE_PRIVATE);
        int backgroundResId = preferences.getInt("backgroundResId", -1); // 默认值为 -1
        if (backgroundResId != -1) {
            mText.setBackgroundResource(backgroundResId);
            mText.setTag(backgroundResId);  // 恢复背景资源 ID
        }

        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (mCursor != null) {
            // 恢复背景资源
            SharedPreferences preferences = getSharedPreferences("NoteEditorPrefs", MODE_PRIVATE);
            int backgroundResId = preferences.getInt("backgroundResId", -1); // 默认值为 -1
            if (backgroundResId != -1) {
                mText.setBackgroundResource(backgroundResId);
                mText.setTag(backgroundResId);  // 恢复背景资源 ID
            }

            // 重新查询笔记数据
            mCursor.requery();
            mCursor.moveToFirst();

            if (mState == STATE_EDIT) {
                // 设置活动标题为笔记标题
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String note = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(note);

            if (mOriginalContent == null) {
                mOriginalContent = note;
            }
        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int backgroundResId = (Integer) mText.getTag();  // 存储背景资源的 ID 在 Tag 中
        outState.putInt("backgroundResId", backgroundResId);
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mCursor != null) {
            // 保存背景资源 ID
            int backgroundResId = (Integer) mText.getTag();
            SharedPreferences preferences = getSharedPreferences("NoteEditorPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("backgroundResId", backgroundResId);
            editor.apply();  // 保存背景设置

            // 获取当前的笔记内容
            String text = mText.getText().toString();
            int length = text.length();

            if (isFinishing() && (length == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();
            } else if (mState == STATE_EDIT) {
                // 更新笔记
                updateNote(text, null);
            } else if (mState == STATE_INSERT) {
                updateNote(text, text);
                mState = STATE_EDIT;
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        // Only add extra menu items for a saved note 
        if (mState == STATE_EDIT) {
            Intent intent = new Intent(null, mUri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }

        return super.onCreateOptionsMenu(menu);
    }



    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
        String savedNote = mCursor.getString(colNoteIndex);
        String currentNote = mText.getText().toString();
        if (savedNote.equals(currentNote)) {
            menu.findItem(R.id.menu_revert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_revert).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
        case R.id.menu_save:
            String text = mText.getText().toString();
            updateNote(text, null);
            finish();
            break;
        case R.id.menu_delete:
            deleteNote();
            finish();
            break;
        case R.id.menu_revert:
            cancelNote();
            break;
        case R.id.menu_export:
            export();
            break;
        case R.id.menu_background:
            showBackgroundImagePickerDialog();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void showBackgroundImagePickerDialog() {
        final int[] imageResIds = {
                R.drawable.ic_background_back1,
                R.drawable.ic_background_back2,
                R.drawable.ic_background_back3
        };

        final String[] imageNames = {"竹", "湖", "景"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Background Image");

        builder.setItems(imageNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 根据选择的图片资源 ID 设置背景
                int selectedResId = imageResIds[which];
                mText.setBackgroundResource(selectedResId);

                // 保存选择的背景资源 ID
                SharedPreferences preferences = getSharedPreferences("NoteEditorPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt("backgroundResId", selectedResId);
                editor.apply();
            }
        });

        builder.show();
    }



    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            // 恢复背景资源 ID
            int backgroundResId = savedInstanceState.getInt("backgroundResId", -1);
            if (backgroundResId != -1) {
                mText.setBackgroundResource(backgroundResId);
                mText.setTag(backgroundResId);  // 恢复背景资源 ID
            }
        }
    }

    private static final int REQUEST_CODE_EXPORT = 100; // 100 是一个随意选定的
    private void export() {
        // 创建一个输入框
        final EditText input = new EditText(this);
        input.setHint("请输入文件名");

        // 弹出对话框
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("导出笔记")
                .setView(input)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        String fileName = input.getText().toString().trim();
                        if (fileName.isEmpty()) {
                            Toast.makeText(NoteEditor.this, "文件名不能为空", Toast.LENGTH_SHORT).show();
                        } else {
                            // 启动文件选择器
                            openFilePicker(fileName);
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_EXPORT && resultCode == RESULT_OK) {
            Uri fileUri = data.getData();

            if (fileUri != null) {
                saveNoteToFile(fileUri);
            } else {
                Toast.makeText(this, "文件创建失败", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void saveNoteToFile(Uri fileUri) {
        try {
            // 获取笔记内容
            String noteContent = mText.getText().toString();

            // 打开输出流并写入数据
            try (OutputStream outputStream = getContentResolver().openOutputStream(fileUri)) {
                if (outputStream != null) {
                    outputStream.write(noteContent.getBytes());
                    outputStream.flush();
                    Toast.makeText(this, "笔记导出成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openFilePicker(String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName + ".txt"); // 用户输入的文件名
        startActivityForResult(intent, REQUEST_CODE_EXPORT);
    }


    private final void performPaste() {

        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        ContentResolver cr = getContentResolver();

        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {

            String text=null;
            String title=null;

            ClipData.Item item = clip.getItemAt(0);
            Uri uri = item.getUri();
            if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {
                Cursor orig = cr.query(
                        uri,            // URI for the content provider
                        PROJECTION,     // Get the columns referred to in the projection
                        null,           // No selection variables
                        null,           // No selection variables, so no criteria are needed
                        null            // Use the default sort order
                );

                if (orig != null) {
                    if (orig.moveToFirst()) {
                        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                        int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                        text = orig.getString(colNoteIndex);
                        title = orig.getString(colTitleIndex);
                    }

                    // Closes the cursor.
                    orig.close();
                }
            }
            if (text == null) {
                text = item.coerceToText(this).toString();
            }

            // Updates the current note with the retrieved title and text.
            updateNote(text, title);
        }
    }

    private final void updateNote(String text, String title) {

        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

        if (mState == STATE_INSERT) {
            if (title == null) {
                int length = text.length();

                title = text.substring(0, Math.min(30, length));

                if (length > 30) {
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        title = title.substring(0, lastSpace);
                    }
                }
            }
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        } else if (title != null) {
            // In the values map, sets the value of the title
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        }

        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

        getContentResolver().update(
                mUri,    // The URI for the record to update.
                values,  // The map of column names and new values to apply to them.
                null,    // No selection criteria are used, so no where columns are necessary.
                null     // No where columns are used, so no where arguments are necessary.
            );


    }

    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                // Put the original note text back into the database
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);
                getContentResolver().update(mUri, values, null, null);
            } else if (mState == STATE_INSERT) {
                // We inserted an empty note, make sure to delete it
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }
}

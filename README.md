#  NotepadMaster 笔记应用

## 简介
NotepadMaster 是一款基于 Google Notepad Master 开发的安卓笔记应用，旨在帮助用户高效管理个人笔记。应用主要提供了笔记的基本管理功能，包括记录笔记时间戳、搜索功能、笔记分类和标签管理等，便于用户查看历史记录并快速定位需要的信息。

## 功能特性

### 基础功能
- **时间戳**：每次保存笔记时自动生成时间戳，方便记录笔记时间。
- **搜索功能**：快速搜索笔记内容，帮助你高效找到所需信息。

### 附加功能
- **UI 美化**：简洁且美观的用户界面设计，提供舒适的使用体验。
- **笔记内容导出**：支持将笔记内容导出为文本文件，便于备份和分享。

## 功能实现
### ⏳ 显示时间戳
**功能描述**
应用会自动在每次保存笔记时记录修改时间。时间戳格式为 yyyy-MM-dd HH:mm，例如：2024-12-01 14:30，以方便追踪笔记的修改历史。
在笔记列表中，每条笔记都会显示它的最后修改时间，帮助你快速查看最新的笔记。

**实现原理**：  
   - 获取当前时间戳并格式化为用户友好的日期时间格式。  
   - 使用 SQLite 存储笔记内容和时间戳信息，并在笔记列表中动态加载显示。
   - 
**实现代码**
    -主要是将修改时间从sqlite数据库中读取，然后映射到笔记中，这里只展示获取时间戳和转换时间格式的代码
    1、 为了将时间戳以友好的格式显示，需要进行自定义绑定。在这里，使用了 SimpleCursorAdapter 将数据库中的列映射到视图上的对应位置：
``` java
String[] dataColumns = {
    NotePad.Notes.COLUMN_NAME_TITLE,              // 标题
    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,  // 修改时间
    NotePad.Notes.COLUMN_NAME_NOTE                // 内容
};
int[] viewIDs = {
    android.R.id.text1,    // 映射标题
    android.R.id.text2,    // 映射修改时间
    R.id.content           // 映射内容
};
 ```
2、将 SimpleCursorAdapter 绑定到 ListView 后，通过 setViewBinder 方法实现自定义显示修改时间的逻辑：
``` java
adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
    @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (columnIndex == cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
            long timestamp = cursor.getLong(columnIndex); // 从游标获取时间戳
            String formattedDate = formatDate(timestamp); // 调用格式化方法
            TextView textView = (TextView) view;
            textView.setText(formattedDate); // 设置显示格式化后的日期

            return true; // 表示该列已处理
        }
        return false; // 对其他列不做处理
    }
});
```
3、 格式化时间戳
在 setViewBinder() 中，调用 formatDate() 方法将时间戳转换为易读的日期格式：

``` java
private String formatDate(long timestamp) {
    // 创建一个日期格式化器，指定格式为 yyyy-MM-dd HH:mm
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    // 将时间戳转换为 Date 对象，并格式化为字符串
    return dateFormat.format(new Date(timestamp));
}
```
**功能截图**

![image](https://github.com/user-attachments/assets/d9d09f6d-8bf7-4ce0-80a4-dee00b1d69fb)
---------------------------------------------------------------------------------------------------------


### 🔍 搜索功能
**功能描述**
      在主界面中，点击搜索按钮，输入关键字即可检索笔记标题。支持模糊查询功能，无需输入完整标题即可找到相关笔记，适合大量笔记管理。

   **实现原理**：  
   - 在 SQLite 中使用 `LIKE` 语句进行关键字匹配查询。  
   - 搜索结果会实时更新，并通过 Cursor 加载数据到 UI 层展示。
     
**代码实现**
1. 触发搜索功能的菜单项
``` java
case R.id.menu_search:
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("搜索笔记");

    final EditText input = new EditText(this);
    builder.setView(input);

    builder.setPositiveButton("搜索", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            String searchQuery = input.getText().toString().trim();
            if (!searchQuery.isEmpty()) {
                Intent intent = new Intent(NotesList.this, NotesList.class);
                intent.putExtra("searchQuery", searchQuery); // 将搜索内容传递给NotesList
                startActivity(intent);
            } else {
                Toast.makeText(NotesList.this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            }
        }
    });
    builder.setNegativeButton("取消", null);
    // 显示对话框
    builder.show();
    return true;
```
2. 处理搜索关键字
``` java
String searchQuery = getIntent().getStringExtra("searchQuery");
```
这个代码用于在 NotesList 的 onCreate 方法中获取传递来的 searchQuery，即用户输入的搜索关键字。

3. 根据关键字执行查询
``` java
String selection = null;
String[] selectionArgs = null;
if (searchQuery != null && !searchQuery.isEmpty()) {
    selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?"; // 根据标题过滤
    selectionArgs = new String[]{"%" + searchQuery + "%"}; // 搜索关键字
}
```

**功能截图**

![image](https://github.com/user-attachments/assets/f19568e6-63ef-4618-b4f8-6791338995f5)
--------------------------------------------------------------------------------------------

### UI美化
#### 内容映射至笔记上
**功能描述**
能够在笔记首页浏览到笔记内容开头

**代码实现**
因代码实现与时间戳功能类似，在此不再过多描述

**功能截图**

![image](https://github.com/user-attachments/assets/61d16988-d59a-4380-9687-80fbc9d62860)
------------------------------------------------------------------------------------------

#### 更改笔记背景

**功能描述**
在笔记界面中，可以选择更改笔记背景

**代码实现**
1. 展示背景选择对话框
背景选择对话框让用户选择背景图片，并更新笔记编辑界面的背景。相关的代码部分在 showBackgroundImagePickerDialog() 方法中
``` java
private void showBackgroundImagePickerDialog() {
    // 定义可供选择的背景图片资源 ID 数组
    final int[] imageResIds = {
        R.drawable.ic_background_back1,
        R.drawable.ic_background_back2,
        R.drawable.ic_background_back3
    };

    // 定义背景图片名称数组
    final String[] imageNames = {"竹", "湖", "景"};

    // 创建对话框
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Choose Background Image");

    // 设置对话框选项和点击事件
    builder.setItems(imageNames, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // 根据用户选择的图片更新背景
            int selectedResId = imageResIds[which];
            mText.setBackgroundResource(selectedResId);

            // 保存用户选择的背景资源 ID 到 SharedPreferences
            SharedPreferences preferences = getSharedPreferences("NoteEditorPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("backgroundResId", selectedResId);
            editor.apply(); // 保存背景资源设置
        }
    });

    // 显示对话框
    builder.show();
}
```

2. 保存背景资源 ID
用户选择的背景图片会被保存到 SharedPreferences，以便应用重启时可以恢复该设置。在 onPause() 和 showBackgroundImagePickerDialog() 方法中都有保存背景资源 ID 的逻辑：
``` java
// 保存背景资源 ID
int backgroundResId = (Integer) mText.getTag();
SharedPreferences preferences = getSharedPreferences("NoteEditorPrefs", MODE_PRIVATE);
SharedPreferences.Editor editor = preferences.edit();
editor.putInt("backgroundResId", backgroundResId);
editor.apply();  // 保存背景设置
```
putInt()：将背景资源 ID 存储到 SharedPreferences 中，键名为 "backgroundResId"。

3. 恢复背景设置
当应用再次启动或笔记界面恢复时，通过 onCreate() 和 onResume() 方法从 SharedPreferences 中获取之前保存的背景资源 ID，并重新设置背景：
``` java
SharedPreferences preferences = getSharedPreferences("NoteEditorPrefs", MODE_PRIVATE);
int backgroundResId = preferences.getInt("backgroundResId", -1); // 默认值为 -1
if (backgroundResId != -1) {
    mText.setBackgroundResource(backgroundResId);
    mText.setTag(backgroundResId);  // 恢复背景资源 ID
}
```
getInt()：从 SharedPreferences 中读取存储的背景资源 ID。如果读取到的背景资源 ID 不为默认值 -1，则调用 mText.setBackgroundResource() 方法重新设置背景。
setTag()：为了在其他地方方便存取背景资源 ID，背景资源 ID 被存储在 EditText 的 Tag 中。

4. 保存和恢复实例状态
如果应用因系统状态变化（如屏幕旋转）而销毁，系统会调用 onSaveInstanceState() 保存当前背景设置，并在 onRestoreInstanceState() 恢复：
``` java
@Override
protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    int backgroundResId = (Integer) mText.getTag();  // 存储背景资源的 ID 在 Tag 中
    outState.putInt("backgroundResId", backgroundResId);
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
```
***功能截图***

![image](https://github.com/user-attachments/assets/522c5c05-946f-4e7f-be01-037eabaebb86)
---------------------------------------------------------------------------------------

### 文件导出

**功能描述**
在笔记菜单中选择export功能，笔记将会被以.txt文件的形式导出至storage/download下

**代码实现**

1. 文件导出功能的触发：
导出功能通过 menu_export 选项在菜单中触发
``` java
case R.id.menu_export:
    export();
    break;
```
当用户选择“导出”菜单项时，调用了 export() 方法。

2. export() 方法：
export() 方法展示一个对话框，提示用户输入文件名：
``` java
private void export() {
    final EditText input = new EditText(this);
    input.setHint("请输入文件名");

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
```
首先，EditText 组件让用户输入文件名。
如果用户点击“确定”，系统会检查文件名是否为空。如果为空，提示错误信息；否则，调用 openFilePicker(fileName) 方法来启动文件选择器，让用户选择保存文件的路径。

3. 打开文件选择器：openFilePicker(fileName) 方法：
该方法使用 Intent.ACTION_CREATE_DOCUMENT 启动系统的文件选择器，并允许用户创建新的文本文件：
``` java
private void openFilePicker(String fileName) {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("text/plain");
    intent.putExtra(Intent.EXTRA_TITLE, fileName + ".txt"); // 用户输入的文件名
    startActivityForResult(intent, REQUEST_CODE_EXPORT);
}
```
这个 Intent 用于创建一个新的文档，文件类型为纯文本 (text/plain)。
文件名来自用户输入，后缀为 .txt。
startActivityForResult() 启动文件选择器，并等待用户选择位置后返回结果。

4. 处理文件选择结果：
当用户在文件选择器中选择了保存位置，系统将调用 onActivityResult() 方法
``` java
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
```
首先检查 requestCode 是否为导出文件的请求码 (REQUEST_CODE_EXPORT)。
如果结果是 RESULT_OK，则从 data 中获取保存的文件 Uri，然后调用 saveNoteToFile(fileUri) 方法将笔记内容写入文件。

5. 保存笔记内容到文件：saveNoteToFile() 方法：
``` java
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
```
该方法通过 fileUri 打开一个输出流。
然后，它从 EditText 获取当前的笔记内容 (noteContent)，并将其转换为字节数组写入输出流中。
成功写入后，显示“笔记导出成功”的提示，否则提示相应的错误信息。

**功能截图**

![image](https://github.com/user-attachments/assets/5d5a97e9-e10a-4acd-94e0-997da23c890d) ![image](https://github.com/user-attachments/assets/6c1266c3-4033-4ab0-8f19-d341d5095c1c)

### github仓库地址：https://github.com/wenkaicao/NotePad_new-master
### 作者：曹文凯

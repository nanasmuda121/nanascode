# Proguard rules for feature:editor
-keepclassmembers class com.nanascode.app.feature.editor.EditorJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}

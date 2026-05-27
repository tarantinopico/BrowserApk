package com.example.ui.browser

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import com.example.MainActivity

class CustomWebChromeClient(
    private val tabId: String,
    private val onProgressChanged: (Int) -> Unit,
    private val onReceivedTitle: (String?) -> Unit,
    private val onReceivedIcon: (Bitmap?) -> Unit,
    private val onShowCustomView: (View?, WebChromeClient.CustomViewCallback?) -> Unit,
    private val onHideCustomView: () -> Unit,
    private val requestFileLaunch: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        onReceivedTitle(title)
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        onReceivedIcon(icon)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        callback?.invoke(origin, true, false)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        // Grant all for demonstration if system permissions are granted. 
        // In a real production app we'd prompt the user.
        request?.grant(request.resources)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        onHideCustomView()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        requestFileLaunch(filePathCallback, fileChooserParams)
        return true
    }

    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        // Implement standard AlertDialog if needed, or just let default behavior work by returning false.
        // Returning false lets the WebView show a default alert.
        return false 
    }

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return false
    }

    override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
        return false
    }
}

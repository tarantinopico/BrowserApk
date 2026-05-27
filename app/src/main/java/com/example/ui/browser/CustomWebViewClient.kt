package com.example.ui.browser

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import android.widget.Toast

class CustomWebViewClient(
    private val context: Context,
    private val tabId: String,
    private val onPageStartedUpdate: (String?, Bitmap?, Boolean) -> Unit,
    private val onPageFinishedUpdate: (String?, String?, Boolean, Boolean) -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        
        // Handle custom schemes
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Ignore unknown intents or show toast
                Toast.makeText(context, "Could not handle intent", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return false
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        url ?: return false
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                return true
            }
        }
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        val isSecure = url?.startsWith("https://") == true
        onPageStartedUpdate(url, favicon, isSecure)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.let {
            onPageFinishedUpdate(url, it.title, it.canGoBack(), it.canGoForward())
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
        // In production, we'd show a warning dialog. For now we proceed for compatibility or block.
        // It's safer to cancel.
        handler?.cancel()
        Toast.makeText(context, "SSL Certificate Error", Toast.LENGTH_SHORT).show()
    }

    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onSafeBrowsingHit(view: WebView?, request: WebResourceRequest?, threatType: Int, callback: SafeBrowsingResponse?) {
        // Must back safely away from the malicious site
        callback?.backToSafety(true)
        Toast.makeText(context, "Unsafe site blocked", Toast.LENGTH_LONG).show()
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        // We could load a custom error page here
        // if (request?.isForMainFrame == true) {
        //     view?.loadUrl("file:///android_asset/error.html")
        // }
    }
}

package com.lotato.bridge

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var sunmiPrinterService: SunmiPrinterService? = null

    private val innerPrinterCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService) {
            sunmiPrinterService = service
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "✅ Imprimante prête",
                    Toast.LENGTH_SHORT
                ).show()
                webView.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('sunmi-ready'));",
                    null
                )
            }
        }

        override fun onDisconnected() {
            sunmiPrinterService = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initPrinter()

        webView = findViewById(R.id.webview)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
        }

        webView.addJavascriptInterface(PrintBridge(), "SunmiBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                view?.loadData(offlinePage(), "text/html; charset=UTF-8", null)
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl("https://lotato1.onrender.com")
    }

    private fun initPrinter() {
        try {
            val result = InnerPrinterManager.getInstance().bindService(
                this,
                innerPrinterCallback
            )
            if (!result) {
                Toast.makeText(
                    this,
                    "Pas d'imprimante Sunmi détectée",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: InnerPrinterException) {
            e.printStackTrace()
        }
    }

    inner class PrintBridge {

        @JavascriptInterface
        fun isConnected(): Boolean = sunmiPrinterService != null

        @JavascriptInterface
        fun printTicket(jsonStr: String) {
            val service = sunmiPrinterService
            if (service == null) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Imprimante non connectée",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
            try {
                val json = org.json.JSONObject(jsonStr)
                val header = json.optString("header", "LOTATO PRO")
                val footer = json.optString("footer", "")
                val lines = json.optJSONArray("lines")

                service.printerInit(null)

                service.setAlignment(1, null)
                service.setFontSize(28f, null)
                service.printText("$header\n", null)
                service.setFontSize(18f, null)
                service.printText("================================\n", null)

                service.setAlignment(0, null)
                service.setFontSize(20f, null)
                if (lines != null) {
                    for (i in 0 until lines.length()) {
                        service.printText("${lines.getString(i)}\n", null)
                    }
                }

                if (footer.isNotEmpty()) {
                    service.setAlignment(1, null)
                    service.printText("================================\n", null)
                    service.setFontSize(18f, null)
                    service.printText("$footer\n", null)
                }

                service.lineWrap(3, null)
                service.cutPaper(null)

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Erreur: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        @JavascriptInterface
        fun printText(text: String) {
            val service = sunmiPrinterService ?: return
            try {
                service.printerInit(null)
                service.setAlignment(0, null)
                service.setFontSize(20f, null)
                service.printText("$text\n", null)
                service.lineWrap(3, null)
                service.cutPaper(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun offlinePage() = """
        <!DOCTYPE html><html><head><meta charset="UTF-8">
        <style>
          body{font-family:sans-serif;text-align:center;padding:40px;background:#0D1117;color:white}
          h1{color:#F0A500}
          button{padding:14px 28px;background:#F0A500;border:none;color:#000;
                 font-size:16px;border-radius:8px;margin-top:20px}
        </style></head><body>
          <h1>LOTATO PRO</h1>
          <p>Pas de connexion internet.</p>
          <button onclick="location.reload()">Réessayer</button>
        </body></html>
    """.trimIndent()

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            InnerPrinterManager.getInstance().unBindService(this, innerPrinterCallback)
        } catch (e: InnerPrinterException) {
            e.printStackTrace()
        }
    }
}

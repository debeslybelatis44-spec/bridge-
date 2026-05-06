package com.lotato.bridge

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
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

    // Méthode 1: InnerPrinterManager (SDK officiel)
    private val innerPrinterCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService) {
            sunmiPrinterService = service
            runOnUiThread {
                Toast.makeText(this@MainActivity, "✅ Imprimante connectée (SDK)", Toast.LENGTH_SHORT).show()
                webView.evaluateJavascript("window.dispatchEvent(new CustomEvent('sunmi-ready'));", null)
            }
        }
        override fun onDisconnected() {
            sunmiPrinterService = null
        }
    }

    // Méthode 2: Connexion directe via AIDL (fallback)
    private var aidlBinder: IBinder? = null
    private val aidlConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            aidlBinder = binder
            runOnUiThread {
                Toast.makeText(this@MainActivity, "✅ Imprimante connectée (AIDL)", Toast.LENGTH_SHORT).show()
                webView.evaluateJavascript("window.dispatchEvent(new CustomEvent('sunmi-ready'));", null)
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            aidlBinder = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Essayer les deux méthodes de connexion
        connectPrinter()

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
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                view?.loadData(offlinePage(), "text/html; charset=UTF-8", null)
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl("https://lotato1.onrender.com")
    }

    private fun connectPrinter() {
        // Méthode 1: SDK officiel
        try {
            val ret = InnerPrinterManager.getInstance().bindService(this, innerPrinterCallback)
            if (ret) return // Succès, pas besoin de la méthode 2
        } catch (e: InnerPrinterException) {
            e.printStackTrace()
        }

        // Méthode 2: Connexion AIDL directe au service SunmiPrinter
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.sunmi.innerprinter",
                    "com.sunmi.innerprinter.SunmiPrinterService"
                )
            }
            val bound = bindService(intent, aidlConnection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                // Méthode 3: Autre package possible
                val intent2 = Intent().apply {
                    component = ComponentName(
                        "woyou.aidlservice.jiuiv5",
                        "woyou.aidlservice.jiuiv5.AidlService"
                    )
                }
                bindService(intent2, aidlConnection, Context.BIND_AUTO_CREATE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erreur connexion: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    inner class PrintBridge {

        @JavascriptInterface
        fun isConnected(): Boolean = sunmiPrinterService != null || aidlBinder != null

        @JavascriptInterface
        fun printTicket(jsonStr: String) {
            // Utiliser SDK si disponible
            if (sunmiPrinterService != null) {
                printWithSDK(jsonStr)
            } else if (aidlBinder != null) {
                printWithAIDL(jsonStr)
            } else {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Imprimante non connectée", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun printWithSDK(jsonStr: String) {
            val service = sunmiPrinterService ?: return
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
                    service.printText("$footer\n", null)
                }

                service.lineWrap(3, null)
                service.cutPaper(null)

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Erreur SDK: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        private fun printWithAIDL(jsonStr: String) {
            val binder = aidlBinder ?: return
            try {
                val json = org.json.JSONObject(jsonStr)
                val header = json.optString("header", "LOTATO PRO")
                val footer = json.optString("footer", "")
                val lines = json.optJSONArray("lines")

                fun tx(code: Int, block: android.os.Parcel.() -> Unit) {
                    val data = android.os.Parcel.obtain()
                    val reply = android.os.Parcel.obtain()
                    try {
                        data.writeInterfaceToken("woyou.aidlservice.jiuiv5.IWoyouService")
                        data.block()
                        binder.transact(code, data, reply, 0)
                    } finally {
                        data.recycle()
                        reply.recycle()
                    }
                }

                tx(1) {} // printerInit
                tx(12) { writeInt(1) } // setAlignment centre
                tx(11) { writeFloat(28f) } // setFontSize
                tx(13) { writeString("$header\n") } // printText
                tx(11) { writeFloat(18f) }
                tx(13) { writeString("================================\n") }
                tx(12) { writeInt(0) } // gauche
                tx(11) { writeFloat(20f) }

                if (lines != null) {
                    for (i in 0 until lines.length()) {
                        tx(13) { writeString("${lines.getString(i)}\n") }
                    }
                }

                if (footer.isNotEmpty()) {
                    tx(12) { writeInt(1) }
                    tx(13) { writeString("================================\n") }
                    tx(13) { writeString("$footer\n") }
                }

                tx(19) { writeInt(3) } // lineWrap
                tx(29) {} // cutPaper

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Erreur AIDL: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        @JavascriptInterface
        fun printText(text: String) {
            printTicket(org.json.JSONObject().apply {
                put("header", "LOTATO PRO")
                put("lines", org.json.JSONArray().apply { put(text) })
            }.toString())
        }
    }

    private fun offlinePage() = """
        <!DOCTYPE html><html><head><meta charset="UTF-8">
        <style>
          body{font-family:sans-serif;text-align:center;padding:40px;background:#0D1117;color:white}
          h1{color:#F0A500}
          button{padding:14px 28px;background:#F0A500;border:none;color:#000;font-size:16px;border-radius:8px;margin-top:20px}
        </style></head><body>
          <h1>LOTATO PRO</h1><p>Pas de connexion internet.</p>
          <button onclick="location.reload()">Réessayer</button>
        </body></html>
    """.trimIndent()

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { InnerPrinterManager.getInstance().unBindService(this, innerPrinterCallback) } catch (e: Exception) {}
        try { unbindService(aidlConnection) } catch (e: Exception) {}
    }
}

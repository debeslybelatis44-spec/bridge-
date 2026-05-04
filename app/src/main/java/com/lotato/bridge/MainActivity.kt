package com.lotato.finalapp

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var sunmiBinder: IBinder? = null

    // ── Connexion au service AIDL Sunmi ───────────────────────────────────────
    private val sunmiConn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            sunmiBinder = binder
            // Informer le PWA que l'imprimante est prête
            runOnUiThread {
                webView.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('sunmi-ready'));", null
                )
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            sunmiBinder = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Connexion Sunmi
        connectSunmi()

        // WebView setup
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

        // Pont JavaScript → Android
        webView.addJavascriptInterface(SunmiBridge(), "SunmiBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                view?.loadData(pageSansInternet(), "text/html; charset=UTF-8", null)
            }
        }

        webView.webChromeClient = WebChromeClient()

        webView.loadUrl("https://lotato1.onrender.com")
    }

    // ── Connexion AIDL Sunmi ──────────────────────────────────────────────────
    private fun connectSunmi() {
        try {
            val intent = Intent().apply {
                setPackage("woyou.aidlservice.jiuiv5")
                action = "woyou.aidlservice.jiuiv5.IWoyouService"
            }
            bindService(intent, sunmiConn, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Envoi commande AIDL ───────────────────────────────────────────────────
    private fun tx(code: Int, block: Parcel.() -> Unit) {
        val binder = sunmiBinder ?: return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("woyou.aidlservice.jiuiv5.IWoyouService")
            data.block()
            binder.transact(code, data, reply, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    // ── Pont JavaScript accessible depuis le PWA ──────────────────────────────
    inner class SunmiBridge {

        @JavascriptInterface
        fun isConnected(): Boolean = sunmiBinder != null

        /**
         * Impression ticket complet depuis le PWA
         * Appelé comme :
         * SunmiBridge.printTicket(JSON.stringify({
         *   header: "LOTATO PRO",
         *   lines: ["Ticket #123", "Mise: 500 HTG"],
         *   footer: "Bonne chance!"
         * }))
         */
        @JavascriptInterface
        fun printTicket(jsonStr: String) {
            if (sunmiBinder == null) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "Imprimante non connectée", Toast.LENGTH_SHORT).show()
                }
                return
            }
            try {
                val json = org.json.JSONObject(jsonStr)
                val header = json.optString("header", "LOTATO PRO")
                val footer = json.optString("footer", "")
                val lines = json.optJSONArray("lines")

                // Init
                tx(1) {}
                // Header - centré, grande police
                tx(12) { writeInt(1) }
                tx(11) { writeFloat(28f) }
                tx(13) { writeString("$header\n") }
                tx(11) { writeFloat(18f) }
                tx(13) { writeString("================================\n") }
                // Lignes - gauche, police normale
                tx(12) { writeInt(0) }
                tx(11) { writeFloat(20f) }
                if (lines != null) {
                    for (i in 0 until lines.length()) {
                        tx(13) { writeString("${lines.getString(i)}\n") }
                    }
                }
                // Footer
                if (footer.isNotEmpty()) {
                    tx(12) { writeInt(1) }
                    tx(13) { writeString("================================\n") }
                    tx(11) { writeFloat(18f) }
                    tx(13) { writeString("$footer\n") }
                }
                // Avancer et couper
                tx(28) { writeInt(80) }
                tx(29) {}

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        /**
         * Impression texte simple
         */
        @JavascriptInterface
        fun printText(text: String) {
            if (sunmiBinder == null) return
            try {
                tx(1) {}
                tx(12) { writeInt(0) }
                tx(11) { writeFloat(20f) }
                tx(13) { writeString("$text\n") }
                tx(28) { writeInt(60) }
                tx(29) {}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun pageSansInternet() = """
        <!DOCTYPE html><html><head><meta charset="UTF-8">
        <style>
          body{font-family:sans-serif;text-align:center;padding:40px;background:#0D1117;color:white}
          h1{color:#F0A500}
          button{padding:14px 28px;background:#F0A500;border:none;color:#000;
                 font-size:16px;border-radius:8px;cursor:pointer;margin-top:20px}
        </style></head><body>
          <h1>LOTATO PRO</h1>
          <p>Pas de connexion internet.<br>Vérifiez votre WiFi.</p>
          <button onclick="location.reload()">Réessayer</button>
        </body></html>
    """.trimIndent()

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unbindService(sunmiConn) } catch (e: Exception) {}
    }
}

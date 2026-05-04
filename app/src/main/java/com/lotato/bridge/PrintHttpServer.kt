package com.lotato.bridge

import android.os.IBinder
import android.os.Parcel
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class PrintHttpServer(private val service: PrintService) : NanoHTTPD(8787) {

    // Codes de transaction AIDL Sunmi (constants du SDK)
    companion object {
        private const val PRINTER_INIT       = 1
        private const val SET_ALIGNMENT      = 12
        private const val SET_FONT_SIZE      = 11
        private const val PRINT_TEXT         = 13
        private const val PRINT_AND_FEED     = 28
        private const val CUT_PAPER          = 29
    }

    override fun serve(session: IHTTPSession): Response {
        val cors = mapOf(
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
            "Access-Control-Allow-Headers" to "Content-Type"
        )

        if (session.method == Method.OPTIONS) {
            return cors(newFixedLengthResponse("OK"), cors)
        }

        // GET /status
        if (session.method == Method.GET && session.uri == "/status") {
            val json = JSONObject().apply {
                put("server", "LOTATO PrintBridge v1.0")
                put("printer", if (service.sunmiBinder != null) "connected" else "disconnected")
            }
            return cors(newFixedLengthResponse(Response.Status.OK, "application/json", json.toString()), cors)
        }

        // POST /print
        if (session.method == Method.POST && session.uri == "/print") {
            return try {
                val len = session.headers["content-length"]?.toInt() ?: 0
                val buf = ByteArray(len)
                session.inputStream.read(buf, 0, len)
                val json = JSONObject(String(buf, Charsets.UTF_8))

                val binder = service.sunmiBinder
                if (binder == null) {
                    return cors(errJson("Imprimante non connectée. Vérifiez que le Sunmi V2S a son service d'impression actif."), cors)
                }

                when (json.optString("type", "text")) {
                    "ticket" -> printTicket(binder, json)
                    "html"   -> printHtml(binder, json.optString("text", ""))
                    else     -> printText(binder, json.optString("text", ""))
                }

                cors(newFixedLengthResponse(Response.Status.OK, "application/json", """{"success":true}"""), cors)
            } catch (e: Exception) {
                cors(errJson(e.message ?: "Erreur inconnue"), cors)
            }
        }

        return cors(newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", """{"error":"not found"}"""), cors)
    }

    // ── Envoyer une commande AIDL au service Sunmi ────────────────────────────
    private fun transact(binder: IBinder, code: Int, block: Parcel.() -> Unit) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("woyou.aidlservice.jiuiv5.IWoyouService")
            data.block()
            binder.transact(code, data, reply, 0)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    // ── Impression texte simple ───────────────────────────────────────────────
    private fun printText(binder: IBinder, text: String) {
        transact(binder, PRINTER_INIT) {}
        transact(binder, SET_ALIGNMENT) { writeInt(0) } // gauche
        transact(binder, SET_FONT_SIZE) { writeFloat(20f) }
        transact(binder, PRINT_TEXT) { writeString("$text\n") }
        transact(binder, PRINT_AND_FEED) { writeInt(60) }
        transact(binder, CUT_PAPER) {}
    }

    // ── Impression HTML converti en texte ────────────────────────────────────
    private fun printHtml(binder: IBinder, html: String) {
        // Convertir HTML basique en texte pour imprimante thermique
        val text = html
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
        printText(binder, text)
    }

    // ── Impression ticket structuré ───────────────────────────────────────────
    private fun printTicket(binder: IBinder, json: JSONObject) {
        transact(binder, PRINTER_INIT) {}

        // Header
        transact(binder, SET_ALIGNMENT) { writeInt(1) } // centre
        transact(binder, SET_FONT_SIZE) { writeFloat(26f) }
        transact(binder, PRINT_TEXT) { writeString("${json.optString("header", "LOTATO PRO")}\n") }
        transact(binder, SET_FONT_SIZE) { writeFloat(18f) }
        transact(binder, PRINT_TEXT) { writeString("================================\n") }

        // Lignes
        transact(binder, SET_ALIGNMENT) { writeInt(0) }
        transact(binder, SET_FONT_SIZE) { writeFloat(20f) }
        val lines = json.optJSONArray("lines")
        if (lines != null) {
            for (i in 0 until lines.length()) {
                transact(binder, PRINT_TEXT) { writeString("${lines.getString(i)}\n") }
            }
        }

        // Footer
        val footer = json.optString("footer", "")
        if (footer.isNotEmpty()) {
            transact(binder, SET_ALIGNMENT) { writeInt(1) }
            transact(binder, PRINT_TEXT) { writeString("================================\n") }
            transact(binder, SET_FONT_SIZE) { writeFloat(18f) }
            transact(binder, PRINT_TEXT) { writeString("$footer\n") }
        }

        transact(binder, PRINT_AND_FEED) { writeInt(80) }
        transact(binder, CUT_PAPER) {}
    }

    private fun errJson(msg: String) = newFixedLengthResponse(
        Response.Status.INTERNAL_ERROR, "application/json",
        """{"success":false,"error":"$msg"}"""
    )

    private fun cors(r: Response, headers: Map<String, String>): Response {
        headers.forEach { (k, v) -> r.addHeader(k, v) }
        return r
    }
}


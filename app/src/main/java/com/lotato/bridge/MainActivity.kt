package com.lotato.bridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        tvStatus.text = "LOTATO PrintBridge\n\nServeur: http://localhost:8787\n\nAppuyez DÉMARRER puis allez sur\nhttps://lotato1.onrender.com"

        btnStart.setOnClickListener {
            startForegroundService(Intent(this, PrintService::class.java))
            tvStatus.text = "✅ Serveur actif!\n\nAllez sur Chrome:\nhttps://lotato1.onrender.com\n\nL'impression fonctionne maintenant."
        }

        btnStop.setOnClickListener {
            stopService(Intent(this, PrintService::class.java))
            tvStatus.text = "⏹ Serveur arrêté."
        }
    }
}


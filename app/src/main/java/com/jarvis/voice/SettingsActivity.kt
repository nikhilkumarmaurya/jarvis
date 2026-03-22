package com.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(s:Bundle?){
        super.onCreate(s); setContentView(R.layout.activity_settings)
        val et=findViewById<EditText>(R.id.etApiKey); val bs=findViewById<Button>(R.id.btnSave); val ba=findViewById<Button>(R.id.btnAccessibility); val tv=findViewById<TextView>(R.id.tvAccessibilityStatus)
        val p=getSharedPreferences("jarvis_prefs",Context.MODE_PRIVATE); val k=p.getString("gemini_api_key","")?:""; if(k.isNotBlank())et.setText(k)
        bs.setOnClickListener{ val k=et.text.toString().trim(); if(k.isBlank()){Toast.makeText(this,"Please enter a valid key",Toast.LENGTH_SHORT).show();return@setOnClickListener}; p.edit().putString("gemini_api_key",k).apply();Toast.makeText(this,"✅ Saved!",Toast.LENGTH_SHORT).show();finish() }
        ba.setOnClickListener{ startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        updateStatus(tv)
    }
    override fun onResume(){super.onResume();updateStatus(findViewById(R.id.tvAccessibilityStatus))}
    private fun updateStatus(tv:TextView){
        val ok=Settings.Secure.getString(contentResolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains("$packageName/${JarvisAccessibilityService::class.java.canonicalName}")==true
        tv.text=if(ok)"✅ Enabled" else "❌ Disabled"; tv.setTextColor(if(ok)getColor(android.R.color.holo_green_dark) else getColor(android.R.color.holo_red_dark))
    }
}

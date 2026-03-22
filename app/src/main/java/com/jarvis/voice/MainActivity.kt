package com.jarvis.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var rvChat: RecyclerView
    private lateinit var etCommand: TextInputEditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var permissionBanner: LinearLayout
    private lateinit var btnEnableAccessibility: android.widget.Button
    private lateinit var chatAdapter: ChatAdapter
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rvChat = findViewById(R.id.rvChat)
        etCommand = findViewById(R.id.etCommand)
        btnSend = findViewById(R.id.btnSend)
        btnMic = findViewById(R.id.btnMic)
        btnSettings = findViewById(R.id.btnSettings)
        tvStatus = findViewById(R.id.tvStatus)
        permissionBanner = findViewById(R.id.permissionBanner)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        chatAdapter = ChatAdapter()
        rvChat.layoutManager = LinearLayoutManager(this).apply{ stackFromEnd=true }
        rvChat.adapter = chatAdapter
        addJarvisMessage("Hi! I'm Jarvis 👇Iow can I help you today?\n\nYou can say:\n• Open WhatsApp\n• Send message to Mom\n• Set alarm for 7 AM\n• Set 5 minute timer")
        if (GeminiApi.getApiKey(this).isBlank()) addJarvisMessage("⚠️ Please set your Gemini API key in Settings first!")
        setupClickListeners(); requestMicPermission(); checkAccessibilityService()
    }
    private fun setupClickListeners() {
        btnSend.setOnClickListener{ sendCommand() }
        etCommand.setOnEditorActionListener{ _,actionId,event ->
            if(actionId==EditorInfo.IME_ACTION_SEND_||(event?.keyCode==KeyEvent.KEYCODE_ENTER&&event.action==KeyEvent.ACTION_DOWN)){sendCommand();true} else false }
        btnMic.setOnClickListener{ if(isListening) stopListening() else startListening() }
        btnSettings.setOnClickListener{ startActivity(Intent(this,SettingsActivity::class.java)) }
        btnEnableAccessibility.setOnClickListener{ startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }
    private fun sendCommand() { val t=etCommand.text?.toString()?.trim()?:return; if(t.isBlank())return; etCommand.setText(""); processCommand(t) }
    private fun processCommand(u: String) {
        chatAdapter.addMessage(ChatMessage("user",u)); scrollToBottom(); setStatus("Thinking…")
        lifecycleScope.launch{
            val r=GeminiApi.chat(this@MainActivity,shatAdapter.getHistory().dropLast(1),u,JarvisAccessibilityService.screenText.take(500))
            addJarvisMessage(ActionExecutor.execute(this@MainActivity,r)); setStatus(getString(R.string.ready))
        }
    }
    private fun addJarvisMessage(t: String) { chatAdapter.addMessage(ChatMessage("jarvis",t)); scrollToBottom() }
    private fun scrollToBottom() { rvChat.post{ rvChat.scrollToPosition(chatAdapter.itemCount-1) } }
    private fun setStatus(t: String) { runOnUiThread{ tvStatus.text=t } }
    private fun startListening() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestMicPermission();return}
        speechRecognizer=SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object:RecognitionListener{
            override fun onReadyForSpeech(p:Bundle?){isListening=true;setStatus(getString(R.string.listening));btnMic.setColorFilter(getColor(R.color.mic_active))}
            override fun onResults(r:Bundle?){val t=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?:return;isListening=false;btnMic.clearColorFilter();processCommand(t)}
            override fun onError(e:Int){isListening=false;btnMic.clearColorFilter();setStatus(getString(R.string.ready));Toast.makeText(this@MainActivity,when(e){SpeechRecognizer.ERROR_NO_MATCH->"Didn't catch that";SpeechRecognizer.ERROR_NETWORK->"Network error";else->"Speech error: $e"},Toast.LENGTH_SHORT).show()}
            override fun onBeginningOfSpeech(){}; override fun onRmsChanged(r:Float){}; override fun onBufferReceived(b:ByteArray?){}; override fun onEndOfSpeech(){}; override fun onPartialResults(p:Bundle?){}; override fun onEvent(e:Int,p:Bundle?){}
        })
        speechRecognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN");putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)})
    }
    private fun stopListening(){speechRecognizer?.stopListening();isListening=false;btnMic.clearColorFilter();setStatus(getString(R.string.ready))}
    private fun requestMicPermission(){if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),101)}
    private fun checkAccessibilityService(){ val e=isAccessibilityServiceEnabled(); permissionBanner.visibility=if(e)android.view.View.GONE else android.view.View.VISIBLE }
    private fun isAccessibilityServiceEnabled():Boolean{val s="$packageName/${JarvisAccessibilityService::class.java.canonicalName}";return Settings.Secure.getString(contentResolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(s)==true}
    override fun onResume(){super.onResume();checkAccessibilityService()}
    override fun onDestroy(){super.onDestroy();speechRecognizer?.destroy()}
}

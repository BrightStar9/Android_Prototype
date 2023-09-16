package app2.intellimuse

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.Context
import android.os.Bundle
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.WindowManager.LayoutParams
import android.util.Log // Import the Log class
import android.widget.Toast
import java.util.Locale


private const val TAG = "absSpeechRecognition" // Define a TAG for your log messages
private const val TAGJI = "absMyJavaScriptInterface" // Define a TAG for your log messages
private const val TAGP = "absPermissions" // Define a TAG for your log messages

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val SPEECH_RECOGNITION_REQUEST_CODE = 100
    private val RECORD_AUDIO_PERMISSION_REQUEST = 1
    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(this)
    }

    @SuppressLint("JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Add this line to keep the screen on
        window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Enable debugging for WebView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        webView = findViewById(R.id.webview)

        // Configure WebView settings
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true

        // Set WebViewClient to handle page navigation
        webView.webViewClient = WebViewClient()

        // Set WebChromeClient for handling JavaScript alerts, dialogs, etc.
        webView.webChromeClient = WebChromeClient()

        // Load a webpage
        webView.loadUrl("https://wizardstoolkit.com/AndroidApp2d.htm")
        // Add a JavaScript interface for handling messages from the PWA
        webView.addJavascriptInterface(MyJavaScriptInterface(this), "Android")
        val data = "Hello, website!" // The data to be sent
        webView.evaluateJavascript("javascript:receiveTextFromAndroid('$data')", null)
    }

    // Define a JavaScript interface class
    inner class MyJavaScriptInterface(private val mContext: Context) {
        // This method can be called from JavaScript
        @JavascriptInterface
        fun onMessage(message: String) {
            // Handle the incoming message from the PWA's JavaScript
            Log.i(TAGJI, message)
            if (message == "startSpeechRecognition") {
                // Call your startSpeechRecognition() function here
                runOnUiThread {
                    startSpeechRecognition()
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()

        // Check for microphone permission and request it if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_REQUEST)
        }
    }

    // Handle back button navigation
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // Handle permission request results
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Microphone permission granted, you can start speech recognition here if needed
                Log.i(TAGP, "Microphone permission granted")
            } else {
                // Microphone permission denied, handle accordingly
                Log.i(TAGP, "Microphone permission denied")
            }
        }
    }


    // Start speech recognition when a button or action triggers it
    private fun startSpeechRecognition() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Voice recognition available.", Toast.LENGTH_SHORT).show()
        }
        val recognitionListener = MyRecognitionListener()
        speechRecognizer.setRecognitionListener(recognitionListener);
        Log.i(TAG, "startSpeechRecognition() called") // Log a message when the function is called
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        try {
            startActivityForResult(intent, SPEECH_RECOGNITION_REQUEST_CODE)

        } catch (e: ActivityNotFoundException) {
            // Handle speech recognition not available on the device
            Log.i(TAG, "Speech recognition not available on this device: ${e.message}")
        }
    }


    // Implement the RecognitionListener to receive speech recognition events
    class MyRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {}
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.i(TAG, "recognitionListener")
            if (matches != null && matches.isNotEmpty()) {
                val recognizedText = matches[0]
                // Handle the recognized text
                Log.i(TAG, "recognitionListener not null")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
    // Handle the result of speech recognition
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        sendTextToWebsite("recognizedText")
        if (requestCode == SPEECH_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (matches != null && matches.isNotEmpty()) {
                val recognizedText = matches[0]
                // Handle the recognized text
                sendTextToWebsite("recognizedText")
            }
        }
    }

    fun sendTextToWebsite(fncText: String) {
//        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        Log.i(TAG, "sendTextToWebsite: $fncText")

        // Step 3: Execute JavaScript code with evaluateJavascript()
        webView.evaluateJavascript("javascript:receiveTextFromAndroid('$fncText');", null)
        Log.i(TAG, "sendTextToWebsite after evaluateJavascript")
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}

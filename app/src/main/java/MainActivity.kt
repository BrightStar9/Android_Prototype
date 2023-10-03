package app2.intellimuse


import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.util.Log
import android.view.WindowManager.LayoutParams
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


private const val TAG = "absSpeechRecognition" // Define a TAG for your log messages
private const val TAGJI = "absMyJavaScriptInterface" // Define a TAG for your log messages
private const val TAGP = "absPermissions" // Define a TAG for your log messages


class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val MICROPHONE_PERMISSION_REQUEST_CODE = 200
    private val RECORD_AUDIO_PERMISSION_REQUEST = 1
    private val PICK_IMAGE_REQUEST = 2
    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(this)
    }


    @SuppressLint("JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//         Add this line to keep the screen on
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
        webView.loadUrl("https://wizardstoolkit.com/dev/AndroidApp.htm")
        // Add a JavaScript interface for handling messages from the PWA
        webView.addJavascriptInterface(MyJavaScriptInterface(this), "Android")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            } else {
                // Permissions already granted
                // Proceed with camera and microphone operations
            }
        } else {
            // For devices running Android version lower than 13, permissions are granted by default
            // Proceed with camera and microphone operations directly
        }

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
            } else if (message.contains("dialPhone-")) {
                // Call your dialPhoneNumber() function here
                runOnUiThread {
                    var phoneNumber = message.substring(10)
                    dialPhoneNumber(phoneNumber)
                }
            } else if (message.contains("photo-")) {
                // Call your pickCamera() function here
                runOnUiThread {
                    var info = message.substring(6);
                    val (fncTable, fncId, fncMode) = info.split("-")
                    if(fncMode == "0")pickCamera()
                    else if (fncMode == "1")selectImageFromGallery()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Check for microphone permission and request it if not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST
            )
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
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted
                    // Proceed with camera-related operations
                    Log.i(TAGP, "Camera permission granted")
                } else {
                    // Camera permission denied
                    // Handle the denial, e.g., show an error message or disable camera functionality
                    Log.i(TAGP, "Camera permission is not granted")
                }
            }
            MICROPHONE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Microphone permission granted
                    // Proceed with microphone-related operations
                    Log.i(TAGP, "Microphone permission is not granted")
                } else {
                    // Microphone permission denied
                    // Handle the denial, e.g., show an error message or disable microphone functionality
                    Log.i(TAGP, "Microphone permission is not granted")
                }
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
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        try {
            startActivityForResult(intent, MICROPHONE_PERMISSION_REQUEST_CODE)

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

//        sendTextToWebsite("recognizedText")
        if (requestCode == MICROPHONE_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (matches != null && matches.isNotEmpty()) {
                val recognizedText = matches[0]
                // Handle the recognized text
                sendTextToWebsite(recognizedText)
            }
        } else  if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val imageUri = Uri.parse(data.data.toString())
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            Log.i("imageUri", bitmap.toString())
            uploadImageToServer("prototypeWTKApiKey", "wtkUsers", "1", "image.png", bitmap)
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun sendTextToWebsite(fncText: String) {
//        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        Log.i(TAG, "sendTextToWebsite: $fncText")

        // Step 3: Execute JavaScript code with evaluateJavascript()
        webView.evaluateJavascript("javascript:receiveTextFromAndroid('$fncText');", null)
        Log.i(TAG, "sendTextToWebsite after evaluateJavascript")
    }

    fun sendPathToWebsite(path:String, fileName: String) {
        val head = "photo"
        webView = findViewById(R.id.webview)
        val message = "$head-$path-$fileName"
        Log.i(TAG, "sendPathToWebsite:$message")
        // Step 3: Execute JavaScript code with evaluateJavascript()
        try {

            runOnUiThread {
                webView.evaluateJavascript("javascript:receiveTextFromAndroid('$message');", null)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                val javascriptCode =
                    "var event = new CustomEvent('message', { detail: '\$message' }); document.dispatchEvent(event);"
                webView.evaluateJavascript(javascriptCode, null)
                Log.i(TAG, "sendPathToWebsite after evaluateJavascript")
            }
        }catch(e: Exception){
            Log.i("upload error", e.message.toString())
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    // Start to dial phone number
    fun dialPhoneNumber(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        startActivity(intent)
    }

    private fun pickCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startCamera.launch(cameraIntent)
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private val startCamera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val uri = data?.data
                if (uri != null) {
                } else {
                    data?.extras?.let { b ->
                        val bmp = b.get("data") as Bitmap
                        uploadImageToServer("prototypeWTKApiKey", "wtkUsers", "1", "image.png", bmp)
                    }
                }
            }
        }
    fun uploadImageToServer(apiKey: String, table: String, id: String, fileName: String, imgFile: Bitmap) {
        // 1. Convert the image  to base64 string
        val bitmap: Bitmap = imgFile

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT).replace("\n", "")
        // 2. Create the JSON payload
        val jsonPayload = "{ " +
                "\"wtkApiKey\": \"$apiKey\"," +
                "\"table\": \"$table\"," +
                "\"id\": \"$id\"," +
                "\"name\": \"$fileName\"," +
                "\"file\": \"$base64String\"" +
                "}"
        sendDataToServer(jsonPayload)
    }
    fun sendDataToServer(jsonPayload: String) {
        GlobalScope.launch(Dispatchers.IO) {
            // 3. Open a connection to the server
            val url = URL("https://wizardstoolkit.com/dev/test/uploadTest.php")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.doInput = true
            connection.doOutput = true
            try {

                //4. Send the JSON payload as raw body data
                val outputStream = BufferedOutputStream(connection.outputStream)
                val writer = OutputStreamWriter(outputStream)
                writer.write(jsonPayload)
                writer.flush()
                //5. Get the server response
                connection.connect()
                val inputStream = BufferedInputStream(connection.inputStream)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                val responseData = response.toString()
                var jsonData = responseData.substring(response.indexOf("{"))
                val jsonObject = JSONObject(jsonData)

                val result = jsonObject.getString("result")
                val path = jsonObject.getString("path")
                val fileName = jsonObject.getString("fileName")
                if(result == "success"){
                    sendPathToWebsite(path, fileName)
                }
            } catch (e: Exception) {
                // Handle any exceptions that occur during the network operation
            } finally {
                // 6. Close the connection
                connection.disconnect()
            }
        }
    }
}


import android.widget.EditText
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var gyroscope: Sensor
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var isRecording = false
    private var samplingRate: Int = 50 // Default sampling rate of 50 Hz
    private val dateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
    private var csvWriter: FileWriter? = null
    private var csvFile: File? = null

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.start_button)
        stopButton = findViewById(R.id.stop_button)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // Request permission to write to external storage if not already granted
if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE)
}

startButton.setOnClickListener {
    if (!isRecording) {
        // Get the sampling rate from user input
        val samplingRateEditText = findViewById<EditText>(R.id.sampling_rate_edit_text)
        val samplingRateText = samplingRateEditText.text.toString()
        if (samplingRateText.isNotEmpty()) {
            samplingRate = samplingRateText.toInt()
        }

        // Open the CSV file for writing
        val timeStamp = dateFormat.format(Date())
        val fileName = "sensor_data_$timeStamp.csv"
        csvFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        try {
            csvWriter = FileWriter(csvFile)
            csvWriter?.append("time,ax,ay,az,gx,gy,gz\n")
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening CSV file", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        // Register listeners for accelerometer and gyroscope sensors
        sensorManager.registerListener(this, accelerometer, 1000000 / samplingRate)
        sensorManager.registerListener(this, gyroscope, 1000000 / samplingRate)

        // Update UI state
        isRecording = true
        startButton.isEnabled = false
        stopButton.isEnabled = true
    }
}

stopButton.setOnClickListener {
    if (isRecording) {
        // Unregister listeners for accelerometer and gyroscope sensors
        sensorManager.unregisterListener(this)

        // Close the CSV file
        try {
            csvWriter?.flush()
            csvWriter?.close()
            Toast.makeText(this, "Sensor data saved to ${csvFile?.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error closing CSV file", Toast.LENGTH_SHORT).show()
        }

        // Update UI state
        isRecording = false
        startButton.isEnabled = true
        stopButton.isEnabled = false
    }
}

companion object {
    private const val PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1
}
override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
        PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE -> {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "Permission denied to write to external storage", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
override fun onSensorChanged(event: SensorEvent) {
    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER || event.sensor.type == Sensor.TYPE_GYROSCOPE) {
        val currentTimeMillis = System.currentTimeMillis()
        val values = event.values
        csvWriter?.append("$currentTimeMillis,${values[0]},${values[1]},${values[2]},${values[3]},${values[4]},${values[5]}\n")
    }
}

override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    // Not used
}

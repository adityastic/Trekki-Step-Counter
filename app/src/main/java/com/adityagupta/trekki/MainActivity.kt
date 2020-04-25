package com.adityagupta.trekki

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class MainActivity : AppCompatActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var csvWriter: CSVWriter? = null
    private var path: File? = null
    private var prev: FloatArray? = floatArrayOf(0f, 0f, 0f)
    private var file: File? = null
    private var menu: Menu? = null
    private var stepView: TextView? = null
    private var stepCount = 0
    private var rawData: LineGraphSeries<DataPoint>? = null
    private var lpData: LineGraphSeries<DataPoint>? = null
    private var graphView: GraphView? = null
    private var graphView1: GraphView? = null
    private var combView: GraphView? = null
    private var rawPoints = 0
    private var sampleCount = 0
    private var startTime: Long = 0
    private var samplingActive = true
    private var streakStartTime: Long = 0
    private var streakPrevTime: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepView = findViewById<View>(R.id.count) as TextView
        path = getExternalFilesDir(null)
        file = File(path, "raghu1.csv")
        Log.e("PATH", file!!.absolutePath)
        try {
            csvWriter = CSVWriter(FileWriter(file))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        rawData = LineGraphSeries()
        rawData!!.title = "Raw Data"
        rawData!!.color = ContextCompat.getColor(this,R.color.colorAccent)
        lpData = LineGraphSeries()
        lpData!!.title = "Smooth Data"
        lpData!!.color = Color.BLUE
        graphView = findViewById<View>(R.id.rawGraph) as GraphView
        graphView1 = findViewById<View>(R.id.lpGraph) as GraphView
        graphView!!.viewport.isYAxisBoundsManual = true
        graphView!!.viewport.setMinY(-40.0)
        graphView!!.viewport.setMaxY(30.0)
        graphView!!.viewport.isXAxisBoundsManual = true
        graphView!!.viewport.setMinX(4.0)
        graphView!!.viewport.setMaxX(80.0)
        // enable scaling and scrolling
        graphView!!.viewport.isScalable = true
        graphView!!.viewport.setScalableY(true)
        graphView!!.viewport.isScrollable = true // enables horizontal scrolling
        graphView!!.viewport.setScrollableY(true) // enables vertical scrolling
        graphView!!.viewport.isScalable = true // enables horizontal zooming and scrolling
        graphView!!.viewport.setScalableY(true) // enables vertical zooming and scrolling
        graphView!!.addSeries(rawData)


        // set manual X bounds
        graphView1!!.viewport.isYAxisBoundsManual = true
        graphView1!!.viewport.setMinY(-30.0)
        graphView1!!.viewport.setMaxY(30.0)
        graphView1!!.viewport.isXAxisBoundsManual = true
        graphView1!!.viewport.setMinX(4.0)
        graphView1!!.viewport.setMaxX(80.0)
        // enable scaling and scrolling
        graphView1!!.viewport.isScalable = true
        graphView1!!.viewport.setScalableY(true)
        graphView1!!.viewport.isScrollable = true // enables horizontal scrolling
        graphView1!!.viewport.setScrollableY(true) // enables vertical scrolling
        graphView1!!.viewport.isScalable = true // enables horizontal zooming and scrolling
        graphView1!!.viewport.setScalableY(true) // enables vertical zooming and scrolling
        graphView1!!.addSeries(lpData)
        combView = findViewById<View>(R.id.combGraph) as GraphView
        combView!!.viewport.isYAxisBoundsManual = true
        combView!!.viewport.setMinY(-70.0)
        combView!!.viewport.setMaxY(70.0)
        combView!!.viewport.isXAxisBoundsManual = true
        combView!!.viewport.setMinX(4.0)
        combView!!.viewport.setMaxX(80.0)
        combView!!.addSeries(rawData)
        combView!!.addSeries(lpData)
        streakPrevTime = System.currentTimeMillis() - 500
    }

    override fun onResume() {
        sensorManager!!.registerListener(this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)
        super.onResume()
        startTime = System.currentTimeMillis()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            csvWriter!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.instrumentation) {
            samplingActive = true
            sampleCount = 0
            startTime = System.currentTimeMillis()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleEvent(event: SensorEvent) {
        prev = lowPassFilter(event.values, prev)
        val raw = Accelerometer(event.values)
        val data = Accelerometer(prev)
        val text = StringBuilder()
        text.append("X: " + data.X)
        text.append("Y: " + data.Y)
        text.append("Z: " + data.Z)
        text.append("R: " + data.R)
        rawData!!.appendData(DataPoint((rawPoints++).toDouble(), raw.R), true, 1000)
        lpData!!.appendData(DataPoint(rawPoints.toDouble(), data.R), true, 1000)
        if (data.R > 10.5f) {
            CURRENT_STATE = ABOVE
            if (PREVIOUS_STATE != CURRENT_STATE) {
                streakStartTime = System.currentTimeMillis()
                if (streakStartTime - streakPrevTime <= 250f) {
                    streakPrevTime = System.currentTimeMillis()
                    return
                }
                streakPrevTime = streakStartTime
                Log.d("STATES:", "$streakPrevTime $streakStartTime")
                stepCount++
            }
            PREVIOUS_STATE = CURRENT_STATE
        } else if (data.R < 10.5f) {
            CURRENT_STATE = BELOW
            PREVIOUS_STATE = CURRENT_STATE
        }
        stepView?.text = "$stepCount"
        val text1 = arrayOfNulls<String>(4)
        text1[0] = System.currentTimeMillis().toString()
        text1[1] = data.X.toString()
        text1[2] = data.Y.toString()
        text1[3] = data.Z.toString()
        csvWriter!!.writeNext(text1)
    }

    private fun lowPassFilter(input: FloatArray?, prev: FloatArray?): FloatArray? {
        val alpha = 0.1f
        if (input == null || prev == null) {
            return null
        }
        for (i in input.indices) {
            prev[i] = prev[i] + alpha * (input[i] - prev[i])
        }
        return prev
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            handleEvent(event)
            if (samplingActive) {
                sampleCount++
                val now = System.currentTimeMillis()
                if (now >= startTime + 5000) {
                    val samplingRate = sampleCount / ((now - startTime) / 1000.0)
                    samplingActive = false
                    Toast.makeText(applicationContext, "Sampling rate of your device is " + samplingRate + "Hz", Toast.LENGTH_LONG).show()
                    val rate = menu!!.findItem(R.id.instrumentation)
                    rate.title = "Sampling Rate : " + samplingRate + "hz"
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    companion object {
        private const val ABOVE = 1
        private const val BELOW = 0
        private var CURRENT_STATE = 0
        private var PREVIOUS_STATE = BELOW
    }
}
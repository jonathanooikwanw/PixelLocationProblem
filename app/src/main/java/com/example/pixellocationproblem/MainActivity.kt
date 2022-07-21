package com.example.pixellocationproblem

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val PERMISSION_REQUEST_CODE = 100
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val cancellationTokenSource = CancellationTokenSource()
    private var currentLocation : String? = null
    private lateinit var textView : TextView
    private lateinit var button : Button
    private lateinit var progressBar : ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        textView = findViewById(R.id.textView)
        button = findViewById(R.id.button)
        progressBar = findViewById(R.id.progress_spinner)
        progressBar.visibility = View.GONE

        requestPermission()
        checkPermissions()

        button.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                textView.text = "Location"
                currentLocation = null
                progressBar.visibility = View.VISIBLE
                if(getCurrentLocation()) {
                    Toast.makeText(this@MainActivity, "Location success", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Location failed", Toast.LENGTH_LONG).show()
                }
                progressBar.visibility = View.GONE
                textView.text = "Location is $currentLocation"

            }
        }
    }

    //requests for permission for camera and location
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
    }


    //checks if permission is allowed for location
    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }



    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Boolean =
        suspendCoroutine { cont ->
            if ((ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED)
            ) {
                var success = false

                //for 'com.google.android.gms:play-services-location:18.0.0'
                //val priority = LocationRequest.PRIORITY_HIGH_ACCURACY

                // for 'com.google.android.gms:play-services-location:20.0.0'
                val priority = Priority.PRIORITY_HIGH_ACCURACY

                Log.d(TAG, "Get current location")
                fusedLocationProviderClient.getCurrentLocation(priority, cancellationTokenSource.token).addOnSuccessListener { loc: Location? ->
                    if (loc == null) {
                        Log.d(TAG, "Failed to get current location")
                        cont.resume(success)
                    } else {
                        val lat = loc.latitude
                        val lon = loc.longitude
                        val currentCountryCode =
                            getCountryCode(lat, lon)
                        Log.d(TAG,"Country label for current is $currentCountryCode")
                        if(currentCountryCode!=null) {
                            success = true
                        }
                        currentLocation = currentCountryCode
                        cont.resume(success)
                    }
                }
            }
        }

    private fun getCountryCode(lat : Double, lon : Double) : String? {
        return try {
            val geocoder = Geocoder(this, Locale.ENGLISH)
            val addresses : List<Address> = geocoder.getFromLocation(lat, lon, 1)

            if (addresses.isNotEmpty()) {
                addresses[0].countryCode
            } else {
                null
            }
        } catch (exception : Exception) {
            null
        }
    }
}
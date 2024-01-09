package com.mack.rccar

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mack.rccar.databinding.ActivityMapsBinding
// Add these imports
import com.google.firebase.database.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var locationRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        locationRef = database.getReference("location")

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mapFragment.getMapAsync { googleMap ->
            binding.mapTypeButton.setOnClickListener {
                showMapTypeSelectionDialog(googleMap)
            }
        }

    }
    private fun showMapTypeSelectionDialog(googleMap: GoogleMap) {
        val mapTypes = arrayOf("Normal", "Satellite", "Terrain", "Hybrid")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Map Type")
            .setItems(mapTypes) { _, which ->
                // Set the selected map type based on the user's choice
                when (which) {
                    0 -> googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                    1 -> googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    2 -> googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                    3 -> googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                }
            }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker for a specific location with fixed values
//        val latitude = 31.480454
//        val longitude = 76.190216
//
//        val location = LatLng(latitude, longitude)
//        mMap.addMarker(MarkerOptions().position(location).title("Marker"))


        // Listen for changes in the location data
        locationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Clear existing markers on the map

                mMap.clear()
                Log.d("location",dataSnapshot.toString())
                Log.d("location",dataSnapshot.toString())
                val latitude = dataSnapshot.child("latitude").getValue(Double::class.java)
                val longitude = dataSnapshot.child("longitude").getValue(Double::class.java)

                if (latitude != null && longitude != null) {
                    val location = LatLng(latitude.toDouble(), longitude.toDouble())
                    mMap.addMarker(MarkerOptions().position(location).title("Marker"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location,30F))
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(14F))
                    mMap.mapType= GoogleMap.MAP_TYPE_NORMAL
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
                Log.e("Firebase", "Error reading data: ${databaseError.message}")
            }
        })
    }
}

package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.IOException
import java.util.*
import kotlin.properties.Delegates


class SelectLocationFragment : BaseFragment() , OnMapReadyCallback{

    private lateinit var latAndLng:LatLng

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap

    //private lateinit var lastLocation: Location
    //private lateinit var fusedLocationClient: FusedLocationProviderClient

private lateinit var address: String
    private val FINE_LOCATION_ACCESS_REQUEST_CODE = 1

    private lateinit var pointOfInterest: PointOfInterest

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        //fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        //TODO: add the map setup implementation
        // Obtain the childFragmentManager and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        TODO: zoom to the user location after taking his permission
      //  onLocationSelected()
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location

        return binding.root
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireContext(),
                R.raw.map_style
            )
        )
        //Default location
        val sydney = LatLng(-34.0, 151.0)
        val zoomLevel = 15f
        map.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, zoomLevel))
        map.uiSettings.isZoomControlsEnabled = true

        enableUserLocation()
      setLocationClick(map)
        setPoiClickListener(map)

       // onLocationSelected()

    }

    private fun setPoiClickListener(map: GoogleMap) {

        map.setOnPoiClickListener { poi ->

            map.clear()
            pointOfInterest = poi

            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )

            map.addCircle(
                CircleOptions()
                    .center(poi.latLng)
                    .radius(200.0)
                    .strokeColor(Color.argb(255, 255, 0, 0))
                    .fillColor(Color.argb(64, 255, 0, 0)).strokeWidth(4F)

            )

            poiMarker.showInfoWindow()


        }
    }
    fun getAddress(lat: Double, lng: Double):String {
        var address = ""
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses: List<Address> = geocoder.getFromLocation(lat, lng, 1)
            val obj: Address = addresses[0]
            var add: String = obj.getAddressLine(0)
            add = """
            $add
            ${obj.getCountryName()}
            """.trimIndent()
            add = """
            $add
            ${obj.getCountryCode()}
            """.trimIndent()
            add = """
            $add
            ${obj.getAdminArea()}
            """.trimIndent()
            add = """
            $add
            ${obj.getPostalCode()}
            """.trimIndent()
            add = """
            $add
            ${obj.getSubAdminArea()}
            """.trimIndent()
            add = """
            $add
            ${obj.getLocality()}
            """.trimIndent()
            add = """
            $add
            ${obj.getSubThoroughfare()}
            """.trimIndent()
            Log.v("IGA", "Address$add")
            // Toast.makeText(this, "Address=>" + add,
            // Toast.LENGTH_SHORT).show();
          address= add
            // TennisAppActivity.showDialog(add);
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            Toast.makeText(activity, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    return address
    }
    private fun setLocationClick(map: GoogleMap){

        map.setOnMapClickListener { latLng ->
            // A Snippet is Additional text that's displayed below the title.
            map.clear()
            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                latLng.latitude,
                latLng.longitude
            )
  latAndLng= latLng
 address= getAddress(latLng.latitude,latLng.longitude)
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

            )
            _viewModel.latitude.value = latLng.latitude
            _viewModel.longitude.value = latLng.longitude
        }

    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence

        binding.saveLocation.setOnClickListener{

            if(this::address.isInitialized){

   _viewModel.reminderSelectedLocationStr.value = address
                _viewModel.latitude.value = latAndLng.latitude
                _viewModel.longitude.value = latAndLng.longitude
                _viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }

          else if (this::pointOfInterest.isInitialized){
                _viewModel.latitude.value = pointOfInterest.latLng.latitude
                _viewModel.longitude.value = pointOfInterest.latLng.longitude
                _viewModel.reminderSelectedLocationStr.value = pointOfInterest.name
                _viewModel.selectedPOI.value = pointOfInterest
                _viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }else{
                Toast.makeText(context, "Please select a location", Toast.LENGTH_LONG).show()
               // _viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }

        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }




    // Your app needs the ACCESS_FINE_LOCATION permission for getting the user’s location details

    private fun enableUserLocation() {

        when {
            (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) -> {

                // You can use the API that requires the permission.
                map.isMyLocationEnabled = true

                /*fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        lastLocation = location
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        val markerOptions = MarkerOptions().position(currentLatLng)
                        map.addMarker(markerOptions)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                    }
                }*/
                Toast.makeText(context, "Location permission is granted.", Toast.LENGTH_LONG).show()
            }
            (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )) ->{
                // Explain why you need the permission
                // Add dialog
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    FINE_LOCATION_ACCESS_REQUEST_CODE
                )
            }

            else ->
                //Request permission
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    FINE_LOCATION_ACCESS_REQUEST_CODE
                )
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the location

        when (requestCode) {
            FINE_LOCATION_ACCESS_REQUEST_CODE -> {

                if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue...
                    enableUserLocation()

                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    Toast.makeText(
                        context,
                        "Location permission was not granted.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }

        }

    }
}

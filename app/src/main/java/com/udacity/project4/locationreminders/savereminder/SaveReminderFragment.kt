package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity.Companion.TAG
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceHelper
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofenceHelper: GeofenceHelper
    private lateinit var geofencingClient: GeofencingClient
    private val GEOFENCE_RADIUS = 500f
    private val randomGeofenceId = UUID.randomUUID().toString()
    private val LOCATION_BACKGROUND_REQUEST_ACCESS_REQUEST_CODE = 2
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        geofenceHelper = GeofenceHelper(context)
        geofencingClient = LocationServices.getGeofencingClient(context!!)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {

            checkDeviceLocationSettingsAndStartGeofence()
//             2) save the reminder to the local db


            _viewModel.navigateToReminderList.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer {
                    if (it) {
                        view.findNavController()
                            .navigate(R.id.action_saveReminderFragment_to_reminderListFragment)
                        _viewModel.navigateToReminderList()
                    }
                })
        }
    }

    private fun createAndAddGeofence() {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value
        val geofenceId = UUID.randomUUID().toString()

        //            TODO: use the user entered reminder details to:
        //             1) add a geofencing request
        if (latitude != null && longitude != null && !TextUtils.isEmpty(title)) {
            if (foregroundAndBackgroundLocationPermissionApproved()) {
                addGeofence(LatLng(latitude, longitude), GEOFENCE_RADIUS, geofenceId)
            } else {
                requestForegroundAndBackgroundLocationPermissions()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(latLng: LatLng, radius: Float, geofenceId: String) {
        val geofence: Geofence = geofenceHelper.getGeofence(
            geofenceId,
            latLng,
            radius,
            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
        )
        val geofencingRequest: GeofencingRequest = geofenceHelper.getGeofencingRequest(geofence)
        val pendingIntent: PendingIntent? = geofenceHelper.getGeofencePendingIntent()
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener(OnSuccessListener<Void?> {
                // Toast.makeText(context,"geofence added",Toast.LENGTH_LONG).show()
                Log.d(TAG, "Geofence Added")
                val title = _viewModel.reminderTitle.value
                val description = _viewModel.reminderDescription.value
                val location = _viewModel.reminderSelectedLocationStr.value
                val latitude = _viewModel.latitude.value
                val longitude = _viewModel.longitude.value
                _viewModel.validateAndSaveReminder( ReminderDataItem(
                    title,
                    description,
                    location,
                    latitude,
                    longitude,
                    geofenceId
                ))

            })
            .addOnFailureListener(OnFailureListener { e ->
                val errorMessage: String = geofenceHelper.getErrorString(e)
                Toast.makeText(
                    context,
                    "Please give background location permission",
                    Toast.LENGTH_LONG
                ).show()
                Log.d(TAG, "fail in creating geofence: $errorMessage")
            })
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this.requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this.requireContext(),
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }


    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return

        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            runningQOrLater -> {
                // this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                Companion.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Request foreground only location permission")
        requestPermissions( permissionsArray,  resultCode)

    //    ActivityCompat.requestPermissions(
      //      this.requireActivity(),
      //      permissionsArray,
      //      resultCode
      //  )
    }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ) {
        //    Log.d(TAG, "onRequestPermissionResult")
       if (
           grantResults.isEmpty() ||
           grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
              (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                     PackageManager.PERMISSION_DENIED)
         ) {
           Toast.makeText(
             this.requireContext(),
             R.string.permission_denied_explanation, Toast.LENGTH_SHORT
         ).show()
    // Permission denied.

       } else {
         checkDeviceLocationSettingsAndStartGeofence()
         }
        }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

        override fun onDestroy() {
            super.onDestroy()
            //make sure to clear the view model after destroy, as it's a single view model.
            _viewModel.onClear()
        }

        private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_LOW_POWER
            }
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val settingsClient = LocationServices.getSettingsClient(requireContext())
            val locationSettingsResponseTask =
                settingsClient.checkLocationSettings(builder.build())
            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException && resolve) {
                    try {
                        startIntentSenderForResult(exception.getResolution().getIntentSender(),   REQUEST_TURN_DEVICE_LOCATION_ON,
                            null, 0, 0, 0, null)

                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                    }
                } else {
                    // Explain user why app needs this permission
                    Snackbar.make(
                        binding.root,
                        R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                    ).setAction(android.R.string.ok) {
                        checkDeviceLocationSettingsAndStartGeofence()
                    }.show()
                }
            }

            locationSettingsResponseTask.addOnCompleteListener {
                if (it.isSuccessful) {
                    createAndAddGeofence()
            }
        }
    }

    companion object {
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private const val TAG = "RemindersActivity"
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 2
    }

}

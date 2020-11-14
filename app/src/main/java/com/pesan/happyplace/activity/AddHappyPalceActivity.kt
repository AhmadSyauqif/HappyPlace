package com.pesan.happypalace.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.PersistableBundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Gallery
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pesan.happyplace.R
import com.pesan.happyplace.activity.MainActivity
import com.pesan.happyplace.model.HappyPlaceModel
import kotlinx.android.synthetic.main.activity_add_happy_palce.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class AddHappyPalceActivity : AppCompatActivity(), View.OnClickListener {

    private var cal = Calendar.getInstance()
    private lateinit var dateLister:
            DatePickerDialog.OnDateSetListener
    private var mLatidude: Double = 0.0
    private var mLogitude: Double = 0.0

    private var mHappyPlaceDetails: HappyPlaceModel? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val GALLERRY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
        private const val PLACE_AUTOCOMPLATE_REQUEST_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_palce)

        mHappyPlaceDetails = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAIL)
        mHappyPlaceDetails.let {
            //menset toolbar
            setSupportActionBar(toolbar_add_place)
            //agar dapat kembali ke activity sebelumnya
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setTitle("Edit Happy Place")
            //membuat aksi balik
            toolbar_add_place.setNavigationOnClickListener {
                onBackPressed()
            }
            if (!Places.isInitialized()) {
                Places.initialize(
                        this@AddHappyPalceActivity,
                        resources.getString(R.string.google_maps_key)
                )
            }

            dateLister = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }
            if (it != null) {
                saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)
                iv_place_image.setImageURI(saveImageToInternalStorage)
                et_title.setText(it!!.title)
                et_description.setText(it!!.description)
                et_date.setText(it!!.date)
                et_location.setText(it!!.location)
                mLatitude = it.latitude
                mLongitude = it.longitude

                btn_save.text = "UPDATE"
            }

            updateDateInView()
            et_date.setOnClickListener(this)
            tv_add_image.setOnClickListener(this)
            btn_save.setOnClickListener(this)
            et_location.setOnClickListener(this)
            tv_select_current_location.setOnClickListener(this)


        }

        //location for client
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 1000
        mLocationRequest.numUpdates = 1

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    private val mLocationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult?) {
            val mLastLocation: Location = locationResult!!.lastLocation
            mLatitude = mLastLocation.latitude
            Log.i("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.i("Current Longitude", "$mLongitude")

            val addressTask = GetAddressFromLatLng(this@AddHappyPlaceActivity, mLatitude, mLongitude)
            addressTask.setAddressListener(object : GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address: String?) {
                    et_location.setText(address)
                }

                override fun onError() {
                    Log.e("get Address::", "Something went wrong")
                }
            })
            addressTask.getAddress()
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                        this@AddHappyPlaceActivity,
                        dateListener,
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf(
                        "Select Photo From Galery",
                        "Capture photo from camera"
                )
                pictureDialog.setItems(pictureDialogItems) { _, which ->
                    when (which) {
                        0 -> choosePhotoFromGalery()
                        1 -> takePhotoFromCamera()
                    }

                }

                pictureDialog.show()
            }

            R.id.btn_save -> {
                when {
                    et_title.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }
                    et_description.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
                    }
                    et_location.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage == null -> {
                        Toast.makeText(this, "please select an image", Toast.LENGTH_SHORT).show()
                    }
                    else -> {

                        val happyPlaceModel = HappyPlaceModel(
                                if (mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
                                et_title.text.toString(),
                                saveImageToInternalStorage.toString(),
                                et_description.text.toString(),
                                et_date.text.toString(),
                                et_location.text.toString(),
                                mLatitude,
                                mLongitude
                        )

                        val dbHandler = DatabaseHandler(this)
                        if (mHappyPlaceDetails == null) {
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)
                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        } else {
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)
                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }

                    }
                }
            }

            R.id.et_location -> {
                try {
                    // These are the list of fields which we required is passed
                    val fields = listOf(
                            Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                            Place.Field.ADDRESS
                    )
                    // Start the autocomplete intent with a unique request code.
                    val intent =
                            Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                                    .build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            R.id.tv_select_current_location -> {
                if (!isLocationEnabled()) {
                    Toast.makeText(this, "Gps Anda Mati, Silahkan Aktifkan GPS", Toast.LENGTH_SHORT)
                            .show()

                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else {
                    Dexter.withActivity(this).withPermissions(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ).withListener(object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            if (report!!.areAllPermissionsGranted()) {
                                requestNewLocationData()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                                permissions: MutableList<PermissionRequest>?,
                                token: PermissionToken?
                        ) {
                            showRationalDialogForPermssions()
                        }
                    }).onSameThread().check()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                if (data != null) {
                    val contentUri = data.data
                    try {
                        val selectedImageBitmap =
                                MediaStore.Images.Media.getBitmap(this.contentResolver, contentUri)

                        saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)

                        Log.e("Save Image: ", "path :: $saveImageToInternalStorage")
                        iv_place_image.setImageBitmap(selectedImageBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(
                                this@AddHappyPlaceActivity,
                                "Failed to Load the image from",
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else if (requestCode == CAMERA) {
                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap
                saveImageToInternalStorage = saveImageToInternalStorage(thumbnail)

                Log.e("Save Image: ", "path :: $saveImageToInternalStorage")
                iv_place_image.setImageBitmap(thumbnail)

            } else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                et_location.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }
        }
    }

    private fun takePhotoFromCamera() {
        Dexter.withActivity(this).withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                if (report.areAllPermissionsGranted()) {
                    val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(galleryIntent, CAMERA)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
            ) {
                showRationalDialogForPermssions()
            }
        }).onSameThread().check()
    }


    private fun choosePhotoFromGalery() {
        Dexter.withActivity(this).withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                if (report.areAllPermissionsGranted()) {
                    val galleryIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.INTERNAL_CONTENT_URI
                    )
                    startActivityForResult(galleryIntent, GALLERRY)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
            ) {
                showRationalDialogForPermssions()
            }
        }).onSameThread().check()
    }

    private fun showRationalDialogForPermssions() {
        AlertDialog.Builder(this).setMessage(
                "" +
                        "It Looks like you have turned off permission required" +
                        "for this feature. It can be enabled under the" +
                        "Applications Settings"
        )
                .setPositiveButton("GO TO SETTINGS") { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }.setNegativeButton("CANCEL") { dialog, which ->
                    dialog.dismiss()
                }.show()
    }

    private fun updateDateInView() {
        val myFormat = "dd-MM-yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        et_date.setText(sdf.format(cal.time).toString())
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return Uri.parse(file.absolutePath)
    }
}
}
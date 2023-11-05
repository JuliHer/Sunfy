package com.artuok.appwork

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.fragmets.SettingsFragment
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.File
import java.io.IOException
import java.text.Normalizer
import java.util.Locale

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var uploadBtn: TextView
    private lateinit var deleteBtn: TextView
    private lateinit var changePassBtn : TextView
    private lateinit var usernameInput: EditText
    private lateinit var descInput: EditText
    private lateinit var saveBtn: TextView
    private lateinit var backBtn: LinearLayout
    private lateinit var profilePicture: ImageView
    private lateinit var progressBar: ProgressBar
    private var uid: String = ""

    private val images: List<String> = ArrayList()
    private val hashimages: List<String> = ArrayList()
    private var tempImage = ""
    private lateinit var map: Bitmap

    private val usersReference = FirebaseDatabase.getInstance().reference.child("user")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        initializeViews()

        backBtn.setOnClickListener {
            finish()
        }

        PushDownAnim.setPushDownAnimTo(uploadBtn)
            .setScale(0.95f)
            .setOnClickListener {
                selectPhoto()
            }

        PushDownAnim.setPushDownAnimTo(saveBtn)
            .setScale(0.95f)
            .setOnClickListener {
                saveData()
                finish()
            }

        PushDownAnim.setPushDownAnimTo(deleteBtn)
            .setScale(0.95f)
            .setOnClickListener {
                removePicture()
            }
        PushDownAnim.setPushDownAnimTo(changePassBtn)
            .setScale(0.95f)
            .setOnClickListener {

            }

        setData()
        setPhoto()
    }

    private fun initializeViews() {
        uploadBtn = findViewById(R.id.upload)
        deleteBtn = findViewById(R.id.remove_image)
        usernameInput = findViewById(R.id.username_input)
        descInput = findViewById(R.id.description_input)
        saveBtn = findViewById(R.id.save)
        backBtn = findViewById(R.id.back_button)
        profilePicture = findViewById(R.id.profile_picture)
        progressBar = findViewById(R.id.progress_bar)
        changePassBtn = findViewById(R.id.change_password)
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    private fun removePicture() {
        if (uid.isEmpty()) return

        val root: String = getExternalFilesDir("Media").toString()
        val appName = getString(R.string.app_name)
        val myDir = File(root, "$appName Profile")
        if (myDir.exists()) {
            val profilePictureRef = FirebaseStorage.getInstance().reference
                .child("chats")
                .child(uid)
                .child("profile-photo.jpg")
            val fName = appName.uppercase(Locale.getDefault()) + "-USER-IMG.jpg"
            val file = File(myDir, fName)
            if (file.exists()) {
                usersReference.child(uid).child("photo").removeValue()
                profilePictureRef.delete()
                    .addOnCompleteListener {
                        progressBar.visibility = View.GONE
                        profilePicture.alpha = 1f
                    }
                file.delete()
                progressBar.visibility = View.VISIBLE
                profilePicture.alpha = 0.6f
                setPhoto()
            }
        }
    }

    private fun setData() {
        if (uid.isEmpty()) return

        usersReference.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    usernameInput.setText(snapshot.child("name").value?.toString() ?: "")
                    descInput.setText(snapshot.child("description").value?.toString() ?: "")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
            }
        })
    }

    private fun saveData() {
        if (uid.isEmpty()) return

        val newName = usernameInput.text.toString()
        val newDesc = descInput.text.toString()
        val normalized = Normalizer.normalize(newName.lowercase(), Normalizer.Form.NFD)
        val searchTerm = normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

        if(searchTerm.isNotEmpty())
            usersReference.orderByChild("lowername").equalTo(searchTerm)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val code = snapshot.childrenCount + 1
                        usersReference.child(uid).child("name").setValue(newName)
                        usersReference.child(uid).child("lowername").setValue(searchTerm)
                        usersReference.child(uid).child("search").setValue("$searchTerm-$code")
                        usersReference.child(uid).child("code").setValue(code)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error if needed
                    }
                })

        usersReference.child(uid).child("description").setValue(newDesc)
    }

    private fun savePicture(){
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            val root: String = getExternalFilesDir("Media").toString()
            val appName = getString(R.string.app_name)
            val myDir = File(root, "$appName Profile")
            if (myDir.exists()) {

                val photo = FirebaseDatabase.getInstance().reference
                    .child("user")
                    .child(uid)
                    .child("photo")
                val key = photo.push().key.toString()

                photo.setValue(key)
                val profilePictureRef = FirebaseStorage.getInstance().reference
                    .child("chats")
                    .child(uid)
                    .child("$key.jpg")
                val fName = appName.uppercase(Locale.getDefault()) + "-USER-IMG.jpg"
                val file = File(myDir, fName)
                if (file.exists()) {
                    profilePictureRef.putFile(Uri.fromFile(file))
                        .addOnCompleteListener{
                            profilePicture.alpha = 1f
                            progressBar.visibility = View.GONE
                        }
                    progressBar.visibility = View.VISIBLE
                    profilePicture.alpha = 0.6f
                }
            }

        }
    }

    private fun setPhoto() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            val root: String = getExternalFilesDir("Media").toString()
            val appname = getString(R.string.app_name)
            val myDir = File(root, "$appname Profile")
            if (myDir.exists()) {
                val fname = appname.uppercase(Locale.getDefault()) + "-USER-IMG.jpg"
                val file = File(myDir, fname)
                val map =
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.path)
                }else{
                    BitmapFactory.decodeResource(resources, R.mipmap.usericon)
                }
                profilePicture.setImageBitmap(map)
            }
            if (!SettingsFragment.isPhotoProfile(this))
                    SettingsFragment.updatedPhotoProfile(this)
        }
    }


    private fun selectPhoto() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_selectimage_layout)
        val openCamera = dialog.findViewById<LinearLayout>(R.id.captureImage)
        val openGallery = dialog.findViewById<LinearLayout>(R.id.obtainImage)
        openCamera.setOnClickListener {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU){
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    openCamera()
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showInContextUI(0)
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }else{
                openCamera()
            }

            dialog.dismiss()
        }
        openGallery.setOnClickListener {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU){
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    getPictureGallery()
                } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showInContextUI(1)
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }else{
                getPictureGallery()
            }
            dialog.dismiss()
        }
        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        dialog.window!!.setGravity(Gravity.BOTTOM)
    }

    private fun getPictureGallery() {
        saveImages()
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        resultForGalleryPicture.launch(i)
    }

    private fun openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            saveImages()
            val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            i.putExtra(MediaStore.EXTRA_OUTPUT, getOutputFile())
            i.putExtra("PathOf", tempImage)
            resultLaunchers.launch(i)
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showInContextUI(2)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun saveImages() {
        val sr: SharedPreferences = getSharedPreferences("images", MODE_PRIVATE)
        val se = sr.edit()
        var i = 1
        for (x in images) {
            val name = "Images\$i"
            se.putString(name, x)
            val tmpname = "ImagesTemp\$i"
            se.putString(tmpname, hashimages[i - 1])
            i++
        }
        for (x in i..3) {
            val name = "Images\$x"
            se.putString(name, "")
            val tmpname = "ImagesTemp\$i"
            se.putString(tmpname, "")
        }
        se.apply()
    }

    private fun getOutputFile(): Uri? {
        val root: String = getExternalFilesDir("Media").toString()
        val appname = getString(R.string.app_name)
        val myDir = File(root, "$appname Temp")
        if (!myDir.exists()) {
            val m = myDir.mkdirs()
            if (m) {
                val nomedia = File(myDir, ".nomedia")
                try {
                    nomedia.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        val fname = "TEMP-"
        var file: File? = null
        try {
            file = File.createTempFile(
                fname,
                "-" + appname.uppercase(Locale.getDefault()) + ".jpg",
                myDir
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
        tempImage = file!!.path
        saveTempImg(tempImage)
        return FileProvider.getUriForFile(
            this, "com.artuok.android.fileprovider",
            file
        )
    }

    private fun showInContextUI(i: Int) {
        val dialog = PermissionDialog()
        dialog.setTitleDialog(getString(R.string.required_permissions))
        dialog.setDrawable(R.drawable.smartphone)
        when (i) {
            0 -> {
                dialog.setTextDialog(getString(R.string.permissions_wres))
                dialog.setPositive { _: DialogInterface?, _: Int ->
                    requestPermissionLauncher.launch(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            }
            1 -> {
                dialog.setTextDialog(getString(R.string.permissions_wres))
                dialog.setPositive { _: DialogInterface?, _: Int ->
                    requestPermissionLauncher.launch(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
            }
            2 -> {
                dialog.setDrawable(R.drawable.camera)
                dialog.setTextDialog(getString(R.string.permissions_camera))
                dialog.setPositive { _: DialogInterface?, _: Int ->
                    requestPermissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                }
            }
        }
        dialog.setNegative { _: DialogInterface?, _: Int -> dialog.dismiss() }
        dialog.show(supportFragmentManager, "permissions")
    }

    private var resultLaunchers = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { it: ActivityResult ->
        if (it.resultCode == RESULT_OK) {
            val path: String = getTempImg()!!
            val i = Intent(this, PhotoSelectActivity::class.java)
            i.putExtra("path", path)
            i.putExtra("icon", true)
            i.putExtra("from", "camera")
            i.getIntExtra("requestCode", 2)
            resultLauncher.launch(i)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->

    }

    private val resultForGalleryPicture = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { it: ActivityResult ->
        if (it.resultCode == RESULT_OK) {
            val data = it.data!!.data
            val i = Intent(this, PhotoSelectActivity::class.java)
            i.data = data
            i.putExtra("from", "gallery")
            i.putExtra("icon", true)
            i.getIntExtra("requestCode", 2)
            resultLauncher.launch(i)
        }
    }

    var resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data!!.getIntExtra("requestCode", 0) == 3) {
            } else if (data.getIntExtra("requestCode", 0) == 2) {
                setPhoto()
                savePicture()
            }
        }
    }

    private fun getTempImg(): String? {
        val sr: SharedPreferences = getSharedPreferences("images", MODE_PRIVATE)
        return sr.getString("TempImg", "")
    }

    fun saveTempImg(temp: String?) {
        val sr: SharedPreferences = getSharedPreferences("images", MODE_PRIVATE)
        val se = sr.edit()
        se.putString("TempImg", temp)
        se.apply()
    }


}
package com.artuok.appwork

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.artuok.appwork.fragmets.SettingsFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImageView
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.*
import java.util.*
import kotlin.math.min

class PhotoSelectActivity : AppCompatActivity() {
    private lateinit var cropper : CropImageView
    private lateinit var back : LinearLayout
    private lateinit var rotate : ImageView
    private lateinit var img : Bitmap
    private lateinit var ok : Button

    private var uploadToIcon = false
    private var carpet = "Sunfy Profile"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_select)
        val appname = getString(R.string.app_name)
        val extras = intent.extras!!
        uploadToIcon = extras.getBoolean("icon", false)
        carpet = extras.getString("carpet", "$appname Profile")
        if(extras.getString("from") == "gallery"){
            val data = intent?.data!!
            val c = contentResolver.query(data, null, null, null, null)
            if(c != null && c.moveToFirst()){
                val s = BufferedInputStream(contentResolver.openInputStream(data))
                img = BitmapFactory.decodeStream(s)
            }
        }else{
            img = BitmapFactory.decodeFile(extras?.getString("path")!!)
        }

        cropper = findViewById(R.id.cropper)
        back = findViewById(R.id.cancel_action)
        rotate = findViewById(R.id.rotate)
        ok = findViewById(R.id.accept)


        ok.setOnClickListener {
            val path = saveImageInDevice(cropper.croppedImage)
            flushTempImages()

            val returnIntent = Intent()
            returnIntent.putExtra("requestCode", 2)
            returnIntent.putExtra("data", path)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
        PushDownAnim.setPushDownAnimTo(rotate)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.95f)
            .setOnClickListener{
                cropper.rotateImage(90)
            }
        back.setOnClickListener {
            finish()
        }

        cropper.setImageBitmap(img)
        cropper.guidelines = CropImageView.Guidelines.ON
        cropper.scaleType = CropImageView.ScaleType.FIT_CENTER
        cropper.setAspectRatio(1, 1)
        cropper.setFixedAspectRatio(true)

        val min : Int = min(cropper.width, cropper.height)
        cropper.maxZoom = min/2
    }

    private fun saveImageInDevice(image: Bitmap): String {
        val root = getExternalFilesDir("Media")
        val appname = getString(R.string.app_name)
        val myDir = File(root, carpet)
        if (!myDir.exists()) {
            val m = myDir.mkdirs()
        }

        var fname = "${appname.uppercase()}-USER-IMG.jpg"

        var file = File(myDir, fname)

        if (file.exists())
            file.delete()
        try {
            val out = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (file.exists()) {
            if (uploadToIcon)
                if (!isMovileDataActive() || !isSaverModeActive()) {
                    val inn = FileInputStream(file)
                    val i = BitmapFactory.decodeStream(inn)
                    val stream = ByteArrayOutputStream()
                    i.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                    uploadImage(ByteArrayInputStream(stream.toByteArray()))
                }
            return file.path
        }



        return ""
    }

    private fun flushTempImages() {
        val root = getExternalFilesDir("Media")
        val appname = getString(R.string.app_name)
        val myDir = File(root, "$appname Temp")
        var m = false
        m = if (!myDir.exists()) {
            myDir.mkdirs()
        } else {
            myDir.delete()
            myDir.mkdirs()
        }

        if (m) {
            val nomedia = File(myDir, ".nomedia")
            nomedia.createNewFile()
        }
    }

    private fun uploadImage(map: InputStream) {
        val auth = FirebaseAuth.getInstance()
        val ref = FirebaseStorage.getInstance()
        val reference = ref.reference.child("chats/${auth.currentUser?.uid}/profile-photo.jpg")
        val uploading = reference.putStream(map)
        uploading.addOnSuccessListener {
            updateProfile(auth.currentUser?.uid!!)
            Toast.makeText(this, "Succesfully", Toast.LENGTH_SHORT).show()
            SettingsFragment.setPhotoProfile(this, true)
        }.addOnProgressListener {

        }
    }

    private fun updateProfile(u: String) {
        val db = FirebaseDatabase.getInstance().reference
        val now = Calendar.getInstance().timeInMillis
        db.child("user").child(u).child("updated").setValue(now)
    }

    private fun isSaverModeActive(): Boolean {
        val s = getSharedPreferences("settings", Context.MODE_PRIVATE)

        return s.getBoolean("datasaver", true)
    }

    private fun isMovileDataActive(): Boolean {
        var mobileDataEnable = false
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val cmClass = Class.forName(cm.javaClass.name)
            val method = cmClass.getDeclaredMethod("getMobileDataEnabled")
            method.isAccessible = true
            mobileDataEnable = method.invoke(cm) as Boolean
        }catch( e : Exception){
            e.printStackTrace()
        }
        return mobileDataEnable
    }
}
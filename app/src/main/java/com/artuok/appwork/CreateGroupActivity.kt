package com.artuok.appwork

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.adapters.ChatAdapter
import com.artuok.appwork.adapters.ShareAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.objects.ChatElement
import com.artuok.appwork.objects.Item
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.*
import java.util.*

class CreateGroupActivity : AppCompatActivity() {


    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewSelected: RecyclerView
    private var elements: ArrayList<Item> = ArrayList()
    private var selected: ArrayList<Item> = ArrayList()
    private lateinit var adapter: ChatAdapter
    private lateinit var adapterSelected: ShareAdapter

    private lateinit var editName: EditText
    private lateinit var createBtn: TextView
    private lateinit var progressbar: ProgressBar
    private lateinit var imagePicker: CardView
    private lateinit var imageView: ImageView
    private var tempImage = ""
    private var image = ""

    private var creating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creat_group)

        recyclerView = findViewById(R.id.recycler_selector)
        recyclerViewSelected = findViewById(R.id.recycler_selected)
        editName = findViewById(R.id.edit_name)
        createBtn = findViewById(R.id.create_btn)
        progressbar = findViewById(R.id.pb)
        imagePicker = findViewById(R.id.select_image)
        imageView = findViewById(R.id.selected_image)

        PushDownAnim.setPushDownAnimTo(imagePicker)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                if (!creating) {
                    openSelectImage()
                }
            }

        PushDownAnim.setPushDownAnimTo(createBtn)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                val name = checkMessage(editName.text.toString())
                if (name.isNotEmpty()) {
                    if (selected.size > 0) {
                        progressbar.visibility = View.VISIBLE
                        createBtn.visibility = View.GONE
                        editName.isEnabled = false
                        editName.alpha = 0.5f

                        createGroup(name, getNumbers(selected))
                        creating = true
                    }
                }
            }

        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val selectedLayoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        adapterSelected = ShareAdapter(this, selected)

        adapterSelected.setOnClickListener { _, pos ->
            if (!creating) {
                val item = selected[pos]
                val chat = item.`object` as ChatElement

                val d = searchSelectedById(chat.id)

                (elements[d].`object` as ChatElement).isLog = !chat.isLog
                selected.removeAt(pos)
                adapterSelected.notifyItemRemoved(pos)
                adapter.notifyItemChanged(d)
            }
        }

        adapter = ChatAdapter(this, elements) { _, pos ->
            if (!creating) {
                val item = elements[pos]
                val chat = item.`object` as ChatElement
                chat.isLog = !chat.isLog
                if (chat.isLog) {
                    selected.add(Item(chat, 0))
                    adapterSelected.notifyItemInserted(selected.size - 1)
                } else {
                    val d = searchSelectedById(chat.id)
                    selected.removeAt(d)
                    adapterSelected.notifyItemRemoved(d)
                }

                (elements[pos].`object` as ChatElement).isLog = chat.isLog
                adapter.notifyItemChanged(pos)
            }
        }

        recyclerViewSelected.layoutManager = selectedLayoutManager
        recyclerViewSelected.adapter = adapterSelected

        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = adapter

        loadUsersLogged()
    }

    private fun getNumbers(chats: ArrayList<Item>): List<String> {
        val elements: ArrayList<String> = ArrayList()
        val user = FirebaseAuth.getInstance().currentUser!!


        elements.add(user.phoneNumber!!)
        for (chat in chats) {
            val element = chat.`object` as ChatElement

            elements.add(element.number)
        }

        return elements
    }

    private fun openSelectImage() {
        //if(!verifyIfManageIfAndroidAcces()) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_selectimage_layout)

        val openCamera = dialog.findViewById<LinearLayout>(R.id.captureImage)
        val openGallery = dialog.findViewById<LinearLayout>(R.id.obtainImage)

        openCamera.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showInContextUI(0)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            dialog.dismiss()
        }

        openGallery.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                getPictureGallery()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showInContextUI(1)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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
        //}
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean? ->
        if (isGranted == true) {

        }
    }

    private fun getPictureGallery() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        resultForGalleryPicture.launch(i)
    }

    private fun openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {

            val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            i.putExtra(MediaStore.EXTRA_OUTPUT, getOutputFile())
            resultLauncher.launch(i)
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showInContextUI(2)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun getOutputFile(): Uri {
        val root = getExternalFilesDir("Media")
        val appname = getString(R.string.app_name)
        val myDir = File(root, "$appname Temp")
        if (!myDir.exists()) {
            val m = myDir.mkdirs()
            if (m) {
                val nomedia = File(myDir, ".nomedia")
                try {
                    nomedia.createNewFile()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val fname = "TEMP-"
        val file = File.createTempFile(fname, "-${appname.uppercase()}.jpg", myDir)
        tempImage = file.path
        saveTempImg(tempImage)
        return FileProvider.getUriForFile(this, "com.artuok.android.fileprovider", file)
    }

    private var resultForGalleryPicture: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            val data = it.data?.data!!
            val c = contentResolver.query(data, null, null, null, null)

            if (c != null && c.moveToFirst()) {
                val i = Intent(this, PhotoSelectActivity::class.java)
                i.putExtra("from", "gallery")
                i.putExtra("carpet", "Sunfy group")
                i.data = data
                resultCropImage.launch(i)
            }
        }
    }

    private var resultCropImage: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val extras = it.data!!.extras!!
                val tempImg = extras.getString("data")!!
                if (tempImg.isNotEmpty()) {
                    image = tempImg
                    val img = BitmapFactory.decodeFile(tempImg)
                    imageView.setImageBitmap(img)
                }
            }
        }
    private var resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {

        if (it.resultCode == RESULT_OK) {
            tempImage = getTempImg()
            val i = Intent(this, PhotoSelectActivity::class.java)
            i.putExtra("from", "camera")
            i.putExtra("path", tempImage)
            i.putExtra("carpet", "Sunfy Group")
            resultCropImage.launch(i)
            saveTempImg("")
        }
    }


    private fun getTempImg(): String {
        val sr = getSharedPreferences("images", Context.MODE_PRIVATE)

        return sr.getString("TempImg", "")!!
    }

    private fun saveTempImg(temp: String) {
        val sr = getSharedPreferences("images", Context.MODE_PRIVATE)
        val se = sr.edit()

        se.putString("TempImg", temp)

        se.apply()
    }

    private fun showInContextUI(i: Int) {

        val dialog = PermissionDialog()
        dialog.setTitleDialog(getString(R.string.required_permissions))
        dialog.setDrawable(R.drawable.smartphone)

        if (i == 0) {
            dialog.setTextDialog(getString(R.string.permissions_wres))
            dialog.setPositive { it, i ->
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else if (i == 1) {
            dialog.setTextDialog(getString(R.string.permissions_wres))
            dialog.setPositive { it, i ->
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else if (i == 2) {
            dialog.setDrawable(R.drawable.camera)
            dialog.setTextDialog(getString(R.string.permissions_camera))
            dialog.setPositive { it, i ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else if (i == 3) {
            dialog.setTextDialog(getString(R.string.permissions_wres))
            dialog.setPositive { it, i ->
                requestPermissionLauncher.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
        }


        dialog.setNegative { view, which ->
            dialog.dismiss()
        }

        dialog.show(supportFragmentManager, "permissions")
    }

    private fun createGroup(name: String, users: List<String>) {
        val db = FirebaseDatabase.getInstance().reference
        val key = db.child("chat").push().key!!

        val now = Calendar.getInstance().timeInMillis
        val hash = mapOf(
            "messages" to true,
            "users" to true,
            "updated" to now,
            "name" to name,
            "type" to 1
        )
        db.child("chat").child(key)
            .setValue(hash)
            .addOnSuccessListener {
                for (user in users) {
                    joinUser(key, user)
                }
                if (image.isNotEmpty()) {
                    val file = File(image)
                    val inn = FileInputStream(file)
                    val i = BitmapFactory.decodeStream(inn)
                    val stream = ByteArrayOutputStream()
                    i.compress(Bitmap.CompressFormat.JPEG, 60, stream)
                    uploadImage(ByteArrayInputStream(stream.toByteArray()), key)
                }
            }.addOnFailureListener {
                if (!isDestroyed)
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun joinUser(chat: String, phone: String) {
        val db = FirebaseDatabase.getInstance().reference
        db.child("user").orderByChild("phone").equalTo(phone)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists())
                        for (child in snapshot.children) {
                            if (child.key == null)
                                return
                            val user = child.key!!

                            db.child("chat").child(chat).child("users").child(user).setValue(phone)
                            db.child("user").child(user).child("chat").child(chat).setValue(true)
                            return
                        }
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun checkMessage(text: String): String {
        var length = text.length
        var start = 0
        if (length > 0) {
            var last = text.substring(length - 1, length)
            while (last == " " && length > 0) {
                length--
                last = text.substring(length - 1, length)
            }
            var first = text.substring(start, start + 1)
            while (first == " " && length - start > 0) {
                start++
                first = text.substring(start, start + 1)
            }
            if (length - start > 0) {
                return text.substring(start, length)
            }
        }
        return ""
    }

    private fun searchSelectedById(id: String): Int {

        return selected
            .asSequence()
            .map { it.`object` as ChatElement }
            .indexOfFirst { it.id == id }
    }

    private fun loadUsersLogged() {
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase
        val query = db.rawQuery(
            "SELECT * FROM ${DbChat.T_CHATS_LOGGED} WHERE log = '1' ORDER BY name COLLATE NOCASE",
            null
        )

        if (query.moveToFirst()) {
            do {
                val id = query.getInt(0)
                val name = query.getString(1)
                val number = query.getString(2)
                val chat = ChatElement("$id", name, "", "", number, "", false, 0)
                elements.add(Item(chat, 3))
            } while (query.moveToNext())
        }
        query.close()
    }

    private fun uploadImage(map: InputStream, name: String) {
        val ref = FirebaseStorage.getInstance()
        val reference = ref.reference.child("chats/$name/profile-photo.jpg")
        val uploading = reference.putStream(map)
        uploading.addOnSuccessListener {
            if (!isDestroyed)
                finish()
        }.addOnProgressListener {
            if (!isDestroyed) {
                val pg: Int = (it.totalByteCount / 100 * it.bytesTransferred).toInt()
                progressbar.max = 100
                progressbar.progress = pg
            }
        }
    }

}
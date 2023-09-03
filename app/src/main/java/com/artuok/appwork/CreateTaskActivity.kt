package com.artuok.appwork

import android.Manifest
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.adapters.ImageAdapter
import com.artuok.appwork.adapters.SubjectAdapter
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.dialogs.UtilitiesDialog
import com.artuok.appwork.dialogs.UtilitiesDialog.OnResponseListener
import com.artuok.appwork.library.Constants
import com.artuok.appwork.objects.ColorSelectElement
import com.artuok.appwork.objects.ItemSubjectElement
import com.artuok.appwork.objects.SubjectElement
import com.artuok.appwork.widgets.TodayTaskWidget
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Random


class CreateTaskActivity : AppCompatActivity() {
    private lateinit var activity: EditText
    private lateinit var chooseSubject: LinearLayout
    private lateinit var textSubject: TextView
    private lateinit var datetext: TextView
    private lateinit var timeText: TextView
    private lateinit var tipText: TextView
    private lateinit var datePicker: LinearLayout
    private lateinit var cameraPicker: CardView
    private lateinit var imgPreview: ImageView
    private lateinit var img: Bitmap;
    private val auth = FirebaseAuth.getInstance()

    private var subject: Int = 0
    private var datetime: Long = 0
    private var dateMilis: Long = 0
    private var timeMilis: Long = 0
    private lateinit var dateText: String

    private lateinit var adapter : ImageAdapter
    private lateinit var recyclerView : RecyclerView
    private val returnIntent = Intent()

    private var tempImage = ""
    private lateinit var progressUpload: ProgressBar

    private val images : ArrayList<String> = ArrayList()
    private val hashimages: ArrayList<String> = ArrayList()
    private var is24HourFormat = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_awaiting)

        activity = findViewById(R.id.description_task)
        chooseSubject = findViewById(R.id.choose_subject)
        textSubject = findViewById(R.id.subject_text)
        datePicker = findViewById(R.id.datepicker)
        datetext = findViewById(R.id.datetext)
        timeText = findViewById(R.id.timetext)
        cameraPicker = findViewById(R.id.camera)
        recyclerView = findViewById(R.id.imagesRecycler)
        progressUpload = findViewById(R.id.on_activity_upload)
        tipText = findViewById(R.id.tip)

        is24HourFormat = DateFormat.is24HourFormat(this)

        adapter = ImageAdapter(this, images) { view, pos ->
            images.removeAt(pos)
            deleteFileInDevices(hashimages[pos])
            hashimages.removeAt(pos)
            adapter.notifyItemRemoved(pos)

            if (images.size >= 6) {
                cameraPicker.visibility = View.GONE
            } else {
                cameraPicker.visibility = View.VISIBLE
            }

            resave()
        }

        val manager = GridLayoutManager(this, 3, RecyclerView.VERTICAL, false)

        recyclerView.layoutManager = manager
        recyclerView.setHasFixedSize(true)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = adapter

        preferences()

        cameraPicker.setOnClickListener {
            openSelectImage()
        }

        chooseSubject.setOnClickListener {
            setSelectSubject(textSubject)
        }

        if (is24HourFormat) {
            timeText.text = "-:--"
        }
        getDeadline()

        val cancel: ImageView = findViewById(R.id.cancel_awaiting)
        val accept: FloatingActionButton = findViewById(R.id.accept_awaiting)

        val rand = Random()

        val numero = rand.nextInt(10)

        val tip = if (numero == 0) {
            getString(R.string.ct_note1)
        } else if (numero == 1) {
            getString(R.string.ct_note2)
        } else if (numero == 2) {
            getString(R.string.ct_note3)
        } else if (numero == 3) {
            getString(R.string.ct_note4)
        } else if (numero == 4) {
            getString(R.string.ct_note5)
        } else if (numero == 5) {
            getString(R.string.ct_note6)
        } else if (numero == 6) {
            getString(R.string.ct_note7)
        } else if (numero == 7) {
            getString(R.string.ct_note8)
        } else if (numero == 8) {
            getString(R.string.ct_note9)
        } else {
            getString(R.string.ct_note10)
        }

        tipText.text = "${getString(R.string.tip)}: $tip"

        datetext.setOnClickListener {
            val calendar = Calendar.getInstance()
            val dd = calendar[Calendar.DAY_OF_MONTH]
            val mm = calendar[Calendar.MONTH]
            val aaaa = calendar[Calendar.YEAR]

            val timePicker = TimePickerDialog(
                this@CreateTaskActivity,
                { timePicker1: TimePicker?, i: Int, i1: Int ->
                    val a = if (i1 < 10) "0$i1" else i1.toString() + ""
                    val e = if (i < 10) "0$i" else i.toString() + ""
                timeMilis = ((i * 60 * 60) + (i1 * 60)) * 1000L
                datetime = dateMilis + timeMilis
                val s = if (is24HourFormat) {
                    "$i:$a"
                } else {
                    if (i >= 12) {
                        val b = if (i == 12) {
                            i
                        } else {
                            i - 12
                        }

                        "$b:$a p. m."
                    } else {
                        "$i:$a a. m."
                    }
                }

                timeText.text = s
            }, 12, 0, is24HourFormat)

            val datePicker = DatePickerDialog(this@CreateTaskActivity, { datePicker1: DatePicker?, i: Int, i1: Int, i2: Int ->
                val m = i1 + 1
                val e = if (m < 10) "0$m" else m.toString()
                val a = if (i2 < 10) "0$i2" else i2.toString()

                val c = Calendar.getInstance()
                c.set(i, i1, i2, 0, 0, 0)
                dateMilis = c.timeInMillis
                datetime = dateMilis + timeMilis

                datetext.text = "$a/$e/$i"
                timePicker.show()
            }, dd, mm, aaaa)

            val c = Calendar.getInstance()
            c[Calendar.YEAR] = aaaa
            c[Calendar.MONTH] = mm
            c[Calendar.DAY_OF_MONTH] = dd

            datePicker.datePicker.minDate = c.timeInMillis
            datePicker.show()

        }

        timeText.setOnClickListener {
            val timePicker = TimePickerDialog(this@CreateTaskActivity, { timePicker1: TimePicker?, i: Int, i1: Int ->
                val a = if (i1 < 10) "0$i1" else i1.toString() + ""
                val e = if (i < 10) "0$i" else i.toString() + ""
                timeMilis = ((i * 60 * 60) + (i1 * 60)) * 1000L
                datetime = dateMilis + timeMilis

                val s = if (is24HourFormat) {
                    "$i:$a"
                } else {
                    if (i >= 12) {
                        val b = if (i == 12) {
                            i
                        } else {
                            i - 12
                        }

                        "$b:$a p. m."
                    } else {
                        "$i:$a a. m."
                    }
                }

                timeText.text = s
            }, 12, 0, is24HourFormat)

            timePicker.show()
        }

        PushDownAnim.setPushDownAnimTo(cancel)
                .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                .setDurationPush(100)
                .setOnClickListener { finish() }
        PushDownAnim.setPushDownAnimTo(accept)
                .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                .setDurationPush(100)
                .setOnClickListener {
                    datetime = dateMilis + timeMilis
                    if (activity.text.toString().length > 0) {
                        if (datetime > 0) {
                            if (subject > 0) {
                                progressUpload.visibility = View.VISIBLE
                                accept.visibility = View.GONE
                                val l = insertAwaiting(activity.text.toString(), datetime, subject)

                                if (images.size != 0)
                                    saveImagesInDevice(images, l)

                                updateWidget()
                                returnIntent.putExtra("requestCode", 2)
                                setResult(RESULT_OK, returnIntent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this@CreateTaskActivity,
                                    getString(R.string.select_subject),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@CreateTaskActivity,
                                "Select deadline",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@CreateTaskActivity,
                            "Write something",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        getImages()
    }

    private fun updateWidget(){
        val i = Intent(this, TodayTaskWidget::class.java)
        i.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        val ids = AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, TodayTaskWidget::class.java))
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(i)
    }

    private fun openSelectImage(){
        //if(!verifyIfManageIfAndroidAcces()) {
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
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
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
        //}
    }


    private fun showInContextUI(i : Int) {

        val dialog = PermissionDialog()
        dialog.setTitleDialog(getString(R.string.required_permissions))
        dialog.setDrawable(R.drawable.smartphone)

        if(i == 0){
            dialog.setTextDialog(getString(R.string.permissions_wres))
            dialog.setPositive { it, i ->
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }else if(i == 1){
            dialog.setTextDialog(getString(R.string.permissions_wres))
            dialog.setPositive { it, i ->
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }else if( i== 2){
            dialog.setDrawable(R.drawable.camera)
            dialog.setTextDialog(getString(R.string.permissions_camera))
            dialog.setPositive { it, i ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }else if( i == 3){
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean? ->
        if (isGranted == true) {

        }
    }

    private fun saveImagesInDevice(images: ArrayList<String>, id : Long) {
        var i = 0
        for (image in images){
            val encodeByte: ByteArray = Base64.decode(image, Base64.DEFAULT)

            try {
                val map = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
                saveImageInDevice(map, id, i)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            i++
        }

        deleteImages()
    }

    private fun deleteFileInDevices(path : String) : Boolean{
        val file = File(path)

        if(file.exists())
            file.delete()
        else
            return false

        return true
    }

    private fun saveImageInDevice(image: Bitmap, id : Long, img: Int) {
        val root = getExternalFilesDir("Media")
        val appname = getString(R.string.app_name)
        val myDir = File(root, "$appname Images")
        if(!myDir.exists()){
            myDir.mkdirs()
        }

        val c = Calendar.getInstance()

        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH)
        val d = c.get(Calendar.DAY_OF_MONTH)


        val mo = if(m+1 < 10) "0${m+1}" else "${m+1}"
        val da = if(d < 10) "0$d" else "$d"

        var i = 0

        var si = "$i"

        val t = "0000".substring(0, 4-si.length) + si

        var fname = "${appname.uppercase()}-$y$mo$da-IMG$t.jpg"


        var file = File(myDir, fname)

        while(file.exists()){
            si = "$i"
            val t = "0000".substring(0, 4-si.length) + si
            fname = "${appname.uppercase()}-$y$mo$da-IMG$t.jpg"
            file = File(myDir, fname)
            i++
        }

        if(file.exists())
            file.delete()
        try {
            val out = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()

            saveImageInDataBase(id, file)
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    fun saveImageInDataBase(id : Long, file : File){
        val dbHelper = DbHelper(this)
        val db = dbHelper.writableDatabase
        val cv = ContentValues()

        val time = Calendar.getInstance().timeInMillis

        cv.put("awaiting", id)
        cv.put("name", file.name)
        cv.put("path", file.path)
        cv.put("timestamp", time)

        db.insert(DbHelper.T_PHOTOS, null, cv)
    }


    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()

        if (images.size >= 6) {
            cameraPicker.visibility = View.GONE
        }
    }

    private fun getDeadline() {
        if (intent.extras != null) {
            val deadline = intent.extras!!.getLong("deadline", 0)
            val c = Calendar.getInstance()
            c.timeInMillis = deadline + 43200000
            dateMilis = deadline
            timeMilis = 43200000
            val dd = c[Calendar.DAY_OF_MONTH]
            val mm = c[Calendar.MONTH] + 1
            val aaaa = c[Calendar.YEAR]
            val hh = c[Calendar.HOUR_OF_DAY]
            val MM =
                if (c[Calendar.MINUTE] < 10) "0${c[Calendar.MINUTE]}" else "${c[Calendar.MINUTE]}"
            val mmm =
                if (c[Calendar.MONTH] + 1 < 10) "0${c[Calendar.MONTH] + 1}" else "${c[Calendar.MONTH] + 1}"
            val tm = if (c[Calendar.AM_PM] == Calendar.AM) "a. m." else "p. m."


            val hhh = if (hh > 12) {
                hh - 12
            } else {
                hh
            }


            datetext.text = "$dd/$mmm/$aaaa"

            if (is24HourFormat) {
                timeText.text = "$hh:$MM"
            } else {
                timeText.text = "$hhh:$MM $tm"
            }
        }
    }

    private fun insertAwaiting(name: String, date: Long, subject: Int): Long {
        val dbHelper = DbHelper(this@CreateTaskActivity)
        val db = dbHelper.writableDatabase
        val now = Calendar.getInstance().timeInMillis
        val values = ContentValues()
        values.put("date", now)
        values.put("completed_date", now)
        values.put("end_date", date)
        values.put("subject", subject)
        values.put("description", name)
        values.put("status", false)
        values.put("favorite", 0)

        if(isLogin()){
            values.put("user", auth.currentUser?.uid)
        }else{
            values.put("user", "noUser")
        }

        return db.insert(DbHelper.T_TASK, null, values)
    }

    private fun isLogin() : Boolean{
        val s = getSharedPreferences("chat", Context.MODE_PRIVATE)

        return s.getBoolean("logged", false)
    }

    private fun getPictureGallery(){
        saveImages()
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        resultForGalleryPicture.launch(i)
    }

    private fun openCamera() {

        if(checkSelfPermission(Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED){
            saveImages()

                val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                i.putExtra(MediaStore.EXTRA_OUTPUT, getOutputFile())
                resultLauncher.launch(i)
        }else if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
            showInContextUI(2)
        }else{

            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun getOutputFile(): Uri{
        val root = getExternalFilesDir("Media")
        val appname = getString(R.string.app_name)
        val myDir = File(root, "$appname Temp")
        if(!myDir.exists()){
            val m = myDir.mkdirs()
            if(m){
                val nomedia = File(myDir, ".nomedia")
                try{
                    nomedia.createNewFile()
                }catch (e : Exception){
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

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putInt("subject", subject)
        outState.putLong("date", datetime)
        outState.putString("activity", activity.getText().toString())
        outState.putString("dateText", dateText)
        outState.putParcelable("img", img)
    }

    private var resultForGalleryPicture: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            val data = it.data?.data!!
            val c = contentResolver.query(data, null, null, null, null)

            if(c != null && c.moveToFirst()){
                val s = BufferedInputStream(contentResolver.openInputStream(data))
                img = BitmapFactory.decodeStream(s)
            }

            val stream = ByteArrayOutputStream()
            img.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val by = stream.toByteArray()
            val s = Base64.encodeToString(by, Base64.DEFAULT)

            saveTempImg("")
            saveImage(s, tempImage)
            getImages()

        }
    }

    private var resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
    ) {

        if (it.resultCode == RESULT_OK) {
            if(tempImage == ""){
                tempImage = getTempImg()
            }
            img = BitmapFactory.decodeFile(tempImage)
            val stream = ByteArrayOutputStream()

            img.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val by = stream.toByteArray()
            val s = Base64.encodeToString(by, Base64.DEFAULT)

            saveTempImg("")
            saveImage(s, tempImage)
            getImages()
        }
    }
    fun getTempImg(): String {
        val sr = getSharedPreferences("images", Context.MODE_PRIVATE)

        return sr.getString("TempImg", "")!!
    }

    fun saveTempImg(temp : String){
        val sr = getSharedPreferences("images", Context.MODE_PRIVATE)
        val se = sr.edit()

        se.putString("TempImg", temp)

        se.apply()
    }

    fun resave(){
        val sr = getSharedPreferences("images", Context.MODE_PRIVATE)
        val se = sr.edit()

        var i = 0
        for (x in images){
            val name = "Images$i"
            se.putString(name, x)
            i++
        }

        for(x in i until 4){
            val name = "Images$x"
            se.putString(name, "")
        }

        se.apply()
    }

    private fun saveImage(s : String, temp : String){
        val sr = getSharedPreferences("images", Context.MODE_PRIVATE)
        val se = sr.edit()
        se.putString("Images0", s)
        se.putString("ImagesTemp0", temp)
        se.apply()
    }

    private fun saveImages(){
        val sr = getSharedPreferences("images", Context.MODE_PRIVATE)
        val se = sr.edit()

        var i = 1
        for (x in images){
            val name = "Images$i"
            se.putString(name, x)
            val tmpname = "ImagesTemp$i"
            se.putString(tmpname, hashimages[i-1])
            i++
        }

        for(x in i until 4){
            val name = "Images$x"
            se.putString(name, "")
            val tmpname = "ImagesTemp$i"
            se.putString(tmpname, "")
        }

        se.apply()
    }

    private fun deleteImages(){
        val sr = getSharedPreferences("images", Context.MODE_PRIVATE)
        val se = sr.edit()

        var i = 0
        for (x in images){
            val name = "Images$i"
            se.putString(name, "")
            val tmpName = "ImagesTemp$i"
            se.putString(tmpName, "")
            deleteFileInDevices(hashimages[i])
            i++
        }

        for(x in i until 4){
            val name = "Images$x"
            se.putString(name, "")
            val tmpName = "ImagesTemp$x"
            se.putString(tmpName, "")
        }

        se.apply()
    }

    private fun getImages(){
        val sr = getSharedPreferences("images", Context.MODE_PRIVATE)
        var data = sr.getString("Images0", "")!!
        var temp = sr.getString("ImagesTemp0", "")!!

        if(data != ""){
            if(!images.contains(data)){
                images.add(data)
                hashimages.add(temp)
            }
        }
        var i = 1
        while(data != ""){
            data = sr.getString("Images$i", "")!!
            temp = sr.getString("ImagesTemp$i", "")!!

            if(data != "") {
                if(!images.contains(data)){
                    images.add(data)
                    hashimages.add(temp)
                }
            }
            i++
        }
        adapter.notifyDataSetChanged()
    }




    private fun setSelectSubject(a: TextView) {
        val subjectDialog = Dialog(this)
        subjectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        subjectDialog.setContentView(R.layout.bottom_sheet_layout)
        subjectDialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        subjectDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        subjectDialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        subjectDialog.window!!.setGravity(Gravity.BOTTOM)
        val recyclerView = subjectDialog.findViewById<RecyclerView>(R.id.subjects_recycler)
        val manager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val elements: List<ItemSubjectElement> = getSubjects()
        val adapter = SubjectAdapter(this, elements) { view: View?, position: Int ->
            subject = (elements[position].getObject() as SubjectElement).id
            val subjectText = (elements[position].getObject() as SubjectElement).name
            subjectDialog.dismiss()
            a.text = subjectText
        }
        val add = subjectDialog.findViewById<LinearLayout>(R.id.add_subject)
        add.setOnClickListener { view: View? ->
            subjectDialog.dismiss()
            UtilitiesDialog.showSubjectCreator(this, object : OnResponseListener{
                override fun onAccept(view: View?, title: TextView?) {
                    val name = Constants.parseText(title!!.text.toString())
                    if (name.isNotEmpty() || name != "") {
                        subjectDialog.dismiss()
                        subject = insertSubject(name, color).toInt()
                        a.text = name
                        returnIntent.putExtra("requestCode2", 8)
                    }
                }

                override fun onDismiss(view: View?) {

                }

                override fun onChangeColor(color: Int) {
                    this@CreateTaskActivity.color = color;
                }

            })
        }
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        subjectDialog.show()
    }

    private fun getSubjects(): List<ItemSubjectElement> {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val elements: MutableList<ItemSubjectElement> = ArrayList()
        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.t_subjects} ORDER BY name DESC", null)
        if (cursor.moveToFirst()) {
            do {
                elements.add(
                    ItemSubjectElement(
                        SubjectElement(
                            cursor.getInt(0),
                            cursor.getString(1),
                            "",
                            cursor.getInt(2)
                        ), 2
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return elements
    }

    fun preferences() {
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val b = sharedPreferences.getBoolean("DarkMode", false)
        if (b) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private lateinit var colorD: ImageView
    private var color: Int = Color.parseColor("#8bc24a")


    fun insertSubject(name: String?, color : Int) : Long {
        val dbHelper = DbHelper(this)
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put("name", name)
        values.put("color", color)
        return db.insert(DbHelper.t_subjects, null, values)
    }

    fun getColors(): List<ColorSelectElement> {
        val e: MutableList<ColorSelectElement> = ArrayList()
        e.add(ColorSelectElement("red", Color.parseColor("#f44236"), Color.parseColor("#b90005")))
        e.add(ColorSelectElement("rose", Color.parseColor("#ea1e63"), Color.parseColor("#af0039")))
        e.add(
            ColorSelectElement(
                "purple",
                Color.parseColor("#9c28b1"),
                Color.parseColor("#6a0080")
            )
        )
        e.add(
            ColorSelectElement(
                "purblue",
                Color.parseColor("#673bb7"),
                Color.parseColor("#320c86")
            )
        )
        e.add(ColorSelectElement("blue", Color.parseColor("#3f51b5"), Color.parseColor("#002983")))
        e.add(
            ColorSelectElement(
                "blueCyan",
                Color.parseColor("#2196f3"),
                Color.parseColor("#006ac0")
            )
        )
        e.add(ColorSelectElement("cyan", Color.parseColor("#03a9f5"), Color.parseColor("#007bc1")))
        e.add(
            ColorSelectElement(
                "turques",
                Color.parseColor("#008ba2"),
                Color.parseColor("#008ba2")
            )
        )
        e.add(
            ColorSelectElement(
                "bluegreen",
                Color.parseColor("#009788"),
                Color.parseColor("#00685a")
            )
        )
        e.add(ColorSelectElement("green", Color.parseColor("#4cb050"), Color.parseColor("#087f23")))
        e.add(
            ColorSelectElement(
                "greenYellow",
                Color.parseColor("#8bc24a"),
                Color.parseColor("#5a9215")
            )
        )
        e.add(
            ColorSelectElement(
                "yellowGreen",
                Color.parseColor("#cddc39"),
                Color.parseColor("#99ab01")
            )
        )
        e.add(
            ColorSelectElement(
                "yellow",
                Color.parseColor("#ffeb3c"),
                Color.parseColor("#c8b800")
            )
        )
        e.add(
            ColorSelectElement(
                "yellowOrange",
                Color.parseColor("#fec107"),
                Color.parseColor("#c89100")
            )
        )
        e.add(
            ColorSelectElement(
                "Orangeyellow",
                Color.parseColor("#ff9700"),
                Color.parseColor("#c66901")
            )
        )
        return e
    }
}
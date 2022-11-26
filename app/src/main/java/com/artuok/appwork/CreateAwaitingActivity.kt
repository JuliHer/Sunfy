package com.artuok.appwork

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.adapters.SubjectAdapter
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.fragmets.homeFragment
import com.artuok.appwork.objects.ItemSubjectElement
import com.artuok.appwork.objects.SubjectElement
import com.thekhaeng.pushdownanim.PushDownAnim
import java.util.*


class CreateAwaitingActivity : AppCompatActivity() {
    private lateinit var activity: EditText
    private lateinit var chooseSubject: TextView
    private lateinit var datePicker: TextView
    private lateinit var cameraPicker: CardView
    private lateinit var imgPreview: ImageView
    private lateinit var img: Bitmap;

    private var subject: String = ""
    private var datetime: String = ""
    private lateinit var dateText: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_awaiting)

        activity = findViewById(R.id.description_task)
        chooseSubject = findViewById(R.id.choose_subject)
        datePicker = findViewById(R.id.datepicker)
        cameraPicker = findViewById(R.id.camera)

        preferences()

        cameraPicker.setOnClickListener {

        }

        chooseSubject.setOnClickListener {
            setSelectSubject(chooseSubject)
        }

        getDeadline()

        val cancel: ImageView = findViewById(R.id.cancel_awaiting)
        val accept: Button = findViewById(R.id.accept_awaiting)

        datePicker.setOnClickListener {
            val calendar = Calendar.getInstance()

            val dd = calendar[Calendar.DAY_OF_MONTH]
            val mm = calendar[Calendar.MONTH]
            val aaaa = calendar[Calendar.YEAR]

            val timePicker = TimePickerDialog(this@CreateAwaitingActivity, { timePicker1: TimePicker?, i: Int, i1: Int ->
                val a = if (i1 < 10) "0$i1" else i1.toString() + ""
                val e = if (i < 10) "0$i" else i.toString() + ""
                datetime += " $e:$a:00"
                if (i > 12) {
                    val b = i - 12
                    dateText += " $b:$a PM"
                } else {
                    dateText += " $i:$a AM"
                }
                datePicker.setText(dateText)
            }, 0, 0, false)

            val datePicker = DatePickerDialog(this@CreateAwaitingActivity, { datePicker1: DatePicker?, i: Int, i1: Int, i2: Int ->
                val m = i1 + 1
                val e = if (m < 10) "0$m" else m.toString() + ""
                val a = if (i2 < 10) "0$i2" else i2.toString() + ""
                datetime = "$i-$e-$a"
                val c = Calendar.getInstance()
                c[i, i1, i2, 12, 0] = 0
                val day = c[Calendar.DAY_OF_WEEK]
                dateText = homeFragment.getDayOfWeek(this, day) + " " + i2 + ", " + homeFragment.getMonthMinor(this, i1) + " " + i

                timePicker.show()
            }, dd, mm, aaaa)

            val c = Calendar.getInstance()
            c[Calendar.YEAR] = aaaa
            c[Calendar.MONTH] = mm
            c[Calendar.DAY_OF_MONTH] = dd

            datePicker.datePicker.minDate = c.timeInMillis
            datePicker.show()
        }

        PushDownAnim.setPushDownAnimTo(cancel)
                .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                .setDurationPush(100)
                .setOnClickListener { view: View? -> finish() }
        PushDownAnim.setPushDownAnimTo(accept)
                .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                .setDurationPush(100)
                .setOnClickListener { view: View? ->
                    if (!datetime.isEmpty()) {
                        if (!subject.isEmpty()) {
                            insertAwaiting("", datetime, subject, activity.getText().toString())
                            finish()
                        } else {
                            Toast.makeText(this@CreateAwaitingActivity, getString(R.string.select_subject), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
    }

    private fun getDeadline() {
        if (intent.extras != null) {
            val deadline = intent.extras!!.getLong("deadline", 0);
            val c = Calendar.getInstance();
            c.timeInMillis = deadline;
            val d = c[Calendar.YEAR].toString() + "-" + (c[Calendar.MONTH] + 1) + "-" + c[Calendar.DAY_OF_MONTH] + " 00:00:00";


            datetime = d;
            val dow = c[Calendar.DAY_OF_WEEK]
            dateText = homeFragment.getDayOfWeek(this@CreateAwaitingActivity, dow) + " " + c[Calendar.DAY_OF_MONTH] + ", " +
                    homeFragment.getMonthMinor(this, c[Calendar.MONTH]) +
                    " " + c[Calendar.YEAR] + " 00:00 AM"
            datePicker.text = dateText
        }
    }

    private fun insertAwaiting(name: String, date: String, subject: String, description: String) {
        val dbHelper = DbHelper(this@CreateAwaitingActivity)
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        val c = Calendar.getInstance()
        val now = c[Calendar.YEAR].toString() + "-" + (c[Calendar.MONTH] + 1) + "-" + c[Calendar.DAY_OF_MONTH] + " " + c[Calendar.HOUR_OF_DAY] + ":" + c[Calendar.MINUTE] + ":" + c[Calendar.SECOND]
        values.put("date", now)
        values.put("title", name)
        values.put("end_date", date)
        values.put("subject", subject)
        values.put("description", description)
        values.put("status", false)
        db.insert(DbHelper.t_task, null, values)

        val returnIntent = Intent()
        returnIntent.putExtra("requestCode", 2)
        setResult(RESULT_OK, returnIntent)
    }

    private fun openCamera() {
        resultLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putString("subject", subject)
        outState.putString("date", datetime)
        outState.putString("activity", activity.getText().toString())
        outState.putString("dateText", dateText)
        outState.putParcelable("img", img)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }


    private var resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {

            val extras = it.data?.extras
            img = extras?.get("data") as Bitmap

            Log.d("catto", "camera " + img)
        }
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
            subject = (elements[position].getObject() as SubjectElement).name
            subjectDialog.dismiss()
            a.setText(subject)
        }
        val add = subjectDialog.findViewById<LinearLayout>(R.id.add_subject)
        add.setOnClickListener { view: View? ->
            subjectDialog.dismiss()
            val returnIntent = Intent()
            returnIntent.putExtra("requestCode", 3)
            setResult(RESULT_OK, returnIntent)
            finish()
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
        val cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " ORDER BY name DESC", null)
        if (cursor.moveToFirst()) {
            do {
                elements.add(ItemSubjectElement(SubjectElement(cursor.getString(1), cursor.getInt(2)), 2))
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
        Log.d("catto", "camera l")
    }
}
package com.artuok.appwork

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
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
import com.artuok.appwork.adapters.ColorSelectAdapter
import com.artuok.appwork.adapters.SubjectAdapter
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.fragmets.homeFragment
import com.artuok.appwork.objects.ColorSelectElement
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
                            insertAwaiting("", datetime, subject, activity.text.toString())
                            finish()
                        } else {
                            Toast.makeText(this@CreateAwaitingActivity, getString(R.string.select_subject), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
    }

    private fun getDeadline() {
        if (intent.extras != null) {
            val deadline = intent.extras!!.getLong("deadline", 0)
            val c = Calendar.getInstance()
            c.timeInMillis = deadline + 43200000
            val d =
                c[Calendar.YEAR].toString() + "-" + (c[Calendar.MONTH] + 1) + "-" + c[Calendar.DAY_OF_MONTH] + " 12:00:00"

            datetime = d;
            val dow = c[Calendar.DAY_OF_WEEK]
            dateText = homeFragment.getDayOfWeek(
                this@CreateAwaitingActivity,
                dow
            ) + " " + c[Calendar.DAY_OF_MONTH] + ", " +
                    homeFragment.getMonthMinor(this, c[Calendar.MONTH]) +
                    " " + c[Calendar.YEAR] + " 12:00 PM"
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
            showSubjectCreator()
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
    }

    private lateinit var colorD: ImageView
    private var color: Int = 0

    fun showSubjectCreator() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_subject_creator_layout)
        val title = dialog.findViewById<TextView>(R.id.title_subject)
        val cancel = dialog.findViewById<Button>(R.id.cancel_subject)
        val accept = dialog.findViewById<Button>(R.id.accept_subject)
        colorD = dialog.findViewById<ImageView>(R.id.color_select)
        val ta: TypedArray =
            getTheme().obtainStyledAttributes(R.styleable.AppWidgetAttrs)
        color = ta.getColor(R.styleable.AppWidgetAttrs_palette_yellow, 0)
        colorD.setColorFilter(color)
        ta.recycle()
        val color = dialog.findViewById<LinearLayout>(R.id.color_picker)
        color.setOnClickListener { view: View? -> showColorPicker() }
        cancel.setOnClickListener { view: View? -> dialog.dismiss() }
        accept.setOnClickListener { view: View? ->
            if (!title.text.toString().isEmpty() || title.text.toString() != "") {
                dialog.dismiss()
                insertSubject(title.text.toString())
            } else {
                Toast.makeText(this, R.string.name_is_empty, Toast.LENGTH_SHORT).show()
            }
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

    private lateinit var adapterC: ColorSelectAdapter
    private lateinit var elementsC: List<ColorSelectElement>

    private fun showColorPicker() {
        val colorSelector = Dialog(this)
        colorSelector.requestWindowFeature(Window.FEATURE_NO_TITLE)
        colorSelector.setContentView(R.layout.bottom_recurrence_layout)
        colorSelector.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        colorSelector.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        colorSelector.window!!.attributes.windowAnimations = R.style.DialogAnimation
        colorSelector.window!!.setGravity(Gravity.BOTTOM)
        val edi = colorSelector.findViewById<LinearLayout>(R.id.color_selecting)
        edi.visibility = View.VISIBLE
        val r = colorSelector.findViewById<RecyclerView>(R.id.recycler)
        val m = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )
        elementsC = getColors()
        adapterC = ColorSelectAdapter(this, elementsC) { view: View?, position: Int ->
            color = elementsC.get(position).getColorVibrant()
            colorD.setColorFilter(color)
            colorSelector.dismiss()
        }
        r.layoutManager = m
        r.setHasFixedSize(true)
        r.adapter = adapterC
        colorSelector.show()
    }

    fun insertSubject(name: String?) {
        val dbHelper = DbHelper(this)
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put("name", name)
        values.put("color", color)
        db.insert(DbHelper.t_subjects, null, values)
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
        e.add(
            ColorSelectElement(
                "orange",
                Color.parseColor("#fe5722"),
                Color.parseColor("#c41c01")
            )
        )
        e.add(ColorSelectElement("gray", Color.parseColor("#9e9e9e"), Color.parseColor("#707070")))
        e.add(ColorSelectElement("grayb", Color.parseColor("#607d8b"), Color.parseColor("#34525d")))
        e.add(ColorSelectElement("brown", Color.parseColor("#795547"), Color.parseColor("#4a2c21")))
        return e
    }
}
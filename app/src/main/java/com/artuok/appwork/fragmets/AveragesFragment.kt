package com.artuok.appwork.fragmets

import android.app.Dialog
import android.content.ContentValues
import android.content.DialogInterface
import android.database.DatabaseUtils
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.artuok.appwork.MainActivity
import com.artuok.appwork.R
import com.artuok.appwork.adapters.AverageAdapter
import com.artuok.appwork.adapters.ColorSelectAdapter
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.dialogs.UtilitiesDialog
import com.artuok.appwork.dialogs.UtilitiesDialog.OnResponseListener
import com.artuok.appwork.fragmets.AverageAsync.ListenerOnEvent
import com.artuok.appwork.library.Constants
import com.artuok.appwork.library.LineChart
import com.artuok.appwork.library.LineChart.LineChartData
import com.artuok.appwork.library.LineChart.LineChartDataSet
import com.artuok.appwork.objects.AverageElement
import com.artuok.appwork.objects.ColorSelectElement
import com.artuok.appwork.objects.Item
import com.artuok.appwork.objects.ItemSubjectElement
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import java.util.Calendar


class AveragesFragment : Fragment() {

    private lateinit var adapterC: ColorSelectAdapter
    private lateinit var elementsC: List<ColorSelectElement>
    private lateinit var completedTask: TextView
    private lateinit var pendingTask: TextView
    private lateinit var lineChart: LineChart
    private lateinit var adapter: AverageAdapter
    private lateinit var manager: LayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var elements: ArrayList<Item>
    private lateinit var skeleton: Skeleton
    private lateinit var task: AverageAsync
    private lateinit var averagedTask: TextView
    private lateinit var colorD: ImageView
    var color = Color.parseColor("#8bc24a")


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root: View = inflater.inflate(R.layout.fragment_averages, container, false)

        completedTask = root.findViewById(R.id.completedTasks)
        pendingTask = root.findViewById(R.id.pendingTasks)
        averagedTask = root.findViewById(R.id.averageTasks)
        lineChart = root.findViewById(R.id.line_chart)
        recyclerView = root.findViewById(R.id.average_recycler)

        elements = ArrayList()
        adapter = AverageAdapter(requireActivity(), elements
        ) { _: View?, pos: Int ->
            notifyDelete((elements[pos].getObject() as AverageElement).subject)
        }
        manager = LinearLayoutManager(requireActivity(), VERTICAL, false)

        recyclerView.layoutManager = manager
        recyclerView.setHasFixedSize(true)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = adapter

        skeleton = recyclerView.applySkeleton(R.layout.skeleton_statistics_layout, 12)
        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val shimmerColor = ta.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = ta.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY)

        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor
        skeleton.maskCornerRadius = 150f

        task = AverageAsync(object : ListenerOnEvent {
            override fun onPreExecute() {
                skeleton.showSkeleton()
            }

            override fun onExecute(b: Boolean) {
                getWeeklyProgress()
                setProgressSubject(b)
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyDataSetChanged()
                skeleton.showOriginal()
            }
        })




        if(!task.isExecuting){
            task.exec(true)
        }

        return root
    }

    fun getLineChartDataSet(): ArrayList<LineChartDataSet> {
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase

        val data: ArrayList<LineChartDataSet> = ArrayList()
        for (i in 0..6) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.DAY_OF_WEEK, i + 1)


            val date1 = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val date2 = calendar.timeInMillis


            val cursor = db.rawQuery(
                "SELECT * FROM ${DbHelper.T_TASK} WHERE status = '2' AND completed_date > '$date1' AND completed_date <= '$date2'",
                null
            )

            if (cursor.moveToFirst()) {

                data.add(LineChartDataSet(getMinDayOfWeek(i), cursor.count))
            } else {
                data.add(LineChartDataSet(getMinDayOfWeek(i), 0))
            }

        }

        return data
    }

    fun getPendingLineChartDataSet(): ArrayList<LineChartDataSet> {
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase

        val data: ArrayList<LineChartDataSet> = ArrayList()
        for (i in 0..6) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, i + 1)
            val yyyy = calendar.get(Calendar.YEAR)
            val mm = calendar.get(Calendar.MONTH)
            val dd = calendar.get(Calendar.DAY_OF_MONTH)
            calendar.set(yyyy, mm,dd, 0,0,0)
            val date1 = calendar.timeInMillis
            calendar.set(yyyy, mm,dd, 23,59,59)
            val date2 = calendar.timeInMillis


            val cursor = db.rawQuery(
                "SELECT * FROM ${DbHelper.T_TASK} WHERE status < '2' AND end_date > '$date1' AND end_date < '$date2'",
                null
            )

            if (cursor.moveToFirst()) {
                data.add(LineChartDataSet(getMinDayOfWeek(i), cursor.count))
            } else {
                data.add(LineChartDataSet(getMinDayOfWeek(i), 0))
            }

            cursor.close()
        }

        return data
    }

    private fun getMinDayOfWeek(dayOfWeek: Int): String? {
        if (isDetached)
            return ""
        if(!isAdded)
            return ""
        when (dayOfWeek) {
            0 -> return requireContext().getString(R.string.min_sunday)
            1 -> return requireContext().getString(R.string.min_monday)
            2 -> return requireContext().getString(R.string.min_tuesday)
            3 -> return requireContext().getString(R.string.min_wednesday)
            4 -> return requireContext().getString(R.string.min_thursday)
            5 -> return requireContext().getString(R.string.min_friday)
            6 -> return requireContext().getString(R.string.min_saturday)
        }
        return ""
    }

    fun getWeeklyProgress() {
        if (!isAdded)
            return
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase
        val c = Calendar.getInstance();
        val week = c.get(Calendar.WEEK_OF_YEAR)
        val year = c.get(Calendar.YEAR)

        val start = getStartEndOFWeek(week, year, true)
        val end = getStartEndOFWeek(week, year, false)

        var cursor = db.rawQuery(
            "SELECT * FROM ${DbHelper.T_TASK} WHERE status = '2' AND completed_date > '$start' AND completed_date <= '$end' AND completed_date < end_date",
            null
        )

        completedTask.text = cursor.count.toString()
        cursor.close()

        cursor = db.rawQuery(
            "SELECT * FROM ${DbHelper.T_TASK} WHERE status = '2' AND end_date > '$start' AND end_date <= '$end' AND completed_date >= end_date",
            null
        )
        pendingTask.text = cursor.count.toString()
        cursor.close()

        cursor = db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE status = '2'", null)

        if (cursor.moveToFirst()) {
            var deltaTime = 0L
            var i = 0
            do {
                val created = cursor.getLong(1)
                val finished = cursor.getLong(8)
                deltaTime += finished - created
                i++
            } while (cursor.moveToNext())

            val averagedTime = deltaTime / i

            val day = averagedTime / 1000 / 60 / 60 / 24
            val hour = averagedTime / 1000 / 60 / 60 % 24
            val minute = averagedTime / 1000 / 60 % 60

            var dates = ""
            dates = if (day > 0) {
                "${day}d ${hour}h ${minute}m"
            } else if (hour > 0) {
                "${hour}h ${minute}m"
            } else if (minute > 0) {
                "${minute}m"
            } else {
                val second = averagedTime / 1000 % 60
                "${second}s"
            }

            averagedTask.text = dates
        }
        cursor.close()


        val data: ArrayList<LineChartData> = ArrayList()
        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val color = ta.getColor(
            R.styleable.AppCustomAttrs_iMainColor,
            requireActivity().getColor(R.color.green_500)
        )
        ta.recycle()
        data.add(
            LineChartData(
                requireActivity().getString(R.string.completed_tasks),
                getLineChartDataSet(),
                color
            )
        )
        data.add(
            LineChartData(
                requireActivity().getString(R.string.pending_tasks),
                getPendingLineChartDataSet(),
                requireActivity().getColor(R.color.red_500)
            )
        )
        lineChart.setData(data)
        lineChart.invalidate()
    }

    private fun getStartEndOFWeek(enterWeek: Int, enterYear: Int, start: Boolean): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(Calendar.WEEK_OF_YEAR, enterWeek)
        calendar.set(Calendar.YEAR, enterYear)
        val startDateInStr: Long = calendar.timeInMillis
        calendar.add(Calendar.DATE, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDaString: Long = calendar.timeInMillis

        return if (start) {
            startDateInStr
        } else {
            endDaString
        }
    }

    fun notifyDataChanged() {
        val task = AverageAsync(object : ListenerOnEvent {
            override fun onPreExecute() {
                skeleton.showSkeleton()
            }

            override fun onExecute(b: Boolean) {
                getWeeklyProgress()
                setProgressSubject(b)
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyDataSetChanged()
                skeleton.showOriginal()
            }
        })

        task.exec(false)
    }

    fun setProgressSubject(isFirstTime: Boolean) {
        if (!isFirstTime) {
            elements.clear()
        }
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.t_subjects} ORDER BY name", null)

        elements.add(Item(ItemSubjectElement(
            1
        ) { _: View?, _: Int ->
            UtilitiesDialog.showSubjectCreator(requireActivity(), object : OnResponseListener {
                override fun onAccept(view: View?, title: TextView?) {
                    val msg: String = Constants.parseText(title!!.text.toString())
                    if (msg.isNotEmpty()) {
                        insertSubject(msg, color)
                    } else {
                        (requireActivity() as MainActivity).showSnackbar(getString(R.string.name_is_empty))
                    }
                }

                override fun onDismiss(view: View?) {

                }

                override fun onChangeColor(color: Int) {
                    this@AveragesFragment.color = color
                }

            })
        }, 1))

        if (cursor.moveToFirst()) {
            do {
                val subject = cursor.getInt(0)
                val color = cursor.getInt(2)

                val completedTasks = db.rawQuery(
                    "SELECT * FROM ${DbHelper.T_TASK} WHERE subject = $subject AND status = '2'",
                    null
                )
                val totalTasks =
                    db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE subject = $subject", null)

                elements.add(
                    Item(
                        AverageElement(
                            cursor.getString(1),
                            color,
                            completedTasks.count,
                            totalTasks.count
                        ), 0
                    )
                )
                completedTasks.close()
                totalTasks.close()
            } while (cursor.moveToNext())

            cursor.close()
        }
    }

    fun insertSubject(name: String?, color: Int) {
        val dbHelper = DbHelper(requireActivity().applicationContext)
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put("name", name)
        values.put("color", color)
        db.insert(DbHelper.t_subjects, null, values)
        notifyDataChanged()
    }

    private fun showColorPicker() {
        val colorSelector = Dialog(requireActivity())
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
        val m = LinearLayoutManager(requireActivity().applicationContext, VERTICAL, false)
        elementsC = getColors()
        adapterC = ColorSelectAdapter(requireActivity(), elementsC) { view: View?, position: Int ->
            color = elementsC.get(position).getColorVibrant()
            colorD.setColorFilter(color)
            colorSelector.dismiss()
        }
        r.layoutManager = m
        r.setHasFixedSize(true)
        r.adapter = adapterC
        colorSelector.show()
    }

    private fun notifyDelete(subject: String) {
        val dialog = PermissionDialog()
        dialog.setTitleDialog(requireActivity().getString(R.string.delete) + " " + subject)
        dialog.setTextDialog(subject + " " + requireActivity().getString(R.string.subject_delete))
        dialog.setDrawable(R.drawable.bookmark)
        dialog.setPositive { view: DialogInterface?, which: Int ->
            deleteSubject(subject)
            dialog.dismiss()
        }
        dialog.setNegative { view: DialogInterface?, which: Int -> dialog.dismiss() }
        dialog.show(requireActivity().supportFragmentManager, "A")
    }

    private fun deleteSubject(subject: String) {
        var subject = subject
        val dbHelper = DbHelper(requireActivity())
        val dbr = dbHelper.readableDatabase
        val db = dbHelper.writableDatabase
        subject = DatabaseUtils.sqlEscapeString(subject)
        val c =
            dbr.rawQuery("SELECT id FROM " + DbHelper.t_subjects + " WHERE name = " + subject, null)
        var idSubject = -1
        if (c.moveToFirst()) {
            idSubject = c.getInt(0)
        }
        if (idSubject >= 0) {
            db.delete(DbHelper.T_TASK, "subject = $subject", null)
            db.delete(DbHelper.t_event, "subject = '$idSubject'", null)
            db.delete(DbHelper.t_subjects, "name = $subject", null)
            (requireActivity() as MainActivity).notifyAllChanged()
            notifyDataChanged()
        }
        c.close()
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
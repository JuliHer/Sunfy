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
        ta.recycle()

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

    fun getLineChartDataSet(miliStart : Long): ArrayList<LineChartDataSet> {
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase

        val data: ArrayList<LineChart.LineChartDataSet> = ArrayList()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = miliStart
        for (i in 0..6) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val date1 = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val date2 = calendar.timeInMillis
            val d = calendar.get(Calendar.DAY_OF_WEEK)-1


            val query = """
                SELECT t.*
                FROM ${DbHelper.T_TASK} AS t
                JOIN ${DbHelper.T_TAG} AS s ON t.subject = s.id
                JOIN ${DbHelper.T_PROJECTS} AS p ON s.proyect = p.id
                WHERE t.status = '2' AND t.complete_date > ? AND t.complete_date <= ?
            """.trimIndent()

            val cursor = db.rawQuery(
                query,
                arrayOf("$date1", "$date2")
            )

            if (cursor.moveToFirst()) {
                data.add(LineChart.LineChartDataSet(Constants.getMinDayOfWeek(requireActivity(), d), cursor.count))
            } else {
                data.add(LineChart.LineChartDataSet(Constants.getMinDayOfWeek(requireActivity(), d), 0))
            }
            cursor.close()
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
                "SELECT * FROM ${DbHelper.T_TASK} WHERE status < '2' AND deadline > '$date1' AND deadline < '$date2'",
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
        val query = """
            SELECT t.*, s.name, s.color, p.name
            FROM ${DbHelper.T_TASK} AS t
            JOIN ${DbHelper.T_TAG} AS s ON t.subject = s.id
            JOIN ${DbHelper.T_PROJECTS} AS p ON s.proyect = p.id
            ORDER BY complete_date DESC
        """.trimIndent()
        val c = db.rawQuery(query, null)

        if(c.moveToFirst()){
            val name = c.getString(12)
            var completedTasks = 0
            var pendingTasks = 0
            var summeryCompletedTasks = 0
            var deltaTime = 0L

            do {
                val status = c.getInt(7)
                if(status >= 2){
                    if(completedTasks < 10){
                        completedTasks++
                        deltaTime += c.getLong(4) - c.getLong(2)
                    }
                    if(c.getLong(4) >= getStartEndOFWeek(true)){
                        summeryCompletedTasks++
                    }
                }

                if(c.getLong(5) >= getStartEndOFWeek(true) && c.getLong(5) <= getStartEndOFWeek(false)){
                    pendingTasks++
                }
            }while (c.moveToNext())

            if(completedTasks != 0){
                val averagedTime = deltaTime / completedTasks

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


            pendingTask.text = "$pendingTasks"
            completedTask.text = "$summeryCompletedTasks"
        }
        val data: ArrayList<LineChart.LineChartData> = ArrayList()
        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val color = ta.getColor(
            R.styleable.AppCustomAttrs_iMainColor,
            requireActivity().getColor(R.color.green_500)
        )
        ta.recycle()
        data.add(
            LineChart.LineChartData(
                getString(R.string.completed_tasks),
                getLineChartDataSet(getStartEndOFWeek(true)),
                color
            )
        )

        lineChart.setData(data)
        lineChart.invalidate()
        c.close()
    }

    private fun getStartEndOFWeek(start: Boolean): Long {
        val cal: Calendar = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23); // Establece la hora en 11 (11 PM)
        cal.set(Calendar.MINUTE, 59);     // Establece los minutos en 59
        cal.set(Calendar.SECOND, 59);     // Establece los segundos en 59
        cal.set(Calendar.MILLISECOND, 999);
        val todayMili = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -7) // Resta 7 días a la fecha actual
        cal[Calendar.HOUR_OF_DAY] = 0 // Establece la hora en 0 (12 AM)
        cal[Calendar.MINUTE] = 0 // Establece los minutos en 0
        cal[Calendar.SECOND] = 0 // Establece los segundos en 0
        cal[Calendar.MILLISECOND] = 0 // Establece los milisegundos en 0
        val lastWeekMili = cal.timeInMillis

        return if (start) {
            lastWeekMili
        } else {
            todayMili
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
        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.T_TAG} ORDER BY name", null)

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
        db.insert(DbHelper.T_TAG, null, values)
        notifyDataChanged()
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
        val dbHelper = DbHelper(requireActivity())
        val dbr = dbHelper.readableDatabase
        val db = dbHelper.writableDatabase
        val c = dbr.rawQuery("SELECT id FROM ${DbHelper.T_TAG} WHERE name = ?", arrayOf(subject))
        var idSubject = -1
        if (c.moveToFirst()) {
            idSubject = c.getInt(0)
        }
        if (idSubject >= 0) {
            db.delete(DbHelper.T_TASK, "subject = ?", arrayOf("$idSubject"))
            db.delete(DbHelper.t_event, "subject = ?", arrayOf("$idSubject"))
            db.delete(DbHelper.T_TAG, "id = ?", arrayOf("$idSubject"))
            (requireActivity() as MainActivity).notifyAllChanged()
            notifyDataChanged()
        }
        c.close()
    }


}
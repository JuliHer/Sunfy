package com.artuok.appwork.fragmets

import android.database.DatabaseUtils
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.artuok.appwork.R
import com.artuok.appwork.adapters.AverageAdapter
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.fragmets.AverageAsync.ListenerOnEvent
import com.artuok.appwork.library.LineChart
import com.artuok.appwork.library.LineChart.LineChartData
import com.artuok.appwork.library.LineChart.LineChartDataSet
import com.artuok.appwork.objects.AverageElement
import com.artuok.appwork.objects.Item
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import java.text.SimpleDateFormat
import java.util.*


class AveragesFragment : Fragment() {

    private lateinit var completedTask: TextView
    private lateinit var pendingTask: TextView
    private lateinit var lineChart: LineChart
    private lateinit var adapter: AverageAdapter
    private lateinit var manager: LayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var elements: ArrayList<Item>
    private lateinit var skeleton: Skeleton
    private lateinit var task: AverageAsync
    private var dataChanged: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root: View = inflater.inflate(R.layout.fragment_averages, container, false)

        completedTask = root.findViewById(R.id.completedTasks)
        pendingTask = root.findViewById(R.id.pendingTasks)
        lineChart = root.findViewById(R.id.line_chart)
        recyclerView = root.findViewById(R.id.average_recycler)

        elements = ArrayList()
        adapter = AverageAdapter(requireActivity(), elements)
        manager = LinearLayoutManager(requireActivity(), VERTICAL, false)

        recyclerView.layoutManager = manager
        recyclerView.setHasFixedSize(true)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = adapter


        skeleton = recyclerView.applySkeleton(R.layout.skeleton_statistics_layout, 12)

        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppWidgetAttrs)
        val shimmerColor = ta.getColor(R.styleable.AppWidgetAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = ta.getColor(R.styleable.AppWidgetAttrs_maskSkeleton, Color.LTGRAY)

        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor

        task = AverageAsync(object : ListenerOnEvent {
            override fun onPreExecute() {
                skeleton.showSkeleton()
            }

            override fun onExecute(b: Boolean) {
                getWeeklyProgress()
                setProgressSubject(b)
                adapter.notifyDataSetChanged()
            }

            override fun onPostExecute(b: Boolean) {

                skeleton.showOriginal()
            }
        })



        task.exec(true)



        return root
    }

    override fun onStart() {

        if (dataChanged) {
            task.exec(false)
        }
        super.onStart()
    }

    fun getLineChartDataSet(): ArrayList<LineChartDataSet> {
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase

        val data: ArrayList<LineChartDataSet> = ArrayList()
        for (i in 0..6) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, i + 1)

            val format = SimpleDateFormat("yyyy-MM-dd 00:00:00")

            val date1 = format.format(calendar.time)
            format.applyPattern("yyyy-MM-dd 23:59:59")
            val date2 = format.format(calendar.time)


            val cursor = db.rawQuery(
                "SELECT * FROM ${DbHelper.t_task} WHERE status = '1' AND date BETWEEN '$date1' AND '$date2'",
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

            val format = SimpleDateFormat("yyyy-MM-dd 00:00:00")

            val date1 = format.format(calendar.time)
            format.applyPattern("yyyy-MM-dd 23:59:59")
            val date2 = format.format(calendar.time)


            val cursor = db.rawQuery(
                "SELECT * FROM ${DbHelper.t_task} WHERE status = '0' AND end_date BETWEEN '$date1' AND '$date2'",
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

    private fun getMinDayOfWeek(dayOfWeek: Int): String? {
        when (dayOfWeek) {
            0 -> return context!!.getString(R.string.min_sunday)
            1 -> return context!!.getString(R.string.min_monday)
            2 -> return context!!.getString(R.string.min_tuesday)
            3 -> return context!!.getString(R.string.min_wednesday)
            4 -> return context!!.getString(R.string.min_thursday)
            5 -> return context!!.getString(R.string.min_friday)
            6 -> return context!!.getString(R.string.min_saturday)
        }
        return ""
    }

    fun getWeeklyProgress() {
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase
        val c = Calendar.getInstance();
        val week = c.get(Calendar.WEEK_OF_YEAR)
        val year = c.get(Calendar.YEAR)

        val start = getStartEndOFWeek(week, year, true)
        val end = getStartEndOFWeek(week, year, false)


        var cursor = db.rawQuery(
            "SELECT * FROM ${DbHelper.t_task} WHERE status = '1' AND date BETWEEN '$start' AND '$end'",
            null
        )

        completedTask.text = cursor.count.toString()
        cursor.close()

        cursor = db.rawQuery("SELECT * FROM ${DbHelper.t_task} WHERE status = '0' ", null)
        pendingTask.text = cursor.count.toString()
        cursor.close()

        val data: ArrayList<LineChartData> = ArrayList()

        data.add(
            LineChartData(
                requireActivity().getString(R.string.completed_tasks),
                getLineChartDataSet(),
                requireActivity().getColor(R.color.green_500)
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

    fun getStartEndOFWeek(enterWeek: Int, enterYear: Int, start: Boolean): String {
        val calendar: Calendar = Calendar.getInstance()
        calendar.clear()
        calendar.set(Calendar.WEEK_OF_YEAR, enterWeek)
        calendar.set(Calendar.YEAR, enterYear)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val startDate: Date = calendar.time
        val startDateInStr: String = formatter.format(startDate)
        calendar.add(Calendar.DATE, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val enddate: Date = calendar.time
        val endDaString: String = formatter.format(enddate)

        if (start) {
            return startDateInStr
        } else {
            return endDaString
        }
    }

    fun notifyDataChanged() {
        task.exec(false)
    }


    fun setProgressSubject(isFirstTime: Boolean) {
        if (!isFirstTime) {
            elements.clear()
        }
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.t_subjects}", null)


        if (cursor.moveToFirst()) {
            do {
                val subject = DatabaseUtils.sqlEscapeString(cursor.getString(1))
                val color = cursor.getInt(2)


                val completedTasks = db.rawQuery(
                    "SELECT * FROM ${DbHelper.t_task} WHERE subject = $subject AND status = '1'",
                    null
                )
                val totalTasks =
                    db.rawQuery("SELECT * FROM ${DbHelper.t_task} WHERE subject = $subject", null)

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
            } while (cursor.moveToNext())
        }
    }
}
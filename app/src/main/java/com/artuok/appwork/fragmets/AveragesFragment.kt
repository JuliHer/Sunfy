package com.artuok.appwork.fragmets

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
        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val shimmerColor = ta.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = ta.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY)

        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor

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
                "SELECT * FROM ${DbHelper.T_TASK} WHERE status = '1' AND date > '$date1' AND date <= '$date2'",
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
                "SELECT * FROM ${DbHelper.T_TASK} WHERE status = '0' AND end_date > '$date1' AND end_date <= '$date2'",
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
        if (isDetached)
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
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase
        val c = Calendar.getInstance();
        val week = c.get(Calendar.WEEK_OF_YEAR)
        val year = c.get(Calendar.YEAR)

        val start = getStartEndOFWeek(week, year, true)
        val end = getStartEndOFWeek(week, year, false)

        var cursor = db.rawQuery(
            "SELECT * FROM ${DbHelper.T_TASK} WHERE status = '1' AND date > '$start' AND date <= '$end'",
            null
        )

        completedTask.text = cursor.count.toString()
        cursor.close()

        cursor = db.rawQuery(
            "SELECT * FROM ${DbHelper.T_TASK} WHERE status = '0' AND date > '$start' AND date <= '$end'",
            null
        )
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

    fun getStartEndOFWeek(enterWeek: Int, enterYear: Int, start: Boolean): Long {
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

        if (start) {
            return startDateInStr
        } else {
            return endDaString
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


        if (cursor.moveToFirst()) {
            do {
                val subject = cursor.getInt(0)
                val color = cursor.getInt(2)

                val completedTasks = db.rawQuery(
                    "SELECT * FROM ${DbHelper.T_TASK} WHERE subject = $subject AND status = '1'",
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
            } while (cursor.moveToNext())
        }
    }
}
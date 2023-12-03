package com.artuok.appwork.fragmets

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.MainActivity
import com.artuok.appwork.ProjectActivity
import com.artuok.appwork.R
import com.artuok.appwork.adapters.TasksAdapter
import com.artuok.appwork.adapters.TasksAdapter.OnRecyclerListener
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.dialogs.CreateTaskDialog
import com.artuok.appwork.dialogs.NewProjectDialog
import com.artuok.appwork.library.Constants
import com.artuok.appwork.library.LineChart.LineChartData
import com.artuok.appwork.library.LineChart.LineChartDataSet
import com.artuok.appwork.objects.AnnouncesElement
import com.artuok.appwork.objects.CountElement
import com.artuok.appwork.objects.Item
import com.artuok.appwork.objects.LineChartElement
import com.artuok.appwork.objects.ProjectsElement
import com.artuok.appwork.objects.ProyectElement
import com.artuok.appwork.objects.TaskElement
import com.artuok.appwork.objects.TasksElement
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MediaContent
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date


class HomeFragment : Fragment() {
    private lateinit var recycler : RecyclerView
    private lateinit var dbr : SQLiteDatabase
    private lateinit var adapter: TasksAdapter
    private var elements: ArrayList<Item> = ArrayList()
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data!!
            val requestCode = data.getIntExtra("requestCode", 0)
            if (requestCode == 2) {
                (requireActivity() as MainActivity).notifyAllChanged()
            } else if (requestCode == 3) {
                val i = data.getIntExtra("positionOfId", -1)
                (requireActivity() as MainActivity).notifyAllChanged()
            }
        }
    }
    private val listener: OnRecyclerListener = OnRecyclerListener { view, position ->
        (requireActivity() as MainActivity).navigateTo(1)
    }
    var advirments = 0


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        initRecyclerView(root)
        loadDashboard()
        registLauncher()
        return root
    }

    fun registLauncher(){
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data!!
                val requestCode = data.getIntExtra("requestCode", 0)
                if (requestCode == 2) {
                    (requireActivity() as MainActivity).notifyAllChanged()
                } else if (requestCode == 3) {
                    val i = data.getIntExtra("positionOfId", -1)
                    (requireActivity() as MainActivity).notifyAllChanged()
                }
            }
        }
    }

    fun loadDashboard(){
        AverageAsync(object : AverageAsync.ListenerOnEvent{
            override fun onPreExecute() {

            }

            override fun onExecute(b: Boolean) {
                getDashboard()
            }

            override fun onPostExecute(b: Boolean) {
                if(isAdded)
                    adapter.notifyDataSetChanged()
            }

        }).exec(true);
    }

    fun initRecyclerView(root: View){
        val dbHelper = DbHelper(requireActivity())
        dbr = dbHelper.readableDatabase
        recycler = root.findViewById(R.id.recyclerview)
        adapter = TasksAdapter(requireActivity(), elements, listener)
        adapter.setAddEventListener { view, pos ->
            val dialog = CreateTaskDialog()
            dialog.setOnCheckListener { view1: View?, id: Long ->
                dialog.dismiss()
                (requireActivity() as MainActivity).notifyAllChanged()
            }
            dialog.show(requireActivity().supportFragmentManager, "Create Task")
        }
        recycler.layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        recycler.adapter = adapter
    }

    fun getDashboard(){
        elements.clear()
        val resume = CountElement(getTimeDay(),
            { view: View? ->
                (requireActivity() as MainActivity).loadExternalFragment(
                    (requireActivity() as MainActivity).chatFragment,
                    requireActivity().getString(R.string.chat)
                )
            }
        ) { view: View? ->
            (requireActivity() as MainActivity).loadExternalFragment(
                (requireActivity() as MainActivity).settingsFragment,
                requireActivity().getString(R.string.settings_menu)
            )
        }

        if (!SettingsFragment.isLogged(requireActivity())) {
            resume.isChatVisible = false
        }

        elements.add(Item(resume, 1))
        val projects: List<Item> = getProjects()
        val linearChart = LineChartElement(getWeeklyProgress(-1)) {
            (requireActivity() as MainActivity).loadExternalFragment((requireActivity() as MainActivity).averagesFragment, requireActivity().getString(R.string.average_fragment_menu))
        }
        if (projects.isNotEmpty())
            elements.add(Item(ProjectsElement(projects) { view: View?, position: Int ->
                if(position > 0){
                    val i = Intent(
                        requireContext(),
                        ProjectActivity::class.java
                    )
                    val id = (projects[position].getObject() as ProyectElement).id.toInt()
                    i.putExtra("project", id)
                    resultLauncher.launch(i)
                }else{
                    val dialog = NewProjectDialog()
                    dialog.setOnCreateProjectListener { view12: View? -> (requireActivity() as MainActivity).notifyAllChanged() }
                    dialog.show(requireActivity().supportFragmentManager, "Create Project")
                }
            }, 3))


        elements.add(Item(TasksElement("", "", 0, getTasks(0)), 0))
        elements.add(Item(TasksElement("Next week", "", 0, getTasks(1)), 0))
        elements.add(Item(linearChart, 2))
        elements.add(Item(TasksElement(requireActivity().getString(R.string.task), requireActivity().getString(R.string.subject), 1, getTags()), 0))

        setAnnounce(4)


    }

    private fun getTimeDay(): String? {
        val c = Calendar.getInstance()
        val hour = c[Calendar.HOUR_OF_DAY]
        return if (hour >= 19) {
            requireActivity().getString(R.string.good_night)
        } else if (hour >= 12) {
            requireActivity().getString(R.string.good_afternoon)
        } else if (hour < 4) {
            requireActivity().getString(R.string.good_night)
        } else {
            requireActivity().getString(R.string.good_morning)
        }
    }

    private fun getTasks(week:Int):ArrayList<TaskElement>{
        val tasks: ArrayList<TaskElement> = ArrayList()
        val weekStart = getWeek(week, true)
        val weekend = getWeek(week, false)
        val query = """
            SELECT t.*, s.color 
            FROM ${DbHelper.T_TASK} AS t 
            JOIN ${DbHelper.T_TAG} AS s ON s.id = t.subject
            JOIN ${DbHelper.T_PROJECTS} AS p ON p.id = s.proyect
            WHERE t.deadline BETWEEN ? AND ? ORDER BY deadline ASC
        """.trimIndent()
        val c = dbr.rawQuery(query, arrayOf("$weekStart", "$weekend"))
        if (c.moveToFirst()) {
            do {
                val checked = c.getInt(7) == 2
                val task = c.getString(1)
                val timeInMillis = c.getLong(5)
                val cal = Calendar.getInstance()
                cal.timeInMillis = timeInMillis
                val d = cal[Calendar.DAY_OF_WEEK]
                val time = Constants.getDayOfWeek(requireActivity(), d)
                val color = c.getInt(10)

                tasks.add(TaskElement(checked, task, time, color, timeInMillis))
            } while (c.moveToNext())
        }
        c.close()
        return tasks
    }

    private fun getTags():ArrayList<TaskElement>{
        val tasks: ArrayList<TaskElement> = ArrayList()
        val query = """
            SELECT t.*, COUNT(t.id) AS num
            FROM ${DbHelper.T_TAG} AS t 
            LEFT JOIN ${DbHelper.T_TASK} AS tt ON t.id = tt.subject
            GROUP BY t.id ORDER BY num DESC
        """.trimIndent()
        val c = dbr.rawQuery(query, null)
        if (c.moveToFirst()) {
            do {

                val color = c.getInt(2)
                val name = c.getString(1)
                val num = c.getString(4)

                tasks.add(TaskElement(false, name, num, color, 9999999999999))
            } while (c.moveToNext())
        }
        c.close()
        return tasks
    }

    private fun getWeeklyProgress(week: Int): ArrayList<LineChartData>? {
        val data = ArrayList<LineChartData>()
        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val color = ta.getColor(
            R.styleable.AppCustomAttrs_iMainColor,
            requireActivity().getColor(R.color.green_500)
        )
        ta.recycle()
        data.add(
            LineChartData(
                requireActivity().getString(R.string.completed_tasks),
                getLineChartDataSet(getWeek(week, true)),
                color
            )
        )
        return data
    }

    private fun getLineChartDataSet(miliStart: Long): ArrayList<LineChartDataSet> {
        val data: ArrayList<LineChartDataSet> = ArrayList()
        val cal = Calendar.getInstance()
        cal.timeInMillis = miliStart
        for (i in 0..6) {
            cal[Calendar.HOUR_OF_DAY] = 0
            cal[Calendar.MINUTE] = 0
            cal[Calendar.SECOND] = 0
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val date1 = cal.timeInMillis
            cal[Calendar.HOUR_OF_DAY] = 23
            cal[Calendar.MINUTE] = 59
            cal[Calendar.SECOND] = 59
            val date2 = cal.timeInMillis
            val d = cal[Calendar.DAY_OF_WEEK] - 1
            val query = """
                SELECT t.* 
                FROM ${DbHelper.T_TASK} AS t 
                JOIN ${DbHelper.T_TAG} AS s ON t.subject = s.id 
                JOIN ${DbHelper.T_PROJECTS} AS p ON s.proyect = p.id 
                WHERE t.status = '2' AND t.complete_date > ? AND t.complete_date <= ?
            """.trimIndent()

            val cursor= dbr.rawQuery(
                query, arrayOf("$date1", "$date2")
            )
            if (cursor.moveToFirst()) {
                data.add(LineChartDataSet(Constants.getMinDayOfWeek(requireActivity(), d), cursor.count)
                )
            } else {
                data.add(LineChartDataSet(Constants.getMinDayOfWeek(requireActivity(), d), 0))
            }
            cursor.close()
        }
        return data
    }

    private fun setAnnounce(pos: Int) {
        val finalPos = pos + advirments
        advirments++
        val adLoader: AdLoader = AdLoader.Builder(requireActivity(), Constants.ID_PUB_AD)
                .forNativeAd { nativeAd: NativeAd ->
                    val adpos = Math.min(finalPos, elements.size)
                    val title = nativeAd.headline
                    val body = nativeAd.body
                    val advertiser = nativeAd.advertiser
                    val price = nativeAd.price
                    val images: MediaContent? = nativeAd.mediaContent
                    val icon: NativeAd.Image? = nativeAd.icon
                    advirments--
                    val element = AnnouncesElement(nativeAd, title, body, advertiser, images, icon)
                    element.action = nativeAd.callToAction
                    element.price = price
                    elements.add(adpos, Item(element, 12))
                    adapter.notifyItemInserted(adpos)
                }.withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                        .setRequestMultipleImages(false)
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
                        .build()
                ).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun NotifyDataChanged(pos: Int){

    }

    fun NotifyDataAdd(){
        loadDashboard()
    }

    private fun getProjects(): ArrayList<Item> {
        val query = dbr.rawQuery("SELECT * FROM ${DbHelper.T_PROJECTS} ORDER BY name ASC", null)
        val projects: ArrayList<Item> = ArrayList()
        projects.add(0, Item(ProyectElement(-1, "", "", 0), 2))
        if (query.moveToFirst()) {
            do {
                val name: String = query.getString(1)
                val id: Int = query.getInt(0)
                val resource: Int = query.getInt(2)
                val key = if (query.getString(3) == null) "" else query.getString(3)
                projects.add(Item(ProyectElement(id.toLong(), key, name, resource), 0))
            } while (query.moveToNext())
        }
        query.close()
        return projects
    }

    private fun getWeek(week: Int, start:Boolean): Long {
        val s = 7*week
        val e = 7
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = 0
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        cal.add(Calendar.DAY_OF_YEAR, s)
        val weekstart = cal.timeInMillis

        cal[Calendar.HOUR_OF_DAY] = 23
        cal[Calendar.MINUTE] = 59
        cal[Calendar.SECOND] = 59
        cal[Calendar.MILLISECOND] = 999
        cal.add(Calendar.DAY_OF_YEAR, e)
        val weekend = cal.timeInMillis

        return if (start) weekstart else weekend
    }
}
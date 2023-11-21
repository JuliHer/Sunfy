package com.artuok.appwork

import android.app.Activity
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.adapters.AverageAdapter
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.dialogs.UtilitiesDialog
import com.artuok.appwork.dialogs.UtilitiesDialog.OnResponseListener
import com.artuok.appwork.fragmets.AverageAsync
import com.artuok.appwork.library.Constants
import com.artuok.appwork.library.LineChart
import com.artuok.appwork.objects.AverageElement
import com.artuok.appwork.objects.Item
import com.artuok.appwork.objects.ItemSubjectElement
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.thekhaeng.pushdownanim.PushDownAnim
import java.util.Calendar


class ProjectActivity : AppCompatActivity() {
    lateinit var projectName : TextView
    private lateinit var completedTask: TextView
    private lateinit var pendingTask: TextView
    private lateinit var averagedTask: TextView
    private lateinit var lineChart: LineChart

    private lateinit var adapter: AverageAdapter
    private lateinit var manager: RecyclerView.LayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var elements: ArrayList<Item>
    private lateinit var skeleton: Skeleton
    private lateinit var backBtn : ImageView
    private lateinit var kanbanBtn : ImageView
    private lateinit var deleteBtn : TextView
    private var newSubjectColor = Color.parseColor("#8bc24a")

    private lateinit var completedChevron : ImageView
    private lateinit var pendingChevron : ImageView

    private lateinit var task: AverageAsync
    lateinit var dbHelper: DbHelper
    var projectId = -1

    private var resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data!!
            val requestCode = data.getIntExtra("requestCode", 0)
            if (requestCode == 2) {
                val intent = Intent()
                intent.putExtra("requestCode", 2)

                setResult(Activity.RESULT_OK, intent)
            } else if (requestCode == 3) {
                val i = data.getIntExtra("positionOfId", -1)
                val intent = Intent()
                intent.putExtra("positionOfId", i)
                intent.putExtra("requestCode", 2)
                setResult(Activity.RESULT_OK, intent)
            }
            notifyDataChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)
        declareVar()
        projectId = intent.getIntExtra("project", -1)
        if(projectId < 0)
            finish()
        initProject(projectId)
        initRecycler()
        intButtons()
    }

    private fun intButtons() {
        backBtn.setOnClickListener {

                finish()
            }
        kanbanBtn.setOnClickListener {
            val i = Intent(this, ProjectKanbanActivity::class.java)
            i.putExtra("project", projectId)
            resultLauncher.launch(i)
        }

        if(projectId == 0){
            deleteBtn.visibility = View.GONE
        }

        PushDownAnim.setPushDownAnimTo(deleteBtn)
            .setOnClickListener {
                val permissionDialog = PermissionDialog()
                val projectName = projectName.text.toString()
                val title = getString(R.string.delete) + " "+ projectName
                permissionDialog.setTitleDialog(title)
                val text = projectName + " "+getString(R.string.project_delete)
                permissionDialog.setDrawable(R.drawable.ic_trash)
                permissionDialog.setTextDialog(text)
                permissionDialog.setPositiveText(getString(R.string.delete))
                permissionDialog.setPositive { view, which ->
                    deleteProject()
                    permissionDialog.dismiss()
                    val intent = Intent()
                    intent.putExtra("requestCode", 2)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
                permissionDialog.setNegativeText(getString(R.string.Cancel_M))
                permissionDialog.setNegative { view, which ->
                    permissionDialog.dismiss()
                }
                permissionDialog.show(supportFragmentManager, "Delete Project")
            }
    }

    private fun declareVar(){
        projectName = findViewById(R.id.project_name)
        completedTask = findViewById(R.id.completed_tasks)
        pendingTask = findViewById(R.id.pending_tasks)
        completedChevron = findViewById(R.id.completed_chevron)
        pendingChevron = findViewById(R.id.pending_chevron)
        averagedTask = findViewById(R.id.average_tasks)
        lineChart = findViewById(R.id.line_chart)
        recyclerView = findViewById(R.id.recycler)
        backBtn = findViewById(R.id.back_button)
        kanbanBtn =findViewById(R.id.kanban_button)
        deleteBtn = findViewById(R.id.delete_project)
        dbHelper = DbHelper(this)
        restartResultLauncher()
    }

    fun initProject(id : Int){
        val db = dbHelper.readableDatabase
        val query = """
           SELECT name
           FROM ${DbHelper.T_PROJECTS} 
           WHERE id = ?
        """.trimIndent()
        val c = db.rawQuery(query, arrayOf("$id"))
        if(c.moveToFirst()){
            val name = c.getString(0)
            projectName.text = name
        }
        c.close()
    }

    fun initValues(id : Int){
        val db = dbHelper.readableDatabase
        val query = """
            SELECT t.*, s.name, s.color, p.name
            FROM ${DbHelper.T_TASK} AS t
            JOIN ${DbHelper.T_TAG} AS s ON t.subject = s.id
            JOIN ${DbHelper.T_PROJECTS} AS p ON s.proyect = p.id
            WHERE p.id = ? ORDER BY complete_date DESC
        """.trimIndent()
        val c = db.rawQuery(query, arrayOf("$id"))

        if(c.moveToFirst()){
            val name = c.getString(12)
            var completedTasks = 0
            var pendingTasks = 0
            var lastPendingTasks = 0
            var lastsummeryCompletedTasks = 0
            var summeryCompletedTasks = 0
            var deltaTime = 0L
            pendingChevron.visibility = View.VISIBLE
            completedChevron.visibility = View.VISIBLE

            projectName.text = name
            do {
                val status = c.getInt(7)
                if(status >= 2){
                    if(completedTasks < 10){
                        completedTasks++
                        deltaTime += c.getLong(4) - c.getLong(2)
                    }

                    if(c.getLong(4) >= getStartEndOFWeek(1,true) && c.getLong(4) <= getStartEndOFWeek(1,false)){
                        lastsummeryCompletedTasks++
                    }

                    if(c.getLong(4) >= getStartEndOFWeek(true)){
                        summeryCompletedTasks++
                    }
                }

                if(c.getLong(5) >= getStartEndOFWeek(1,true) && c.getLong(5) <= getStartEndOFWeek(1,false)){
                    lastPendingTasks++
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

            if(pendingTasks > lastPendingTasks){
                pendingChevron.setImageResource(R.drawable.ic_chevron_up)
                pendingChevron.setColorFilter(getColor(R.color.green_500))
            }else if(pendingTasks < lastPendingTasks){
                pendingChevron.setImageResource(R.drawable.ic_chevron_down)
                pendingChevron.setColorFilter(getColor(R.color.red_500))
            }else{
                pendingChevron.visibility = View.GONE
            }

            if(summeryCompletedTasks > lastsummeryCompletedTasks){
                completedChevron.setImageResource(R.drawable.ic_chevron_up)
                completedChevron.setColorFilter(getColor(R.color.green_500))
            }else if(summeryCompletedTasks < lastsummeryCompletedTasks){
                completedChevron.setImageResource(R.drawable.ic_chevron_down)
                completedChevron.setColorFilter(getColor(R.color.red_500))
            }else{
                completedChevron.visibility = View.GONE
            }

            pendingTask.text = "$pendingTasks"
            completedTask.text = "$summeryCompletedTasks"
        }

        val data: ArrayList<LineChart.LineChartData> = ArrayList()
        val ta = obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val color = ta.getColor(
            R.styleable.AppCustomAttrs_iMainColor,
            getColor(R.color.green_500)
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
        c.close()
    }

    fun initRecycler(){
        elements = ArrayList()
        adapter = AverageAdapter(this, elements
        ) { _: View?, pos: Int ->
            notifyDelete((elements[pos].getObject() as AverageElement).subject)
        }
        manager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        recyclerView.layoutManager = manager
        recyclerView.setHasFixedSize(true)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.adapter = adapter

        skeleton = recyclerView.applySkeleton(R.layout.skeleton_statistics_layout, 12)
        val ta = obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val shimmerColor = ta.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = ta.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY)
        ta.recycle()

        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor
        skeleton.maskCornerRadius = 150f

        task = AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {
                skeleton.showSkeleton()
            }

            override fun onExecute(b: Boolean) {
                initValues(projectId)
                setProgressSubject(projectId, b)
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyDataSetChanged()
                skeleton.showOriginal()
            }
        })

        task.exec(true)
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

    private fun getStartEndOFWeek(lessweek:Int,start: Boolean): Long {
        val cal: Calendar = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23); // Establece la hora en 11 (11 PM)
        cal.set(Calendar.MINUTE, 59);     // Establece los minutos en 59
        cal.set(Calendar.SECOND, 59);     // Establece los segundos en 59
        cal.set(Calendar.MILLISECOND, 999);
        cal.add(Calendar.DAY_OF_YEAR, -7*lessweek)
        val todayMili = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -7*(lessweek+1)) // Resta 7 días a la fecha actual
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

    fun getLineChartDataSet(miliStart : Long): ArrayList<LineChart.LineChartDataSet> {
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
                WHERE p.id = ? AND t.status = '2' AND t.complete_date > ? AND t.complete_date <= ?
            """.trimIndent()

            val cursor = db.rawQuery(
                query,
                arrayOf("$projectId", "$date1", "$date2")
            )

            if (cursor.moveToFirst()) {
                data.add(LineChart.LineChartDataSet(Constants.getMinDayOfWeek(this, d), cursor.count))
            } else {
                data.add(LineChart.LineChartDataSet(Constants.getMinDayOfWeek(this, d), 0))
            }
            cursor.close()
        }

        return data
    }

    fun setProgressSubject(id : Int, isFirstTime : Boolean) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.T_TAG} WHERE proyect = ? ORDER BY name", arrayOf("$id"))
        if (!isFirstTime) {
            elements.clear()
        }
        elements.add(Item(ItemSubjectElement(
            1
        ) { _: View?, _: Int ->
            UtilitiesDialog.showSubjectCreator(this@ProjectActivity, object : OnResponseListener {
                override fun onAccept(view: View?, title: TextView?) {
                    insertSubject(title?.text.toString(), newSubjectColor)
                    val intent = Intent()
                    intent.putExtra("requestCode", 2)
                    setResult(Activity.RESULT_OK, intent)
                }

                override fun onDismiss(view: View?) {
                }

                override fun onChangeColor(color: Int) {
                    newSubjectColor = color
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

    private fun notifyDelete(subject: String) {
        val dialog = PermissionDialog()
        dialog.setTitleDialog("${getString(R.string.delete)} $subject")
        dialog.setTextDialog("$subject ${getString(R.string.subject_delete)}")
        dialog.setDrawable(R.drawable.bookmark)
        dialog.setPositive { _: DialogInterface?, _: Int ->
            deleteSubject(subject)
            dialog.dismiss()
        }
        dialog.setNegative { _: DialogInterface?, _: Int -> dialog.dismiss() }
        dialog.show(supportFragmentManager, "A")
    }

    private fun deleteSubject(subject: String) {
        val dbr = dbHelper.readableDatabase
        val db = dbHelper.writableDatabase
        val c = dbr.rawQuery("SELECT id FROM " + DbHelper.T_TAG + " WHERE name = ?", arrayOf(subject))
        var idSubject = -1
        if (c.moveToFirst()) {
            idSubject = c.getInt(0)
        }
        if (idSubject >= 0) {
            db.delete(DbHelper.T_TASK, "subject = ?", arrayOf("$idSubject"))
            db.delete(DbHelper.t_event, "subject = ?", arrayOf("$idSubject"))
            db.delete(DbHelper.T_TAG, "name = ?", arrayOf(subject))
            notifyDataChanged()
        }
        c.close()
    }

    private fun deleteProject() {
        val dbr = dbHelper.readableDatabase
        val db = dbHelper.writableDatabase
        val cursor = dbr.rawQuery("SELECT * FROM ${DbHelper.T_TAG} WHERE proyect = ?", arrayOf("$projectId"))
        if(cursor.moveToFirst()){
            do {
                val id = cursor.getInt(0)
                db.delete(DbHelper.T_TASK, "subject = ?", arrayOf("$id"))
            }while (cursor.moveToNext())
            db.delete(DbHelper.T_TAG, "proyect = ?", arrayOf("$projectId"))
        }
        db.delete(DbHelper.T_PROJECTS, "id = ?", arrayOf("$projectId"))
        notifyDataChanged()
    }

    fun notifyDataChanged() {
        val task = AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {
                skeleton.showSkeleton()
            }

            override fun onExecute(b: Boolean) {
                initValues(projectId)
                setProgressSubject(projectId, b)
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyDataSetChanged()
                skeleton.showOriginal()
            }
        })

        task.exec(false)
    }

    fun insertSubject(name: String?, color: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues()
        values.put("name", name)
        values.put("color", color)
        values.put("proyect", projectId)
        db.insert(DbHelper.T_TAG, null, values)
        notifyDataChanged()
    }

    fun restartResultLauncher() {
        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data!!
                val requestCode = data.getIntExtra("requestCode", 0)
                if (requestCode == 2) {
                    val intent = Intent()
                    intent.putExtra("requestCode", 2)
                    setResult(Activity.RESULT_OK, intent)
                } else if (requestCode == 3) {
                    val i = data.getIntExtra("positionOfId", -1)
                    val intent = Intent()
                    intent.putExtra("positionOfId", i)
                    intent.putExtra("requestCode", 3)
                    setResult(Activity.RESULT_OK, intent)
                }
                notifyDataChanged()
            }
        }
    }
}
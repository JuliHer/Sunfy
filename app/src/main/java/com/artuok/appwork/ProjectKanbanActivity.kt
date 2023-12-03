package com.artuok.appwork

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.artuok.appwork.adapters.KanbanAdapter
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.dialogs.AnnouncementDialog
import com.artuok.appwork.fragmets.AverageAsync
import com.artuok.appwork.fragmets.AverageAsync.ListenerOnEvent
import com.artuok.appwork.kanban.KanbanFragment
import com.artuok.appwork.kanban.KanbanFragment.OnTaskModifyListener
import com.artuok.appwork.kanban.TaskFragment
import java.io.*
import java.text.ParseException
import java.util.*

class ProjectKanbanActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var pageTitle: TextView
    var mainTitle: String? = null
    var kanbanTitle: String? = null
    var fragmentList: MutableList<Fragment> = ArrayList()

    var taskFragment = TaskFragment()
    var pendingFragment = KanbanFragment()
    var inProcessFragment = KanbanFragment()
    var completedFragment = KanbanFragment()

    var dotsView: LinearLayout? = null
    var projectId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_kanban)
        projectId = intent.getIntExtra("project", -1)
        if(projectId < 0){
            finish()
        }
        initKanban(projectId)
        initViewPager()
        initTable()

        mainTitle = getString(R.string.task_to_do) + " ->"
        kanbanTitle = getString(R.string.kanban_table)

    }

    private fun initKanban(id : Int) {
        taskFragment.initProject(id)
        pendingFragment.initProject(id, 0)
        pendingFragment.setTitle(getString(R.string.pending_activities))
        inProcessFragment.initProject(id, 1)
        inProcessFragment.setTitle(getString(R.string.in_process_activities))
        completedFragment.initProject(id, 2)
        completedFragment.setTitle(getString(R.string.completed_tasks))
    }

    private fun initTable() {
        val viewPagerAdapter = KanbanAdapter(this, fragmentList)
        AverageAsync(object : ListenerOnEvent {
            override fun onPreExecute() {}
            override fun onExecute(b: Boolean) {
                val intent = Intent()

                taskFragment.setOnTaskModify(object : OnTaskModifyListener {
                    override fun onTaskModify(i: Int) {
                        notifyGlobalChanged(i)
                        inProcessFragment.restart(i)
                        pendingFragment.restart(i)
                        completedFragment.restart(i)
                        intent.putExtra("positionOfId", i)
                        intent.putExtra("requestCode", 3)
                        setResult(Activity.RESULT_OK, intent)
                    }
                })
                pendingFragment.setOnTaskModifyListener(object : OnTaskModifyListener {
                    override fun onTaskModify(i: Int) {
                        NotifyChanged()
                        notifyGlobalChanged(i)
                        completedFragment.restart(i)
                        inProcessFragment.restart(i)
                        taskFragment.reinitializate()
                        intent.putExtra("positionOfId", i)
                        intent.putExtra("requestCode", 3)
                        setResult(Activity.RESULT_OK, intent)
                    }
                })
                inProcessFragment.setOnTaskModifyListener(object : OnTaskModifyListener {
                    override fun onTaskModify(i: Int) {
                        NotifyChanged()
                        notifyGlobalChanged(i)
                        val dbHelper = DbHelper(this@ProjectKanbanActivity)
                        val db = dbHelper.readableDatabase
                        val pendingTasks =
                            db.rawQuery(
                                "SELECT * FROM " + DbHelper.T_TASK + " WHERE status < '2'",
                                null
                            )
                        if (pendingTasks.count < 1) {
                            showCongratulations()
                        }
                        pendingTasks.close()
                        pendingFragment.restart(i)
                        completedFragment.restart(i)
                        taskFragment.reinitializate()
                        intent.putExtra("positionOfId", i)
                        intent.putExtra("requestCode", 3)
                        setResult(Activity.RESULT_OK, intent)
                    }
                })
                completedFragment.setOnTaskModifyListener(object : OnTaskModifyListener {
                    override fun onTaskModify(i: Int) {
                        NotifyChanged()
                        notifyGlobalChanged(i)
                        inProcessFragment.restart(i)
                        pendingFragment.restart(i)
                        taskFragment.reinitializate()
                        intent.putExtra("positionOfId", i)
                        intent.putExtra("requestCode", 3)
                        setResult(Activity.RESULT_OK, intent)
                    }
                })

                fragmentList.clear()
                fragmentList.add(taskFragment)
                fragmentList.add(pendingFragment)
                fragmentList.add(inProcessFragment)
                fragmentList.add(completedFragment)
            }

            override fun onPostExecute(b: Boolean) {
                viewPager!!.adapter = viewPagerAdapter
                viewPagerAdapter.notifyItemRangeInserted(0, fragmentList.size)
                taskFragment.reinitializate()
                pendingFragment.reInit()
                inProcessFragment.reInit()
                completedFragment.reInit()
                initDots(viewPagerAdapter.itemCount)
            }
        }).exec(true)
    }

    private fun initDots(numPages: Int) {
        val images = arrayOfNulls<ImageView>(numPages)
        for (i in 0 until numPages) {
            images[i] = ImageView(this)
            if (i == 0) {
                images[i]!!.setImageResource(R.drawable.dot_selected)
            } else {
                images[i]!!.setImageResource(R.drawable.dot_unselected)
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            dotsView!!.addView(images[i], params)
        }
    }

    private fun onIndicatorChange(position: Int) {
        for (i in 0 until dotsView!!.childCount) {
            val image = dotsView!!.getChildAt(i) as ImageView
            if (i == position) {
                image.setImageResource(R.drawable.dot_selected)
            } else {
                image.setImageResource(R.drawable.dot_unselected)
            }
            image.requestLayout()
        }
    }

    private fun initViewPager() {
        pageTitle = findViewById(R.id.page_title)
        pageTitle.text = mainTitle
        dotsView = findViewById(R.id.dotsLayout)
        viewPager = findViewById(R.id.viewpager)

        viewPager.clipToPadding = false
        viewPager.clipChildren = false
        viewPager.offscreenPageLimit = 3
        viewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(40))
        viewPager.setPageTransformer(compositePageTransformer)
        viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == 0) {
                    pageTitle.text = mainTitle
                } else {
                    pageTitle.text = kanbanTitle
                }
                onIndicatorChange(position)
            }
        })
    }


    fun showCongratulations() {
        val mp = MediaPlayer.create(this, R.raw.completed)
        mp.start()
        val dialog = AnnouncementDialog()
        dialog.setTitle(getString(R.string.completed_tasks))
        dialog.setText(getString(R.string.congratulations_1))
        dialog.setDrawable(R.drawable.ic_check_circle)
        dialog.setAgree(false)
        dialog.setBackgroundCOlor(getColor(R.color.blue_400))
        dialog.setOnPositiveClickListener(getString(R.string.Accept_M)) { dialog.dismiss() }
        dialog.setOnNegativeClickListener(getString(R.string.dismiss)) { dialog.dismiss() }
        dialog.show(supportFragmentManager, "Congratulations to user")
    }

    fun NotifyChanged() {
//        pendingFragment.reinitializate();
//        inProcessFragment.reinitializate();
//        completedFragment.reinitializate();
//        taskFragment.reinitializate();
    }

    fun notifyGlobalChanged(id: Int) {

    }


    @Throws(ParseException::class)
    fun getPositionOfId(context: Context?, id: Int): Int {
        val dbHelper = DbHelper(context)
        val db = dbHelper.readableDatabase
        val i = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE id = '" + id + "'", null)
        var pos = -1
        if (i.moveToFirst()) {
            val date = i.getLong(5)
            val c = Calendar.getInstance()
            val today = c[Calendar.DAY_OF_WEEK] - 1
            c.timeInMillis = date
            for (j in 0..6) {
                if (c[Calendar.DAY_OF_WEEK] - 1 == (today + j) % 7) {
                    pos = j
                    break
                }
            }
        }
        i.close()
        return pos
    }
}
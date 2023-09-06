package com.artuok.appwork.kanban

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.MainActivity
import com.artuok.appwork.R
import com.artuok.appwork.ExpandActivity
import com.artuok.appwork.adapters.AwaitingAdapter
import com.artuok.appwork.adapters.AwaitingAdapter.OnClickListener
import com.artuok.appwork.adapters.AwaitingAdapter.OnMoveListener
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.fragmets.AverageAsync
import com.artuok.appwork.fragmets.TasksFragment
import com.artuok.appwork.fragmets.HomeFragment
import com.artuok.appwork.library.Constants
import com.artuok.appwork.library.FalseSkeleton
import com.artuok.appwork.objects.AnnouncesElement
import com.artuok.appwork.objects.AwaitElement
import com.artuok.appwork.objects.Item
import com.artuok.appwork.objects.TextElement
import com.artuok.appwork.widgets.TodayTaskWidget
import com.faltenreich.skeletonlayout.Skeleton
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import java.text.ParseException
import java.util.Calendar
import java.util.Random

class PendingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AwaitingAdapter
    private lateinit var manager: LinearLayoutManager
    private lateinit var skeleton: Skeleton
    private lateinit var taskModifyListener: OnTaskModifyListener
    private lateinit var moveListener : OnMoveListener
    private lateinit var listener : OnClickListener
    private var advirments = 0
    private val elements = ArrayList<Item>()

    fun setOnTaskModifyListener(taskModifyListener: OnTaskModifyListener) {
        this.taskModifyListener = taskModifyListener
    }

    fun containsId(id: Int): Boolean {
        return elements.any { it.type == 0 && (it.`object` as AwaitElement).id == id }
    }

    fun restart(id : Int) {
        if (::skeleton.isInitialized) {
            AverageAsync(object : AverageAsync.ListenerOnEvent {
                override fun onPreExecute() {}

                override fun onExecute(b: Boolean) {
                    taskModify(id)
                }

                override fun onPostExecute(b: Boolean) {

                }
            }).exec(true)
        }
    }

    fun reinitializate() {
        if (::skeleton.isInitialized) {
            skeleton.showSkeleton()
            elements.clear()
            AverageAsync(object : AverageAsync.ListenerOnEvent {
                override fun onPreExecute() {}

                override fun onExecute(b: Boolean) {
                    loadPendings()
                }

                override fun onPostExecute(b: Boolean) {
                    adapter.notifyDataSetChanged()
                    skeleton.showOriginal()
                }
            }).exec(true)
        }
    }

    private fun taskModify(id : Int){
        val i = getElementPositionById(id)

        if(i >= 0){
             removeElement(i)
        }
        val task = newTask(id);
        if(task != null){
            val pos = 1
            elements.add(pos, Item(task, 0))
            adapter.notifyItemInserted(pos)
        }
    }

    private fun removeElement(pos: Int){
        elements.removeAt(pos)
        adapter.notifyItemRemoved(pos)
    }

    private fun getElementPositionById(id: Int) : Int{
        for ((i, element) in elements.withIndex()){
            if(element.type == 0){
                val item = element.`object` as AwaitElement
                if(item.id == id){
                    return i
                }
            }
        }
        return -1;
    }

    private fun newTask(id : Int) : AwaitElement?{
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase

        val q = db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE id = ? AND status = '0'", arrayOf(id.toString()))
        if(q.moveToFirst()){
            val c = Calendar.getInstance()
            val today = c.timeInMillis
            val date = q.getLong(2)
            val dates = Constants.getDateString(requireContext(), date)
            val times = Constants.getTimeString(requireContext(), date)
            val done = q.getInt(5) == 1
            val liked = q.getInt(7) == 1
            val subjectName = q.getInt(3)
            val s = db.rawQuery(
                "SELECT * FROM " + DbHelper.t_subjects + " WHERE id = " + subjectName,
                null
            )
            var colors = 0
            var subject: String? = ""
            if (s.moveToFirst()) {
                colors = s.getInt(2)
                subject = s.getString(1)
            }
            s.close()
            val id = q.getInt(0)
            val title = q.getString(4)
            val status: String = daysLeft(today < date, date)
            val statusColor: Int = statusColor(today >= date, date)
            val eb = AwaitElement(id, title, status, dates, times, colors, statusColor, true)
            eb.isDone = done
            eb.subject = subject
            eb.isLiked = liked
            return eb
        }
        q.close()

        return null
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val requestCode = data?.getIntExtra("requestCode", 0)
            when (requestCode) {
                2 -> {
                    val taskId = data.getIntExtra("taskModify", 0)
                    taskModifyListener.onTaskModify(taskId)
                }
                3 -> {
                }
                else -> {
                    val shareCode = data?.getIntExtra("shareCode", 0)
                    if (shareCode == 2) {
                        (requireActivity() as MainActivity).notifyToChatChanged()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_pending, container, false)
        setupListener()
        setupRecyclerView(root)

        setupSkeleton()
        showFirstTimeSnackbar()
        return root
    }

    private fun setupRecyclerView(root: View) {
        adapter = AwaitingAdapter(requireActivity(), elements)
        adapter.setOnClickListener(listener)
        adapter.setOnMoveListener(moveListener)
        manager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        recyclerView = root.findViewById(R.id.recycler)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
    }

    public fun setupSkeleton() {

        skeleton = FalseSkeleton.applySkeleton(recyclerView, R.layout.skeleton_awaiting_layout, 12)
        val a: TypedArray = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val shimmerColor = a.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = a.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY)
        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor
        a.recycle()
        skeleton.showSkeleton()
        AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {}

            override fun onExecute(b: Boolean) {
                loadPendings()
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyDataSetChanged()
                skeleton.showOriginal()
            }
        }).exec(true)
    }

    private fun showFirstTimeSnackbar() {
        val s: SharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val firstTime = s.getBoolean("firstAwaitingOpens", true)
        if (elements.isNotEmpty() && firstTime) {
            (requireActivity() as MainActivity).showSnackbar("Swipe left to check")
            val se: SharedPreferences.Editor = s.edit()
            se.putBoolean("firstAwaitingOpens", false)
            se.apply()
        }
    }
    fun setupListener() {
        moveListener = object : OnMoveListener{
            override fun onMoveLeft(view: View?, position: Int) {
                if (elements[position].type == 0) {
                    val id = (elements[position].`object`as AwaitElement).id
                    removeTask(position)
                    (requireActivity() as MainActivity).updateWidget()
                    taskModifyListener.onTaskModify(id)
                }

            }

            override fun onMoveRight(view: View?, position: Int) {
                if (elements[position].type == 0) {
                    val id = (elements[position].`object`as AwaitElement).id
                    checkTask(position)
                    (requireActivity() as MainActivity).updateWidget()
                    taskModifyListener.onTaskModify(id)
                }

            }

        }
        listener = AwaitingAdapter.OnClickListener { view: View?, p: Int ->
            if (elements[p].type == 0) {
                val id = (elements[p].getObject() as AwaitElement).id
                val i = Intent(requireActivity(), ExpandActivity::class.java)
                i.getIntExtra("requestCode", 2)
                i.putExtra("id", id)
                resultLauncher.launch(i)
            }
        }
    }

    fun loadPendings() {
        if (!isAdded) return
        val dbHelper = DbHelper(requireActivity().applicationContext)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0' ORDER BY end_date ASC",
            null
        )

        val el = TextElement(requireActivity().getString(R.string.pending_activities))
        elements.add(Item(el, 2))
        if (cursor.moveToFirst()) {
            do {
                val c = Calendar.getInstance()
                val today = c.timeInMillis
                val date = cursor.getLong(2)
                val dates = Constants.getDateString(requireContext(), date)
                val times = Constants.getTimeString(requireContext(), date)
                val done = cursor.getInt(5) == 1
                val liked = cursor.getInt(7) == 1
                val subjectName = cursor.getInt(3)
                val s = db.rawQuery(
                    "SELECT * FROM " + DbHelper.t_subjects + " WHERE id = " + subjectName,
                    null
                )
                var colors = 0
                var subject: String? = ""
                if (s.moveToFirst()) {
                    colors = s.getInt(2)
                    subject = s.getString(1)
                }
                s.close()
                val id = cursor.getInt(0)
                val title = cursor.getString(4)
                val status: String = daysLeft(today < date, date)
                val statusColor: Int = statusColor(today >= date, date)
                val eb = AwaitElement(id, title, status, dates, times, colors, statusColor, true)
                eb.isDone = done
                eb.subject = subject
                eb.isLiked = liked
                elements.add(Item(eb, 0))
            } while (cursor.moveToNext())
        }

        cursor.close()
    }
    private fun statusColor(isClosed: Boolean, time: Long): Int {
        val ta = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val colorB = ta.getColor(
            R.styleable.AppCustomAttrs_subTextColor,
            requireActivity().getColor(R.color.yellow_700)
        )
        ta.recycle()
        val today = Calendar.getInstance()
        val tod = today.timeInMillis
        val rest = (time - tod) / 86400000
        return if (isClosed) {
            colorB
        } else {
            if (rest > 2) {
                requireActivity().getColor(R.color.green_500)
            } else {
                requireActivity().getColor(R.color.yellow_700)
            }
        }
    }

    private fun daysLeft(isOpen: Boolean, time: Long): String {
        var d = ""
        val c = Calendar.getInstance()
        c.timeInMillis = time
        val today = Calendar.getInstance()
        val tod = today.timeInMillis
        if (isOpen) {
            var rest = (time - tod) / 86400000
            if (tod < time) {
                val toin = today[Calendar.DAY_OF_YEAR]
                val awin = c[Calendar.DAY_OF_YEAR]
                rest = if (awin < toin) {
                    (awin + 364 - toin).toLong()
                } else {
                    (awin - toin).toLong()
                }
            }
            val dow = c[Calendar.DAY_OF_WEEK]
            if (rest == 1L) {
                d = requireActivity().getString(R.string.tomorrow)
            } else if (rest == 0L) {
                d = requireActivity().getString(R.string.today)
            } else if (rest < 7) {
                if (dow == 1) {
                    d = requireActivity().getString(R.string.sunday)
                } else if (dow == 2) {
                    d = requireActivity().getString(R.string.monday)
                } else if (dow == 3) {
                    d = requireActivity().getString(R.string.tuesday)
                } else if (dow == 4) {
                    d = requireActivity().getString(R.string.wednesday)
                } else if (dow == 5) {
                    d = requireActivity().getString(R.string.thursday)
                } else if (dow == 6) {
                    d = requireActivity().getString(R.string.friday)
                } else if (dow == 7) {
                    d = requireActivity().getString(R.string.saturday)
                }
            } else {
                d = rest.toString() + " " + requireActivity().getString(R.string.day_left)
            }
        }else{
            d = getString(R.string.overdue)
        }
        return d
    }

    private fun setAnnounce(pos: Int) {
        val finalPos = pos + advirments
        advirments++
        val adLoader = AdLoader.Builder(requireActivity(), "ca-app-pub-5838551368289900/1451662327")
            .forNativeAd { nativeAd: NativeAd ->
                val title = nativeAd.headline
                val body = nativeAd.body
                val advertiser = nativeAd.advertiser
                val price = nativeAd.price
                val images =
                    nativeAd.images
                val icon = nativeAd.icon
                advirments--
                val element =
                    AnnouncesElement(nativeAd, title, body, advertiser, images, icon)
                element.action = nativeAd.callToAction
                element.price = price
                val s = if(finalPos >= elements.size) elements.size else finalPos
                elements.add(s, Item(element, 12))
                adapter.notifyItemInserted(s)
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                }
            }).withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_SQUARE)
                    .setRequestMultipleImages(false)
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_BOTTOM_RIGHT)
                    .build()
            ).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun removeTask(position: Int) {
        if (elements[position].type == 0) {
            val id = (elements[position].getObject() as AwaitElement).id
            val dialog = PermissionDialog()
            dialog.setTitleDialog(requireActivity().getString(R.string.remove))
            dialog.setTextDialog(requireActivity().getString(R.string.remove_task))
            dialog.setDrawable(R.drawable.ic_trash)
            adapter.notifyItemChanged(position)
            dialog.setNegative { view: DialogInterface?, which: Int ->
                adapter.notifyItemChanged(position)
                dialog.dismiss()
            }
            dialog.setPositive { view: DialogInterface?, which: Int ->
                val dbHelper = DbHelper(requireActivity())
                val db = dbHelper.readableDatabase
                var i = 0
                try {
                    i = TasksFragment.getPositionOfId(requireActivity(), id)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                val cursor =
                    db.rawQuery(
                        "SELECT * FROM " + DbHelper.T_TASK + " WHERE id = '" + id + "'",
                        null
                    )
                if (cursor.moveToFirst()) {
                    val db2 = dbHelper.writableDatabase
                    db2.delete(DbHelper.T_TASK, " id = '$id'", null)
                    db2.delete(DbHelper.T_PHOTOS, " awaiting = '$id'", null)
                }
                elements.removeAt(position)
                adapter.notifyItemRemoved(position)
                cursor.close()
                if (i >= 0) {
                    (requireActivity() as MainActivity).notifyChanged(i)
                } else {
                    (requireActivity() as MainActivity).notifyAllChanged()
                }
                dialog.dismiss()
                updateWidget()
            }
            dialog.show(requireActivity().supportFragmentManager, "Remove")
        }
    }

    private fun checkTask(pos: Int){
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase
        adapter.notifyItemChanged(pos)
        if (elements[pos].type == 0) {
            val id = (elements[pos].getObject() as AwaitElement).id
            val cursor =
                db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE id = '" + id + "'", null)
            if (cursor.moveToFirst() && cursor.count == 1) {

                val values = ContentValues()
                (elements[pos].getObject() as AwaitElement).isDone = false
                values.put("status", 1)
                val db2 = dbHelper.writableDatabase
                db2.update(DbHelper.T_TASK, values, " id = '$id'", null)
                elements.removeAt(pos)
                adapter.notifyItemRemoved(pos)
                var i = 0
                try {
                    i = TasksFragment.getPositionOfId(requireActivity(), id)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                cursor.close()
                if (i >= 0) {
                    (requireActivity() as MainActivity).notifyChanged(i)
                } else {
                    (requireActivity() as MainActivity).notifyAllChanged()
                }

                updateWidget()
            }
            cursor.close()

        }
    }

    private fun updateWidget() {
        if (!isAdded) return
        val i = Intent(requireActivity(), TodayTaskWidget::class.java)
        i.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(requireActivity()).getAppWidgetIds(
            ComponentName(
                requireActivity(),
                TodayTaskWidget::class.java
            )
        )
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        requireActivity().sendBroadcast(i)
    }

    interface OnSwipeListener {
        fun onSwipe()
    }
    interface OnTaskModifyListener {
        fun onTaskModify(i : Int)
    }

}
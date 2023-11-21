package com.artuok.appwork.kanban

import android.content.ContentValues
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.R
import com.artuok.appwork.adapters.AwaitingAdapter
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.fragmets.AverageAsync
import com.artuok.appwork.library.Constants
import com.artuok.appwork.library.FalseSkeleton
import com.artuok.appwork.objects.AnnouncesElement
import com.artuok.appwork.objects.AwaitElement
import com.artuok.appwork.objects.Item
import com.faltenreich.skeletonlayout.Skeleton
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import java.util.Calendar

class TaskFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AwaitingAdapter
    private lateinit var manager: LinearLayoutManager
    private val elements : ArrayList<Item> = ArrayList()
    private var listener : KanbanFragment.OnTaskModifyListener? = null
    private lateinit var skeleton: Skeleton
    private var projectId = -1;

    public fun initProject(project: Int){
        this.projectId = project
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_task, container, false)
        initializateViews(root)
        initRecycler()
        initTasks()
        return root
    }

    public fun setOnTaskModify(listener: KanbanFragment.OnTaskModifyListener){
        this.listener = listener
    }

    private fun initTasks(){
        AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {}

            override fun onExecute(b: Boolean) {
                loadTasks()
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyDataSetChanged()
                skeleton.showOriginal()
            }
        }).exec(true)
    }

    private fun loadTasks(){
        if(!isAdded) {
            return
        }
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase

        val query = """
            SELECT t.*, e.name AS name, e.color AS color, p.name
            FROM ${DbHelper.T_TASK} AS t
            JOIN ${DbHelper.T_TAG} AS e ON t.subject = e.id
            JOIN ${DbHelper.T_PROJECTS} AS p ON e.proyect = p.id
            WHERE ${if (projectId >= 0) "p.id = ?" else 1} ORDER BY t.status ASC, t.deadline ASC;
        """.trimIndent()
        val arguments = if(projectId >= 0) arrayOf("$projectId") else null
        val q = db.rawQuery(query, arguments)

        val size = q.count

        setAd(if(size > 0) 1 else size)

        if(q.moveToFirst()){
            do{
                val c = Calendar.getInstance()
                val today = c.timeInMillis
                val date = q.getLong(5)
                val dates = Constants.getDateString(requireContext(), date)
                val times = Constants.getTimeString(requireContext(), date)
                val done = q.getInt(7) == 2
                val stat = q.getInt(7) == 1
                val liked = q.getInt(9) == 1
                val colors = q.getInt(11)
                val subject: String? = q.getString(10)


                val id = q.getInt(0)
                val title = q.getString(1)
                val status: String = if (done) requireActivity().getString(R.string.done_string) else daysLeft(today < date, date)
                val statusColor: Int = statusColor(today >= date && !done, date)
                val eb = AwaitElement(id, title, status, dates, times, colors, statusColor, stat)
                eb.projectName = q.getString(12)
                eb.isDone = done
                eb.subject = subject
                eb.isLiked = liked
                elements.add(Item(eb, 0))
            }while (q.moveToNext())
        }
        q.close()
    }

    public fun reinitializate(){
        if (::skeleton.isInitialized) {
            skeleton.showSkeleton()
            elements.clear()
            initTasks()
        }
    }



    private fun getElementById(id : Long) : AwaitElement?{
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase

        val q = db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE id = $id", null)

        if(q.moveToFirst()){
            val title = q.getString(1)
            val c = Calendar.getInstance()
            val today = c.timeInMillis
            val date = q.getLong(5)
            val dates = Constants.getDateString(requireActivity(), date)
            val times = Constants.getTimeString(requireActivity(), date)
            val subjectName = q.getInt(6)
            val s = db.rawQuery(
                "SELECT * FROM " + DbHelper.T_TAG + " WHERE id = " + subjectName,
                null
            )
            var colors = 0
            var subject: String? = ""
            if (s.moveToFirst()) {
                colors = s.getInt(2)
                subject = s.getString(1)
            }

            val stat = q.getInt(7) == 1
            val done = q.getInt(7) == 2
            val liked = q.getInt(9) == 1
            val statusColor: Int =  statusColor(if(!done) today >= date else true, date )
            val status: String = if(!done) daysLeft(today < date, date) else requireActivity().getString(R.string.done_string)
            val te = AwaitElement(id.toInt(), title, status, dates, times, colors, statusColor, stat)
            te.isDone = done
            te.subject = subject
            te.isLiked = liked
            q.close()
            s.close()
            return te
        }

        q.close()
        return null
    }

    private fun setAd(pos : Int){
        val finalPos = pos
        val adLoader = AdLoader.Builder(requireActivity(), Constants.ID_PUB_AD)
            .forNativeAd { nativeAd: NativeAd ->
                val title = nativeAd.headline
                val body = nativeAd.body
                val advertiser = nativeAd.advertiser
                val price = nativeAd.price
                val images = nativeAd.mediaContent
                val icon = nativeAd.icon
                val element =
                    AnnouncesElement(nativeAd, title, body, advertiser, images, icon)
                element.action = nativeAd.callToAction
                element.price = price
                val s = if(finalPos >= elements.size) elements.size else finalPos
                elements.add(s, Item(element, 12))
                adapter.notifyItemInserted(s)
            }.withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .setRequestMultipleImages(false)
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
                    .build()
            ).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun initRecycler(){
        adapter = AwaitingAdapter(requireActivity(), elements)
        adapter.setOnClickListener { view, position ->
            if(elements[position].type == 4) {
                var item = elements[position].`object` as AwaitElement
                item.isDone = !item.isDone
                setCheckTask(item.id.toLong(), item.isDone)
                item = getElementById(item.id.toLong())!!
                elements.removeAt(position)
                elements.add(position, Item(item, 4))
                adapter.notifyItemChanged(position)
                if(listener != null)
                    listener!!.onTaskModify(item.id)
            }
        }
        manager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        skeleton = FalseSkeleton.applySkeleton(recyclerView, R.layout.skeleton_awaiting_layout, 12);
        val a: TypedArray = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)

        val shimmerColor = a.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = a.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY)

        skeleton.maskColor = maskColor
        skeleton.shimmerColor = shimmerColor
        a.recycle()
        skeleton.showSkeleton()
    }

    private fun setCheckTask(id: Long, check: Boolean){
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.writableDatabase
        val values = ContentValues()

        val stat = if(check) 2 else 0
        values.put("status", stat)
        if(check)
            values.put("complete_date", Calendar.getInstance().timeInMillis)
        db.update(DbHelper.T_TASK, values, "id = ?", arrayOf("$id"))
    }

    private fun initializateViews(root : View){
        recyclerView = root.findViewById(R.id.recycler)
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
}
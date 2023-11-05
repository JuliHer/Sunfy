package com.artuok.appwork.kanban

import android.app.Activity
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.ExpandActivity
import com.artuok.appwork.MainActivity
import com.artuok.appwork.R
import com.artuok.appwork.adapters.AwaitingAdapter
import com.artuok.appwork.adapters.AwaitingAdapter.OnClickListener
import com.artuok.appwork.adapters.AwaitingAdapter.OnMoveListener
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.dialogs.CreateTaskDialog
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.fragmets.AverageAsync
import com.artuok.appwork.fragmets.TasksFragment
import com.artuok.appwork.library.Constants
import com.artuok.appwork.library.FalseSkeleton
import com.artuok.appwork.objects.AnnouncesElement
import com.artuok.appwork.objects.AwaitElement
import com.artuok.appwork.objects.Item
import com.artuok.appwork.objects.TextElement
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

class KanbanFragment(private val project: Int, private val status:Int) : Fragment() {

    //Recycler
    private val elements : ArrayList<Item> = ArrayList()
    private lateinit var adapter : AwaitingAdapter
    private lateinit var manager : LinearLayoutManager
    private lateinit var recycler : RecyclerView
    private var skeleton : Skeleton? = null
    private var advirments = 0
    private var title : String? = null

    //Listeners
    private lateinit var moveListener: OnMoveListener
    private lateinit var taskModifyListener : OnTaskModifyListener
    private lateinit var clickListener: OnClickListener
    private lateinit var resultLauncher : ActivityResultLauncher<Intent>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_kanban, container, false)
        initListeners()
        initRecycler(root)
        initElements()
        return root
    }
    //initializators
    private fun initListeners(){
        moveListener = object : OnMoveListener{
            override fun onMoveLeft(view: View?, position: Int) {
                if(elements[position].type == 0){
                    val id = (elements[position].`object`as AwaitElement).id
                    (requireActivity() as MainActivity).updateWidget()
                    if(status == 0){
                        removeTask(position)
                    }else{
                        moveTask(position, false)
                    }
                    modifyTask(id)
                }
            }

            override fun onMoveRight(view: View?, position: Int) {
                if(elements[position].type == 0){
                    val id = (elements[position].`object`as AwaitElement).id
                    (requireActivity() as MainActivity).updateWidget()
                    if(status == 2){
                        removeTask(position)
                    }else{
                        moveTask(position, true)
                    }
                    modifyTask(id)
                }
            }
        }

        clickListener = OnClickListener { view, p ->
            if (elements[p].type == 0) {
                val id = (elements[p].getObject() as AwaitElement).id
                val i = Intent(requireActivity(), ExpandActivity::class.java)
                i.getIntExtra("requestCode", 2)
                i.putExtra("id", id)
                resultLauncher.launch(i)
            }else if(elements[p].type == 5){
                val create = CreateTaskDialog(status)
                create.setOnCheckListener { _, id ->
                    create.dismiss()
                    taskModify(id.toInt())
                    modifyTask(id.toInt())
                }
                create.show(requireActivity().supportFragmentManager, "Create Task")
            }
        }

        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){
            if (it.resultCode == Activity.RESULT_OK) {
                val data = it.data
                if (data?.getIntExtra("requestCode", 0) == 2) {
                    val i = data.getIntExtra("taskModify", 0)
                    modifyTask(i)
                }
                if (data?.getIntExtra("shareCode", 0) == 2) {
                    (requireActivity() as MainActivity).notifyToChatChanged()
                }
            }
        }
    }
    private fun initRecycler(root: View){
        adapter = AwaitingAdapter(requireContext(), elements)
        adapter.setOnClickListener(clickListener)
        adapter.setOnMoveListener(moveListener)
        manager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        recycler = root.findViewById(R.id.recycler)

        recycler.setHasFixedSize(true)
        recycler.layoutManager = manager
        recycler.adapter = adapter
        skeleton = FalseSkeleton.applySkeleton(recycler, R.layout.skeleton_awaiting_layout, 12)
        val a: TypedArray = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs)

        val shimmerColor = a.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY)
        val maskColor = a.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY)

        skeleton?.maskColor = maskColor
        skeleton?.shimmerColor = shimmerColor
        a.recycle()
        skeleton?.showSkeleton()
    }
    private fun initElements(){
        AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {}

            override fun onExecute(b: Boolean) {
                loadData()
            }

            override fun onPostExecute(b: Boolean) {
                adapter.notifyDataSetChanged()
                skeleton?.showOriginal()
                if(elements.size <= 2){
                    recycler.background = requireActivity().getDrawable(R.drawable.background_empty_kanban)
                }else{
                    recycler.background = null
                }
            }
        }).exec(true)
    }

    //Private methods
    private fun loadData(){
        if (!isAdded) return
        val dbHelper = DbHelper(requireActivity().applicationContext)
        val db = dbHelper.readableDatabase
        val query = """
            SELECT t.*, e.name AS name, e.color AS color
            FROM ${DbHelper.T_TASK} AS t
            JOIN ${DbHelper.T_TAG} AS e ON t.subject = e.id
            JOIN ${DbHelper.T_PROJECTS} AS p ON e.proyect = p.id
            WHERE p.id = ? AND t.status = ? ORDER BY t.complete_date DESC;
        """.trimIndent()
        val cursor = db.rawQuery(
            query,
            arrayOf("$project", "$status")
        )

        if(title != null){

            val text = TextElement(title)
            elements.add(0, Item(text, 2))
        }
        if(cursor.moveToFirst()){
            val random = Random()
            do {
                val c = Calendar.getInstance()
                val today = c.timeInMillis
                val date = cursor.getLong(5)
                val dates = Constants.getDateString(requireContext(), date)
                val times = Constants.getTimeString(requireContext(), date)
                val done = cursor.getInt(7) == 2
                val liked = cursor.getInt(9) == 1
                val colors = cursor.getInt(11)
                val subject: String? = cursor.getString(10)

                val p = random.nextInt(6)
                if(p == 4){
                    setAnnounce(elements.size)
                }
                val id = cursor.getInt(0)
                val title = cursor.getString(1)
                val status: String = if (done) requireActivity().getString(R.string.done_string) else daysLeft(today < date, date)
                val statusColor: Int = statusColor(today >= date && !done, date)
                val eb = AwaitElement(id, title, status, dates, times, colors, statusColor, true)
                eb.isDone = done
                eb.subject = subject
                eb.isLiked = liked
                elements.add(Item(eb, 0))
            }while (cursor.moveToNext())
        }
        val add = TextElement("")
        elements.add(Item(add, 5))
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
        return -1
    }
    private fun newTask(id : Int) : AwaitElement?{
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase

        val query = """
            SELECT t.*, e.name AS name, e.color AS color
            FROM ${DbHelper.T_TASK} AS t
            JOIN ${DbHelper.T_TAG} AS e ON t.subject = e.id
            JOIN ${DbHelper.T_PROJECTS} AS p ON e.proyect = p.id
            WHERE t.id = ? AND p.id = ? AND t.status = ? ORDER BY t.complete_date DESC;
        """.trimIndent()
        val q = db.rawQuery(query, arrayOf(id.toString(), "$project", "$status"))
        if(q.moveToFirst()){
            val c = Calendar.getInstance()
            val today = c.timeInMillis
            val date = q.getLong(5)
            val dates = Constants.getDateString(requireContext(), date)
            val times = Constants.getTimeString(requireContext(), date)
            val done = q.getInt(7) == 2
            val liked = q.getInt(9) == 1
            val colors = q.getInt(11)
            val subject = q.getString(10)

            val title = q.getString(1)
            val status: String = getString(R.string.done_string)
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
    private fun removeTask(pos : Int){
        if(elements[pos].type == 0){
            val item = (elements[pos].getObject() as AwaitElement)
            val id = item.id
            val dialog = PermissionDialog()
            dialog.setTitleDialog(requireActivity().getString(R.string.remove))
            dialog.setTextDialog(requireActivity().getString(R.string.remove_task))
            dialog.setDrawable(R.drawable.ic_trash)
            dialog.setNegative { _: DialogInterface?, _: Int ->
                adapter.notifyItemChanged(pos)
                dialog.dismiss()
            }
            dialog.setPositive { view: DialogInterface?, which: Int ->
                val dbHelper = DbHelper(requireActivity())
                val db = dbHelper.readableDatabase
                val query = "SELECT * FROM ${DbHelper.T_TASK} WHERE id = ?"
                val cursor = db.rawQuery(query, arrayOf("$id"))
                if (cursor.moveToFirst()) {
                    val db2 = dbHelper.writableDatabase
                    db2.delete(DbHelper.T_TASK, " id = ?", arrayOf("$id"))
                    db2.delete(DbHelper.T_PHOTOS, " awaiting = ?", arrayOf("$id"))
                }
                elements.removeAt(pos)
                adapter.notifyItemRemoved(pos)
                cursor.close()
                var i = 0
                try {
                    i = TasksFragment.getPositionOfId(requireActivity(), id)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                if (i >= 0) {
                    (requireActivity() as MainActivity).notifyChanged(i)
                } else {
                    (requireActivity() as MainActivity).notifyAllChanged()
                }
                dialog.dismiss()
            }
            dialog.show(requireActivity().supportFragmentManager, "Remove")
        }
    }
    private fun moveTask(pos: Int, increment: Boolean){
        val nS = if(increment) status+1 else status-1
        if(elements[pos].type == 0){
            val item = (elements[pos].`object` as AwaitElement)
            val dbHelper = DbHelper(requireActivity())
            val dbr = dbHelper.readableDatabase
            val dbw = dbHelper.writableDatabase
            val id = item.id
            val query = dbr.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE id = ?;", arrayOf("$id"))
            if(query.moveToFirst()){
                val values = ContentValues()
                item.isDone = nS == 2
                values.put("status", nS)
                dbw.update(DbHelper.T_TASK, values, "id = ?", arrayOf("$id"))
                taskModify(id)
                var i = 0
                try {
                    i = TasksFragment.getPositionOfId(requireActivity(), id)
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
                if (i >= 0) {
                    (requireActivity() as MainActivity).notifyChanged(i)
                } else {
                    (requireActivity() as MainActivity).notifyAllChanged()
                }
            }

            query.close()
        }
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
                element.position = finalPos
                element.action = nativeAd.callToAction
                element.price = price
                val s = if(finalPos >= elements.size) elements.size else finalPos
                elements.add(s, Item(element, 12))
                adapter.notifyItemInserted(s)
            }.withAdListener(object : AdListener() {

            }).withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .setRequestMultipleImages(false)
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
                    .build()
            ).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    //Public methods
    fun setOnTaskModifyListener(taskModifyListener: OnTaskModifyListener){
        this.taskModifyListener = taskModifyListener
    }
    fun taskModify(id : Int){
        val i = getElementPositionById(id)

        if(i >= 0){
            removeElement(i)
        }
        val task = newTask(id)
        if(task != null){
            val pos = 1
            elements.add(pos, Item(task, 0))
            adapter.notifyItemInserted(pos)
        }
    }
    fun restart(id : Int){
        AverageAsync(object : AverageAsync.ListenerOnEvent {
            override fun onPreExecute() {}

            override fun onExecute(b: Boolean) {
                taskModify(id)
            }

            override fun onPostExecute(b: Boolean) {
                if(elements.size <= 2){
                    recycler.background = requireActivity().getDrawable(R.drawable.background_empty_kanban)
                }else{
                    recycler.background = null
                }
            }
        }).exec(true)
    }
    fun reInit() {
        if(skeleton != null){
            skeleton?.showSkeleton()
            elements.clear()
            AverageAsync(object : AverageAsync.ListenerOnEvent {
                override fun onPreExecute() {}

                override fun onExecute(b: Boolean) {
                    loadData()
                }

                override fun onPostExecute(b: Boolean) {
                    adapter.notifyDataSetChanged()
                    skeleton?.showOriginal()
                    if(elements.size <= 2){
                        recycler.background = requireActivity().getDrawable(R.drawable.background_empty_kanban)
                    }else{
                        recycler.background = null
                    }
                }
            }).exec(true)
        }
    }

    fun modifyTask(id: Int){
        if(elements.size <= 2){
            recycler.background = requireActivity().getDrawable(R.drawable.background_empty_kanban)
        }else{
            recycler.background = null
        }
        taskModifyListener.onTaskModify(id)
    }

    fun setTitle(title : String){
        this.title = title
    }


    interface OnTaskModifyListener {
        fun onTaskModify(id : Int)
    }
}
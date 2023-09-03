package com.artuok.appwork

import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.res.TypedArray
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.artuok.appwork.adapters.AwaitingAdapter
import com.artuok.appwork.adapters.ChatAdapter
import com.artuok.appwork.adapters.PublicationImageAdapter
import com.artuok.appwork.db.DbChat
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.dialogs.PermissionDialog
import com.artuok.appwork.fragmets.homeFragment
import com.artuok.appwork.library.MessageControler
import com.artuok.appwork.library.TextViewLetterAnimator
import com.artuok.appwork.objects.*
import com.thekhaeng.pushdownanim.PushDownAnim
import java.io.File
import java.util.*

class ExpandActivity : AppCompatActivity() {

    private lateinit var name: TextView
    private lateinit var subject: TextView
    private lateinit var day: TextView
    private lateinit var desc: TextView
    private lateinit var photo : ImageView
    private lateinit var dayOfWeek : TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PublicationImageAdapter
    private val elements : ArrayList<PublicationImageElement> = ArrayList()
    private lateinit var shareButton: LinearLayout
    private lateinit var likeButton: LinearLayout
    private lateinit var deleteButton: LinearLayout
    private lateinit var likeIndicator: ImageView
    private var id: Int = 0
    private lateinit var titleTask: String
    private lateinit var userUid: String
    private var liked = false;

    private var endDate: Long = 0
    private var chatIdselected = ""
    private val suggests: ArrayList<Item> = ArrayList()
    private lateinit var awaitRecyclerView: RecyclerView
    private lateinit var adapterAwait: AwaitingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expand)
        val backButton = findViewById<ImageView>(R.id.back_button)
        subject = findViewById(R.id.title_subject)
        dayOfWeek = findViewById(R.id.dayofweek)
        day = findViewById(R.id.days_left)
        desc = findViewById(R.id.description_task)
        likeButton = findViewById(R.id.like_publication)
        shareButton = findViewById(R.id.share_publication)
        deleteButton = findViewById(R.id.delete_publication)
        photo = findViewById(R.id.usericon)
        name = findViewById(R.id.name)
        likeIndicator = findViewById(R.id.image_like)

        awaitRecyclerView = findViewById(R.id.more_views)


        adapterAwait = AwaitingAdapter(this, suggests)
        adapterAwait.setOnClickListener(null)

        awaitRecyclerView.setHasFixedSize(true)
        awaitRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        awaitRecyclerView.isNestedScrollingEnabled = false
        awaitRecyclerView.adapter = adapterAwait

        val a = obtainStyledAttributes(R.styleable.AppCustomAttrs)

        PushDownAnim.setPushDownAnimTo(shareButton)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.95f)
            .setOnClickListener {
                showShareDialog()
            }

        PushDownAnim.setPushDownAnimTo(likeButton)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.95f)
            .setOnClickListener {
                liked = !liked
                setLike(liked)
                updateLike(liked)
            }
        PushDownAnim.setPushDownAnimTo(deleteButton)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.95f)
            .setOnClickListener {
                val dialog = PermissionDialog()
                dialog.setTitleDialog(getString(R.string.remove))
                dialog.setTextDialog(getString(R.string.remove_task))
                dialog.setDrawable(R.drawable.ic_trash)
                dialog.setPositiveText(getString(R.string.delete))
                dialog.setNegativeText(getString(R.string.Cancel_M))
                dialog.setPositive { _, _ ->
                    deletePublication(id)
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                dialog.setNegative { _, _ ->
                    dialog.dismiss()
                }
                dialog.show(supportFragmentManager, "Delete Task")
            }

        recyclerView = findViewById(R.id.recyclerImages)


        adapter = PublicationImageAdapter(this, elements
        ) { view, pos ->

        }

        val manager = GridLayoutManager(this, 2, RecyclerView.VERTICAL, false)

        recyclerView.layoutManager = manager
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false

        PushDownAnim.setPushDownAnimTo(backButton)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener {
                    finish()
                }
        id = intent.getIntExtra("id", 0)

        a.recycle()
        loadData(id)
        loadImages(id)
        preparePendingTasks()
        enableButton()
        loadPendingTasks()
    }

    private fun deletePublication(id: Int) {
        val dbHelper = DbHelper(this)
        val db = dbHelper.writableDatabase

        db.delete(DbHelper.T_TASK, "id = ?", arrayOf(id.toString()))
        db.delete(DbHelper.T_PHOTOS, "awaiting = ?", arrayOf(id.toString()))
    }

    private fun loadPendingTasks() {
        adapterAwait.notifyItemRangeInserted(0, suggests.size)
    }

    private fun preparePendingTasks() {
        val helper = DbHelper(this)
        val db = helper.readableDatabase
        val c = db.rawQuery(
            "SELECT * FROM ${DbHelper.T_TASK} WHERE status < '2' AND id != '$id' ORDER BY end_date DESC",
            null
        )

        if (c.moveToFirst()) {
            do {
                val cc = Calendar.getInstance()
                val date: Long = c.getLong(2)
                cc.timeInMillis = date
                val day = cc[Calendar.DAY_OF_MONTH]
                val month = cc[Calendar.MONTH]
                val year = cc[Calendar.YEAR]
                val hourFormat = DateFormat.is24HourFormat(this)
                var hour = cc[Calendar.HOUR_OF_DAY]
                if (!hourFormat) hour = if (cc[Calendar.HOUR] == 0) 12 else cc[Calendar.HOUR]

                val minute = cc[Calendar.MINUTE]

                val dd = if (day < 10) "0$day" else "" + day
                val dates = dd + " " + homeFragment.getMonthMinor(
                    this,
                    month
                ) + " " + year + " "
                val mn = if (minute < 10) "0$minute" else "" + minute
                var times = "$hour:$mn"

                if (!hourFormat) {
                    times += if (cc[Calendar.AM_PM] == Calendar.AM) " a. m." else " p. m."
                }

                val done = c.getInt(5) == 1
                val liked = c.getInt(7) == 1
                val subjectName: Int = c.getInt(3)

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

                val id: Int = c.getInt(0)
                val title: String = c.getString(4)
                val status: String = daysLeft(true, date)
                val statusColor: Int = statusColor()

                val eb = AwaitElement(id, title, status, dates, times, colors, statusColor, false)
                eb.isDone = done
                eb.subject = subject
                eb.isLiked = liked
                suggests.add(Item(eb, 3))
            } while (c.moveToNext())
        }

        c.close()
        loadPendingTasks()
    }

    private fun statusColor(): Int {
        val ta: TypedArray = obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val colorB = ta.getColor(
            R.styleable.AppCustomAttrs_subTextColor,
            getColor(R.color.yellow_700)
        )
        ta.recycle()
        return colorB

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
                d = getString(R.string.tomorrow)
            } else if (rest == 0L) {
                d = getString(R.string.today)
            } else if (rest < 7) {
                if (dow == 1) {
                    d = getString(R.string.sunday)
                } else if (dow == 2) {
                    d = getString(R.string.monday)
                } else if (dow == 3) {
                    d = getString(R.string.tuesday)
                } else if (dow == 4) {
                    d = getString(R.string.wednesday)
                } else if (dow == 5) {
                    d = getString(R.string.thursday)
                } else if (dow == 6) {
                    d = getString(R.string.friday)
                } else if (dow == 7) {
                    d = getString(R.string.saturday)
                }
            } else {
                d = rest.toString() + " " + getString(R.string.day_left)
            }
        }
        return d
    }

    private fun loadImages(id: Int) {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.T_PHOTOS} WHERE awaiting = '$id'", null)

        var i = 0;
        if(cursor.moveToFirst()){
            do{

                if(i < 4){
                    val path = cursor.getString(3)
                    val map = BitmapFactory.decodeFile(path)
                    elements.add(PublicationImageElement(map))
                }else{
                    elements[3].isLast = true
                }

                i++
            }while (cursor.moveToNext())
            adapter.notifyDataSetChanged()
        }

        if(elements.size > 0){

            var s =
                if (elements.size > 3) {
                    2
                } else {
                    elements.size
                }

            val manager = GridLayoutManager(this, s, RecyclerView.VERTICAL, false)
            desc.textSize = convertToSpToPx(11).toFloat()
            recyclerView.layoutManager = manager
        } else {
            recyclerView.visibility = View.GONE
        }
        cursor.close()
    }

    private fun enableButton(){
        val s = getSharedPreferences("chat", MODE_PRIVATE)
        val loggin = s.getBoolean("logged", false)

        if(userUid != "" && userUid != "noUser"){
            if(loggin){
                shareButton.visibility = View.VISIBLE
            }else{
                shareButton.visibility = View.GONE
            }
        }else{
            shareButton.visibility = View.GONE
        }
    }

    private fun getNameById(uid: String): String {
        return getString(R.string.you)
    }

    private fun loadData(id: Int) {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE id = '$id'", null)

        var title = ""
        var user = ""
        var days = ""
        titleTask = ""
        var color = 0
        var dow = ""
        if (cursor.moveToFirst()) {
            title = getSubjectNameById(cursor.getInt(3))
            color = getSubjectColorById(cursor.getInt(3))
            val t = cursor.getLong(2)
            endDate = t
            days = getDayLeft(t)
            titleTask = cursor.getString(4)

            liked = cursor.getInt(7) == 1
            setLike(cursor.getInt(7) == 1)
            user = cursor.getString(6)
            dow = getDOW(t)
        }


        photo.setColorFilter(color)
        dayOfWeek.text = dow
        userUid = user

        name.text = getNameById(user)
        subject.text = title
        day.text = days
        desc.text = titleTask
        cursor.close()
    }

    private fun setLike(like: Boolean) {
        val a = obtainStyledAttributes(R.styleable.AppCustomAttrs)
        val likedColor = a.getColor(R.styleable.AppCustomAttrs_iMainColor, Color.TRANSPARENT)
        val unlikedColor = a.getColor(R.styleable.AppCustomAttrs_subTextColor, Color.TRANSPARENT)

        a.recycle()
        if (like) {
            likeIndicator.setImageDrawable(getDrawable(R.drawable.heart_fill))
            likeIndicator.setColorFilter(likedColor)
        } else {
            likeIndicator.setImageDrawable(getDrawable(R.drawable.heart))
            likeIndicator.setColorFilter(unlikedColor)
        }
    }

    private fun updateLike(like: Boolean) {
        val dbhelper = DbHelper(this)
        val db = dbhelper.writableDatabase
        val values = ContentValues()
        values.put("favorite", like)

        db.update(DbHelper.T_TASK, values, "id = '$id'", null)
    }

    private fun getDOW(time: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = time

        val d = c[Calendar.DAY_OF_WEEK]

        val day = homeFragment.getDayOfWeek(this, d)

        return day
    }


    private fun getSubjectNameById(id: Int): String {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val c = db.rawQuery("SELECT * FROM ${DbHelper.t_subjects} WHERE id = '$id'", null)

        var name = ""
        if (c.moveToFirst()) {
            name = c.getString(1)
        }

        c.close()
        return name
    }

    private fun getSubjectColorById(id: Int): Int {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase
        val c = db.rawQuery("SELECT * FROM ${DbHelper.t_subjects} WHERE id = '$id'", null)

        var color = 0
        if (c.moveToFirst()) {
            color = c.getInt(2)
        }
        c.close()
        return color
    }

    private fun convertToSpToPx(sp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
    fun dow(tim : Long){
            val d : String
            val c = Calendar.getInstance()
            c.timeInMillis = tim
            val day = c.get(Calendar.DAY_OF_MONTH)
            val month = c.get(Calendar.MONTH)
            val year = c.get(Calendar.YEAR)
            val hour = if(c.get(Calendar.HOUR) == 0) 12 else c.get(Calendar.HOUR)
            val minute = c.get(Calendar.MINUTE)

            val dd = if (day < 10) "0$day" else "$day"
            var datetime = "$dd " + homeFragment.getMonthMinor(this, month) + " $year "
            val mn = if (minute < 10) "0$minute" else "" + minute
            datetime += "$hour:$mn"
            datetime += if (c.get(Calendar.AM_PM) == Calendar.AM) " a. m." else " p. m."
            d = datetime
        }

    private fun getDayLeft(tim: Long): String {
        val d : String
        val c = Calendar.getInstance()
        c.timeInMillis = tim

        val today = Calendar.getInstance()

        val tod = today.timeInMillis

        var rest: Long = (tim - tod) / 86400000

        if (tod < tim) {
            val toin = today[Calendar.DAY_OF_YEAR]
            val awin = c[Calendar.DAY_OF_YEAR]
            rest = if (awin < toin) {
                (awin + 364 - toin).toLong()
            } else {
                (awin - toin).toLong()
            }
        }

        val open = tod < tim

        if (open) {
            if (rest == 1L) {
                d = getString(R.string.tomorrow)
            } else if (rest == 0L) {
                d = getString(R.string.today)
            } else if (rest < 7) {
                d = rest.toString() + " " + getString(R.string.day_left)
            } else {
                val day = c.get(Calendar.DAY_OF_MONTH)
                val month = c.get(Calendar.MONTH)
                val year = c.get(Calendar.YEAR)
                val hour = if(c.get(Calendar.HOUR) == 0) 12 else c.get(Calendar.HOUR)
                val minute = c.get(Calendar.MINUTE)

                val dd = if (day < 10) "0$day" else "$day"
                var datetime = "$dd " + homeFragment.getMonthMinor(this, month) + " $year "
                val mn = if (minute < 10) "0$minute" else "" + minute
                datetime += "$hour:$mn"
                datetime += if(c.get(Calendar.AM_PM) == Calendar.AM) " a. m." else " p. m."
                d = datetime
            }
        }else {
            val day = c.get(Calendar.DAY_OF_MONTH)
            val month = c.get(Calendar.MONTH)
            val year = c.get(Calendar.YEAR)
            val hour = if(c.get(Calendar.HOUR) == 0) 12 else c.get(Calendar.HOUR)
            val minute = c.get(Calendar.MINUTE)

            val dd = if (day < 10) "0$day" else "$day"
            var datetime = "$dd " + homeFragment.getMonthMinor(this, month) + " $year "
            val mn = if (minute < 10) "0$minute" else "" + minute
            datetime += "$hour:$mn"
            datetime += if (c.get(Calendar.AM_PM) == Calendar.AM) " a. m." else " p. m."
            d = datetime
        }

        return d
    }

    private fun showShareDialog(){
        val subjectDialog = Dialog(this)
        subjectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        subjectDialog.setContentView(R.layout.dialog_share)
        subjectDialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        subjectDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        subjectDialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        subjectDialog.window!!.setGravity(Gravity.BOTTOM)
        val recyclerView = subjectDialog.findViewById<RecyclerView>(R.id.recyclerview)
        val username = subjectDialog.findViewById<TextView>(R.id.username)
        val sendBtn = subjectDialog.findViewById<LinearLayout>(R.id.send_btn)

        val manager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val elements: List<Item> = getUserToShare()
        val adapter = ChatAdapter(this, elements){ _, pos ->
            val element = elements.get(pos)
            if(element.type == 4){
                val chat = element.`object` as ChatElement
                chatIdselected = chat.chatId
                TextViewLetterAnimator.animateText(username, chat.name, 500)

                sendBtn.visibility = View.VISIBLE
            }
        }

        PushDownAnim.setPushDownAnimTo(sendBtn)
            .setOnClickListener {
                if(chatIdselected.isNotEmpty()){
                    sendMessage(chatIdselected, "")
                    subjectDialog.dismiss()
                }
            }

        subjectDialog.setOnDismissListener {
            chatIdselected = ""
        }

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        subjectDialog.show()
    }

    private fun sendMessage(chat : String, name: String){
        val task = getTaskById(id)
        val message = MessageControler.Message.Builder(getString(R.string.task))
            .setTask(task)
            .build()
        val msgController = MessageControler(this, chat, name, MessageControler.ChatType.CONTACT)
        msgController.send(message)
    }

    private fun getTaskById(id:Int) : EventMessageElement?{
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery("SELECT * FROM ${DbHelper.T_TASK} WHERE id = '$id'", null)


        if (cursor.moveToFirst()) {
            val title = cursor.getString(4)
            val deadline = cursor.getLong(2)
            val user = cursor.getString(6)
            cursor.close()
            return EventMessageElement(deadline, title, user)
        }

        cursor.close()
        return null
    }

    private fun getUserToShare():ArrayList<Item>{
        val dbChat = DbChat(this)
        val db = dbChat.readableDatabase
        val query = "SELECT * FROM ${DbChat.T_CHATS_MSG} WHERE timestamp IN " +
                "(SELECT MAX(timestamp) FROM ${DbChat.T_CHATS_MSG} GROUP BY chat) " +
                "ORDER BY timestamp DESC"
        val chatsQ = db.rawQuery(query, null)
        val chats = ArrayList<Item>()
        if(chatsQ.moveToFirst()){
            do {
                val id = chatsQ.getInt(7)
                val message = chatsQ.getString(1)
                val timestamp = chatsQ.getLong(3)
                val status = chatsQ.getInt(5)

                val msg = MessageControler.Message.Builder(message)

                    .setStatus(status)
                    .setTimestamp(timestamp)
                    .build()

                val chatElement = getChatById(db, id, msg)

                if(chatElement != null){
                    chats.add(Item(chatElement, 4))
                }

            }while (chatsQ.moveToNext())
        }

        chatsQ.close()
        return chats
    }

    private fun getChatById(db : SQLiteDatabase, chat : Int, msg : MessageControler.Message) : ChatElement?{
        val chats = db.rawQuery("SELECT * FROM ${DbChat.T_CHATS} WHERE id = '$chat'", null)
        if(chats.moveToFirst()){
            val name = chats.getString(1)
            val chatId = chats.getString(4)
            val publicKey = chats.getString(6)
            val pictureName = chats.getString(5)
            val picture = getPicture(pictureName)

            chats.close()
            return ChatElement(name, msg.message, chatId, publicKey, pictureName, picture, msg.status, msg.timestamp)
        }
        chats.close()
        return null
    }

    private fun getPicture(photo: String) : Bitmap?{
        val root: String = externalCacheDir.toString()
        val fName = "$photo.jpg"
        val file = File(root, fName)

        if (file.exists()) {
            return BitmapFactory.decodeFile(file.path)
        }
        return null
    }
}



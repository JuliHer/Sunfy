package com.artuok.appwork

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.fragmets.homeFragment
import com.thekhaeng.pushdownanim.PushDownAnim
import java.util.*

class ViewActivity : AppCompatActivity() {

    private lateinit var subject: TextView
    private lateinit var day: TextView
    private lateinit var desc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)
        val backButton = findViewById<ImageView>(R.id.back_button)
        subject = findViewById(R.id.title_subject)
        day = findViewById(R.id.days_left)
        desc = findViewById(R.id.description_task)

        PushDownAnim.setPushDownAnimTo(backButton)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener {
                    finish()
                }
        val id = intent.getIntExtra("id", 0)
        loadData(id)
    }

    private fun loadData(id: Int) {
        val dbHelper = DbHelper(this)
        val db = dbHelper.readableDatabase

        val cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE id = '" + id + "'", null)

        var title = ""
        var days = ""
        var des = ""
        if (cursor.moveToFirst()) {
            title = cursor.getString(4)
            val t = cursor.getString(3).split(" ")

            val date = t[0].split("-")
            val year = date[0].toInt()
            val month = date[1].toInt()
            val day = date[2].toInt()

            val time = t[1].split(":")
            var hour = time[0].toInt()
            val minute = time[1].toInt()
            val c = Calendar.getInstance()

            c.set(year, (month - 1), day, hour, minute)
            val dd = if (day < 10) "0$day" else "$day"
            var datetime = "$dd " + homeFragment.getMonthMinor(this, month - 1) + " $year "

            val mn = if (minute < 10) "0$minute" else "" + minute
            if (hour > 12) {
                hour -= 12
                datetime += "$hour:$mn PM"
            } else {
                datetime += "$hour:$mn"
                datetime += if (hour == 12) " PM" else " AM"
            }

            days = datetime

            des = cursor.getString(5)
        }

        subject.text = title
        day.text = days
        desc.text = des

        cursor.close()
    }
}



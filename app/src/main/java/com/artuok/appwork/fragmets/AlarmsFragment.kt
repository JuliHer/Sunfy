package com.artuok.appwork.fragmets

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import com.artuok.appwork.R
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.services.AlarmWorkManager
import com.thekhaeng.pushdownanim.PushDownAnim
import java.util.*

class AlarmsFragment : Fragment() {

    private lateinit var switch: Switch

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_alarms, container, false)

        switch = root.findViewById(R.id.turnalarm)

        setAlarm()
        setAlarms()


        switch.setOnCheckedChangeListener() { compoundButton, b ->
            val db = DbHelper(requireActivity())
            val dbw = db.writableDatabase
            val values = ContentValues()

            values.put("alarm", if (b) 1 else 0)
            dbw.update(DbHelper.t_alarm, values, "title = 'TTDH'", null)
            setAlarms()
        }
        val alarm: LinearLayout = root.findViewById(R.id.ttdh)

        PushDownAnim.setPushDownAnimTo(alarm)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener() {
                val timepicker = TimePickerDialog(
                    requireActivity(),
                    { timePicker1: TimePicker?, i: Int, i1: Int ->
                        val db = DbHelper(requireActivity())
                        val dbw = db.writableDatabase
                        val hour = (i * 60 * 60) + (i1 * 60)
                        val values = ContentValues()
                        values.put("hour", hour)
                        values.put("last_alarm", 9)
                        dbw.update(DbHelper.t_alarm, values, "title = 'TTDH'", null)
                        setAlarms()
                    },
                    0,
                    0,
                    false
                )

                timepicker.show()
            }

        return root
    }

    fun setAlarm() {
        val dbHelper = DbHelper(requireActivity())
        val dbr = dbHelper.readableDatabase
        val dbw = dbHelper.writableDatabase
        val row = dbr.rawQuery(
            "SELECT * FROM ${DbHelper.t_alarm} WHERE title = 'TTDH' AND (alarm = '0' OR alarm = '1')",
            null
        )

        if (row.count < 1) {
            val values = ContentValues()
            values.put("title", "TTDH")
            values.put("hour", 39600L)
            values.put("last_alarm", 1)
            values.put("alarm", 0)
            values.put("sunday", 1)
            values.put("monday", 1)
            values.put("tuesday", 1)
            values.put("wednesday", 1)
            values.put("thursday", 1)
            values.put("friday", 1)
            values.put("saturday", 1)

            dbw.insert(DbHelper.t_alarm, null, values)
        } else {
            if (row.moveToFirst()) {
                val turn = row.getInt(4) > 0
                switch.isChecked = turn
            }
        }


    }

    fun setAlarms() {
        var dow = 0
        var id = -1
        while (dow < 7) {
            val b = getAlarm(dow)

            if (b >= 0) {
                id = b
                break
            }
            dow++
        }

        if (id >= 0) {
            val dbHelper = DbHelper(requireActivity())
            val db = dbHelper.readableDatabase
            val row = db.rawQuery("SELECT * FROM ${DbHelper.t_alarm} WHERE id = '$id'", null)

            if (row.moveToFirst() && row.count == 1) {
                val rest = row.getLong(2) * 1000
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                val thour: Long = (60L * 60L * hour) + (60L * minute)

                val tdow = calendar.get(Calendar.DAY_OF_WEEK) - 1
                dow += tdow
                val r = if (tdow > dow) 7 - (tdow + 1) + (dow + 1) else (dow + 1) - (tdow + 1)
                Log.d("cattoLog", "$dow - $tdow")
                val time = (r * 86400000L) + rest - (thour * 1000)
                Log.d("cattoLog", "$r x 86400000 + $rest - $thour")
                setNotify(row.getInt(4), time)

            }
        }
    }

    private fun setNotify(type: Int, diff: Long) {
        val start = Calendar.getInstance().timeInMillis + diff

        Log.d(
            "cattoRest",
            "${diff / 1000 / 60 / 60 / 24}d ${diff / 1000 / 60 / 60 % 24}h ${diff / 1000 / 60 % 60}m" +
                    " ${diff / 1000 % 60}s"
        )
        Log.d("cattoRest", "$diff")
        val notify = Intent(requireActivity(), AlarmWorkManager::class.java)

        if (type == 0) {
            notify.action = AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK
        } else if (type == 1) {
            notify.action = AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK
            notify.putExtra("alarm", 1)
        }

        val pendingNotify = PendingIntent.getBroadcast(
            requireActivity(),
            0, notify,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val manager: AlarmManager =
            requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingNotify)
        manager.setExact(AlarmManager.RTC_WAKEUP, start, pendingNotify)
    }

    private fun getAlarm(i: Int): Int {
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase
        val calendar = Calendar.getInstance()
        var query = ""
        val dow = (((calendar.get(Calendar.DAY_OF_WEEK) - 1) + i) % 7) + 1

        if (dow == 1) {
            query = "AND sunday = '1'"
        } else if (dow == 2) {
            query = "AND monday = '1'"
        } else if (dow == 3) {
            query = "AND tuesday = '1'"
        } else if (dow == 4) {
            query = "AND wednesday = '1'"
        } else if (dow == 5) {
            query = "AND thursday = '1'"
        } else if (dow == 6) {
            query = "AND friday = '1'"
        } else if (dow == 7) {
            query = "AND saturday = '1'"
        }

        val row = db.rawQuery(
            "SELECT * FROM ${DbHelper.t_alarm} WHERE last_alarm != '$dow' $query ORDER BY hour ASC",
            null
        )

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val time: Long = (60L * 60L * hour) + (60L * minute)


        var id: Int = -1
        if (row.moveToFirst()) {
            do {
                if (calendar.get(Calendar.DAY_OF_WEEK) == dow) {
                    if (row.getLong(2) >= time) {
                        id = row.getInt(0)
                        break
                    }
                } else {
                    id = row.getInt(0)
                    break
                }
            } while (row.moveToNext())
        }

        row.close()
        return id
    }
}
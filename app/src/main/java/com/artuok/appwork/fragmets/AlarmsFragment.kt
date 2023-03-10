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
import android.widget.TextView
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import com.artuok.appwork.R
import com.artuok.appwork.db.DbHelper
import com.artuok.appwork.services.AlarmWorkManager
import com.thekhaeng.pushdownanim.PushDownAnim
import java.util.*
import kotlin.math.floor

class AlarmsFragment : Fragment() {

    private lateinit var switch: Switch
    private lateinit var timer: TextView
    private lateinit var nDETimer: TextView
    private lateinit var nDSTimer: TextView

    private val TimeToDoHomework : String = "TTDH"
    private val TomorrowEvent = "TE"
    private val TomorrowSubjects = "TS"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_alarms, container, false)

        timer = root.findViewById(R.id.time)
        nDETimer = root.findViewById(R.id.nextDayEvents)
        nDSTimer = root.findViewById(R.id.nextDaySubjects)

        prepareAlarm()
        setAlarm()
        activateAlarms()

        val alarm: LinearLayout = root.findViewById(R.id.ttdh)
        val NDE: LinearLayout = root.findViewById(R.id.NDE)
        val NDS: LinearLayout = root.findViewById(R.id.NDS)

        PushDownAnim.setPushDownAnimTo(NDS)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                val timepicker = TimePickerDialog(
                    requireActivity(),
                    { _: TimePicker?, i: Int, i1: Int ->
                        val db = DbHelper(requireActivity())
                        val dbw = db.writableDatabase
                        val hour = (i * 60 * 60) + (i1 * 60)
                        val values = ContentValues()

                        val min = if(i1 < 10){
                            "0$i1"
                        }else{
                            "$i1"
                        }

                        val time = if(i > 12){
                            "${i-12}:$min p. m."
                        }else if(i == 12){
                            "$i:$min p. m."
                        }else if(i == 0){
                            "12:$min a. m."
                        }else{
                            "$i:$min a. m."
                        }

                        nDSTimer.text = time

                        values.put("hour", hour)
                        values.put("last_alarm", 9)
                        dbw.update(DbHelper.t_alarm, values, "title = '$TomorrowSubjects'", null)
                        setAlarm()
                        activateAlarms()
                    },
                    0,
                    0,
                    false
                )

                timepicker.show()
            }

        PushDownAnim.setPushDownAnimTo(NDE)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                val timepicker = TimePickerDialog(
                    requireActivity(),
                    { _: TimePicker?, i: Int, i1: Int ->
                        val db = DbHelper(requireActivity())
                        val dbw = db.writableDatabase
                        val hour = (i * 60 * 60) + (i1 * 60)

                        val min = if(i1 < 10){
                            "0$i1"
                        }else{
                            "$i1"
                        }

                        val time = if(i > 12){
                            "${i-12}:$min p. m."
                        }else if(i == 12){
                            "$i:$min p. m."
                        }else if(i == 0){
                            "12:$min a. m."
                        }else{
                            "$i:$min a. m."
                        }

                        nDETimer.text = time

                        val values = ContentValues()
                        values.put("hour", hour)
                        values.put("last_alarm", 9)
                        dbw.update(DbHelper.t_alarm, values, "title = '$TomorrowEvent'", null)
                        setAlarm()
                        activateAlarms()
                    },
                    0,
                    0,
                    false
                )

                timepicker.show()
            }

        PushDownAnim.setPushDownAnimTo(alarm)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                val timepicker = TimePickerDialog(
                    requireActivity(),
                    { _: TimePicker?, i: Int, i1: Int ->
                        val db = DbHelper(requireActivity())
                        val dbw = db.writableDatabase
                        val hour = (i * 60 * 60) + (i1 * 60)
                        val values = ContentValues()

                        val min = if(i1 < 10){
                            "0$i1"
                        }else{
                            "$i1"
                        }

                        val time = if(i > 12){
                            "${i-12}:$min p. m."
                        }else if(i == 12){
                            "$i:$min p. m."
                        }else if(i == 0){
                            "12:$min a. m."
                        }else{
                            "$i:$min a. m."
                        }

                        timer.text = time

                        values.put("hour", hour)
                        values.put("last_alarm", 9)
                        dbw.update(DbHelper.t_alarm, values, "title = '$TimeToDoHomework'", null)
                        setAlarm()
                        activateAlarms()
                    },
                    0,
                    0,
                    false
                )

                timepicker.show()
            }

        return root
    }

    private fun prepareAlarm() {
        val dbHelper = DbHelper(requireActivity())
        val dbr = dbHelper.readableDatabase
        val row = dbr.rawQuery(
            "SELECT * FROM ${DbHelper.t_alarm}",
            null
        )


        if(row.moveToFirst()){
            do {
                val alarm = row.getString(1)
                val hour = row.getLong(2)
                val i = floor(hour / 60f / 60f).toInt()
                val i1 = floor((hour / 60f) % 60).toInt()

                val min = if(i1 < 10){
                    "0$i1"
                }else{
                    "$i1"
                }

                val time = if(i > 12){
                    "${i-12}:$min p. m."
                }else if(i == 12){
                    "$i:$min p. m."
                }else if(i == 0){
                    "12:$min a. m."
                }else{
                    "$i:$min a. m."
                }
                if(alarm == TomorrowEvent) {
                    nDETimer.text = time
                }else if(alarm == TomorrowSubjects) {
                    nDSTimer.text = time
                }else if(alarm == TimeToDoHomework){
                    timer.text = time
                }
            }while (row.moveToNext())
        }

        row.close()
    }

    private fun setAlarm() {
        val dbHelper = DbHelper(requireActivity())
        val dbr = dbHelper.readableDatabase
        val dbw = dbHelper.writableDatabase
        val row = dbr.rawQuery(
            "SELECT * FROM ${DbHelper.t_alarm}",
            null
        )

        val alarms = ArrayList<String>()

        if(row.moveToFirst()){
            do {
                val alarm = row.getString(1)
                alarms.add(alarm)
            }while (row.moveToNext())
        }

        row.close()

        if(!alarms.contains(TimeToDoHomework)){
            val cv = ContentValues()
            cv.put("title", TimeToDoHomework)
            cv.put("hour", 39600)
            cv.put("last_alarm", 9)
            cv.put("alarm", 0)

            dbw.insert(DbHelper.t_alarm, null, cv)
        }

        if(!alarms.contains(TomorrowSubjects)){
            val cv = ContentValues()
            cv.put("title", TomorrowSubjects)
            cv.put("hour", 57600)
            cv.put("last_alarm", 9)
            cv.put("alarm", 0)

            dbw.insert(DbHelper.t_alarm, null, cv)
        }

        if(!alarms.contains(TomorrowEvent)){
            val cv = ContentValues()
            cv.put("title", TomorrowEvent)
            cv.put("hour", 64800)
            cv.put("last_alarm", 9)
            cv.put("alarm", 0)

            dbw.insert(DbHelper.t_alarm, null, cv)
        }

        row.close()
    }



    private fun nextAlarm() : Int{
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase
        val row = db.rawQuery("SELECT * FROM ${DbHelper.t_alarm} ORDER BY hour ASC", null)

        val today = Calendar.getInstance()

        val hour = (today.get(Calendar.HOUR_OF_DAY) * 60 * 60) + (today.get(Calendar.MINUTE) * 60) + (today.get(Calendar.SECOND))

        var id = -1
        var idt = 0

        if(row.moveToFirst()){
            idt = row.getInt(0)
            do {
                val l = row.getLong(2)

                if(l > hour){
                    id = row.getInt(0)
                    Log.d("CattoNextAlarm", row.getString(1))
                    return id
                }
            }while (row.moveToNext())
        }

        row.close()
        return idt
    }

    private fun activateAlarms(){
        val id = nextAlarm()
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase
        val row = db.rawQuery("SELECT * FROM ${DbHelper.t_alarm} WHERE id = '$id'", null)

        if(row.moveToFirst()){
            val hour = row.getLong(2) * 1000
            val calendar = Calendar.getInstance()
            val hourd = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)
            val thour: Long = (((60L * 60L * hourd) + (60L * minute) + second)*1000) + calendar.get(Calendar.MILLISECOND)

            val time = if(hour <= thour) {
                hour + 86400000L - thour
            } else{
                hour - thour
            }

            setTimeOut(row.getString(1), time)
        }

        row.close()
    }


    private fun setTimeOut(type : String, diff: Long){
        val start = Calendar.getInstance().timeInMillis + diff

        val notify = Intent(requireActivity(), AlarmWorkManager::class.java)

        notify.action = when (type) {
                TimeToDoHomework -> {
                    AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK
                }
                TomorrowEvent -> {
                    AlarmWorkManager.ACTION_TOMORROW_EVENTS
                }
                TomorrowSubjects -> {
                    AlarmWorkManager.ACTION_TOMORROW_SUBJECTS
                }
                else -> {
                    AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK
                }
            }

        val pendingNotify = PendingIntent.getBroadcast(requireActivity(), 0, notify, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val manager: AlarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingNotify)
        manager.setExact(AlarmManager.RTC_WAKEUP, start, pendingNotify)
    }
}
package com.artuok.appwork.fragmets

import android.Manifest
import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.artuok.appwork.InActivity
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

    private val TimeToDoHomework: String = "TTDH"
    private val TomorrowEvent = "TE"
    private val TomorrowSubjects = "TS"

    private var requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean? ->
        if (isGranted == true) {
            setAlarmPendingTask(true)
        }
    }

    fun registrate() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean? ->
            if (isGranted == true) {
                setAlarmPendingTask(true)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_alarms, container, false)

        timer = root.findViewById(R.id.time)
        nDETimer = root.findViewById(R.id.nextDayEvents)
        nDSTimer = root.findViewById(R.id.nextDaySubjects)
        switch = root.findViewById(R.id.ATP)
        registrate()
        prepareAlarm()
        setAlarm()
        activateAlarms()
        getAlarmStatus()

        val alarm: LinearLayout = root.findViewById(R.id.ttdh)
        val NDE: LinearLayout = root.findViewById(R.id.NDE)
        val NDS: LinearLayout = root.findViewById(R.id.NDS)
        val TES: LinearLayout = root.findViewById(R.id.te_sound)
        val TSS: LinearLayout = root.findViewById(R.id.ts_sound)
        val TTDHS: LinearLayout = root.findViewById(R.id.ttdh_sound)


        PushDownAnim.setPushDownAnimTo(TES)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                getTESound()
            }

        PushDownAnim.setPushDownAnimTo(TSS)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                getTSSound()
            }

        PushDownAnim.setPushDownAnimTo(TTDHS)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                getTTDHSound()
            }

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

                        val min = if (i1 < 10) {
                            "0$i1"
                        } else {
                            "$i1"
                        }

                        val hourFormat = DateFormat.is24HourFormat(requireActivity())
                        val time = if (!hourFormat) {
                            if (i > 12) {
                                "${i - 12}:$min p. m."
                            } else if (i == 12) {
                                "$i:$min p. m."
                            } else if (i == 0) {
                                "12:$min a. m."
                            } else {
                                "$i:$min a. m."
                            }
                        } else {
                            "$i:$min"
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

                        val min = if (i1 < 10) {
                            "0$i1"
                        } else {
                            "$i1"
                        }

                        val hourFormat = DateFormat.is24HourFormat(requireActivity())
                        val time = if (!hourFormat) {
                            if (i > 12) {
                                "${i - 12}:$min p. m."
                            } else if (i == 12) {
                                "$i:$min p. m."
                            } else if (i == 0) {
                                "12:$min a. m."
                            } else {
                                "$i:$min a. m."
                            }
                        } else {
                            "$i:$min"
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

                        val min = if (i1 < 10) {
                            "0$i1"
                        } else {
                            "$i1"
                        }
                        val hourFormat = DateFormat.is24HourFormat(requireActivity())
                        val time = if (!hourFormat) {
                            if (i > 12) {
                                "${i - 12}:$min p. m."
                            } else if (i == 12) {
                                "$i:$min p. m."
                            } else if (i == 0) {
                                "12:$min a. m."
                            } else {
                                "$i:$min a. m."
                            }
                        } else {
                            "$i:$min"
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

        switch.setOnCheckedChangeListener { compoundButton, b ->
            setAlarmPendingTask(b)
        }

        return root
    }


    private fun getAlarmStatus() {
        val s = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val b = s.getBoolean("alarm", false)
        switch.isChecked = b
    }

    private fun setAlarmPendingTask(b: Boolean) {
        val s = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val se = s.edit()
        se.putBoolean("alarm", b)
        se.apply()
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

                val min = if (i1 < 10) {
                    "0$i1"
                } else {
                    "$i1"
                }

                val hourFormat = DateFormat.is24HourFormat(requireActivity())
                val time = if (!hourFormat) {
                    if (i > 12) {
                        "${i - 12}:$min p. m."
                    } else if (i == 12) {
                        "$i:$min p. m."
                    } else if (i == 0) {
                        "12:$min a. m."
                    } else {
                        "$i:$min a. m."
                    }
                } else {
                    "$i:$min"
                }

                if (alarm == TomorrowEvent) {
                    nDETimer.text = time
                } else if (alarm == TomorrowSubjects) {
                    nDSTimer.text = time
                } else if (alarm == TimeToDoHomework) {
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

    private fun getTESound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.EMPTY)
        resultLauncherTE.launch(intent)
    }

    private fun getTSSound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.EMPTY)
        resultLauncherTS.launch(intent)
    }

    private fun getTTDHSound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Tone")
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.EMPTY)
        resultLauncherTTDH.launch(intent)
    }

    private val resultLauncherTE =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri =
                    it.data!!.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

                val notificationChannel = NotificationChannel(
                    InActivity.CHANNEL_ID_4,
                    "Tomorrow Events",
                    NotificationManager.IMPORTANCE_HIGH
                )

                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                notificationChannel.setSound(uri, attr)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                notificationChannel.description = "Events to do tomorrow"

                val manager: NotificationManager =
                    requireActivity().getSystemService<NotificationManager>(
                        NotificationManager::class.java
                    )
                manager.createNotificationChannel(notificationChannel)
            }
        }

    private fun saveUriToSharedPreferences(name: String, uri: String) {
        val s = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val se = s.edit()
        se.putString(name, uri)
        se.apply()
    }

    private val resultLauncherTS =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri =
                    it.data!!.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                val notificationChannel = NotificationChannel(
                    InActivity.CHANNEL_ID_5,
                    "Tomorrow SUBJECTS",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.description = "Subjects tomorrow"

                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                notificationChannel.setSound(uri, attr)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

                val manager: NotificationManager =
                    requireActivity().getSystemService<NotificationManager>(
                        NotificationManager::class.java
                    )
                manager.createNotificationChannel(notificationChannel)
            }
        }

    private val resultLauncherTTDH =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri =
                    it.data!!.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

                val notificationChannel = NotificationChannel(
                    InActivity.CHANNEL_ID_2,
                    "Homework",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.description =
                    "Channel for remember when you need to do homework"

                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                notificationChannel.setSound(uri, attr)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

                val manager: NotificationManager =
                    requireActivity().getSystemService<NotificationManager>(
                        NotificationManager::class.java
                    )
                manager.createNotificationChannel(notificationChannel)
            }
        }

    private fun nextAlarm(): Int {
        val dbHelper = DbHelper(requireActivity())
        val db = dbHelper.readableDatabase
        val row = db.rawQuery("SELECT * FROM ${DbHelper.t_alarm} ORDER BY hour ASC", null)

        val today = Calendar.getInstance()

        val hour =
            (today.get(Calendar.HOUR_OF_DAY) * 60 * 60) + (today.get(Calendar.MINUTE) * 60) + (today.get(
                Calendar.SECOND
            ))

        var id = -1
        var idt = 0

        if(row.moveToFirst()){
            idt = row.getInt(0)
            do {
                val l = row.getLong(2)

                if(l > hour){
                    id = row.getInt(0)
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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            if (requireActivity().checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        val pendingNotify = PendingIntent.getBroadcast(requireActivity(), 0, notify, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val manager: AlarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingNotify)
        manager.setExact(AlarmManager.RTC_WAKEUP, start, pendingNotify)
    }
}
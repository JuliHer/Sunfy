package com.artuok.appwork.fragmets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.artuok.appwork.R
import com.artuok.appwork.services.AlarmWorkManager
import com.google.android.gms.common.SignInButton
import com.thekhaeng.pushdownanim.PushDownAnim


class BackupsFragment : Fragment() {

    private lateinit var createBackup: LinearLayout
    private lateinit var restoreBackup: LinearLayout
    private lateinit var login: SignInButton


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_backups, container, false)
        createBackup = root.findViewById(R.id.create_backup_layout)
        restoreBackup = root.findViewById(R.id.restore_backup_layout)

        PushDownAnim.setPushDownAnimTo(createBackup)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                setBackup()
            }

        PushDownAnim.setPushDownAnimTo(restoreBackup)
            .setDurationPush(100)
            .setScale(PushDownAnim.MODE_SCALE, 0.98f)
            .setOnClickListener {
                getBackup()
            }


        return root
    }


    private fun setBackup() {
        val i = Intent(requireActivity(), AlarmWorkManager::class.java)
        i.action = AlarmWorkManager.ACTION_SET_BACKUP
        requireActivity().sendBroadcast(i)
    }

    private fun getBackup() {
        val i = Intent(requireActivity(), AlarmWorkManager::class.java)
        i.action = AlarmWorkManager.ACTION_RESTORE_BACKUP
        requireActivity().sendBroadcast(i)
        requireActivity().finish()
    }
}
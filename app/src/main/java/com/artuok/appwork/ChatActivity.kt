package com.artuok.appwork

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class ChatActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val name = intent.extras?.getString("name")
        toolbar = findViewById(R.id.toolbar2)
        (toolbar.findViewById<View>(R.id.title) as TextView).text = name
        toolbar.navigationIcon = getDrawable(R.drawable.ic_arrow_left)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }


}
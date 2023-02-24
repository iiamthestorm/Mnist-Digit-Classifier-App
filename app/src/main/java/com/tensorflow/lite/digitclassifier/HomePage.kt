package com.tensorflow.lite.digitclassifier

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class HomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        val firstActbutton = findViewById<Button>(R.id.first_act_btn)
        firstActbutton.setOnClickListener{
            val intent2 = Intent(this, PreAug::class.java)
            startActivity(intent2)
        }

        val secondActbutton = findViewById<Button>(R.id.second_act_btn)
        secondActbutton.setOnClickListener{
            val intent = Intent(this, PostAug::class.java)
            startActivity(intent)
        }
    }
}
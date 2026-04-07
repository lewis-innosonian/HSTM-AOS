package com.example.hstm_aos.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hstm_aos.Fragment.GuideAndHelpFragment
import com.example.hstm_aos.R
import com.example.hstm_aos.customview.RoundedButton

class GuideHelpActivity : BaseActivity() {

    private val docList = mutableListOf<GuideAndHelpFragment.GuideMenuItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide_help)

        val skipRoundedButton = findViewById<RoundedButton>(R.id.skipRoundedButton)

        skipRoundedButton.setOnClickListener {
            finish()
        }
        val docs = intent.getSerializableExtra("documents")
                as? ArrayList<GuideAndHelpFragment.GuideMenuItem>

        docs?.let {
            docList.addAll(it)
        }

        val fragment = GuideAndHelpFragment().apply {
            arguments = Bundle().apply {
                putSerializable("documents", ArrayList(docList))
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
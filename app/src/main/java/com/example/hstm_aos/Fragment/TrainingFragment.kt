package com.example.hstm_aos.Fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.hstm_aos.R
import com.example.hstm_aos.TrainingActivity
import kotlinx.coroutines.launch

class TrainingFragment : Fragment(R.layout.fragment_training) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as TrainingActivity

        viewLifecycleOwner.lifecycleScope.launch {
            activity.blePacketFlow.collect { (address, data) ->
            }
        }
    }
}

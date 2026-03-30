package com.example.hstm_aos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.hstm_aos.Fragment.GuideAndHelpFragment
import com.example.hstm_aos.model.OpenSkill

class SharedDataViewModel : ViewModel() {

    private val _skills = MutableLiveData<List<OpenSkill>>(emptyList())
    val skills: LiveData<List<OpenSkill>> get() = _skills

    private val _documents = MutableLiveData<List<GuideAndHelpFragment.GuideMenuItem>>(emptyList())
    val documents: LiveData<List<GuideAndHelpFragment.GuideMenuItem>> get() = _documents

    fun setSkills(list: List<OpenSkill>) {
        _skills.value = list
    }

    fun setDocuments(list: List<GuideAndHelpFragment.GuideMenuItem>) {
        _documents.value = list
    }
}
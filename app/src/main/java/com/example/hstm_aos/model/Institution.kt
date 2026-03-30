package com.example.hstm_aos.model

data class Institution(
    val inst_id: String?,
    val inst_name: String?,
    val is_bridge_user: Boolean?,
    val Open_Skill: List<OpenSkill>?,
    val feedback: String?,
    val help_docs: HelpDocs?
)

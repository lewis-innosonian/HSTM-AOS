package com.example.hstm_aos.model

data class Organization(
    val org_id: String?,
    val org_name: String?,
    val First_name: String?,
    val Last_name: String?,
    val show_welcome_screen: Boolean?,
    val Institutions: List<Institution>?
)

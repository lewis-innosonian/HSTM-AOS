package com.example.hstm_aos

data class HStreamConfig(
    val success: Boolean,
    val Watermark: String?,
    val ClientId: String,
    val ClientSecret: String,
    val CognitoIdentityUserPoolRegion: String,
    val CognitoIdentityUserPoolId: String,
    val CognitoIdentityUserPoolAppClientId: String,
    val CognitoIdentityUserPoolAppClientSecret: String,
    val AWSCognitoUserPoolsSignInProviderKey: String,
    val GetSkillsURL: String,
    val SendResultURL: String,
    val ForgetPassURL: String,
    val AuthorizeURL: String,
    val AccessTokenURL: String,
    val CalcURL: List<String>,
    val DocumentURL: String
)

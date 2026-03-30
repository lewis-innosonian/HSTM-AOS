package com.example.hstm_aos

object ConfigManager {

    var clientId: String = ""
    var clientSecret: String = ""

    var cognitoRegion: String = ""
    var cognitoUserPoolId: String = ""
    var cognitoAppClientId: String = ""
    var cognitoAppClientSecret: String = ""

    var signInProviderKey: String = ""

    var getSkillsURL: String = ""
    var sendResultURL: String = ""
    var forgetPassURL: String = ""
    var authorizeURL: String = ""
    var accessTokenURL: String = ""

    var calcURL: List<String> = emptyList()
    var documentURL: String = ""

    fun isReady(): Boolean {
        return clientId.isNotEmpty() && authorizeURL.isNotEmpty()
    }
}

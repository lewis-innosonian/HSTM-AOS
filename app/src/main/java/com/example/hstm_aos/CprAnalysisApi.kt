package com.example.hstm_aos

import com.example.hstm_aos.model.HstmResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CprAnalysisApi {

    @Multipart
    @POST("cpr-analysis")
    suspend fun finishTraining(
        @Part("condition") condition: RequestBody,
        @Part("access_token") accessToken: RequestBody,
        @Part("DeviceInfo") deviceInfo: RequestBody,
        @Part("Organization") organization: RequestBody,
        @Part("Dummy") dummy: RequestBody,
        @Part("Open_Skill") openSkill: RequestBody,
        @Part("Usage") usage: RequestBody,
        @Part("Institution") institution: RequestBody,
        @Part("CalculationService") calculationService: RequestBody,
        @Part("Guide_prompts") guidePrompts: RequestBody,
        @Part("Custom") custom: RequestBody,
        @Part("Certification") certification: RequestBody,
        @Part rawHexBPfile: MultipartBody.Part,
        @Part aedHexBPfile: MultipartBody.Part? = null,
        @Part("vp_event_list") vp_event_list : RequestBody? = null
    ): Response<HstmResponse>
}


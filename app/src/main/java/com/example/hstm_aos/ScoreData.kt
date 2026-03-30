package com.example.hstm_aos

import java.io.Serializable

class ScoreData : Serializable {
    var total: String? = ""
    var ccf: Int? = null   //가슴압박분율점수
    var compression_depth: Int? = null //가슴압박 깊이 점수
    var compression_rate:  Int? = null//가슴압박 속도 점수
    var compression_recoil:  Int? = null // 가슴압박 이완 점수
    var handposition: Int? = null //가슴압박 위치 점수
    var ventilation_volume: Int? = null
    var compression_no : Int? = null
    var compression_count : Int? = null
    var ventilation_rate:  Int? = null
    var ventilation_speed:  Int? = null // 호흡 주입속도 점수
    var ventilation_count:  Int? = null

    var by_cycle: ArrayList<ScoreData> = ArrayList()

}
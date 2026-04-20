package com.example.hstm_aos

object FontScaleManager {

    const val STEP_SMALL = 0   // 1단계
    const val STEP_MEDIUM = 1  // 2단계
    const val STEP_LARGE = 2   // 3단계

    fun getScale(step: Int): Float {
        return when (step) {
            0 -> 1.0f
            1 -> 1.25f
            2 -> 1.5f
            else -> 1.0f
        }
    }
}
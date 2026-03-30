package com.example.hstm_aos

object UserTrainingState {
    private val completedKeys = mutableSetOf<String>()

    private fun key(content: ContentsItem.Content): String {
        val types = content.trainingType.joinToString("_") { it.name }
        return "${content.skillTypeId}_${content.certType}_${types}"
    }

    fun markCompleted(content: ContentsItem.Content) {
        completedKeys.add(key(content))
    }

    fun isCompleted(content: ContentsItem.Content): Boolean {
        return completedKeys.contains(key(content))
    }

    fun clear() {
        completedKeys.clear()
    }
}

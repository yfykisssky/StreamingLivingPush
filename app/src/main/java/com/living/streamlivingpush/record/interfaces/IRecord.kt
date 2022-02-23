package com.living.streamlivingpush.record.interfaces

interface IRecord {
    fun initRecoder()
    fun startRecode()
    fun stopRecode()

    fun usePriImgPush(usePri: Boolean)
}
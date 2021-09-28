package com.common.base

data class EventBusMsg(
    var type: Int = -1,
    var obj: Any? = null
)
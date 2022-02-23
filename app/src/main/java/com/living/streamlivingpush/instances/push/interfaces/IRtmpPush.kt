package com.living.streamlivingpush.instances.push.interfaces

import com.living.streamlivingpush.instances.interfaces.IInstance

interface IRtmpPush : IInstance {
    fun startPushing(pushUrl: String)
    fun stopPushing()
}
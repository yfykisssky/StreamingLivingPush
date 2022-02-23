package com.living.streamlivingpush.instances.push.interfaces

import com.living.streamlivingpush.instances.interfaces.IInstance

interface ISocketPush : IInstance {
    fun startPushing(connectIp: String, connectPort: Int)
    fun stopPushing()
}
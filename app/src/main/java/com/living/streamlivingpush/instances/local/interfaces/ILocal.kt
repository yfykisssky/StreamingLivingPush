package com.living.streamlivingpush.instances.local.interfaces

import com.living.streamlivingpush.instances.interfaces.IInstance

interface ILocal : IInstance {
    fun startLocal(needSave: Boolean = false)
    fun stopLocal()
}
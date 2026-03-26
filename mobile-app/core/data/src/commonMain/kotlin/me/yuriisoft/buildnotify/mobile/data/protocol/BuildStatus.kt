package me.yuriisoft.buildnotify.mobile.data.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class BuildStatus {
    STARTED,
    SUCCESS,
    FAILED,
    CANCELLED,
}

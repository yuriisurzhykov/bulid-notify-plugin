package me.yuriisoft.buildnotify.build.model

import kotlinx.serialization.Serializable

@Serializable
enum class BuildStatus {
    STARTED,
    SUCCESS,
    FAILED,
    CANCELLED,
}
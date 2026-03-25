package me.yuriisoft.buildnotify.mobile.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BuildStatus {
    STARTED,
    SUCCESS,
    FAILED,
    CANCELLED,
}

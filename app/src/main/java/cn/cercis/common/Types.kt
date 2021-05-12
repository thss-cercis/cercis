package cn.cercis.common

typealias CommonId = Long
typealias UserId = Long
typealias ApplyId = Long
typealias ChatId = Long
typealias MessageId = Long
typealias ActivityId = Long
typealias MediumId = Long
typealias CommentId = Long
typealias SerialId = Long
typealias WSMessageTypeId = Long
typealias Timestamp = Long

enum class MediaType {
    IMAGE, VIDEO;
    companion object {
        val NONE = IMAGE
    }
}

package dev.inmo.plaguposter.posts.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class PostId(
    val string: String
)

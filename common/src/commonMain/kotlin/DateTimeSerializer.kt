package dev.inmo.plaguposter.common

import com.soywiz.klock.DateTime
import kotlinx.serialization.Serializer

@Serializer(DateTime::class)
object DateTimeSerializer

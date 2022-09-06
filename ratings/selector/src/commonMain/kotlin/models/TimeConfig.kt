package dev.inmo.plaguposter.ratings.selector.models

import com.soywiz.klock.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class TimeConfig(
    @Serializable(TimeSerializer::class)
    val from: Time,
    @Serializable(TimeSerializer::class)
    val to: Time
) {
    @Transient
    val range = from .. to

    val isActive: Boolean
        get() = DateTime.now().time in range


    object TimeSerializer : KSerializer<Time> {
        val format = TimeFormat("HH:mm")
        override val descriptor: SerialDescriptor = String.serializer().descriptor

        override fun deserialize(decoder: Decoder): Time {
            return format.parseTime(decoder.decodeString())
        }

        override fun serialize(encoder: Encoder, value: Time) {
            encoder.encodeString(format.format(value))
        }
    }
}

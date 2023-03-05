package dev.inmo.plaguposter.common

import com.soywiz.klock.DateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DateTimeSerializer : KSerializer<DateTime> {
    override val descriptor: SerialDescriptor = Double.serializer().descriptor
    override fun deserialize(decoder: Decoder): DateTime = DateTime(decoder.decodeDouble())
    override fun serialize(encoder: Encoder, value: DateTime) = encoder.encodeDouble(value.unixMillis)
}

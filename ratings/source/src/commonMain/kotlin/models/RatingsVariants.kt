package dev.inmo.plaguposter.ratings.source.models

import dev.inmo.plaguposter.ratings.models.Rating
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

typealias RatingsVariants = Map<String, Rating>
object RatingsVariantsSerializer : KSerializer<RatingsVariants> {
    val surrogate = JsonObject.serializer()
    override val descriptor: SerialDescriptor = surrogate.descriptor
    override fun deserialize(decoder: Decoder): RatingsVariants {
        val o = surrogate.deserialize(decoder)
        return o.entries.mapNotNull { (key, value) ->
            val doubleValue =  (value as? JsonPrimitive) ?.doubleOrNull ?: return@mapNotNull null
            key to Rating(doubleValue)
        }.toMap()
    }

    override fun serialize(encoder: Encoder, value: RatingsVariants) {
        surrogate.serialize(
            encoder,
            buildJsonObject {
                value.forEach { (text, rating) ->
                    put(text, rating.double)
                }
            }
        )
    }

}

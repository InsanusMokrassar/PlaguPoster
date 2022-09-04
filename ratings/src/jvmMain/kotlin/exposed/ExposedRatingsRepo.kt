package dev.inmo.plaguposter.ratings.exposed

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.exposed.keyvalue.ExposedKeyValueRepo
import dev.inmo.micro_utils.repos.mappers.withMapper
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.models.Rating
import dev.inmo.plaguposter.ratings.repo.RatingsRepo
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database

class ExposedRatingsRepo(
    database: Database
) : RatingsRepo, KeyValueRepo<PostId, Rating> by ExposedKeyValueRepo(
    database,
    { text("post_id") },
    { double("rating") },
    "ratings"
).withMapper(
    { string },
    { double },
    { PostId(this) },
    { Rating(this) }
)

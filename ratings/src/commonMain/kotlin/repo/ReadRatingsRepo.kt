package dev.inmo.plaguposter.ratings.repo

import dev.inmo.micro_utils.repos.ReadKeyValueRepo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.models.Rating

interface ReadRatingsRepo : ReadKeyValueRepo<PostId, Rating>

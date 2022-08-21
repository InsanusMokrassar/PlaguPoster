package dev.inmo.plaguposter.ratings.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.models.Rating

interface RatingsRepo : KeyValueRepo<PostId, Rating>, ReadRatingsRepo, WriteRatingsRepo

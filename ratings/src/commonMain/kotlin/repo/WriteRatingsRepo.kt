package dev.inmo.plaguposter.ratings.repo

import dev.inmo.micro_utils.repos.WriteKeyValueRepo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.plaguposter.ratings.models.Rating

interface WriteRatingsRepo : WriteKeyValueRepo<PostId, Rating>

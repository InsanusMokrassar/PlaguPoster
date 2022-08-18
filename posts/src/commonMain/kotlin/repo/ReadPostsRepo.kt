package dev.inmo.plaguposter.posts.repo

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.plaguposter.posts.models.*

interface ReadPostsRepo : ReadCRUDRepo<RegisteredPost, PostId>

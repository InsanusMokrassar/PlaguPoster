package dev.inmo.plaguposter.posts.repo

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import dev.inmo.micro_utils.repos.WriteCRUDRepo
import dev.inmo.plaguposter.posts.models.*

interface WritePostsRepo : WriteCRUDRepo<RegisteredPost, PostId, NewPost>

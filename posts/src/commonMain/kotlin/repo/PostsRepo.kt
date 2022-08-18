package dev.inmo.plaguposter.posts.repo

import dev.inmo.micro_utils.repos.CRUDRepo
import dev.inmo.plaguposter.posts.models.*

interface PostsRepo : CRUDRepo<RegisteredPost, PostId, NewPost>, ReadPostsRepo, WritePostsRepo {
}

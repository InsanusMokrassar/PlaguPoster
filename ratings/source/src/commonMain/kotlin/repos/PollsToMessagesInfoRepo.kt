package dev.inmo.plaguposter.ratings.source.repos

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.plaguposter.common.ShortMessageInfo
import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.tgbotapi.types.PollIdentifier

interface PollsToMessagesInfoRepo : KeyValueRepo<PollIdentifier, ShortMessageInfo>

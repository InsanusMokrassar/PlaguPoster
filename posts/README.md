# Posts Plugin

Main plugin, which should be included in your config.

## Plugin name

```
dev.inmo.plaguposter.posts.Plugin
```

## Plugin config

```json
{
    "posts": {
        "chats": {
            "targetChat": -1001234567890,
            "targetChat description": "Chat identifier where your posts should appear after publishing",
            "sourceChat": -1001234567890,
            "sourceChat description": "Chat identifier where your posts will be placed for polls and other management",
            "cacheChat": -1001234567890,
            "cacheChat description": "Chat identifier where your posts will be cached before the publishing to actualize content of posts"
        },
        "autoRemoveMessages": true,
        "autoRemoveMessages description": "If autoRemoveMessages is enabled, with dropping of posts all the post messages will be removed is possible",
        "deleteAfterPublishing": true,
        "deleteAfterPublishing description": "If deleteAfterPublishing is enabled, posts will be automatically dropped after publishing"
    }
}
```

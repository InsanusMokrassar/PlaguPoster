# PlaguPoster

This is a posting system for the Telegram based on usage of three chats:

* Target chat where the posts will be published to
* Source chat where the posts will be stored, available for management and ratings
* Cache chat where sometimes will appear your posts to be cached in memory of bot

And different plugins. Sample config is presented in the root of this repository.
Each plugin describes its own format of subconfig. Anyway, most of config parts will be stored in one
file `config.json` (you may name it anyhow).

## How does it work

1. You are sending post to __source__ chat
2. Bot register it
3. (Optionally) bot attaching ratings poll

## How to launch the bot

There are several ways to launch the bot:

* With Docker (and `docker-compose`)
* With using of `gradle` and `run` command
* Using `zip`/`tar` after project building



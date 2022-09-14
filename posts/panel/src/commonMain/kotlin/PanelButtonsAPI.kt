package dev.inmo.plaguposter.posts.panel

import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import kotlinx.coroutines.flow.MutableSharedFlow

class PanelButtonsAPI(
    private val preset: List<PanelButtonBuilder>,
    private val rootPanelButtonText: String
) {
    private val _buttons = mutableSetOf<PanelButtonBuilder>().also {
        it.addAll(preset)
    }
    internal val buttonsBuilders: List<PanelButtonBuilder>
        get() = _buttons.toList()
    internal val forceRefreshFlow = MutableSharedFlow<PostId>()

    val RootPanelButtonBuilder = PanelButtonBuilder {
        CallbackDataInlineKeyboardButton(
            rootPanelButtonText,
            "$openGlobalMenuDataPrefix${it.id.string}"
        )
    }

    fun add(button: PanelButtonBuilder) = _buttons.add(button)
    fun remove(button: PanelButtonBuilder) = _buttons.remove(button)
    suspend fun forceRefresh(postId: PostId) {
        forceRefreshFlow.emit(postId)
    }

    companion object {
        internal const val openGlobalMenuData = "force_refresh_panel"
        internal const val openGlobalMenuDataPrefix = "$openGlobalMenuData "
    }
}

package dev.inmo.plaguposter.posts.panel

import dev.inmo.plaguposter.posts.models.PostId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import kotlinx.coroutines.flow.MutableSharedFlow

class PanelButtonsAPI(
    private val preset: Map<Int, List<PanelButtonBuilder>>,
    private val rootPanelButtonText: String
) {
    private val _buttonsMap = mutableMapOf<Int, MutableList<PanelButtonBuilder>>().also {
        it.putAll(preset.map { it.key to it.value.toMutableList() })
    }
    internal val buttonsBuilders: List<PanelButtonBuilder>
        get() = _buttonsMap.toList().sortedBy { it.first }.flatMap { it.second }
    internal val forceRefreshFlow = MutableSharedFlow<PostId>()

    val RootPanelButtonBuilder = PanelButtonBuilder {
        CallbackDataInlineKeyboardButton(
            rootPanelButtonText,
            "$openGlobalMenuDataPrefix${it.id.string}"
        )
    }

    fun add(button: PanelButtonBuilder, weight: Int = button.weight) = _buttonsMap.getOrPut(weight) { mutableListOf() }.add(button)
    fun remove(button: PanelButtonBuilder) = _buttonsMap.mapNotNull { (k, v) ->
        v.remove(button)
        k.takeIf { v.isEmpty() }
    }.forEach {
        _buttonsMap.remove(it)
    }
    suspend fun forceRefresh(postId: PostId) {
        forceRefreshFlow.emit(postId)
    }

    companion object {
        internal const val openGlobalMenuData = "force_refresh_panel"
        internal const val openGlobalMenuDataPrefix = "$openGlobalMenuData "
    }
}

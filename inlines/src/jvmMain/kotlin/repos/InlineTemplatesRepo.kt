package dev.inmo.plaguposter.inlines.repos

import dev.inmo.plaguposter.inlines.models.OfferTemplate

class InlineTemplatesRepo(
    private val _templates: MutableSet<OfferTemplate>
) {
    internal val templates
        get() = _templates.toList()
    suspend fun addTemplate(offerTemplate: OfferTemplate): Boolean {
        return _templates.add(offerTemplate)
    }
    suspend fun dropTemplate(offerTemplate: OfferTemplate): Boolean {
        return _templates.remove(offerTemplate)
    }
}

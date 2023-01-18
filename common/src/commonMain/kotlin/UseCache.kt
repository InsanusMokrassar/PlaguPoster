package dev.inmo.plaguposter.common

import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

val Scope.useCache: Boolean
    get() = getOrNull(named("useCache")) ?: false

val Koin.useCache: Boolean
    get() = getOrNull(named("useCache")) ?: false

fun Module.useCache(useCache: Boolean) {
    single(named("useCache")) { useCache }
}

package com.atmaweapon.composable2dos.sdk

import com.lightningkite.lightningserver.db.*
import kotlinx.serialization.builtins.*

open class CachedApi(val uncached: Api) {
	open val appReleases = ModelCache(uncached.appRelease, com.atmaweapon.composable2dos.AppRelease.serializer())
	open val users = ModelCache(uncached.user, com.atmaweapon.composable2dos.User.serializer())
	open val taskSets = ModelCache(uncached.taskSet, com.atmaweapon.composable2dos.TaskSet.serializer())
	open val tasks = ModelCache(uncached.task, com.atmaweapon.composable2dos.Task.serializer())
	open val sessions = ModelCache(uncached.userAuth, com.lightningkite.lightningserver.sessions.Session.serializer(com.atmaweapon.composable2dos.User.serializer(), kotlin.uuid.Uuid.serializer()))
	open val totpSecrets = ModelCache(uncached.userAuth.totp, com.lightningkite.lightningserver.sessions.TotpSecret.serializer())
	open val passwordSecrets = ModelCache(uncached.userAuth.password, com.lightningkite.lightningserver.sessions.PasswordSecret.serializer())
	open val fcmTokens = ModelCache(uncached.fcmToken, com.atmaweapon.composable2dos.FcmToken.serializer())
}

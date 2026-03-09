package com.atmaweapon.composable2dos.sdk

import kotlin.uuid.Uuid


class UserSession(
    val api: Api,
    val userId: Uuid,
) : CachedApi(api) {

}

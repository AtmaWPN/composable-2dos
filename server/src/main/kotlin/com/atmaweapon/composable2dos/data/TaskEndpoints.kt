package com.atmaweapon.composable2dos.data

import com.lightningkite.lightningserver.auth.id
import com.lightningkite.lightningserver.auth.require
import com.lightningkite.lightningserver.definition.StartupTask
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.definition.generalSettings
import com.lightningkite.lightningserver.typed.ModelRestEndpoints
import com.lightningkite.lightningserver.typed.auth
import com.lightningkite.lightningserver.typed.modelInfo
import com.lightningkite.lightningserver.typed.startupOnce
import com.atmaweapon.composable2dos.*
import com.atmaweapon.composable2dos.UserAuth.RoleCache.userRole
import com.atmaweapon.composable2dos._id
import com.atmaweapon.composable2dos.email
import com.atmaweapon.composable2dos.role
import com.lightningkite.services.database.Condition
import com.lightningkite.services.database.ModelPermissions
import com.lightningkite.services.database.condition
import com.lightningkite.services.database.eq
import com.lightningkite.services.database.insertOne
import com.lightningkite.services.database.inside
import com.lightningkite.services.database.or
import com.lightningkite.services.database.updateRestrictions
import com.lightningkite.toEmailAddress
import kotlin.uuid.Uuid

object TaskEndpoints : ServerBuilder() {

    val info = Server.database.modelInfo(
        auth = UserAuth.require(),
        permissions = {
            val admin: Condition<Task> =
                if (this.auth.userRole() >= UserRole.Admin) Condition.Always else Condition.Never
            val self = condition<Task> { it.user eq auth.id }
            ModelPermissions(
                create = admin or self,
                read = admin or self,
                update = admin or self,
                updateRestrictions = updateRestrictions {
                    it.createdAt.cannotBeModified()
                },
                delete = admin or self,
            )
        }
    )

    val rest = path include ModelRestEndpoints(info)
//    val socketUpdates = ModelRestUpdatesWebsocket(path, Server.database, info)

}
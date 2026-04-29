package com.atmaweapon.composable2dos

import com.lightningkite.lightningserver.cors.CorsSettings
import com.lightningkite.lightningserver.definition.loggingSettings
import com.lightningkite.lightningserver.definition.secretBasis
import com.lightningkite.lightningserver.definition.telemetrySettings
import com.lightningkite.lightningserver.engine.awsserverless.AwsAdapter
import com.lightningkite.lightningserver.terraform.AwsSecretSource
import com.lightningkite.lightningserver.terraform.SecretSource
import com.lightningkite.lightningserver.terraform.awsserverless.TerraformAwsServerlessDomainBuilder
import com.lightningkite.lightningserver.terraform.generated
import com.lightningkite.services.LoggingSettings
import com.lightningkite.services.cache.dynamodb.awsDynamoDb
import com.lightningkite.services.database.mongodb.mongodbAtlasFree
import com.lightningkite.services.email.javasmtp.awsSesDomain
import com.lightningkite.services.email.javasmtp.awsSesSmtp
import com.lightningkite.services.files.s3.awsS3Bucket
import com.lightningkite.services.otel.OpenTelemetrySettings
import com.lightningkite.services.terraform.TerraformProvider
import com.lightningkite.services.terraform.TerraformProviderImport
import com.lightningkite.services.terraform.byVariable
import com.lightningkite.services.terraform.direct
import com.lightningkite.toEmailAddress
import io.github.oshai.kotlinlogging.Level
import kotlinx.serialization.json.JsonObject
import software.amazon.awssdk.regions.Region
import java.io.File
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes


object ProdEnv : TerraformAwsServerlessDomainBuilder<Server>(Server) {
    override val displayName = "composable-2dos"
    override val domain = "api.composable2dos.atmaweapon.net"
    override val domainZone = "atmaweapon.net"
    override val terraformRoot: File = File("server/terraform/lk")

    override val handler: KClass<out AwsAdapter> = AwsHandler::class
    override val timeout: Duration = 5.minutes

    override val storageBucket = "theatmaweapon-terraform"
    override val storageBucketPath: String
        get() = super.storageBucketPath
    override val debug = true
    override val emergencyContact = "ajdittli@gmail.com".toEmailAddress()

    override val region = Region.US_WEST_2!!

    override val secretsSource: SecretSource = AwsSecretSource("default", projectPrefix, region)

    override fun Server.settings() {
        require(TerraformProviderImport.mongodbAtlas)
        require(TerraformProvider(TerraformProviderImport.mongodbAtlas, null, JsonObject(emptyMap())))
        println(this@ProdEnv.terraformProviderImports)
        println(this@ProdEnv.terraformProviders)

        loggingSettings.direct(LoggingSettings(
            default = LoggingSettings.ContextSettings(
                filePattern = null,
                toConsole = true,
                level = Level.DEBUG,
                additive = false
            ),
            logger = mapOf(
                "org.mongodb" to LoggingSettings.ContextSettings(
                    filePattern = null,
                    toConsole = false,
                    level = Level.INFO,
                    additive = false
                ),
                "software.amazon.awssdk" to LoggingSettings.ContextSettings(
                    filePattern = null,
                    toConsole = false,
                    level = Level.INFO,
                    additive = false
                ),
                "io.netty" to LoggingSettings.ContextSettings(
                    filePattern = null,
                    toConsole = false,
                    level = Level.INFO,
                    additive = false
                ),
            )
        ))
        database.mongodbAtlasFree(orgId = "69c2e8d698931d8bba6017c7")
        awsSesDomain("email",emergencyContact)
        email.awsSesSmtp("email")
        files.awsS3Bucket(signedUrlDuration = 1.days)
        cache.awsDynamoDb()
        secretBasis.generated()
        telemetrySettings.direct(OpenTelemetrySettings("console", batching = null))
        cors.direct(CorsSettings(
                limitToDomains = listOf(
                    "app.composable2dos.atmaweapon.net",
                    "www.app.composable2dos.atmaweapon.net",
                    "api.composable2dos.atmaweapon.net",
                    "www.api.composable2dos.atmaweapon.net",
                ),
                limitToHeaders = listOf("*"),
                limitToMethods = listOf("*"),
                allowCredentials = true,
                exposedHeaders = emptyList(),
        ))
        notifications.byVariable()
        webUrl.direct("https://app.composable2dos.atmaweapon.net")
    }
}

object DemoEnvDeploy {
    @JvmStatic
    fun main(vararg args: String) = ProdEnv.deploy()
}
object DemoEnvEdit {
    @JvmStatic
    fun main(vararg args: String) = ProdEnv.editVars()
}
object DemoEnvPrepare {
    @JvmStatic
    fun main(vararg args: String): Unit = ProdEnv.prepareTerraform().let(::println)
}
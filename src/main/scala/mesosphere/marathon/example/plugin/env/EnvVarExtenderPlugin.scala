package mesosphere.marathon.example.plugin.env

import mesosphere.marathon.plugin.task._
import mesosphere.marathon.plugin.plugin.PluginConfiguration
import org.apache.mesos.Protos
import org.slf4j.LoggerFactory
import java.net.URL

import play.api.libs.json.Json

import scalaj.http.{Http, HttpOptions, HttpResponse}

class EnvVarExtenderPlugin extends RunSpecTaskProcessor with PluginConfiguration {
  private[env] var envVariables = Map.empty[String, String]
  private val log = LoggerFactory.getLogger(getClass)

  def initialize(marathonInfo: Map[String, Any], configuration: play.api.libs.json.JsObject): Unit = {
    envVariables = (configuration \ "env").as[Map[String, String]]
    log.info("EnvVarExtenderPlugin initialized")
  }

  def apply(runSpec: mesosphere.marathon.plugin.RunSpec, builder: org.apache.mesos.Protos.TaskInfo.Builder): Unit = {
    val envBuilder = builder.getCommand.getEnvironment.toBuilder
    val maybeVaultAddr = envVariables.get("address")
    val maybeToken = envVariables.get("token")

    log.info("EnvVarExtenderPlugin applied")

    for {
      vaultAddr <- maybeVaultAddr
      token <- maybeToken
    } yield {
      log.debug(s"EnvVarExtenderPlugin address: $vaultAddr")
      runSpec.secrets.foreach {
        case(key, secret) =>
          val url = makeVaultUrl(vaultAddr, s"v1/secret/${secret.source}")
          log.debug(s"EnvVarExtenderPlugin Connecting to $url")

          val resp = fetchSecrets(url, token)
          if(resp.is2xx) {
            val jsonresp = Json.parse(resp.body)
            val secretval = (jsonresp \ "data" \ "value").as[String]
            val envVariable = Protos.Environment.Variable.newBuilder()
            envVariable.setName(key)
            envVariable.setValue(secretval)
            envBuilder.addVariables(envVariable)
            log.debug("EnvVarExtenderPlugin added envVariable")
          } else {
            log.error(s"got unexpected response from vault $resp")
          }
      }
    }

    if (maybeVaultAddr.isEmpty || maybeToken.isEmpty) {
      log.error(s"missing address and/or token in plugin config")
    }

    val commandBuilder = builder.getCommand.toBuilder
    commandBuilder.setEnvironment(envBuilder)
    builder.setCommand(commandBuilder)
  }

  protected def makeVaultUrl(host: String, path: String): String = {
    // This ignores paths on `host`, e.g. company.com/vault + /v1/sys/self-capabilities
    new URL(new URL(host), path).toString
  }

  def fetchSecrets(url: String, token: String): HttpResponse[String] = {
    Http(url).header("X-Vault-Token",token).option(HttpOptions.allowUnsafeSSL).asString
  }

}

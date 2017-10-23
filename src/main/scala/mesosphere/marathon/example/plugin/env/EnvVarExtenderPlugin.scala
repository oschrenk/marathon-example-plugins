package mesosphere.marathon.example.plugin.env

import mesosphere.marathon.plugin.task._
import mesosphere.marathon.plugin.plugin.PluginConfiguration
import org.apache.mesos.Protos
import org.slf4j.LoggerFactory

class EnvVarExtenderPlugin extends RunSpecTaskProcessor with PluginConfiguration {
  private[env] var envVariables = Map.empty[String, String]
  private val log = LoggerFactory.getLogger(getClass)

  def initialize(marathonInfo: Map[String, Any], configuration: play.api.libs.json.JsObject): Unit = {
    envVariables = (configuration \ "env").as[Map[String, String]]
    log.info("EnvVarExtenderPlugin initialized")
  }

  def apply(runSpec: mesosphere.marathon.plugin.RunSpec, builder: org.apache.mesos.Protos.TaskInfo.Builder): Unit = {
    val envBuilder = builder.getCommand.getEnvironment.toBuilder
    envVariables.foreach {
      case (key, value) =>
        if (key == "token")
          log.info("EnvVarExtenderPlugin token is set")
        else if (key == "address")
          log.info(s"EnvVarExtenderPlugin address is $value")
        else {
          val envVariable = Protos.Environment.Variable.newBuilder()
          envVariable.setName(key)
          envVariable.setValue(value)
          envBuilder.addVariables(envVariable)
        }
    }
    val commandBuilder = builder.getCommand.toBuilder
    commandBuilder.setEnvironment(envBuilder)
    builder.setCommand(commandBuilder)
  }
}

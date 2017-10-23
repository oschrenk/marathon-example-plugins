package mesosphere.marathon.example.plugin.env

import mesosphere.marathon.plugin._
import org.apache.mesos.Protos
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsObject, Json}

import scalaj.http.HttpResponse


class EnvVarExtenderPluginTest extends FlatSpec with Matchers {
  "Initialization with a configuration" should "work" in {
    val f = new Fixture
    val map = Map("token" -> "b2d0959d-6d0d-c44d-a2eb-9d62d2778db0", "address" -> "https://vault.marathon.mesos:8200/")
    f.envVarExtender.envVariables should be(map)
  }

  "Applying the plugin" should "work" in {
    val f = new Fixture
    val mySecret: Secret = new Secret {
      override def source = "hello"
    }
    val envNormal = new EnvVarString {
      override def value = "somevalue"
    }
    val envWithSecret = new EnvVarSecretRef {
      override def secret = "secret0"
    }
    val runSpec: RunSpec = new RunSpec {
      override def acceptedResourceRoles: Option[Set[String]] = None
      override def env: Map[String, EnvVarValue] = Map("NORMAL_ENV" -> envNormal, "DATABASE_PW" -> envWithSecret)
      override def secrets: Map[String, Secret] = Map("secret0" -> mySecret)
      override def labels: Map[String, String] = Map()
      override def id: PathId = ???
      override def user: Option[String] = None
    }
    val builder = Protos.TaskInfo.newBuilder()
    f.envVarExtender(runSpec, builder)
    val secretEnv = builder.getCommand.getEnvironment.getVariablesList.get(0)
    secretEnv.getName should be("DATABASE_PW")
    secretEnv.getValue should be("world")
  }

  class Fixture {
    private val json =
      """{
        |    "env": {
        |        "token": "b2d0959d-6d0d-c44d-a2eb-9d62d2778db0",
        |        "address": "https://vault.marathon.mesos:8200/"
        |    }
        |}
      """.stripMargin
    private val config = Json.parse(json).as[JsObject]
    val envVarExtender: EnvVarExtenderPlugin = new EnvVarExtenderPlugin() {
      override def fetchSecrets(url: String, token: String): HttpResponse[String] = {
        if (url.endsWith("hello")) {
          val json =
            """{"request_id":"79a6fc29-4191-8f5e-f088-ceae7c6abe69","lease_id":"","renewable":false,"lease_duration":2764800,"data":{"value":"world"},"wrap_info":null,"warnings":null,"auth":null}""".stripMargin
          HttpResponse(json, 200, Map())
        } else {
          throw new IllegalArgumentException("Wrong token")
        }

      }
    }
    envVarExtender.initialize(Map.empty, config)
  }
}

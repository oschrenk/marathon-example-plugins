# Marathon Vault plugin

Most of the code is copied from [blackgold/marathon-vault-plugin](https://github.com/blackgold/marathon-vault-plugin) but as I couldn't get that plugin to work, I rebuilt it from scratch adding piece by piece and changed some things on the way.

## Main differences

- *NEW* error handling with logging if connecting to Vault or parsing the json from the result throws an exception
- *NEW* sets the original environment variable with the secret value, instead if injecting the secret reference as a new one
- *FIXED* json path to secret was different for my vault version, was just `\ "data"` but needed to be `"data" \ "value"`
- *NEW* working tests
- *CHANGE* Changed to sbt assembly

## Installation

Copy jar

```
sbt assembly
scp target/scala-2.11/env-plugin-assembly-1.0.jar dcos:/path/to/marathon/libs
```

Edit the `plugin-conf.json.sample` and add the right token, then copy

```
sbt assembly
scp plugin-conf.json.sample dcos:/path/to/marathon/conf
```

Enable `secrets` feature and set the right directories in the service config

```
$ ssh dcos
user@dcos-master$ cd /opt/mesosphere/packages/marathon-*
user@dcos-master$ vim dcos.target.wants_master/dcos-marathon.service
```

like so (full features set depdends on your needs)

```
    --plugin_dir="/path/to/marathon/libs" \
    --plugin_conf="/path/to/marathon/conf/plugin-conf.json" \
    --enable_features "secrets,vips,task_killing,external_volumes" \
```

then reload and restart marathon

```
systemctl daemon-reload
systemctl restart dcos-marathon
```

## Resources

For me it was helpful to have a running Vault installation and then interact with the server to the JSON response

```
curl --insecure \
    -H "X-Vault-Token: b2d5953d-6f0d-c44d-e2eb-9b62d2371db8" \
    -X GET \
    https://vault.marathon.mesos:8200/v1/secret/hello
{"request_id":"79a6fc29-4191-8f5e-f088-ceae7c6abe69","lease_id":"","renewable":false,"lease_duration":2764800,"data":{"value":"world"},"wrap_info":null,"warnings":null,"auth":null}
```


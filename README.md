# yaml-rule-plugin
A repo that contains all the plugins for the light-gateway request and response transformer

Each plugin will be a module in the repo and all of them will be released to the maven central along with light-4j release. A README.md will be in each module to describe how the module can be used in the light-gateway configuration.

### Document

##### [How to write a plugin](doc/action-plugin.md)


### Note

For each plugin, we have a separate jar file so that it would be easily deployed to the light-gateway plugin folder. For each jar, we have included the Implementation-Title and Implementation-Version in the MANIFEST.MF file so that it can be easily identified in the light-gateway during the rule loading time. Due to some limitations, we need to make sure that the action classes are not in the package of com.networknt.rule as this is the parent package for the rule loader. If you are using the same package name, then the getPackage() will not work for the class as the parent class loader is used.

This is why we have changed several plugin package names.

### Build

To build it from the source code for developers.

```
mvn clean install
```

### Release

* First, increase the version with maven command and upgrade the dependencies.

```
mvn versions:set -DnewVersion=1.1.10 -DgenerateBackupPoms=false
```

* Second, check in the updated codebase to the master branch.


* Finally, release from the master to the maven central.

```
mvn clean install deploy -DperformRelease
```

## Jackson 3 source line

This line requires Jackson 3.2.3, yaml-rule 4.0.0-SNAPSHOT and the Jackson 3 light-4j/http-client prerequisites. Build and install these sources in dependency order before building this reactor; Jackson 2 snapshot binaries are incompatible. Annotations retain com.fasterxml.jackson.annotation. Public SOAP serialization and transformer node types now use tools.jackson.

JSON mappers use the shared Jackson2Defaults profile. SOAP XML/YAML builders inherit its common feature policy and stream constraints. Mappers are immutable; encoding selects compact ObjectWriters instead of changing shared settings. JacksonException is unchecked, and the existing transform fallback/error handling is preserved. JDK8 and Java time modules are built into Jackson 3. YAML uses YAML 1.2 scalars. Classic SnakeYAML cleanup is deferred to the stack-wide final phase.

Local qualification covers the full offline reactor and sanitized SOAP/XML/JSON/YAML/token-request parity against the Jackson 2 baseline. Synthetic request-body tests do not contact an identity provider. No deployed integration or remote CI qualification is implied.

### Reproducible Jackson 3 source build

`.github/workflows/jackson3-source-build.yml` builds the prerequisite source stack in a fresh isolated Maven repository and runs its full test suites. It resolves every prerequisite to a full commit SHA once and uploads `source-refs.json`, `source-commits.txt` and Surefire reports. The shared mapper policy prerequisite is light-4j #2821; merge that before its consumers.

The optional `source_refs` workflow-dispatch input replays recorded full SHAs. GitHub enables dispatch only after the workflow exists on the default branch; before merge, reproduce from the recorded source checkouts locally. A green local suite is separate from a successful remote source-build run.

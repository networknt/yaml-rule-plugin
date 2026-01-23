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
mvn versions:set -DnewVersion=1.1.7 -DgenerateBackupPoms=false
```

* Second, check in the updated codebase to the master branch.


* Finally, release from the master to the maven central.

```
mvn clean install deploy -DperformRelease
```

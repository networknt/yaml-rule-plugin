# yaml-rule-plugin
A repo that contains all the plugins for the light-gateway request and response transformer

Each plugin will be a module in the repo and all of them will be released to the maven central along with light-4j release. A README.md will be in each module to describe how the module can be used in the light-gateway configuration. 

### Build

To build it from the source code for developers.

```
mvn clean install
```

### Release

* First, increase the version with maven command and upgrade the dependencies.  

```
mvn versions:set -DnewVersion=1.0.5 -DgenerateBackupPoms=false
```

* Second, check in the updated codebase to the master branch. 


* Finally, release from the master to the maven central.

```
mvn clean install deploy -DperformRelease
```



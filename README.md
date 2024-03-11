# sailfish-gradle-plugin

The Gradle plugin used within `Sailfish` projects

## How to use the plugin

### With `plugins` scope

```groovy
plugins {
   id 'com.exactpro.sailfish-gradle-plugin'
}
```

### With `apply plugin`

Add the plugin to classpath and replace it `<version-of-plugin>` with correct version of plugin at the end of `classpath` method

```groovy
buildscript {
    dependencies {
        classpath(group: 'com.exactpro.sailfish-gradle-plugin', name: 'sailfish-gradle-plugin', version: '<version-of-plugin>')
    }
}
```

and apply the plugin from the classpath with following command

```groovy
apply plugin: 'com.exactpro.sailfish-gradle-plugin'
```

## Information about the plugin

### Available tasks:

**generateXmlFAST**

_Implemented in_ `ConvertFASTTemplate` _class_

Generates FAST dictionaries from templates

**writeBuildInfo**

_Implemented in_ `BuildInfoWriter` _class_

Prints out information about the build artifacts

**writeFile**

_Implemented in_ `WriteFileTask` _class_

The task for writing into a file

**generateXmlFix**

_Implemented in_ `ConvertFixDictionary` _class_

**checkCompatibility**

_Implemented in_ `CompatibilityChecker` _class_

Checks compatibility of classes in a plugin with components of core

**validateDictionary**

_Implemented in_ `DictionaryValidatorPlugin` _class_

Validates dictionaries in plugins with associated validator classes

**generateVersionClass**

_Implemented in_ `GenerateVersionClass` _class_

Generates a class that provides information the version about the package. The resulting file has the following format: `*Version.java`

**generateXmlQuicfixj**

_Implemented in_ `ConvertSailfishDictionaryToQuickfixj` _class_

Generates FIX dictionaries from templates

**collectDependencies**

_Implemented in_ `DependencyCollector` _class_

Collects information about dependencies of `com.exactpro.sf` group with some additional data

**convertFixOrchestraToSailfishDictionary**

_Implemented in_ `OrchestraToSailfishConverter` _class_
</br>
</br>

<img width="512" src="https://exactpro.com/themes/expro_theme/logo/logo.svg" alt="Exactpro Logo"/>

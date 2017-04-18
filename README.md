# Java ILP master repository [![gitter][gitter-image]][gitter-url] [![CI][CI-image]][CI-url] 

[gitter-image]: https://badges.gitter.im/interledger/java.svg
[gitter-url]: https://gitter.im/interledger/java

[CI-image]: https://travis-ci.org/everis-innolab/java-ilp-master.svg?branch=master
[CI-url]: https://travis-ci.org/everis-innolab/java-ilp-master

## About
Java in-memory ledger implementing the Rest/Websocket expected by ilp-plugin-bells and VertX for java reactive programming

### Build:
```  $ gradle build ```

### Testing:
```  $ gradle test ```

### Running the ledger:
  * Option 1:(gradle): ``` $ gradle :launchServer ```
  * Option 2:(eclipse/Netbeans/...): Run/debug next class as a java application:
     ```.../org/interledger/ilp/ledger/api/Main.java ```

### Eclipse integration:
  * Option 1: (Easiest): Add official Eclipse Gradle pluging. Then import this root project as gradle project.
  
  * Option 2: (Manual): Create eclipse .project & .classpath files for each project with ``` $ gradle eclipse ```.
    (Then use File -> Import ... -> Existing projects from workspace and select the "Search for nested projects")

### Other common tasks:
``` 
./gradlew clean install check
```

``` 
./gradlew test
```

``` 
Generate random Private/Public keys used in application.conf: 
   - ledger.ed25519.conditionSignPrivateKey 
   - ledger.ed25519.conditionSignPublicKey
   - ledger.ed25519.notificationSignPrivateKey
   - ledger.ed25519.notificationSignPublicKey

./gradlew printRandomDSAEd25519PrivPubKey
``` 

## iConfiguration

 * application.conf is the main configuration file. app.conf can be used to overload and customize the setup.

 * Loggin can be configured at: (src/main/resources/)logback.xml

### Configuration - detailed
The configuration package (`org.interledger.ilp.common.config`) contains a [Config](blob/master/src/main/java/org/interledger/ilp/common/config/Config) *façade* to a [Configuration](blob/master/src/main/java/org/interledger/ilp/common/config/core/Configuration.java) [implementation](blob/master/src/main/java/org/interledger/ilp/common/config/core/DefaultConfigurationImpl.java).

This Config object enforces the use of enums as keys as a better method than static constants. There is a central enumeration [`Key`](blob/master/src/main/java/org/interledger/ilp/common/config/Key) holding all keys needed as a common repository/namespace.

The access to the values is hiearchical (key1.key2) eg:

- connector.host
- user.name  

The format used in current implementation is [HOCON] that is very flexible json-like human friendly configuration format.

The default configuration file (*application.conf*) is loaded from the classpath. 

The [HOCON] format allows include more configurations from local files and urls too.

### Sample configuration.cfg:

```
#Java properties like:
key.subkey = A string value
key.subkey2 = 100 //An int value. Comments allowed here!!

//HOCON format:

key {
    subkey: A string value that overwrites previously defined key.subkey
    subkey2= 123 //Notice the = sign! Also overwrites previously defined key.subkey2
    more {
        nesting: is cool :-)
    }
}

```

### Sample code:

``` java
    import org.interledger.ilp.common.config.Config;
    import static org.interledger.ilp.common.config.Key.*;
    
    ...
    Config config = Config.load(); //COC load application.conf from classpath
    //Read a String:
    String host = config.getString(CONNECTOR,HOST);
    //Read an int:
    int port = config.getInt(CONNECTOR,PORT);
    //Read a boolean:
    boolean https = config.getBoolean(CONNECTOR,USE_HTTPS);
    
```



## Development

Common code snippets are documented at dev_docs/code_snippets.txt (work in progress)

## Contributors

Any contribution is very much appreciated! [![gitter][gitter-image]][gitter-url]

## License

This code is released under the Apache 2.0 License. Please see [LICENSE](LICENSE) for the full text.

# Java ILP master repository [![gitter][gitter-image]][gitter-url] [![CI][CI-image]][CI-url] 

[gitter-image]: https://badges.gitter.im/interledger/java.svg
[gitter-url]: https://gitter.im/interledger/java

[CI-image]: https://travis-ci.org/everis-innolab/java-ilp-master.svg?branch=master
[CI-url]: https://travis-ci.org/everis-innolab/java-ilp-master

## About
Java ledger implementing HTLA Interledger RFC .
https://github.com/interledger/rfcs/tree/master/0022-hashed-timelock-agreements

 expected by ilp-plugin-bells and the java interfaces defined @ https://github.com/interledger/java-ilp-core/ . It uses an in-memory database, so all changes are lost after reboot.

Read developers docs @ dev_docs for more info

### Build:
```  $ gradle build ```

### Unit-Testing:
```  $ gradle test ```

### Functional-Testing:
   This project tries to keep compatibility with the REST/WS API of 
   [five-bells-ledger](https://github.com/interledgerjs/five-bells-ledger), that automatically
   warrants compatibility with the [plugin](https://github.com/interledgerjs/ilp-plugin-bells) for the
   [ilp connector reference implementation](https://github.com/interledgerjs/ilp-connector)

   A subset of five-bells-ledger tests adapted to this project are available at:
   https://github.com/interledgerjs/five-bells-ledger, branch: earizon-adaptedTest4JavaVertXLedger

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

```
Create HTTPS TLS certificates:
copy create_tls_certificate_example.sh, adjust parameters (DOMAIN, SUBJ, DAYS_TO_EXPIRE) to suits your setup and finally execute it.

The files $DOMAIN.key and $DOMAIN.cert will be created. Update 'server.tls_key' and 'server.tls_cert' parameters 
  at application.conf  accordingly.
```

## Configuration

 * application.conf is the main configuration file. app.conf can be used to overload and customize the setup.

 * Loggin can be configured at: (src/main/resources/)logback.xml



## Development

Common code snippets are documented at dev_docs/code_snippets.txt (work in progress)

## Contributors

Any contribution is very much appreciated! [![gitter][gitter-image]][gitter-url]

## License

This code is released under the Apache 2.0 License. Please see [LICENSE](LICENSE) for the full text.

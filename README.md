## Zendesk Migration and API Tools

Quick and dirty API tools for Zendesk

+ Migrate tickets, users and Help Center content between Zendesk accounts
+ Batch update existing tickets
+ Share new tickets with a sister account that has an existing sharing agreement
+ AWS Lambda support

### Configuration

1. Rename ConstantsSample to Constants and enter user credentials.
2. Import dependencies from Maven.

### Build & Run Packaged JAR

1. Run `mvn package`
2. Execute `java -jar target/zendesk-tools-1.0.jar`

### License

[MPL-2.0](https://www.mozilla.org/en-US/MPL/2.0/)

See [LICENSE](LICENSE)
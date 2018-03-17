## Zendesk Migration and API Tools

Quick and dirty API tools for Zendesk.

+ Migrate tickets, users and Help Center content between Zendesk accounts
+ Batch update existing tickets
+ Share new tickets with a sister account that was an existing sharing agreement
+ AWS Lambda support

### Configuration

1. Rename ConstantsSample to Constants and enter user credentials.

2. Import dependencies from Maven.

3. For MacOS, you may need to bump the limit on the maximum number of open files (for using Ticket.getTickets())

```bash
$ echo kern.maxfiles=65536 | sudo tee -a /etc/sysctl.conf
$ echo kern.maxfilesperproc=65536 | sudo tee -a /etc/sysctl.conf
$ sudo sysctl -w kern.maxfiles=65536
$ sudo sysctl -w kern.maxfilesperproc=65536
$ ulimit -n 65536 65536
```

### Build & Run Packaged JAR

1. Run `mvn package`

2. Execute `java -jar target/zendesk-tools-1.0.jar`

### License

Copyright 2018 Ghostery, Inc. All rights reserved.
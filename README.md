## Zendesk Migration Tool

Migrate tickets, users and Help Center content between Zendesk accounts.

### Configuration

1. Rename ConstantsSample to Constants and enter user credentials.

2. Import dependencies from Maven.

3. For MacOS, you'll need to bump the limit on the maximum number of open files (for using Ticket.getTickets())

```bash
$ echo kern.maxfiles=65536 | sudo tee -a /etc/sysctl.conf
$ echo kern.maxfilesperproc=65536 | sudo tee -a /etc/sysctl.conf
$ sudo sysctl -w kern.maxfiles=65536
$ sudo sysctl -w kern.maxfilesperproc=65536
$ ulimit -n 65536 65536
```

### Build & Run Packaged JAR

1. Run `mvn package`

2. Execute `java -jar target/zendesk-migration-1.0.jar`

### License

Copyright 2017 Ghostery, Inc. All rights reserved.

See https://www.ghostery.com/eula for license
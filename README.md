# irc-flock
A flock app to connect a flock channel with an irc channel

# Dependencies:
- Maven
- Java 8
- Redis (Optional)

### Arguments
- appId : appId of the flock app
- appSecret : appSecret of the flock app
- webhookToken : outgoing webhook token
- port : port on which events will be received
- ircHostName (Optional) : irc server host name (Default value=irc.freenode.net) 
- redisHostName (Optiona) : host name of the redis server, if you want to persist the auth details

# Build
```
$ mvn install
$ mvn package
$ mvn -DappId=<appId> -DappSecret=<appSecret> -DwebhookToken=<webhootToken> -Dport=<port> exec:java

# irc-flock
A flock app to connect a flock channel with an irc channel

# Steps
1. Create a new flock app from [here](https://dev.flock.com/apps/new)
2. Set the Event Listener URL as `https://<server domain>/event`
3. Register a new slash command `irc` and set it's action to `Send to event listener URL`
4. Take a note of the `App Id` and `App Secret` as these will be passed as params later on.
5. Create a new outgoing webhook from [here](https://admin.flock.com/#!/webhooks/add/outgoing)
6. Select your channel that you want link and set the Callback URL to `https://<server domain>/message`
7. Take a note of the `Token` generated here, as this will be passed as param later on.
8. Clone and run the project using [run instructions](#run), and pass all the requrired params.
9. After successfuly running the app follow [post running steps](#post-running-steps)

# Run
### Dependencies:
- Maven
- Java 8
- Redis (Optional)

### Arguments
- appId : appId of the flock app
- appSecret : appSecret of the flock app
- webhookToken : outgoing webhook token
- port : port on which events will be received
- ircHostName (Optional) : irc server host name (Default value=irc.freenode.net) 
- redisHostName (Optiona) : host name of the redis server, if you want to persist the auth details and user tokens

```
$ mvn install
$ mvn package
$ mvn -DappId=<appId> -DappSecret=<appSecret> -DwebhookToken=<webhootToken> -Dport=<port> exec:java
```

# Post Running Steps
1. Install the flock app that you just created.
2. Go to the channel run the slash command `/irc login <username> <password>` to login to irc
3. To connect flock channel with irc channel run `/irc setChannel #<irc channel name>`
4. Now flock channel is successfully linked with irc channel

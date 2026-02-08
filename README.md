# LocalhostBridgeCore

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://mit-license.org/)
[![BungeeCord](https://img.shields.io/badge/Requires-BungeeCord-green.svg)](https://mit-license.org/)
[![Spigot](https://img.shields.io/badge/Requires-Spigot-green.svg)](https://mit-license.org/)
[![Maven](https://img.shields.io/badge/Maven_Repository-CodeMC-orange.svg)](https://mit-license.org/)

## Project Description
> ~~A helper for communicating between BungeeCord and Bukkit, intended for developers' own servers and requires pre-deployment. Compared to PluginMessage, it may be easier to use, and most importantly, it can work without any proxy players.~~

Emm, I started planning to make a replacement tool as soon as I found out about the proxy player limit in PluginMessage, but I needed to transfer info between different servers to the bots on my social platforms anytime. That's why this tool is so rough — didn't want to bother with authentication, so I just stripped it down to only work on localhost.

But it's enough! It can solve some problems and make wrappers to provide more convenience for developers.

## Quick Start
> **Attention!** Before you start, you need to understand whether it fits your needs.
>
> This is a plugin that needs to be pre-deployed and requires full server support. It will not work at all if you just shade it.

### Adding API to your project

#### Maven
```xml
<dependency>
    <groupId>io.github.messycraft</groupId>
    <artifactId>lbc-api</artifactId>
    <version>1.0.3-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

#### Gradle
Groovy DSL:
```groovy
compileOnly 'io.github.messycraft:lbc-api:1.0.3-SNAPSHOT'
```
Kotlin DSL:
```kotlin
compileOnly("io.github.messycraft:lbc-api:1.0.3-SNAPSHOT")
```

### Obtaining an instance of the API
The root API interface is `LocalhostBridgeCoreAPI`. You need to obtain an instance of this interface in order to do anything.

Just invoke this:
```java
LocalhostBridgeCoreAPI api = LocalhostBridgeCoreAPIProvider.getAPI();
```
It can be used on BungeeCord or Bukkit platform with the

### Useful information

#### Send
All relative methods are in the instance of `LocalhostBridgeCoreAPI`. You can choose whether needing reply or not.

#### Monitor
Use the `api.getListenerManager()` to get an instance of `ListenerManager`, which contains the basic event-method such as `subscribe(args)` and `unsubscribe(args)`. 

The custom listener require to override abstract method of `ChannelListner`.

#### Callback
All of api operating can be invoked at primary thread.

#### Other
We will keep updating docs. Thanks for reading.

## Future
* Support Velocity platform. We don't have enough reasons to reject a young project.
* Support Folia-Paper. The work will update in next version soon.

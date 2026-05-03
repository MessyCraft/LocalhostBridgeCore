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
<repository>
    <id>messycraft-repo</id>
    <url>https://repo.codemc.io/repository/messycraft/</url>
</repository>
```
```xml
<dependency>
    <groupId>io.github.messycraft</groupId>
    <artifactId>lbc-api</artifactId>
    <version>{NEWEST_VERSION}</version>
    <scope>provided</scope>
</dependency>
```

#### Gradle
Groovy DSL:
```groovy
maven { url = 'https://repo.codemc.io/repository/messycraft/' }
```
```groovy
compileOnly 'io.github.messycraft:lbc-api:{NEWEST_VERSION}'
```
Kotlin DSL:
```kotlin
maven { url = uri("https://repo.codemc.io/repository/messycraft/") }
```
```kotlin
compileOnly("io.github.messycraft:lbc-api:{NEWEST_VERSION}")
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

## Features

### Plugin Updater
Centralized plugin management system for multi-server networks. Deploy configuration and JAR updates from BungeeCord to all Bukkit servers.

**Key Features:**
- **Hot Configuration Updates**: Push config changes without server restart (`/lbc updater push`)
- **JAR Management**: Deploy plugin updates with automatic restart (`/lbc updater reboot`)
- **Smart Sync**: SHA-256 comparison prevents unnecessary updates
- **Placeholder Support**: Auto-replace `$$LBC_SERVER_NAME$$` in configs per server
- **Backup System**: Automatic backups to `shared-path/backup/{server}/`
- **Selective Updates**: Per-server ignore lists and custom reload commands

**Setup:**
1. Configure `plugin-updater.shared-path` in BungeeCord's `config-bungee.yml`
2. Place plugins in `{shared-path}/repo/{PluginName}/` (folders for configs, JARs in root)
3. Edit `{shared-path}/lbc-updater-config.yml` to define access plugins and reload commands
4. Use `/lbc updater push [server]` or `/lbc updater reboot [server]` from BungeeCord console

## Changelog

### v1.1.1-SNAPSHOT (2026-05-03)
- **Fixed**: A data with "%" causes failures

### v1.1.0-SNAPSHOT (2026-03-18)
- **Added**: Plugin Updater (major update)
- **Added**: Centralized management and backup system
- **Fixed**: empty data sending error
- **Improved**: Modified namespace special character restrictions

## Future
* Support Velocity platform. We don't have enough reasons to reject a young project.
* Support Folia-Paper. The work will update in next version soon.

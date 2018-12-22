# CraftMGR
Minecraft game server management system.

## Features
* Platform independent. Runs on everything the JRE does.
* Web based UI
 * Realtime list of all servers
 * Realtime server console view
* Commandline interface
* TCP-Socket interface, for communication with your own application.
* All server versions supported, including:
 * CraftBukkit/Spigot
 * BungeeCord
 * Forge
* User-defined server templates (place server.jar or a *.zip archive containing server files into the templates directory)
* .ini config files
* BCrypt encrypted passwords

## Dependencies

Build:
- \>= commons-lang3-3.7
http://commons.apache.org/proper/commons-lang/download_lang.cgi
- \>= ini4j-0.5.4
https://sourceforge.net/projects/ini4j/files/ini4j-bin/0.5.4/ini4j-0.5.4-bin.zip/download
- \>= jbcrypt-0.4
https://github.com/jeremyh/jBCrypt/releases/tag/jbcrypt-0.4
- \>= nanohttpd-2.3.1
https://github.com/NanoHttpd/nanohttpd


Runtime:
- Java 8
- ```screen``` is not required to run :D

## Setup
1) Build or get CraftMGR.jar
2) Optional: For additional security, create system user to run it with. ```adduser minecraft```
3) Place CraftMGR.jar into desired directory on your server. e.G. on linux ```/home/minecraft```
4) Run it with ```java -jar CraftMGR.jar```. You may use ```screen``` or a init-script to run it in background
5) Connect via web browser to ```http://<Address of your server>:9000/```
Default password is: ```imnotgerman```
6) Enjoy

### TODOs:
- Command line interface translation (very sorryy for lack of this!)
- Password change trough UI
- HTTPS Support
- Documentation
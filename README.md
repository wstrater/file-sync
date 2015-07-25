file-sync
========================
[TOC]

# Overview

This is a utility consisting of a an HTTP server and client written in Java to synchronize files over HTTP. 
The obvious question is why write one since there some existing products out there such as [Crash Plan](http://www.code42.com/crashplan/), [JFileSync](http://sourceforge.net/projects/jfilesync/) or [BitTorrent Sync](https://www.getsync.com/).
I have even used [Fileconveyor](http://fileconveyor.org/) to sync to the cloud.
The answer is just as obvious to me. 
I like to code and this looked like a good chance to play with [Jersey](https://jersey.java.net/) and to write some code I could use.

# Design Decisions

Since this code was written to synchronize large files, images, using a slow computer, a Rasberry Pi, over a slow network, hotel WiFi, to another slow computer, another Raspbery Pi, compact size and speed are important.
(I will find out if I hit either of those two goals.)

The biggest concern for speed is transmitting the files in blocks using binary data.
JSON supports byte arrays but I believe they are Base64 encoded during transmission.
This would mean I would need 1.5 times as much "bandwidth" for a given file plus the extra processing for the encoding and decoding.

The most logical approach would be to use the body of the HTTP response and request for reading and writing binary data.
This meant that other data would need to be transmitted differently.
The directory paths and file names are appended to the end of the URI.
Flags and discrete values such as flags, offset and block size would be sent using request parameters.
Response status fields such as success, length and CRC are returned in the HTTP header.
A few requests such as directory listing return the output in a JSON object in the response body but still use the HTTP header for the status fields to be consistent.

# Time Zone

Since this project uses the last modified date of the file to decide if it should synchronize two copies of the same file, it is important that these time stamps be are correct.
The following approaches are taken to keep them accurate.

Both the server and client need to know there current time zone when they are started.
This can be specified using the `--time-zone` CLI parameter if Java can not detect the correct default time zone.
This will happen if the client crosses a time zone and the computer was not updated to reflect the correct value.

All time stamps are adjusted for raw offset and day light savings using the client's time zone when they are sent to the server.
The server re-adjusts the time stamp using it's own time zone when it receives the request.

The last modified of the file being written to is adjusted two day's earlier than the file being read.
This will ensure that if the synchronization fails before completion the partial copy will be older than the original.
The last modified of the file being written to is set to the correct value after the last block has been successfully written.

# Sub-Projects

This code is broken into 3 sub-projects; `file-sync-common`, `file-sync-server` and `file-sync-client`.

## file-sync-common

This project contains code that is common to both `file-sync-server` and `file-sync-client` but does not contain code dependent on `Jersey`.

Most of the base functionality of reading, writing, hashing and listing directories are in utility classes that can be shared by both the `file-sync-server` and `file-sync-client`.
These utilities are then wrapped with a facade that has a local and remote implementation.
This allows the client to operate on either a local or remote file system using the same code but different implementations.
An example would be the `com.wstrater.server.fileSync.common.utils.FileUtils` is wrapped by `com.wstrater.server.fileSync.common.file.BlockWriter` with a local implementation of  `com.wstrater.server.fileSync.common.file.BlockWriterLocalImpl` and a remote implementation of `com.wstrater.server.fileSync.client.BlockWriterRemoteImpl`.
The remote implementations are defined in the `file-sync-client` project.

## file-sync-server

This project contains an embedded Jersey server, `com.wstrater.server.fileSync.server.FileSyncServer`, and controllers.
The code in this project is rather straight forward.
The controllers receives a request such as to read or write a block and passes the request to a common utility class using a local facade.
The decisions are made by the client.

## file-sync-client

This is a command line utility, `com.wstrater.server.fileSync.client.FileSyncClient`, and is where all of the decisions are made.

### Synching

A sync is performed with the `--sync` command line option. There are three main classes that perform the synchronization; `com.wstrater.server.fileSync.client.Syncer`, `com.wstrater.server.fileSync.client.PlanMapper` and `com.wstrater.server.fileSync.client.PlanDecider`.

The `com.wstrater.server.fileSync.client.Syncer` class recursively steps through the local and remote directories.
It passes the two directory listings to the `com.wstrater.server.fileSync.client.PlanMapper` to build an ordered list of files to be processed.
It then steps through the list and processes the files accordingly.

The `com.wstrater.server.fileSync.client.PlanMapper` class compares the two directories and comes up with list of actions for the files to be processed using the `com.wstrater.server.fileSync.client.PlanDecider`.
The actions include `DeleteFileFromRemote`, `CopyFileToRemote`, `DeleteFileFromLocal`, `CopyFileToLocal`, `DeleteDirFromRemote`, `SyncLocalDirToRemote`, `DeleteDirFromLocal`, `SyncRemoteDirToLocal`, `Skip`.

The `com.wstrater.server.fileSync.client.PlanDecider` is the "brains" of the synchronization.
It uses where the copies of each file exists, (`Local`, `Remote`, `Both`), which copy is newer (`Local`, `Remote`, `Same`, `Different`), the mode of synchronization (`Local`, `Remote`, `Both`) and permissions, (`localDelete`, `localWrite`, `remoteDelete`, `remoteWrite`).
It returns an action that includes `CopyToLocal`, `DeleteFromLocal`, `CopyToRemote`, `DeleteFromRemote`, `Skip`.

### Testing

The `com.wstrater.server.fileSync.client.SyncerTest` is used to test the `com.wstrater.server.fileSync.client.Syncer` and as such an important unit test.
It creates two sub-directories in the temp directory and uses them for both the local and remote file systems.
It then uses a package protected constructor of the `com.wstrater.server.fileSync.client.Syncer` to wrap the "remote" interface around the remote directory on the local file system.
Once the `com.wstrater.server.fileSync.client.Syncer` is set up, it runs through 64 permutations of synchronization mode and file permissions.
It creates files in the local and remote file systems based on a list of setup rules for each of these permutations.
These setup rules include `LocalOnly`, `RemoteOnly`, `Same`, `NewerLocal`, `NewerRemote`, `Different`.
Once the setup for the permutation is done it runs the `com.wstrater.server.fileSync.client.Syncer`, hashes the directories, lists the directories and verifies the results.
A total of 288 unique conditions are tested.

The `com.wstrater.server.fileSync.client.SyncerRemoteClientTest` test class is an integration test that extends `com.wstrater.server.fileSync.client.SyncerTest` and uses a remote server for the `com.wstrater.server.fileSync.client.Syncer`.
You need to start `com.wstrater.server.fileSync.server.FileSyncServer` with the correct base direcotry, `--base-dir`, to match the remote directory created for the test.
The test verification is still done through the local file system so the time zones must match between the test and server.

### Plan Report

Sometimes it is nice do know what would happen when syncing without doing the sync.
The `--plan` command line option will use the same classes to come up with the plan, `com.wstrater.server.fileSync.client.PlanMapper` and `com.wstrater.server.fileSync.client.PlanDecider`, but will produce a report instead of acting on the plan.

The report is produced using [JMTE](https://code.google.com/p/jmte/) templating engine.
Currently there are two templates for producing HTML, `planTemplateHTML.jmte`, and CSV, `planTemplateCSV.jmte`.

The HTML file format is rather tolerant of extra white spaces including new lines.
The CSV file format is not as tolerant.
There are two approaches to producing a report without extra new lines.
You can write the entire template on one line without extra white spaces or you can remove extra white spaces later.
The `--plan-eol` command line parameter can be used to specify a literal in the generated report to be replaced with new line characters.
The current `planTemplateCSV.jmte` template uses `--plan-eol=[EOL]`.
All lines are trimmed and any new lines are removed by replacing `\s*\n\s*` with an empty string.
The text is then replaced with a `\n`.

### Hashing

Hashing can be used to compare the contents of local files to remote files.
This information is used if the files have the same time stamps and the files have been hashed.

Hashing generates a digest for each file is kicked off with the `--hash` command line parameter.
The results are stored in the index file and retrieved with the directory listing.
This is a slow process and is queued to run in the background by a single thread.
The client will wait for the local thread to finish before exiting but the remote thread could still be running.

# Security

Since `FileSyncServer` is designed to be accessable from the Internet, it is vital that it is secured.
There are three important steps to securing it.

- Limit the server's access.
- Secure the transmission.
- Authenticate the client
 
I have not heard of an embedded `Jetty` being hacked but that does not mean it hasn't happened or can't happen.
The first step in limiting the server's access is to create a new user with limited rights on the host system for running `FileSyncServer`.
Typically this user is the only member of a new group.

```
sudo groupadd fileSync
sudo useradd -g fileSync fileSync
sudo passwd fileSync
```

Running it on start up depends on the OS and is beyond my experties but here are some suggestions that might work if you don't have an OS specific way to configure a service, daemon or init script.
Just remember to make the process a daemon or run in the background or it will terminate when the terminal session ends.

```
sudo -u fileSync /path/to/app/fileSyncServer.sh
su fileSync -c "nohup /path/to/app/fileSyncServer.sh &"
su fileSync -c 'daemon /path/to/app/fileSyncServer.sh &> /dev/null &'
```

The next step in limiting the server's access is to set the base directory to a new directory that is only used by the server for file upload.
This means that important files can't be overridden or uploaded files given access because of where there were copied.

The next step to securing your server is to secure the transmission.
While you may not care if anyone can see the files you are transmitting, they should definitely not see your authentication or they could us it for replay.
Once someone else has access to your system, they can read, write and delete files.

Using SSL is the simplest way to secure your transmission.
SSL can be enabled on your server using the following command line properties.
The values used here are based on the samples created by the `createKeyStores.sh` script that creates key store and trust stores for both the client and server.

```
--ssl
--store-file=server-keystore.jks
--store-pass=OBF:1yta1t331v8w1v9q1t331ytc
--key-pass=OBF:1yta1t331v8w1v9q1t331ytc
```

The passwords are obsfucated using `org.eclipse.jetty.util.security.Password` or using the `--enc-pass` command line parameter on `FileSyncClient`.
The passwords are only obsfucated so they could be made more secure by storing them in a properties file in your home directory.
Just make sure you limit how can access them.

```
chmod 600 ~/fileSyncServer.properties
```

You then use the properties file with the following command line property.

```
--props /home/fileSync/fileSyncServer.properties
```

If you are using a self-signed certificate, you need to ensure that the `FileSyncClient` can trust it with the following command line properties based on generated samples.
The trust store contains the public key for `FileSyncServer`

```
--ssl
--trust-file=client-truststore.jks
--trust-pass=OBF:1yta1t331v8w1v9q1t331ytc
```

The next step to securing the server is to authenticate the client.
Currently there are two approaches, Basic Auth and Two Way SSL.

Basic Auth is a well understood concept in the REST world.
The `Jersey` client adds an encoded user name and password to each HTTP request and the `Jetty` server verifies it.
Here is how you specify the user name and password for the `FileSyncClient`.
Notice that I am also using SSL.
**It is vital that you use SSL when using Basic Auth since the user name and password are in the HTTP header.**

```
--ssl
--user-name=userName
--user-pass=OBF:1v2j1uum1xtv1zej1zer1xtn1uvk1v1v
```

The server needs to be given a list of acceptable users.
The following is an example of the command line parameters for `FileSyncServer`.

```
--ssl
--user-file=/home/fileSync/fileSyncUsers.properties
```

The users file contains user names and passwords so it should be readable only by the user running the server.

```
chmod 600 ~/fileSyncUsers.properties
```

The users file has the following format.
This example uses an implementation of Unix Crypt for the password.

```
userName:CRYPT:usjRS48E8ZADM,user
```

Two Way SSL is another approach to authenticating users.
It is similar exchanging keys for `SSH` instead of entering a user name and password.
Both the serer and the client have a key pair and the public keys are exchanged and validated during the SSL handshake.

The `FileSyncServer` is configured with the following command line parameters.
I have repeated the key store parameters from above but added a trust store.
The trust store contains the public key of every user that has access.

```
--ssl=TwoWay
--store-file=server-keystore.jks
--store-pass=OBF:1yta1t331v8w1v9q1t331ytc
--key-pass=OBF:1yta1t331v8w1v9q1t331ytc
--trust-file=server-truststore.jks
--trust-pass=OBF:1yta1t331v8w1v9q1t331ytc
```

The `--ssl` parameter has been changed to ensure that `Jetty` requires a client certificate.

The changes for `FileSyncClient` are similar.
I have added a key store to the command line properties.

```
--ssl
--store-file=client-keystore.jks
--store-pass=OBF:1yta1t331v8w1v9q1t331ytc
--key-pass=OBF:1yta1t331v8w1v9q1t331ytc
--trust-file=client-truststore.jks
--trust-pass=OBF:1yta1t331v8w1v9q1t331ytc
```

The `--ssl` parameter does not need to be changed for `Jersey` since the server
requests the client key.

You will notice that I have obsfucated all my sample passwords, `secret` and `password`, despite using trivial examples.
It is to emphasize that while obsfucation is not encryption, it is a lot harder for someone to memorize or understand an obsfucated password than a plan text password in case they happen to see it.
*Obsfucating a randomly generated password that is stored in a properties file tat only the user running the program can see is a good approach in the realm of the project.*

# To Do
- [ ] Restart. Need to be able to restart a file copy without copying the entire file. Have the basis of it in the IndexManager but that needs more work.
- [ ] Ignore Errors. Need to be able to ignore occasional errors. If there is an issue with one file, then move one. This program is meant to run all night or until it is done. Not fail two files in and waste precious time. Too many errors should fail the sync.
- [ ] Remote Permissions. Need to be able to query the serer for permissions.

<hr/>
Edited with [StackEdit](http://stackedit.io),   [Dillenger](http://dillinger.io/) or [MultiMarkdown](http://multimarkdown.com/).

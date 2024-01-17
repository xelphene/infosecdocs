
# DNS-based Java Deserialization Vulnerability Probe

## About

This code generates a super simple simple probe to test for the possible
presence of Java deserialization vulnerabilities.  The probe is a serialized
instance of the java.net.URL class with it's hashCode set to an invalid
value so that a DNS lookup of the hostname will be performed upon
deserialization.  Since the lookup is performed during the deserialization
process itself, the vulnerable code need not do anything in particular with
the payload object.

The same approach used by ysoserial's URLDNS module.  I mostly have this
here as a standalone PoC I can include in documents as needed.

## Useage

[PayloadGenerator](PayloadGenerator.java) creates the URL object, serializes
it to payload.ser

Compile PayloadGenerator and run it:
```
% javac javac PayloadGenerator.java
(ignore warnings)
% java --add-opens java.base/java.net=ALL-UNNAMED PayloadGenerator
```

There should now be a file named 'payload.ser' in the current directory.

[PayloadTester](PayloadTester.java) deserializes the payload for testing.

```
% javac PayloadTester.java
% java PayloadTester
```

You should see a lookup of the given hostname immediately upon running the
last command.

Send the contents of payload.ser to whatever it is that is deserializing
things and see if the same happens.  If the results are promising,
[ysoserial](https://github.com/frohoff/ysoserial) may help with
exploitation.


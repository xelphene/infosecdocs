
# Modifying Android Apps to Allow TLS Intercept with User CAs

## Introduction

Often when testing Android apps, one wants to gain visibility into HTTP
requests that the app makes in order to test the back-end services for
security vulnerabilities.  Naturally these days, this traffic is TLS
encrypted.  To enable yourself as a man-in-the-middle for your own device,
you can install custom Certificate Authorities and configure the device to
use an HTTP proxy just as you would a browser.

However, Android distinguishes between certificates installed by the user
and certificates that came with the operating system.  Apps can chose to
trust only the system certificates, and apps that target API level 24 and up
do this by default.  This article describes how to modify an app to make it
trust User CA certificates.

This article assumes:

* You have the TLS intercept proxy of your choice up and
  running (such as Burp Suite).

* Your Android device has the CA certificate that the proxy is using installed
  on the device as a User CA (search settings for
  Certificates).

* Your Android device is using the proxy (configured in the advanced
  settings for your WiFi connection) and the proxy can see TLS traffic
  through apps that will trust your CA by default (such as Chrome).

* The app you're working with is not doing certificate pinning in code.

## Extract the Original App Package

Extract the original app APK file using [apktool](https://ibotpeaches.github.io/Apktool/):

```
apktool d app.apk
```

This will unpack the APK file into the `app` subdirectory, where you'll find
the code and various resources for the app.

## Modify the App

Next, we'll modify AndroidManifest.xml to load a custom XML configuration
snippet that will enable trust for user CAs in the app.  Add an attribute
`android:networkSecurityConfig="@xml/network_security_config"` to the
`<application>` tag. Here's an example abbreviated AndroidManifest.xml:

```
<?xml version="1.0" encoding="utf-8" standalone="no"?><manifest ...>
	...
    <application ... android:networkSecurityConfig="@xml/network_security_config">
		...
    </application>
</manifest>
```

This will cause Android to include the XML configuration snippet at
app/res/xml/network_security_config.xml. Next we'll place the relevant configuration
there. Create the app/res/xml directory if it doesn't exist. Here's the
entirety of app/res/xml/network_security_config.xml:

```
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
</network-security-config>

```

This configuration will make the app trust user-installed CAs during TLS
connections.

If you'd like to make any other modifications to the app, now is the perfect
opportunity. We'll be repackaging the app next.

## Repackaging the APK

Build a new APK file incorporating your changes. Again, assuming the
app code modified above is in the `app/` subdirectory:

```
apktool b app -o app-modified.apk
```


The APK file needs to be signed, but any signature at all will work. This
guide will generate a new key and certificate for this purpose.

Generate a 1024 bit RSA keypair and store it in the `key` file using openssl:

```
openssl genrsa -out key 1024
```

Next, convert the key to PKCS#8 as required by APK. The key in PKCS#8 format
will be placed in `key.pkcs8`:

```
openssl pkcs8 -topk8 -in key -out key.pkcs8 -outform DER -nocrypt
```

Now generate a certificate, sign it with our key and store it in `cert.pem`:

```
openssl req -x509 -key key -out cert.pem -days 3650 -nodes -subj '/CN=example.com'
```

Next, zipalign the APK (`zipalign` is a part of Android Studio and can be found at
~/Android/Sdk/build-tools/<version> with Android Studio is installed):

```
zipalign 4 app-modified.apk app-modified-zipaligned.apk
```

Finally, sign the APK file with `apksigner` using the key and certificate we
generated above.  `apksigner` is a part of Android Studio and can be found
at ~/Android/Sdk/build-tools/<version> with Android Studio is installed.

```
apksigner sign --key key.pkcs8 --cert cert.pem --out app-modified-signed.apk
app-zipaligned.apk
```

`app-modified-signed.apk` is the app with your modifications ready for
installation. Copy it to your device and install it. If it is your first
time, you may need to grant app install permissions to your file manager app
(or whatever app you're initiating the installation from). If you were
successful, you should now see HTTPS traffic requested by the app in your
proxy.

## References

[Android apksigner Reference](https://developer.android.com/studio/command-line/apksigner)

[Changes to Trusted CAs](https://android-developers.googleblog.com/2016/07/changes-to-trusted-certificate.html)

[Manifest Security Config Reference](https://developer.android.com/training/articles/security-config)


## Configure the Proxy

You'll next want to set up a TLS intercept proxy on your workstation.  There
are a few options out there for such software.  I recommend Burp Suite. 
This is proprietary software but a free Community Version version is
available which will work great. Configuration and installation of the proxy
itself on your workstation is beyond the scope of this article but isn't too
difficult. See Burp's Getting Started Guide for help with that:
https://portswigger.net/support/getting-started-with-burp-suite.

A key item is that you'll need to configure Burp Suite to listen for
connections on all network interfaces (see the Proxy - Options tab in Burp
Suite). Also, you'll need to configure your workstation's firewall to allow
these connections. The network connections in question will come from your
Android device.

## Configure the Android Device

The manual proxy settings are somewhat buried in Android. Open Settings and
navigate to Network & internet -> Wi-Fi. Details for your particular Wi-Fi
network will be shown:

![Network Details][1_network_details]

Tap the pencil icon near the top right. This creates opens a dialog with
more options. Expand the "Advanced options" part and you'll finally see the
proxy settings:

![Proxy Settings][3_proxy_filled_in]

Fill in the information for the proxy service running on your workstation
and then save.

At this point you should be able to test that the proxy is configured
properly using Chrome on the Android device. Browse to any web site and you
should see your HTTP requests in the proxy (i.e. Burp Suite).

If that is working correctly.

[1_network_details]: 1_network_details.png "Network Details"
[2_advanced_options]: 2_advanced_options_expanded.png "Advance Options"
[3_proxy_filled_in]: 3_proxy_filled_in.png "Proxy Settings"

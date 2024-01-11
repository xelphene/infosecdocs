
# PAM pam_access Origin Restriction Bypass

## Introduction

Linux's PAM (Pluggable Authentication Modules) pam_access module can be
configured in /etc/security/access.conf to allow logins for certain users
only when coming from certain origins, such as a hostname, IP address, or
locally.  pam_access treats non-hostname origins such a TTY name or `LOCAL`
as a hostname to be looked up under certain conditions.

If someone can control the results of these DNS lookups, then such a person
can bypass access this control configuration.

## Example

The user `localadmin` should only be able to log in locally with the
following pam_access configuration at /etc/security/access.conf (shared by
`sshd`, `login` and more):

```
+:localadmin:LOCAL
-:localadmin:ALL
```

Login attempts as `localadmin` succeed on the console and fail via SSH as
expected, but pam_access is trying to look up `LOCAL` with DNS:

syslog:
```
Jan 10 23:54:47 server sshd[2051]: pam_access(sshd:account): cannot resolve hostname "LOCAL"
Jan 10 23:54:47 server sshd[2051]: pam_access(sshd:account): access denied for user `localadmin' from `10.0.0.33'
```

Packet capture, meanwhile:

```
admin@server:~$  tcpdump -i eth0 -n port 53
tcpdump: verbose output suppressed, use -v[v]... for full protocol decode
listening on wlp3s0, link-type EN10MB (Ethernet), snapshot length 262144 bytes
23:53:37.230335 IP 10.0.0.44.35353 > 10.0.0.2.53: 8066+ A? LOCAL.example.com. (35)
23:53:37.230927 IP 10.0.0.44.51002 > 10.0.0.2.53: 39403+ AAAA? LOCAL.example.com. (35)
```

To try a practical example, first logins fail as localadmin via SSH when
LOCAL.example.com can't be resolved:

```
user@clienthost:~$ dig +short LOCAL.example.com
user@clienthost:~$ ssh localadmin@10.0.0.44
localadmin@10.0.0.44's password:
Connection closed by 10.0.0.44 port 22
```

Trying again after adding LOCAL.example.com to DNS yields a successful login:

```
user@clienthost:~$ dig +short LOCAL.example.com
10.0.0.33
user@clienthost:~$ ssh localadmin@10.0.0.44
localadmin@10.0.0.44's password:
localadmin@server:~$ 
```

The same is true of TTY names in the origin field of pam_access's config file.

## History

- 2023-12-15: Reported to and confirmed by Ubuntu: https://bugs.launchpad.net/ubuntu/+source/pam/+bug/2046526

- 2024-01-11: Reported to the Linux-PAM project: https://github.com/linux-pam/linux-pam/issues/711

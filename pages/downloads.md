---
layout: page-fullwidth
title: "Downloads"
permalink: "/downloads/"
---

{% assign stable = (site.data.releases | where:"status", "stable" | first) %}

## WebAnno {{ stable.version }}

[Release notes](https://github.com/webanno/webanno/releases/tag/webanno-{{ stable.version }})

**If you upgrade an existing WebAnno installation, be sure to closely follow the upgrade instructions.**

### Standalone Version

Use this version if you want to use or try out WebAnno on your workstation. You download a runnable
JAR that can be started using a double-click in your file manager or on the command line using
`java -jar webanno-standalone-{{ stable.version }}.jar`. WebAnno will create a directory called
`.webanno` under your home directory and store its database and files there.

* [WebAnno {{ stable.version }} standalone (executable JAR)](https://github.com/webanno/webanno/releases/download/webanno-{{ stable.version }}/webanno-standalone-{{ stable.version }}.jar)
* [JAR installation instructions]({{ site.url }}/releases/{{ stable.version }}/docs/user-guide.html#sect_installation) 
* [JAR upgrade instructions]({{ site.url }}/releases/{{ stable.version }}/docs/user-guide.html#sect_upgrade) 

### Server Version

Use this version if you want to run WebAnno on a server. This version requires a Tomcat server and
should be used in conjunction with a MySQL database. Be sure to follow the installation guide
closely.

* [WebAnno {{ stable.version }} WAR-archive](https://github.com/webanno/webanno/releases/download/webanno-{{ stable.version }}/webanno-webapp-{{ stable.version }}.war)
* [WAR installation instructions]({{ site.url }}/releases/{{ stable.version }}/docs/admin-guide.html#sect_installation)
* [WAR upgrade instructions]({{ site.url }}/releases/{{ stable.version }}/docs/admin-guide.html#sect_upgrade)


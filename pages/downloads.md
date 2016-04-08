---
layout: page-fullwidth
title: "Downloads"
permalink: "/downloads/"
---

{% assign stable = (site.data.releases | where:"status", "stable" | first) %}

* [WebAnno {{ stable.version }} standalone (executable JAR)](https://github.com/webanno/webanno/releases/download/webanno-{{ stable.version }}/webanno-standalone-{{ stable.version }}.jar)
    * [JAR installation instructions]({{ site.url }}/releases/{{ stable.version }}/docs/user-guide.html#sect_installation) 
    * [JAR upgrade instructions]({{ site.url }}/releases/{{ stable.version }}/docs/user-guide.html#sect_upgrade) 
* [WebAnno {{ stable.version }} WAR-archive](https://github.com/webanno/webanno/releases/download/webanno-{{ stable.version }}/webanno-webapp-{{ stable.version }}.war)
    * [WAR installation instructions]({{ site.url }}/releases/{{ stable.version }}/docs/admin-guide.html#sect_installation)
    * [WAR upgrade instructions]({{ site.url }}/releases/{{ stable.version }}/docs/admin-guide.html#sect_upgrade)
* [WebAnno {{ stable.version }} release notes](https://github.com/webanno/webanno/releases/tag/webanno-{{ stable.version }})

**If you upgrade an existing WebAnno installation, be sure to closely follow the upgrade instructions.**
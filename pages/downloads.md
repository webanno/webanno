---
layout: page-fullwidth
title: "Downloads"
---

{% assign stable = (site.data.releases | where:"status", "stable" | first) %}

* [WebAnno {{ stable.version }} standalone (executable JAR)](https://bintray.com/artifact/download/webanno/downloads/webanno-standalone-{{ stable.version }}.jar) - [Installation instructions]({{ site.url }}/releases/{{ stable.version }}/docs/user-guide.html#sect_installation) 
* [WebAnno {{ stable.version }} WAR-archive](https://bintray.com/artifact/download/webanno/downloads/webanno-webapp-{{ stable.version }}.war) - [Installation instructions]({{ site.url }}/releases/{{ stable.version }}/docs/admin-guide.html#sect_installation)
* [SampleProjects Example projects]


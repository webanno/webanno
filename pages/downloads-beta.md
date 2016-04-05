---
layout: page-fullwidth
title: "Downloads (beta)"
permalink: "/downloads-beta/"
---

{% assign unstable = (site.data.releases | where:"status", "unstable" | first) %}
{% assign beta = (site.data.releases | where:"status", "beta" | first) %}

**THIS PAGE CONTAINS PRE-RELEASE VERSIONS. USE AT YOUR OWN RISK.**

**Do not use this software for serious work** - before its final release, WebAnno might change in ways incompatible with this prerelease.

* [WebAnno {{ beta.version }} standalone (executable JAR)](https://github.com/webanno/webanno/releases/download/webanno-{{ beta.version }}/webanno-standalone-{{ beta.version }}.jar)
    * [JAR installation instructions]({{ unstable.user_guide_url }}#sect_installation) 
    * [JAR upgrade instructions]({{ unstable.user_guide_url }}#sect_upgrade) 
* [WebAnno {{ beta.version }} WAR-archive](https://github.com/webanno/webanno/releases/download/webanno-{{ beta.version }}/webanno-webapp-{{ beta.version }}.war)
    * [WAR installation instructions]({{ unstable.admin_guide_url }}#sect_installation)
    * [WAR upgrade instructions]({{ unstable.admin_guide_url }}#sect_upgrade)
* [WebAnno {{ beta.version }} release notes](https://github.com/webanno/webanno/releases/tag/webanno-{{ beta.version }})

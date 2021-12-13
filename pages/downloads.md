---
layout: page-fullwidth
title: "Downloads"
permalink: "/downloads/"
---

{% assign stable = site.data.releases | where: "status", "stable" | first %}

<table width="100%">
<tr>
<td style="vertical-align: middle;">
<a href="https://inception-project.github.io"><img src="{{ site.url }}/images/logos/inception-banner.png"/></a>
</td>
<td>
<p style="text-align: center  ;">You like WebAnno?</p>
<p style="text-align: center  ;">
<b style="font-size: 150%;"><a href="https://inception-project.github.io">Use INCEpTION!</a></b>
</p>
<div>
INCEpTION is the new open source text annotation platform that has all the flexibility and many more exciting features including a completely new human-in-the-loop annotation assistance
support, the ability to search texts and annotations, support for RDF/SPARQL knowledge bases for
entity linking, and much more. <br/>
And best: it can import your WebAnno annotation<sup>1</sup> projects. <br/>
<span style="font-size: 75%">
<sup>1</sup> <i>Projects of type 'automation' or 'correction' are not supported.</i>
</span>
</div>
</td>
</tr>
</table>

## WebAnno {{ stable.version }}

[Release notes](https://github.com/webanno/webanno/releases/tag/webanno-{{ stable.version }})

**If you upgrade an existing WebAnno installation, be sure to closely follow the upgrade instructions.**

### Standalone Version

Use this version if you want to use or try out WebAnno on your workstation. You download a runnable
JAR that can be started using a double-click in your file manager or on the command line using
`java -jar webanno-webapp-{{ stable.version }}-standalone.jar`. WebAnno will create a directory called
`.webanno` under your home directory and store its database and files there.

* [WebAnno {{ stable.version }} standalone (executable JAR)](https://github.com/webanno/webanno/releases/download/webanno-{{ stable.version }}/webanno-webapp-{{ stable.version }}-standalone.jar) <github-downloads user='webanno' repo='webanno' tag='webanno-{{ stable.version }}' asset='webanno-webapp-{{ stable.version }}-standalone.jar' ></github-downloads>
* [JAR installation instructions]({{ site.url }}/releases/{{ stable.version }}/docs/user-guide.html#sect_installation) 
* [JAR upgrade instructions]({{ site.url }}/releases/{{ stable.version }}/docs/user-guide.html#sect_upgrade) 

### Server Version

Use this version if you want to run WebAnno on a server. This version requires a Tomcat server and
should be used in conjunction with a MySQL database. Be sure to follow the installation guide
closely.

* [WebAnno {{ stable.version }} WAR-archive](https://github.com/webanno/webanno/releases/download/webanno-{{ stable.version }}/webanno-webapp-{{ stable.version }}.war) <github-downloads user='webanno' repo='webanno' tag='webanno-{{ stable.version }}' asset='webanno-webapp-{{ stable.version }}.war' ></github-downloads>
* [WAR installation instructions]({{ site.url }}/releases/{{ stable.version }}/docs/admin-guide.html#sect_installation)
* [WAR upgrade instructions]({{ site.url }}/releases/{{ stable.version }}/docs/admin-guide.html#sect_upgrade)

{% include examples.md %}
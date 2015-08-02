---
layout: page-fullwidth
title: "Documentation"
permalink: "/documentation/"
---

{% assign stable = (site.data.releases | where:"status", "stable" | first) %}
{% assign unstable = (site.data.releases | where:"status", "unstable" | first) %}

## WebAnno {{ stable.version }}
_latest release_

* [User Guide]({{ site.url }}/releases/{{ stable.version }}/docs/user-guide.html)
* [Admin Guide]({{ site.url }}/releases/{{ stable.version }}/docs/admin-guide.html)

{% comment %}
----

#### WebAnno {{ unstable.version }}
_upcoming release - links may be temporarily broken while a build is in progress_

* [User Guide]({{ unstable.user_guide_url }})
* [Admin Guide]({{ unstable.admin_guide_url }})
* [Developer Guide]({{ unstable.developer_guide_url }})

{% endcomment %}

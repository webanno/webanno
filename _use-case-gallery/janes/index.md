---
title: JANES
subheadline: Manually annotated datasets of user-generated Slovene
permalink: /use-case-gallery/janes/
screenshot: screenshot.png
thumbnail: screenshot-thumb.png
hidden: false
---

**Source**: <i>This example was kindly contributed by <a href="http://nl.ijs.si/et/">Tomaž Erjavec</a>,
Department of Knowledge Technologies, Jožef Stefan Institute, Slovenia</i>

The [JANES](http://nl.ijs.si/janes/) project developed resources and tools for processing 
user-generated Slovene, such as tweets, comments on news articles, blogs etc. as well as 
investigated the linguistic behaviour of such data. As part of the project we also produced a 
number of manually annotated datasets that either served as training data for machine learning 
annotation tools or as the data for empirical linguistic investigations. 

The manually annotated data was produced with the help of WebAnno. The data is annotated in TEI, 
and substantial effort went into developing conversion from TEI to the WebAnno TSV format and 
then into automatic merging of the manual annotations in TSV with the original TEI files. The 
annotations and conversion was further complicated by the fact that we also wanted to enable 
manual corrections of tokenisation and word normalisations, where one original token be split 
or joined, and a token can be normalised to several standard ones or vice versa.


##### Publications

* Tomaž Erjavec, Špela Arhar Holdt, Jaka Čibej, Kaja Dobrovoljc, Darja Fišer, Cyprian 
  Laskowski and Katja Zupan. 2016. Annotating CLARIN.SI TEI corpora with WebAnno. <i>Proceedings of 
  the CLARIN Annual Conference 2016</i>, Aix-en-Provence.
  [[PDF](https://www.clarin.eu/sites/default/files/erjavec-etal-CLARIN2016_paper_17.pdf)]


##### Data

All the datasets are available under CC licences though the CLARIN.SI repository, in particular:

* Erjavec, Tomaž; Fišer, Darja; Čibej, Jaka; Arhar Holdt, Špela. 2016. 
  CMC training corpus Janes-Norm 1.2, Slovenian language resource repository CLARIN.SI, 
  [hdl.handle.net/11356/1084](http://hdl.handle.net/11356/1084)
* Erjavec, Tomaž; Fišer, Darja; Čibej, Jaka; Arhar Holdt, Špela; Ljubešić, Nikola; Zupan, Katja. 2017. 
  CMC training corpus Janes-Tag 2.0, Slovenian language resource repository CLARIN.SI, 
  [hdl.handle.net/11356/1123](http://hdl.handle.net/11356/1123")
* Arhar Holdt, Špela; Erjavec, Tomaž; Fišer, Darja, 2017, 
  CMC training corpus Janes-Syn 1.0, Slovenian language resource repository CLARIN.SI, 
  [hdl.handle.net/11356/1086](http://hdl.handle.net/11356/1086)
* Reher, Špela; Erjavec, Tomaž; Fišer, Darja, 2017, 
  Tweet code-switching corpus Janes-Preklop 1.0, Slovenian language resource repository CLARIN.SI, 
  [hdl.handle.net/11356/1154](http://hdl.handle.net/11356/1154)
* Popič, Damjan; Zupan, Katja; Logar, Polona; Kavčič, Teja; Erjavec, Tomaž; Fišer, Darja. 2017. 
  Tweet comma corpus Janes-Vejica 1.0, Slovenian language resource repository CLARIN.SI, 
  [hdl.handle.net/11356/1088](http://hdl.handle.net/11356/1088)
* Goli, Teja; Osrajnik, Eneja; Fišer, Darja and Erjavec, Tomaž. 2017. 
  CMC shortening corpus Janes-Kratko 1.0, Slovenian language resource repository CLARIN.SI, 
  [hdl.handle.net/11356/1087](http://hdl.handle.net/11356/1087)

I Installation of webanno war file
1. If you have an old installation
	1.1 Export all of your old projects -- Go to Projects-->Export/Import-->Export the whole project to a file system
	1.2 Copy also manually the database and the webanno directory (so that you will be safe in case importing of these project fails later)
		1.2.1 Export database - mysqldum webanno > webanno.sql
		1.2.2 Export the directory /srv/webanno/ to another location
	1.3 Once you finished copying the projects/database.
		1.3.1 drop the database 
			1.3.1.1 mysql -u DBUSERNAME -p
			1.3.1.2 drop database webanno
		1.3.2 Remove the webanno directory from /srv/webanno/repository
2. From this point on, proceed with the installation instruction given here: https://code.google.com/p/webanno/wiki/InstallationGuide
3. Create required users
4. Import exported projects in as done at 1.1 above, if any.

II Installing the standalone version
1. If you have an old standalone installation (WebAnno 1.1.0)
	1.1 Remove the Winstone temporary directory 
		1.1.2 On Linux, run: rm -R /tmp/winstoneEmbeddedWAR
		1.1.2 on Mac Ox run:  rm -R $TMPDIR/winstoneEmbeddedWAR
2. Follow the installation instruction here: https://code.google.com/p/webanno/wiki/WebAnnoStandalone
# searchengine

Search engine implementation. This program:
* parses web-sites
* gets lemmas from pages
* saves indexation of lemmas to the DB(mysql)
* gives the ability to search text amoung the parsed sites.

# Tech info
Backend: Java 8+, mysql(docker container), Spring boot 2.7.1, apache.lucene.morphology for text processing, jsoup for web parsing.
Frontend: javascript+html

# how to run:

## Start up DB with docker or local:
docker run --name some-mysql_new -p 3306:3306 -e MYSQL_ROOT_PASSWORD=pwd -d mysql

* download jar file.
* download application.yaml
* configure password for database in **application.yaml**

run jar from command line: **java -jar SearchEngine-1.0.jar**

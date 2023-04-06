FROM tomcat:latest
EXPOSE 8080
RUN rm -frv /usr/local/tomcat/webapps/*
ADD target/*.war /usr/local/tomcat/webapps/
CMD ["catalina.sh", "run"]

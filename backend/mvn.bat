@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.4.1"
set "M2_HOME=C:\Users\SAI KIRAN\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0"
"%M2_HOME%\bin\mvn.cmd" %*

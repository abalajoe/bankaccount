:: Bank Account App Script
:: @joeabala

:: Set Java path, edit to your path
set path="C:\Program Files (x86)\Java\jdk1.7.0_51\bin"

:: Weather file and Jar file
set conf_file=config.properties
set log_file=log4j.properties
set jar=bankaccount.jar

:: Run App
java -Dlog4j.configuration=file:%log_file% -Dbankaccount.config=%wconf_file% -jar %jar%
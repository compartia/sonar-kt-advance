export SONAR_DEV_MODE=TRUE
export SONAR_DEV_MODE_DIR=/Users/artem/work/KestrelTechnology/sonar-kt-advance-plugin/src/main/resources

cwd=$(pwd)
jarstem=sonar-kt-advance-plugin
jarname=$jarstem-0.jar
mvn clean sass:update-stylesheets
mvn package

cd $SONARQUBE_HOME/bin/macosx-universal-64
sh sonar.sh stop
rm $SONARQUBE_HOME/extensions/plugins/$jarstem*.jar

echo -------
echo COPYING $cwd/target/$jarname INTO $SONARQUBE_HOME/extensions/plugins/
cp $cwd/target/$jarname $SONARQUBE_HOME/extensions/plugins/

cd $SONARQUBE_HOME/bin/macosx-universal-64
sh sonar.sh start

cd $cwd

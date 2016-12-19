echo "*** restoring sonar db ***"
set -e
pg_restore -d sonar -U sonar docker-entrypoint-initdb.d/sonar.dump

# echo "wait-script is here"

set -e

host="$1"

echo host

shift
cmd="$@"

echo cmd

until PGPASSWORD=sonar psql -h "$host" -U "sonar" -c '\l'; do
  >&2 echo "Postgres is unavailable - sleeping"
  sleep 1
done

>&2 echo "Postgres is up - executing command"
exec $cmd

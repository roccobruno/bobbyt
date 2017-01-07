#!/bin/bash


docker run -d --name d2b -v ~/couchbase/node:/opt/couchbase/var -p 8091:8091 -p 8092:8092 -p 8093:8093 -p 11210:11210 couchbase/server

while true
do
  STATUS=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8091/pools)
  if [ $STATUS -eq 200 ]; then
    echo "Got 200! All done!"
    break
  else
    echo "Got $STATUS :( Not done yet..."
  fi
  sleep 10
done


echo "starting setting up db"
echo ""
echo "Memory settings"
echo ""
curl -u Administrator:Administrator -X POST  http://localhost:8091/pools/default -d 'memoryQuota=500' -d 'indexMemoryQuota=269'

echo "DB Services settings"

curl -u Administrator:Administrator -v -X POST http://localhost:8091/node/controller/setupServices -d 'services=kv%2Cn1ql%2Cindex'
echo ""
echo "Indexes settings"
curl -u Administrator:Administrator -X POST  http://localhost:8091/settings/indexes -d 'storageMode=forestdb'
echo ""
echo "Web db interface settings"
curl -u Administrator:Administrator -X POST  http://localhost:8091/settings/web -d 'password=Administrator&username=Administrator&port=SAME'
echo ""
echo "Buckets settings"
curl -u Administrator:Administrator -X POST  http://localhost:8091/pools/default/buckets -d 'ramQuotaMB=100&name=bobbit&replicaNumber=1&authType=none&proxyPort=13001'
curl -u Administrator:Administrator -X POST  http://localhost:8091/pools/default/buckets -d 'ramQuotaMB=100&name=default&replicaNumber=1&authType=none&proxyPort=13002'
curl -u Administrator:Administrator -X POST  http://localhost:8091/pools/default/buckets -d 'ramQuotaMB=100&name=tube&replicaNumber=1&authType=none&proxyPort=13003'
echo ""
echo "Done!"

sleep 60


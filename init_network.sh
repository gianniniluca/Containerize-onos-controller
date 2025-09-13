#! /bin/bash
pip install netconf-console2
pip3 install six

echo "Configuring etsi qkd EMULATED node identifiers..."
./configure-qkd-nodes.sh
sleep 2

echo "Posting etsi EMULATED qkd nodes..."
/root/onos/bin/onos-netcfg localhost ./quancom_qkd_devices.json
sleep 2

echo "Posting emulated fiber links..."
/root/onos/bin/onos-netcfg localhost ./quancom_two_links.json
sleep 2

echo "Setting the node tipe to QKD... node 1"

curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' 'http://localhost:8181/onos/quantum-app/nodes/postNode?deviceId=netconf%3A172.25.0.101%3A830'

echo "Setting the node tipe to QKD...node 2"

curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' 'http://localhost:8181/onos/quantum-app/nodes/postNode?deviceId=netconf%3A172.25.0.102%3A830'

echo "Setting the node tipe to QKD...node 3"

curl -u karaf:karaf -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' 'http://localhost:8181/onos/quantum-app/nodes/postNode?deviceId=netconf%3A172.25.0.103%3A830'

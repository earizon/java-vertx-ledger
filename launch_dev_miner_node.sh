#!/bin/bash

# REF: https://github.com/ethereum/go-ethereum/wiki/Private-network

# NOTE: The allocated accounts with balance must match those of the
#       java code. In the test-code we have something similar to:
#       |   final String ownerPrivKey = "1", // Address: 0x7e5f4552091a69125d5dfcb7b8c2659029395bdf
#       |                user1PrivKey = "2"; // Address: 0x2b5ad5c4795c026514f8317c7a215e218dccd6cf
CHAIN_ID=1500

DIR_L="tmp/ethash tmp/ethereum tmp/js"

for DIR in $DIR_L ; do
    if [ ! -d $DIR ] ; then mkdir -p $DIR ; fi
    sudo chcon -Rt svirt_sandbox_file_t ${DIR} # Fix SElinux problems in RedHat/CentOS
done

# PUB_KEY4PRIV_KEY* MUST match keystore_test/*
PUB_KEY4PRIV_KEY_1="ec84a1d40430aaeed7733823a145e57d91430357"

cat << EOF >tmp/ethereum/genesis.json
{
    "config": {
        "chainId": ${CHAIN_ID},
        "homesteadBlock": 0,
        "eip155Block": 0,
        "eip158Block": 0
    },
    "difficulty": "0x400",
    "gasLimit": "2100000",
    "alloc": {
        "${PUB_KEY4PRIV_KEY_1}": { "balance": "100000000" }
    }
}
EOF
sudo cp keystore_tests/* tmp/ethereum/keystore/

cat << EOF >/dev/null
     # REF: https://github.com/ethereum/go-ethereum/wiki/Private-network
     To create a database that uses this genesis block, run the following command. 
     This will import and set the canonical genesis block for your chain.
         $ geth --datadir path/to/custom/data/folder init genesis.json
     Future runs of geth on this data directory will use the genesis block you have defined.
         $ geth --datadir path/to/custom/data/folder --networkid 15
EOF

DOCKER_CMD=""
DOCKER_CMD="$DOCKER_CMD  sudo docker run    --name ethereum "
DOCKER_CMD="$DOCKER_CMD              -v ${PWD}/tmp/ethash:/root/.ethash "
DOCKER_CMD="$DOCKER_CMD              -v ${PWD}/tmp/ethereum:/root/.ethereum "
DOCKER_CMD="$DOCKER_CMD              -v ${PWD}/tmp/js:/tmp/.js "
DOCKER_CMD="$DOCKER_CMD              -p 8545:8545 "
DOCKER_CMD="$DOCKER_CMD              -p 30303:30303 "
DOCKER_CMD="$DOCKER_CMD              --rm "

DOCKER_CMD_IMG=""
DOCKER_CMD_IMG="$DOCKER_CMD_IMG  lwieske/ethereumcore:geth-1.5 "

printf '1234\012' > tmp/ethereum/password01
GETH_MINING_OPTS=""
GETH_MINING_OPTS="${GETH_MINING_OPTS} --mine "
GETH_MINING_OPTS="${GETH_MINING_OPTS} --fakepow " # disable proof-of-work
GETH_MINING_OPTS="${GETH_MINING_OPTS} --minerthreads=1 "
GETH_MINING_OPTS="${GETH_MINING_OPTS}  --etherbase=0x${PUB_KEY4PRIV_KEY_1}"

GETH_OPTS=""
GETH_OPTS="$GETH_OPTS --verbosity 3 "
GETH_OPTS="$GETH_OPTS --maxpeers 0 " # 0 => network disabled
GETH_OPTS="$GETH_OPTS --nodiscover "
GETH_OPTS="$GETH_OPTS --dev "
GETH_OPTS="$GETH_OPTS --rpc  --ws "
# GETH_OPTS="$GETH_OPTS --rpcapi eth,web3 "
GETH_OPTS="$GETH_OPTS --rpcaddr 0.0.0.0 "
GETH_OPTS="$GETH_OPTS -rpccorsdomain \"*\" "
GETH_OPTS="$GETH_OPTS --networkid ${CHAIN_ID} "
GETH_OPTS="$GETH_OPTS --unlock 0x$PUB_KEY4PRIV_KEY_1 "
GETH_OPTS="$GETH_OPTS --password /root/.ethereum/password01"
GETH_OPTS="$GETH_OPTS --datadir /root/.ethereum/ "

# GETH_OPTS="$GETH_OPTS --unlock 0 "


GETH_OPTS="$GETH_OPTS $GETH_MINING_OPTS "

# STEP 1: Init genesis block.
####cat << EOF 
####Execute next command inside docker to initialize from the genesion.json: 
####    # geth --dev --networkid ${CHAIN_ID} --datadir /root/.ethereum/ init /root/.ethereum/genesis.json
####Execute next command to create/import new accounts: 
####    # geth --dev --networkid ${CHAIN_ID} --datadir /root/.ethereum/ account list 
####    # geth --dev --networkid ${CHAIN_ID} --datadir /root/.ethereum/ account new
####    # geth --dev --networkid ${CHAIN_ID} --datadir /root/.ethereum/ account update
####    # geth --dev --networkid ${CHAIN_ID} --datadir /root/.ethereum/ account import .../key.prv
####EOF
####$DOCKER_CMD  -ti --entrypoint "/bin/bash" $DOCKER_CMD_IMG 

# STEP 2: Launch miner server:
echo "COINBASE: 0x$PUB_KEY4PRIV_KEY_1 "
  $DOCKER_CMD  -ti                          $DOCKER_CMD_IMG $GETH_OPTS # console

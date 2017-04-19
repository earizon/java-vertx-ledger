DOMAIN="ledger1"
SUBJ="/C=ES/ST=Aragon/L=Zaragoza/O=Everis/CN=${DOMAIN}" 
DAYS_TO_EXPIRE="365"

openssl req -newkey rsa:2048 -nodes -keyout ${DOMAIN}.key \
    -x509 -days ${DAYS_TO_EXPIRE} -out ${DOMAIN}.crt -subj "${SUBJ}" \
    -x509: create self-signed cert



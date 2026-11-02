#!/bin/bash
set -euo pipefail

# Publica um evento no tópico do produtor, simulando o produtor real (filtro por type + raw delivery)
# Variáveis: TYPE e TOPIC (obrigatórias), SERVICE_ORDER_ID, EVENT_ID e PAYLOAD (opcionais, PAYLOAD padrão {})

# O uuidgen não existe no Git Bash do Windows; nesse caso o UUID sai do /dev/urandom
new_uuid() {
  if command -v uuidgen > /dev/null; then
    uuidgen
  else
    od -An -N16 -tx1 /dev/urandom | tr -d ' \n' | sed -E 's/(.{8})(.{4})(.{4})(.{4})(.{12})/\1-\2-\3-\4-\5/'
  fi
}

LOCALSTACK_CONTAINER=${LOCALSTACK_CONTAINER:-ms-ofisy-stock-local-localstack}
TYPE=${TYPE:?Informe TYPE=<tipo do evento>}
TOPIC=${TOPIC:?Informe TOPIC=<nome do tópico>}
TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:$TOPIC

EVENT_ID=${EVENT_ID:-$(new_uuid)}
SERVICE_ORDER_ID=${SERVICE_ORDER_ID:-$(new_uuid)}
PAYLOAD=${PAYLOAD:-"{}"}

MESSAGE=$(cat <<EOF
{"eventId":"$EVENT_ID","type":"$TYPE","serviceOrderId":"$SERVICE_ORDER_ID","occurredAt":"$(date -u +%Y-%m-%dT%H:%M:%SZ)","payload":$PAYLOAD}
EOF
)

docker exec "$LOCALSTACK_CONTAINER" awslocal sns publish \
  --topic-arn "$TOPIC_ARN" \
  --message-group-id "$SERVICE_ORDER_ID" \
  --message-deduplication-id "$(new_uuid)" \
  --message "$MESSAGE" > /dev/null

echo "Evento $TYPE publicado em $TOPIC"
echo "  EVENT_ID=$EVENT_ID"
echo "  SERVICE_ORDER_ID=$SERVICE_ORDER_ID"
echo
echo "Ver as movimentações da OS: make db-movements SERVICE_ORDER_ID=$SERVICE_ORDER_ID"

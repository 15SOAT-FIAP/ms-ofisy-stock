#!/bin/bash
set -euo pipefail

# Publica um evento no tópico do produtor, simulando o produtor real (filtro por type + raw delivery)
# Variáveis: TYPE e TOPIC (obrigatórias), SERVICE_ORDER_ID, EVENT_ID e PAYLOAD (opcionais, PAYLOAD padrão {})

LOCALSTACK_CONTAINER=${LOCALSTACK_CONTAINER:-ms-ofisy-stock-local-localstack}
TYPE=${TYPE:?Informe TYPE=<tipo do evento>}
TOPIC=${TOPIC:?Informe TOPIC=<nome do tópico>}
TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:$TOPIC

EVENT_ID=${EVENT_ID:-$(uuidgen)}
SERVICE_ORDER_ID=${SERVICE_ORDER_ID:-$(uuidgen)}
PAYLOAD=${PAYLOAD:-"{}"}

MESSAGE=$(cat <<EOF
{"eventId":"$EVENT_ID","type":"$TYPE","serviceOrderId":"$SERVICE_ORDER_ID","occurredAt":"$(date -u +%Y-%m-%dT%H:%M:%SZ)","payload":$PAYLOAD}
EOF
)

docker exec "$LOCALSTACK_CONTAINER" awslocal sns publish \
  --topic-arn "$TOPIC_ARN" \
  --message-group-id "$SERVICE_ORDER_ID" \
  --message-deduplication-id "$(uuidgen)" \
  --message "$MESSAGE" > /dev/null

echo "Evento $TYPE publicado em $TOPIC"
echo "  EVENT_ID=$EVENT_ID"
echo "  SERVICE_ORDER_ID=$SERVICE_ORDER_ID"
echo
echo "Ver as movimentações da OS: make db-movements SERVICE_ORDER_ID=$SERVICE_ORDER_ID"

#!/bin/bash
set -euo pipefail

# Fila de entrada do estoque, com a DLQ

DLQ_URL=$(awslocal sqs create-queue \
  --queue-name ofisy-stock-dlq.fifo \
  --attributes FifoQueue=true \
  --query QueueUrl --output text)

DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$DLQ_URL" \
  --attribute-names QueueArn \
  --query Attributes.QueueArn --output text)

QUEUE_URL=$(awslocal sqs create-queue \
  --queue-name ofisy-stock.fifo \
  --attributes "{\"FifoQueue\":\"true\",\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"}" \
  --query QueueUrl --output text)

QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$QUEUE_URL" \
  --attribute-names QueueArn \
  --query Attributes.QueueArn --output text)

# Na AWS o tópico de eventos da OS pertence ao core; aqui ele é criado só para exercitar o fluxo localmente

SERVICE_ORDER_TOPIC_ARN=$(awslocal sns create-topic \
  --name ofisy-service-order-events.fifo \
  --attributes FifoTopic=true \
  --query TopicArn --output text)

awslocal sns subscribe \
  --topic-arn "$SERVICE_ORDER_TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$QUEUE_ARN" \
  --attributes '{"RawMessageDelivery":"true","FilterPolicyScope":"MessageBody","FilterPolicy":"{\"type\":[\"ServiceOrderCancelled\"]}"}'

# Tópico de eventos publicados por este ms. A fila de inspeção existe só localmente, para ver o que foi publicado

STOCK_TOPIC_ARN=$(awslocal sns create-topic \
  --name ofisy-stock-events.fifo \
  --attributes FifoTopic=true \
  --query TopicArn --output text)

INSPECT_QUEUE_URL=$(awslocal sqs create-queue \
  --queue-name ofisy-stock-events-inspect.fifo \
  --attributes FifoQueue=true \
  --query QueueUrl --output text)

INSPECT_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$INSPECT_QUEUE_URL" \
  --attribute-names QueueArn \
  --query Attributes.QueueArn --output text)

awslocal sns subscribe \
  --topic-arn "$STOCK_TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$INSPECT_QUEUE_ARN" \
  --attributes '{"RawMessageDelivery":"true"}'

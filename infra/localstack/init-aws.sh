#!/usr/bin/env bash
# Mantem a infraestrutura local deterministica em Linux e Docker Desktop.
set -euo pipefail

region="us-east-1"
account_id="000000000000"
topic_name="nota-fiscal-gerada"
topic_arn="arn:aws:sns:${region}:${account_id}:${topic_name}"

awslocal sns create-topic --name "${topic_name}" >/dev/null

for consumer in estoque registro entrega financeiro; do
  queue_name="nota-fiscal-${consumer}"
  dlq_name="${queue_name}-dlq"

  dlq_url="$(awslocal sqs create-queue --queue-name "${dlq_name}" --query QueueUrl --output text)"
  dlq_arn="$(awslocal sqs get-queue-attributes --queue-url "${dlq_url}" --attribute-names QueueArn --query Attributes.QueueArn --output text)"

  redrive_policy="{\"deadLetterTargetArn\":\"${dlq_arn}\",\"maxReceiveCount\":\"3\"}"
  queue_attributes="$(python3 -c 'import json, sys; print(json.dumps({"RedrivePolicy": sys.argv[1]}))' "${redrive_policy}")"
  queue_url="$(awslocal sqs create-queue --queue-name "${queue_name}" --attributes "${queue_attributes}" --query QueueUrl --output text)"
  queue_arn="$(awslocal sqs get-queue-attributes --queue-url "${queue_url}" --attribute-names QueueArn --query Attributes.QueueArn --output text)"

  policy="{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"sns.amazonaws.com\"},\"Action\":\"sqs:SendMessage\",\"Resource\":\"${queue_arn}\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"${topic_arn}\"}}}]}"
  policy_attributes="$(python3 -c 'import json, sys; print(json.dumps({"Policy": sys.argv[1]}))' "${policy}")"
  awslocal sqs set-queue-attributes --queue-url "${queue_url}" --attributes "${policy_attributes}"
  awslocal sns subscribe \
    --topic-arn "${topic_arn}" \
    --protocol sqs \
    --notification-endpoint "${queue_arn}" \
    --attributes RawMessageDelivery=true >/dev/null
done

# Valor local propositalmente ficticio. Segredos reais nunca pertencem ao repositorio.
awslocal secretsmanager create-secret \
  --name case-nota-fiscal/local \
  --secret-string '{"example":"local-only"}' >/dev/null

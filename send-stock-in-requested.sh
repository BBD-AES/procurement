#!/usr/bin/env bash
# procurement.stock-in-requested 토픽에 메시지 1건 발행
# 사용: ./send-stock-in-requested.sh [브로커] [PO번호] [payload.json]
set -euo pipefail

BROKER="${1:-kafka.inwoohub.com:9092}"
KEY="${2:-PO-2026-000001}"
PAYLOAD_FILE="${3:-stock-in-requested.sample.json}"
TOPIC="procurement.stock-in-requested"

# JSON을 한 줄로 압축 (jq 있으면 jq, 없으면 python)
if command -v jq >/dev/null 2>&1; then
  VALUE=$(jq -c . "$PAYLOAD_FILE")
else
  VALUE=$(python3 -c "import json,sys;print(json.dumps(json.load(open(sys.argv[1]))))" "$PAYLOAD_FILE")
fi

# key.separator 와 충돌 없도록 탭(\t)을 구분자로 사용
printf '%s\t%s\n' "$KEY" "$VALUE" | kafka-console-producer.sh \
  --bootstrap-server "$BROKER" \
  --topic "$TOPIC" \
  --property "parse.key=true" \
  --property "key.separator=	"

echo "sent -> topic=$TOPIC key=$KEY"

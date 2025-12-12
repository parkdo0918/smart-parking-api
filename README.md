# 🚗 Smart Parking API

Azure AI 기반 스마트 주차장 실시간 관리 시스템

## 기술 스택

- **Backend**: Spring Boot 3.2, Java 17
- **Database**: Azure SQL Database
- **Cache**: Azure Cache for Redis
- **Storage**: Azure Blob Storage
- **AI**: Azure Computer Vision

## API 명세

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/health` | GET | 서버 상태 확인 |
| `/api/entry` | POST | 차량 입차 (이미지 업로드) |
| `/api/exit` | POST | 차량 출차 |
| `/api/parking/status` | GET | 실시간 주차 현황 |
| `/api/parking/history` | GET | 출입 기록 조회 |
| `/api/parking/current` | GET | 현재 주차 중인 차량 |

## 환경 변수

```bash
DB_PASSWORD=your_db_password
REDIS_PASSWORD=your_redis_password
AZURE_STORAGE_CONNECTION_STRING=your_storage_connection_string
AZURE_CV_KEY=your_computer_vision_key
```

## 로컬 실행

```bash
./gradlew bootRun
```

## Docker 실행

```bash
docker build -t smart-parking-api .
docker run -p 8080:8080 \
  -e DB_PASSWORD=xxx \
  -e REDIS_PASSWORD=xxx \
  -e AZURE_STORAGE_CONNECTION_STRING=xxx \
  -e AZURE_CV_KEY=xxx \
  smart-parking-api
```

## 아키텍처

```
[CCTV Image] → [Blob Storage] → [Computer Vision] → [SQL Database]
                                                    ↓
[Client] ← [Application Gateway] ← [VM Scale Set] ← [Redis Cache]
```
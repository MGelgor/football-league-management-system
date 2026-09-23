# Futbol Ligi Yönetim ve Simülasyon Sistemi

Takım yönetimi, fikstür oluşturma, maç simülasyonu ve puan durumu takibi yapan bir lig yönetim sistemi (PoC).
Geliştirme planı için [GELISTIRME_PLANI.md](GELISTIRME_PLANI.md) dosyasına bakınız.

## Stack

- **Backend:** Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, springdoc-openapi (Swagger)
- **Frontend:** React, TypeScript, Vite
- **Veritabanı:** PostgreSQL (Docker Compose ile)

## Kurulum

### 1. Veritabanı

```bash
docker compose up -d
```

Bu komut `localhost:5432` üzerinde `football_league` adlı bir PostgreSQL veritabanını ayağa kaldırır
(kullanıcı: `football_league`, şifre: `football_league`).

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

- Uygulama: http://localhost:5173

## Proje Yapısı

```
.
├── backend/    # Spring Boot REST API (katmanlı mimari: controller → service → repository → entity)
├── frontend/   # React + TypeScript SPA
└── docker-compose.yml   # Yerel PostgreSQL servisi
```

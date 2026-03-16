# ERP_HRM

ERP_HRM la du an Spring Boot quan ly quy trinh tuyen dung, su dung PostgreSQL lam co so du lieu va Docker de dong goi moi truong chay.

## Cong nghe

- Java 21
- Spring Boot 4.0.3
- Maven Wrapper
- PostgreSQL 15
- Docker, Docker Compose

## Chay local bang Docker

### 1. Tao file moi truong

```powershell
Copy-Item .env.example .env
```

Vi du file `.env`:

```env
POSTGRES_DB=erp_hrm
POSTGRES_USER=admin
POSTGRES_PASSWORD=0000
POSTGRES_PORT=5432
APP_PORT=8080
```

### 2. Khoi dong app va database

```powershell
docker compose up --build -d
```

### 3. Kiem tra

```powershell
docker compose ps
docker compose logs -f postgres_db
docker compose logs -f spring_app
```

### 4. Truy cap

- App: <http://localhost:8080>
- PostgreSQL: localhost:5432

## Ket noi pgAdmin

Thong so ket noi:

- Host: localhost
- Port: 5432
- Database: erp_hrm
- Username: admin
- Password: 0000

Query test:

```sql
select current_user, current_database();
```

## Chay local khong Docker

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/erp_hrm"
$env:SPRING_DATASOURCE_USERNAME="admin"
$env:SPRING_DATASOURCE_PASSWORD="0000"
.\mvnw.cmd spring-boot:run
```

Hoac build jar:

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\erp-hrm-0.0.1-SNAPSHOT.jar
```

## File quan trong

- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `CONTRIBUTING.md`

## Lenh thuong dung

```powershell
docker compose up --build -d
docker compose down
docker compose down -v
docker compose logs -f spring_app
docker compose logs -f postgres_db
.\mvnw.cmd -DskipTests package
```

## Workflow team

Xem quy uoc branch va PR tai `CONTRIBUTING.md`.

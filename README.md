# ERP_HRM

ERP_HRM la du an Spring Boot quan ly quy trinh tuyen dung, su dung PostgreSQL lam co so du lieu va Docker de dong goi moi truong chay.

## Cong nghe

- Java 21
- Spring Boot 4.0.3
- Maven Wrapper
- PostgreSQL 15
- Docker, Docker Compose
- GitHub Actions
- Render

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

## CI/CD voi GitHub Actions va Render

Repo da duoc cau hinh san:

- CI workflow: `.github/workflows/ci.yml`
- CD workflow: `.github/workflows/deploy-render.yml`
- Render blueprint: `render.yaml`

### CI

Chay khi push/PR vao `develop` va `main`, va build bang:

```bash
./mvnw -DskipTests package
```

### CD

Trigger deploy Render khi CI thanh cong tren `main` (hoac chay tay workflow dispatch).

Ban can tao GitHub Secret:

- Name: `RENDER_DEPLOY_HOOK_URL`
- Value: Deploy Hook URL tu Render service

## Huong dan Render

### 1. Tao service bang Blueprint

1. Vao Render dashboard.
2. Chon New -> Blueprint.
3. Chon repo `qtuwn/ERP_HRM`.
4. Render doc `render.yaml` va tao:
- Web service `erp-hrm`
- Postgres `erp-hrm-db`

### 2. Tao Deploy Hook

1. Mo service `erp-hrm`.
2. Vao Settings.
3. Tim Deploy Hook va tao hook moi.
4. Copy URL.

### 3. Them GitHub secret

1. Vao GitHub repo -> Settings -> Secrets and variables -> Actions.
2. Tao secret `RENDER_DEPLOY_HOOK_URL` voi gia tri la hook URL vua copy.

### 4. Deploy

- Merge code vao `main`.
- GitHub Actions se trigger deploy Render.

## File quan trong

- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `render.yaml`
- `.github/workflows/ci.yml`
- `.github/workflows/deploy-render.yml`
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

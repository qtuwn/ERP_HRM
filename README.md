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

- Host: `localhost`
- Port: `5432`
- Database: `erp_hrm`
- Username: `admin`
- Password: `0000`

Query test:

```sql
select current_user, current_database();
```

## Chay local khong Docker

PowerShell:

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
- Render Blueprint: `render.yaml`

### CI

Workflow CI se chay khi:

- push len `develop`
- push len `main`
- tao pull request vao `develop`
- tao pull request vao `main`

CI hien tai build jar bang lenh:

```bash
./mvnw -DskipTests package
```

### CD

Workflow deploy se chay khi:

- CI tren nhanh `main` thanh cong
- hoac ban chay tay bang `workflow_dispatch`

CD su dung Render Deploy Hook, vi vay ban can tao secret tren GitHub.

## Cach deploy len Render

### 1. Tao PostgreSQL va Web Service tren Render bang Blueprint

Trong Render:

1. Chon New +
2. Chon Blueprint
3. Ket noi repo GitHub `qtuwn/ERP_HRM`
4. Render se doc file `render.yaml`
5. Xac nhan tao:
   - Web Service `erp-hrm`
   - PostgreSQL `erp-hrm-db`

### 2. Lay Deploy Hook tren Render

Sau khi Web Service duoc tao:

1. Mo service `erp-hrm`
2. Vao Settings
3. Tim muc Deploy Hook
4. Tao 1 deploy hook moi
5. Copy URL hook

### 3. Them GitHub Secret

Trong GitHub repo:

1. Vao Settings
2. Secrets and variables -> Actions
3. New repository secret
4. Tao secret:
   - Name: `RENDER_DEPLOY_HOOK_URL`
   - Value: URL deploy hook vua copy tu Render

### 4. Luong deploy de nghi

- Lam viec tren `feature/*`
- Tao PR vao `develop`
- Test va review tren `develop`
- Merge `develop` vao `main`
- GitHub Actions tu dong trigger deploy len Render

## Bien moi truong production

Render Blueprint da map san cac bien sau tu PostgreSQL managed:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Ung dung cung da cau hinh `server.port=${PORT:8080}`, nen Render co the cap port dong luc.

## File quan trong

- `Dockerfile`: build va chay app
- `docker-compose.yml`: moi truong local app + postgres
- `.env.example`: mau bien moi truong local
- `render.yaml`: khai bao dich vu Render
- `.github/workflows/ci.yml`: pipeline build
- `.github/workflows/deploy-render.yml`: pipeline deploy
- `CONTRIBUTING.md`: quy uoc branch va PR cho team

## Lenh thuong dung

```powershell
docker compose up --build -d
docker compose down
docker compose down -v
docker compose logs -f spring_app
docker compose logs -f postgres_db
.\mvnw.cmd -DskipTests package
```

## Loi thuong gap

### Doi user/password trong `.env` nhung pgAdmin khong dang nhap duoc

Nguyen nhan: volume PostgreSQL cu van giu user ban dau.

Xu ly nhanh:

```powershell
docker compose down -v
docker compose up --build -d
```

### Render deploy thanh cong nhung app khong len

Can kiem tra:

- service co dung `Dockerfile`
- GitHub secret `RENDER_DEPLOY_HOOK_URL` da dung chua
- log cua service tren Render
- datasource da duoc Render map tu database chua

## Workflow team

Xem huong dan branch va PR tai `CONTRIBUTING.md`.

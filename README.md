# ERP_HRM

He thong ERP Recruitment Management cho VTHR Solutions, xay dung bang Spring Boot + PostgreSQL.

## 1. Cong nghe su dung

- Java 21
- Spring Boot 4.0.3
- Maven Wrapper (mvnw, mvnw.cmd)
- PostgreSQL 15
- Docker, Docker Compose

## 2. Yeu cau moi truong

Can cai dat it nhat mot trong hai cach chay:

### Cach A: Chay local

- JDK 21
- PostgreSQL (neu khong dung Docker cho DB)

### Cach B: Chay bang Docker (khuyen nghi)

- Docker Desktop
- Docker Compose (di kem Docker Desktop)

## 3. Cau truc cau hinh quan trong

- `src/main/resources/application.properties`: cau hinh datasource theo bien moi truong `SPRING_DATASOURCE_*`
- `.env`: bien moi truong cho Docker Compose (khong commit)
- `.env.example`: file mau de copy tao `.env`
- `docker-compose.yml`: khoi tao app va PostgreSQL
- `Dockerfile`: build app Spring Boot va dong goi image

## 4. Cai dat va chay du an bang Docker (de nhat)

### Buoc 1: Tao file .env

Copy file mau:

```powershell
Copy-Item .env.example .env
```

Mo file `.env` va dien thong tin, vi du:

```env
POSTGRES_DB=erp_hrm
POSTGRES_USER=admin
POSTGRES_PASSWORD=0000
POSTGRES_PORT=5432
APP_PORT=8080
```

### Buoc 2: Build va chay

```powershell
docker compose up --build -d
```

### Buoc 3: Kiem tra trang thai

```powershell
docker compose ps
docker compose logs -f postgres_db
docker compose logs -f spring_app
```

### Buoc 4: Truy cap

- App: http://localhost:8080
- PostgreSQL: localhost:5432

## 5. Ket noi PostgreSQL bang pgAdmin

Trong pgAdmin: Register -> Server

- Name: ERP_HRM (tu dat)
- Host name/address: localhost
- Port: 5432
- Maintenance database: erp_hrm
- Username: admin
- Password: 0000

Query test:

```sql
select current_user, current_database();
```

## 6. Chay local khong Docker (tuy chon)

### Buoc 1: Dat bien moi truong datasource

PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/erp_hrm"
$env:SPRING_DATASOURCE_USERNAME="admin"
$env:SPRING_DATASOURCE_PASSWORD="0000"
```

### Buoc 2: Chay app

```powershell
.\mvnw.cmd spring-boot:run
```

Hoac build jar:

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\erp-hrm-0.0.1-SNAPSHOT.jar
```

## 7. Lenh Docker thuong dung

```powershell
docker compose up --build
docker compose up --build -d
docker compose down
docker compose down -v
docker compose logs -f spring_app
docker compose logs -f postgres_db
```

Luu y:

- `docker compose down` chi dung container, khong xoa du lieu DB.
- `docker compose down -v` xoa ca volume DB (mat du lieu).

## 8. Loi thuong gap va cach xu ly

### 8.1 Sai password PostgreSQL trong pgAdmin

Neu doi `POSTGRES_USER` hoac `POSTGRES_PASSWORD` trong `.env` sau khi DB da duoc tao truoc do, container se van dung user cu trong volume.

Cach nhanh nhat:

```powershell
docker compose down -v
docker compose up --build -d
```

### 8.2 Port 5432 hoac 8080 dang bi chiem

Doi gia tri `POSTGRES_PORT` hoac `APP_PORT` trong `.env`, sau do chay lai:

```powershell
docker compose up --build -d
```

### 8.3 Kiem tra app da ket noi DB chua

Xem log app:

```powershell
docker compose logs -f spring_app
```

Neu thay thong tin HikariPool start completed va PostgreSQL JDBC URL thi ket noi thanh cong.

## 9. Quy trinh nhanh cho team

- Nhanh goc: `main` (on dinh), `develop` (tich hop), `feature/*`, `fix/*`
- Xem chi tiet tai file `CONTRIBUTING.md`

## 10. Tai lieu tham khao

- Spring Boot: https://docs.spring.io/spring-boot/4.0.3/reference/
- Maven: https://maven.apache.org/guides/index.html
- PostgreSQL: https://www.postgresql.org/docs/
- Docker Compose: https://docs.docker.com/compose/

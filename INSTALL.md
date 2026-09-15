# INSTALL.md — วิธีติดตั้งและรัน Khao Chalak Outdoor Navigation (Milestone 0)

คู่มือนี้เขียนสำหรับ **Windows** (เพราะไฟล์งานอยู่ที่ `D:\Claude Workspace\khao-chalak-nav\`)
มี 2 วิธีให้เลือก: **วิธี A (Docker Compose)** ง่ายสุด รันทีเดียวได้ทั้งระบบ
หรือ **วิธี B (รันเอง)** ถ้ายังไม่อยากติดตั้ง Docker

---

## 0. เตรียมเครื่อง (ทำครั้งเดียว)

ต้องมีโปรแกรมพวกนี้ก่อน:

| โปรแกรม | ใช้ตรวจสอบว่ามีหรือยัง | ดาวน์โหลด |
|---|---|---|
| Node.js 20 ขึ้นไป | เปิด PowerShell พิมพ์ `node -v` | https://nodejs.org (เลือก LTS) |
| Python 3.11 ขึ้นไป | พิมพ์ `python --version` | https://www.python.org/downloads/ (ตอนติดตั้งติ๊ก "Add python.exe to PATH" ด้วย) |
| Docker Desktop (จำเป็นเฉพาะวิธี A) | พิมพ์ `docker --version` | https://www.docker.com/products/docker-desktop/ |

ถ้าพิมพ์แล้วมันบอก "not recognized" แปลว่ายังไม่ได้ติดตั้ง หรือติดตั้งแล้วแต่ยังไม่ได้ปิดเปิด PowerShell ใหม่ (ต้องเปิดหน้าต่างใหม่หลังติดตั้งเสมอ)

## 1. แตกไฟล์ zip

1. ไปที่ `D:\Claude Workspace\khao-chalak-nav\`
2. คลิกขวาที่ `khao-chalak-nav-milestone0.zip` → **Extract All...** → เลือกปลายทางเป็น `D:\Claude Workspace\khao-chalak-nav\` (จะได้โฟลเดอร์ `khao-chalak-nav\` ซ้อนอยู่ข้างใน ก็ปกติ ให้ใช้ตัวที่แตกออกมา)
3. เปิด PowerShell แล้ว `cd` เข้าไปในโฟลเดอร์ที่แตกออกมา เช่น:
   ```powershell
   cd "D:\Claude Workspace\khao-chalak-nav\khao-chalak-nav"
   ```

---

## วิธี A — Docker Compose (แนะนำ ง่ายสุด)

### A.1 เปิด Docker Desktop
เปิดโปรแกรม Docker Desktop รอจนไอคอนที่มุมล่างขวาจอ (system tray) ขึ้นเป็นสีเขียว/สถานะ "Running" (ครั้งแรกอาจใช้เวลา 1-2 นาที และอาจต้องให้เปิดใช้ WSL2 ถ้ายังไม่เคยเปิด — ทำตามที่ Docker Desktop แนะนำได้เลย)

### A.2 คัดลอกไฟล์ environment
```powershell
copy .env.example .env
```
ไฟล์ `.env` นี้เก็บค่า config สำหรับเครื่องเรา (พอร์ต, รหัสฐานข้อมูล dev) — **ไม่ต้องแก้อะไร** ก็รันได้เลย เพราะเป็นค่า default สำหรับ dev เท่านั้น

### A.3 สั่งรัน
```powershell
docker compose up --build
```
ครั้งแรกจะช้าหน่อย (ต้องดาวน์โหลด image ของ Postgres/PostGIS และติดตั้ง dependency ของ frontend/backend) รอจนเห็น log ของทั้ง 3 service (`khaochalak-db`, `khaochalak-backend`, `khaochalak-frontend`) นิ่งๆ ไม่ error

### A.4 เข้าใช้งาน
เปิดเบราว์เซอร์ไปที่:
- แผนที่ (frontend): http://localhost:3000
- เอกสาร API (backend): http://localhost:8000/docs
- เช็คสถานะ backend: http://localhost:8000/health → ควรเห็น `{"status":"ok",...}`

### A.5 ปิดระบบ
กด `Ctrl+C` ในหน้าต่าง PowerShell ที่รันอยู่ แล้วพิมพ์:
```powershell
docker compose down
```

---

## วิธี B — รันเอง (ไม่ใช้ Docker)

ต้องเปิด PowerShell **2 หน้าต่าง** พร้อมกัน (หนึ่งไว้รัน backend อีกหนึ่งไว้รัน frontend)

### B.1 รัน Backend (หน้าต่างที่ 1)
```powershell
cd "D:\Claude Workspace\khao-chalak-nav\khao-chalak-nav\backend"
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```
> ถ้า `.venv\Scripts\activate` ขึ้น error เรื่อง "execution policy" ให้เปิด PowerShell แบบ **Run as Administrator** แล้วพิมพ์ครั้งเดียว:
> ```powershell
> Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
> ```
> แล้วค่อยกลับมารัน `.venv\Scripts\activate` ใหม่

เช็คว่า backend ขึ้นจริง: เปิดเบราว์เซอร์ไป http://localhost:8000/health ควรเห็น `{"status":"ok",...}`

### B.2 รัน Frontend (หน้าต่างที่ 2 — เปิดใหม่ อย่าปิดหน้าต่างแรก)
```powershell
cd "D:\Claude Workspace\khao-chalak-nav\khao-chalak-nav\frontend"
copy .env.example .env
npm install
npm run dev
```
พอเสร็จจะมีลิงก์ขึ้นมาในหน้าจอ ประมาณ `http://localhost:5173` — เปิดตามนั้นได้เลย (ถ้ารันแบบนี้ตรงๆ จะเป็นพอร์ต 5173 ไม่ใช่ 3000 เหมือนวิธี Docker)

### B.3 ปิดระบบ
กด `Ctrl+C` ในแต่ละหน้าต่าง PowerShell

หมายเหตุ: วิธี B ยังไม่มีฐานข้อมูล PostGIS ให้ (ตอนนี้ backend ยังไม่ได้ต่อ database จริง เพิ่งจะเริ่มต่อใน Milestone 5) เพราะฉะนั้น Milestone 0 นี้ใช้วิธี B ทดสอบแผนที่ + API `/health` ได้ครบแล้ว

---

## ปัญหาที่เจอบ่อย

| อาการ | สาเหตุ/วิธีแก้ |
|---|---|
| `docker compose up` ค้างที่ "Waiting for db to be healthy" | รอเพิ่ม (ครั้งแรก Postgres ใช้เวลา init) ถ้าเกิน 2-3 นาทีให้ `docker compose down` แล้ว `docker compose up --build` ใหม่ |
| พอร์ต 3000/8000/5432 ถูกใช้อยู่แล้ว | แก้เลขพอร์ตในไฟล์ `.env` (เช่น `FRONTEND_PORT=3001`) แล้วรันใหม่ |
| `npm install` error เรื่อง permission | อย่ารันใน `D:\` ที่เป็น cloud sync (OneDrive ฯลฯ) ถ้าเป็นไปได้ให้ย้ายโปรเจกต์ไปโฟลเดอร์ธรรมดาก่อน |
| หน้าเว็บขึ้น "API: offline" | backend ยังไม่ได้รัน หรือรันคนละพอร์ตกับที่ frontend คาดไว้ — เช็คไฟล์ `frontend/.env` ว่า `VITE_API_BASE_URL` ตรงกับพอร์ตที่ backend รันอยู่จริง |
| แผนที่ไม่ขึ้น / ขึ้นข้อความ error สีแดงบนแผนที่ | ส่วนใหญ่เพราะเน็ตเข้าไม่ถึง `demotiles.maplibre.org` (ต้องมีอินเทอร์เน็ต เพราะ Milestone 0 ยังไม่มี basemap แบบ offline) |

ติดตรงไหนแจ้งได้เลยครับ จะได้ช่วยดู log ต่อ

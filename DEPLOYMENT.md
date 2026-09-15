# DEPLOYMENT.md — เอาขึ้นออนไลน์ให้คนอื่นใช้ (Milestone 0)

สโคปของคู่มือนี้: เอา **Milestone 0** (skeleton — แผนที่ placeholder + backend `/health`)
ขึ้นออนไลน์ ฟรีทั้งหมด ให้คนอื่นเข้าเว็บมาดูได้จริง ยังไม่มี trail/GPS/routing จริง
(อันนั้นมาทีหลังตาม Roadmap) แต่โครงสร้าง deploy ที่วางไว้นี้ใช้ต่อได้ทุก Milestone

**ตัวเลือกที่ใช้:** Backend → **Render** (free) · Frontend → **Cloudflare Pages** (free) · โค้ด → **GitHub**

---

## ทำไมต้องผ่าน GitHub ก่อน

ทั้ง Render และ Cloudflare Pages ทำงานแบบ "เชื่อมกับ GitHub repo แล้ว deploy อัตโนมัติทุกครั้งที่ push"
เพราะฉะนั้นขั้นแรกคือเอาโค้ดขึ้น GitHub ก่อน

## ขั้นที่ 1 — Push โค้ดขึ้น GitHub

1. เข้า https://github.com/new สร้าง repo ใหม่ ชื่อ เช่น `khao-chalak-nav` เลือก **Private** หรือ **Public** ก็ได้ (ถ้าจะให้คนนอกเห็นโค้ดด้วยค่อยเปลี่ยนเป็น Public) **ไม่ต้อง** ติ๊ก "Add a README" (เพราะเรามีไฟล์อยู่แล้ว)
2. เปิด PowerShell ที่โฟลเดอร์โปรเจกต์ (ที่แตกจาก zip แล้ว):
   ```powershell
   cd "D:\Claude Workspace\khao-chalak-nav\khao-chalak-nav"
   git init
   git add .
   git commit -m "Milestone 0 — project foundation"
   git branch -M main
   git remote add origin https://github.com/<YOUR-GITHUB-USERNAME>/khao-chalak-nav.git
   git push -u origin main
   ```
   แทน `<YOUR-GITHUB-USERNAME>` ด้วย username จริงของพี่ (ถ้า GitHub ขอ login ให้ทำตามที่มันแนะนำ — ปกติจะเด้งเบราว์เซอร์ให้ authorize)
3. เช็คว่า push ผ่าน โดยรีเฟรชหน้า repo บน GitHub แล้วเห็นไฟล์ครบ (README.md, frontend/, backend/ ฯลฯ)

> **สำคัญ:** ไฟล์ `.env` (ถ้ามีจากตอนทดสอบในเครื่อง) จะไม่ถูก push เพราะอยู่ใน `.gitignore` แล้ว — อย่าไปแก้ `.gitignore` เอาออกเด็ดขาด เพราะนั่นคือสิ่งที่กันไม่ให้ความลับหลุดขึ้น GitHub

---

## ขั้นที่ 2 — Deploy Backend บน Render

1. สมัคร/ล็อกอิน https://render.com (สมัครด้วยบัญชี GitHub ได้เลย จะเชื่อม repo ง่ายกว่า)
2. กด **New +** → **Blueprint**
3. เลือก repo `khao-chalak-nav` ที่เพิ่ง push ไป — Render จะเจอไฟล์ `render.yaml` ที่เตรียมไว้ให้แล้วอัตโนมัติ (มันนิยาม service ชื่อ `khaochalak-backend` ให้)
4. กด **Apply** รอ build เสร็จ (ครั้งแรกประมาณ 2-5 นาที)
5. เสร็จแล้วจะได้ URL ประมาณ `https://khaochalak-backend.onrender.com`
6. ทดสอบ: เปิด `https://khaochalak-backend.onrender.com/health` ต้องเห็น `{"status":"ok",...}`

### ข้อจำกัดของ Render free tier (สำคัญ ต้องรู้ไว้)
- **Sleep:** ถ้าไม่มีคนเข้าเว็บ 15 นาที service จะ "หลับ" พอมีคนเข้าใหม่ต้องรอ ~1 นาทีให้มันตื่น (ปกติสำหรับ demo/MVP แต่ถ้าอยากให้ตื่นตลอดต้องอัปเป็นแผนเสียเงิน)
- **ชั่วโมงฟรี:** 750 ชั่วโมง/เดือน/workspace (พอสำหรับ service เดียวรันทั้งเดือนสบายๆ)
- ตอนนี้ **ยังไม่ได้ผูกฐานข้อมูล** เพราะ backend ยังไม่ใช้ database จริงจนกว่าจะถึง Milestone 5 — เจตนาไม่สร้าง Postgres ตอนนี้เพราะ Postgres free ของ Render **หมดอายุใน 30 วันแล้วลบข้อมูลถ้าไม่อัปเกรด** ถ้าสร้างไว้ตอนนี้จะเสียเปล่า พอถึง Milestone 5 ค่อยตัดสินใจอีกที (ดูหัวข้อ "แผนสำหรับฐานข้อมูล" ด้านล่าง)

---

## ขั้นที่ 3 — Deploy Frontend บน Cloudflare Pages

1. สมัคร/ล็อกอิน https://dash.cloudflare.com
2. ไปที่ **Workers & Pages** → **Create** → **Pages** → **Connect to Git**
3. เลือก repo `khao-chalak-nav` → ตั้งค่า build ดังนี้:
   | ช่อง | ค่าที่ใส่ |
   |---|---|
   | Root directory | `frontend` |
   | Build command | `npm run build` |
   | Build output directory | `dist` |
4. เพิ่ม Environment variable: `VITE_API_BASE_URL` = URL backend จาก Render ที่ได้ในขั้นที่ 2 (เช่น `https://khaochalak-backend.onrender.com`) — **ห้ามมี `/` ปิดท้าย**
5. กด **Save and Deploy** รอ build (~1-2 นาที)
6. จะได้ URL ฟรีประมาณ `https://khao-chalak-nav.pages.dev` — นี่คือลิงก์ที่ส่งให้คนอื่นเข้าได้เลย

## ขั้นที่ 4 — เปิดประตูให้ frontend คุยกับ backend ได้ (CORS)

พอรู้ URL จริงของ Cloudflare Pages แล้ว (เช่น `https://khao-chalak-nav.pages.dev`) กลับไปที่ Render:
1. เข้า service `khaochalak-backend` → **Environment**
2. แก้ค่า `CORS_ALLOW_ORIGINS` ให้เป็น URL จริงของ Cloudflare Pages (คั่นด้วย comma ได้ถ้ามีหลาย URL เช่น preview URL ด้วย)
3. Save — Render จะ redeploy ให้เอง
4. เปิด URL ของ Cloudflare Pages แล้วเช็คว่า badge "API: online" ขึ้นสีเขียว (ถ้ายังขึ้น "API: offline" ให้รอ redeploy เสร็จแล้วลอง hard refresh)

---

## เช็คให้ครบก่อนบอกว่า "ใช้งานได้แล้ว"

- [ ] เปิด `https://<ชื่อโปรเจกต์>.pages.dev` จากมือถือ/เครื่องอื่นได้ (ลองส่งลิงก์ให้เพื่อนเปิดดูจริง)
- [ ] badge บนหน้าเว็บขึ้น "API: online"
- [ ] `https://khaochalak-backend.onrender.com/health` ตอบ 200
- [ ] ลองปิดเว็บทิ้งไว้ 20 นาทีแล้วเปิดใหม่ — ต้องเห็น backend ตื่นขึ้นมาเอง (แค่ช้าไปแป๊บนึง ไม่ error)

## แผนสำหรับฐานข้อมูล (ใช้ตอน Milestone 5)

ตอนถึง Milestone 5 (Spatial Database) ต้องมี PostgreSQL+PostGIS จริงออนไลน์ มี 2 ทางเลือก:
1. **Render Postgres** — ตั้งง่ายสุดเพราะอยู่ workspace เดียวกับ backend แต่ free tier **หมดอายุ 30 วัน** (มี grace period อีก 14 วันก่อนลบจริง) เหมาะกับ dev/testing ระยะสั้นเท่านั้น ถ้าจะใช้จริงต้องอัปเกรดเป็นแผนเสียเงิน (เดือนละไม่กี่ร้อยบาท)
2. **Supabase free tier** — ไม่หมดอายุตามเวลา แต่ pause โปรเจกต์ถ้าไม่มีการใช้งานเกิน 1 สัปดาห์ (กด resume ได้ฟรี ข้อมูลไม่หาย) รองรับ PostGIS extension มีให้ 500MB storage ฟรี เหมาะกับ MVP ที่ยังไม่มีคนใช้เยอะ

จะเลือกทางไหนค่อยตัดสินใจตอนเริ่ม Milestone 5 ก็ได้ครับ ไม่กระทบ Milestone 0-4

## ปัญหาที่เจอบ่อยตอน deploy

| อาการ | สาเหตุ/วิธีแก้ |
|---|---|
| Cloudflare build fail ตรง `npm run build` | เช็คว่าตั้ง Root directory เป็น `frontend` จริง ไม่ใช่ root ของ repo |
| หน้าเว็บขึ้น "API: offline" ตลอด | เช็ค `VITE_API_BASE_URL` ใน Cloudflare ต้องตรงกับ URL ของ Render เป๊ะๆ (https, ไม่มี `/` ท้าย) แล้วเช็ค `CORS_ALLOW_ORIGINS` ฝั่ง Render ว่าใส่ URL Cloudflare ถูกหรือยัง |
| Render backend ตอบช้ามากตอนเข้าครั้งแรก | ปกติ (free tier sleep 15 นาทีแล้วตื่นช้า ~1 นาที) ไม่ใช่ bug |
| Push ขึ้น GitHub แล้วไม่มีไฟล์ `node_modules` — ปกติไหม | ปกติครับ ตั้งใจไม่ push เพราะ Render/Cloudflare จะ `npm install` ให้เองตอน build |

ติดตรงไหนบอกได้เลยครับ จะช่วยดู log การ deploy ต่อ

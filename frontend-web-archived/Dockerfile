# Milestone 0 dev image. A separate multi-stage build (Vite build ->
# static file server) is introduced when deployment is tackled
# (see DEPLOYMENT.md, later milestone).
FROM node:20-slim

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

EXPOSE 5173

CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]

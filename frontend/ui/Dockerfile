# ---------- build stage ----------
    FROM node:20-alpine AS build
    WORKDIR /app
    
    COPY package*.json ./
    RUN npm ci
    
    COPY . .
    
    ARG BUILD_CONFIGURATION=production
    RUN npm run build -- --configuration ${BUILD_CONFIGURATION}
    
    # ---------- runtime stage ----------
    FROM nginx:1.25-alpine
    
    COPY nginx.conf /etc/nginx/conf.d/default.conf
    
    ARG APP_NAME=ui
    COPY --from=build /app/dist/${APP_NAME}/browser /usr/share/nginx/html
    
    EXPOSE 80
# ---- Stage 1: builder ----
# Imagen con JDK completo: solo se usa para preparar el artefacto, no llega al runtime.
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# El JAR ya viene compilado por el pipeline CD (stage 'Build JAR').
COPY target/*.jar app.jar

# ---- Stage 2: runtime ----
# JRE slim: sin compilador ni herramientas de build -> imagen mucho más pequeña.
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia únicamente el artefacto desde el stage builder (descarta capas del JDK).
COPY --from=builder /build/app.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java","-jar","app.jar"]
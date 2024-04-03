FROM openjdk:17-slim-bullseye AS dev
COPY . /app
WORKDIR /app
EXPOSE 1100
EXPOSE 1099
RUN javac -cp lib/jsoup-1.17.2.jar *.java
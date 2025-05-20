docker run -p 5555:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:6432/cool_project_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=s3cret! \
  cool-project:latest

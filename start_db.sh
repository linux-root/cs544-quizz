#!/bin/bash
docker run -p 6432:5432 -e POSTGRES_PASSWORD=s3cret! \
  -e POSTGRES_DB=cool_project_db postgres

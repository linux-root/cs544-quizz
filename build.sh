#!/bin/bash
set -e # Exit immediately if a command exits with a non-zero status.

IMAGE_NAME="chickentooth/cs544-quizz"
COMMIT_ID=$(git rev-parse --short HEAD)
FULL_IMAGE_NAME_WITH_TAG="${IMAGE_NAME}:${COMMIT_ID}"

echo "Building Docker image ${FULL_IMAGE_NAME_WITH_TAG}..."
docker build -t "${FULL_IMAGE_NAME_WITH_TAG}" .

echo "Docker image build complete: ${FULL_IMAGE_NAME_WITH_TAG}"

echo "Pushing Docker image ${FULL_IMAGE_NAME_WITH_TAG}..."
docker push "${FULL_IMAGE_NAME_WITH_TAG}"
echo "Docker image push complete: ${FULL_IMAGE_NAME_WITH_TAG}"


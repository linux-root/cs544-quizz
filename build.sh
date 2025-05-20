#!/bin/bash
set -e # Exit immediately if a command exits with a non-zero status.

IMAGE_NAME="chickentooth/cs544-quizz"
IMAGE_TAG="latest"

echo "Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

echo "Docker image build complete: ${IMAGE_NAME}:${IMAGE_TAG}"


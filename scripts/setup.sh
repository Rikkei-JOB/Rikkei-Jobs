#!/usr/bin/env bash
# Quick dev setup script for Rikkei Jobs

echo "=== Rikkei Jobs Environment Setup ==="
if [ ! -f .env ]; then
    echo "Creating .env from .env.example..."
    cp .env.example .env
fi

echo "Starting infrastructure containers..."
cd infra && docker compose up -d
cd ..

echo "Done! Infra is running."

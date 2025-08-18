#!/bin/bash

# Database backup script for AI Code Review System
# This script creates daily backups of the PostgreSQL database

set -e

# Configuration
DB_HOST="postgres"
DB_PORT="5432"
DB_NAME="reviewdb"
DB_USER="review_user"
BACKUP_DIR="/backup"
DATE=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="reviewdb_backup_${DATE}.sql"
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILE}"

# Create backup directory if it doesn't exist
mkdir -p ${BACKUP_DIR}

echo "Starting database backup at $(date)"

# Create database dump
pg_dump -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} \
    --verbose \
    --clean \
    --no-owner \
    --no-privileges \
    --format=plain \
    > ${BACKUP_PATH}

# Compress the backup
gzip ${BACKUP_PATH}
COMPRESSED_BACKUP="${BACKUP_PATH}.gz"

echo "Backup completed: ${COMPRESSED_BACKUP}"

# Verify backup
if [ -f "${COMPRESSED_BACKUP}" ]; then
    SIZE=$(du -h "${COMPRESSED_BACKUP}" | cut -f1)
    echo "Backup size: ${SIZE}"
else
    echo "ERROR: Backup file not found!"
    exit 1
fi

# Clean up old backups (keep last 7 days)
find ${BACKUP_DIR} -name "reviewdb_backup_*.sql.gz" -mtime +7 -delete
echo "Old backups cleaned up"

# Optional: Upload to cloud storage
# aws s3 cp ${COMPRESSED_BACKUP} s3://your-backup-bucket/database/
# echo "Backup uploaded to S3"

echo "Backup process completed successfully at $(date)"
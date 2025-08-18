#!/bin/bash

# Database restore script for AI Code Review System
# This script restores a PostgreSQL database from backup

set -e

# Configuration
DB_HOST="postgres"
DB_PORT="5432"
DB_NAME="reviewdb"
DB_USER="review_user"
BACKUP_DIR="/backup"

# Check if backup file is provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <backup_file>"
    echo "Available backups:"
    ls -la ${BACKUP_DIR}/reviewdb_backup_*.sql.gz 2>/dev/null || echo "No backups found"
    exit 1
fi

BACKUP_FILE=$1
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILE}"

# Check if backup file exists
if [ ! -f "${BACKUP_PATH}" ]; then
    echo "ERROR: Backup file ${BACKUP_PATH} not found!"
    exit 1
fi

echo "Starting database restore from ${BACKUP_FILE} at $(date)"

# Stop the application temporarily (optional)
# docker-compose stop review-app

# Create a temporary restore file
TEMP_FILE="/tmp/restore_$(date +%s).sql"

# Decompress backup if it's gzipped
if [[ ${BACKUP_FILE} == *.gz ]]; then
    echo "Decompressing backup file..."
    gunzip -c ${BACKUP_PATH} > ${TEMP_FILE}
else
    cp ${BACKUP_PATH} ${TEMP_FILE}
fi

# Verify backup file
if [ ! -s "${TEMP_FILE}" ]; then
    echo "ERROR: Backup file is empty or corrupted!"
    rm -f ${TEMP_FILE}
    exit 1
fi

echo "Backup file verified, proceeding with restore..."

# Create a backup of current database before restore
CURRENT_BACKUP="/tmp/current_db_backup_$(date +%s).sql"
echo "Creating backup of current database..."
pg_dump -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} > ${CURRENT_BACKUP}
echo "Current database backed up to ${CURRENT_BACKUP}"

# Restore database
echo "Restoring database from backup..."
psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} < ${TEMP_FILE}

# Clean up temporary files
rm -f ${TEMP_FILE}

echo "Database restore completed successfully at $(date)"

# Restart the application (optional)
# docker-compose start review-app

echo "Restore process completed. Current database backup saved to: ${CURRENT_BACKUP}"
echo "You can delete this backup manually if the restore was successful."
#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

log_info()    { echo -e "${CYAN}[INFO]${NC}    $1"; }
log_success() { echo -e "${GREEN}[OK]${NC}      $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}    $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC}   $1"; }

CLEAN_MODE=false

for arg in "$@"; do
    case $arg in
        --clean)
            CLEAN_MODE=true
            ;;
        --force)
            FORCE_MODE=true
            ;;
        --help|-h)
            echo "Usage: ./stop.sh [--clean] [--force]"
            echo ""
            echo "  --clean    Remove all Docker volumes (deletes database data)"
            echo "  --force    Force kill containers without graceful shutdown"
            echo ""
            exit 0
            ;;
    esac
done

echo ""
echo -e "${BOLD}${RED}============================================${NC}"
echo -e "${BOLD}${RED}  Emergency Platform - Docker Dev Stop${NC}"
echo -e "${BOLD}${RED}============================================${NC}"
echo ""

if ! command -v docker &>/dev/null; then
    log_error "Docker is not installed"
    exit 1
fi

if ! docker info &>/dev/null 2>&1; then
    log_error "Docker daemon is not running"
    exit 1
fi

if docker compose version &>/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
elif command -v docker-compose &>/dev/null; then
    COMPOSE_CMD="docker-compose"
else
    log_error "Docker Compose is not available"
    exit 1
fi

if $CLEAN_MODE; then
    echo -e "${BOLD}${YELLOW}  WARNING: --clean mode will DELETE ALL DATABASE DATA${NC}"
    echo -e "${BOLD}${YELLOW}  This includes MySQL, PostgreSQL, Redis, and MinIO data!${NC}"
    echo ""
    read -r -p "  Type 'YES' to confirm: " CONFIRM
    if [ "$CONFIRM" != "YES" ]; then
        log_warn "Clean operation cancelled"
        exit 0
    fi
    echo ""
fi

echo -e "${BOLD}  Stopping business services...${NC}"
log_info "Stopping frontend (emergency-frontend)..."
$COMPOSE_CMD stop frontend 2>/dev/null || true

log_info "Stopping AI service (emergency-ai-service)..."
$COMPOSE_CMD stop ai-service 2>/dev/null || true

log_info "Stopping backend (emergency-backend)..."
$COMPOSE_CMD stop backend 2>/dev/null || true

log_success "Business services stopped"

echo ""
echo -e "${BOLD}  Stopping infrastructure...${NC}"
log_info "Stopping MinIO (emergency-minio)..."
$COMPOSE_CMD stop minio 2>/dev/null || true

log_info "Stopping Redis (emergency-redis)..."
$COMPOSE_CMD stop redis 2>/dev/null || true

log_info "Stopping PostgreSQL (emergency-vector)..."
$COMPOSE_CMD stop postgres 2>/dev/null || true

log_info "Stopping MySQL (emergency-mysql)..."
$COMPOSE_CMD stop mysql 2>/dev/null || true

log_success "All services stopped gracefully"

if $CLEAN_MODE; then
    echo ""
    echo -e "${BOLD}${YELLOW}  Removing all containers and volumes...${NC}"
    $COMPOSE_CMD down -v --remove-orphans 2>/dev/null || true
    log_success "Containers and volumes removed"
else
    echo ""
    echo -e "${BOLD}  Removing containers (data preserved)...${NC}"
    $COMPOSE_CMD down --remove-orphans 2>/dev/null || true
    log_success "Containers removed (data preserved in volumes)"
fi

echo ""
echo -e "${BOLD}${GREEN}============================================${NC}"
echo -e "${BOLD}${GREEN}  All services stopped!                   ${NC}"
echo -e "${BOLD}${GREEN}============================================${NC}"
echo ""

if $CLEAN_MODE; then
    log_info "All data volumes removed. Next start will initialize fresh databases."
else
    log_info "Data preserved in volumes. Next start will use existing data."
fi

log_info "Use ./start.sh to start the platform again."
echo ""

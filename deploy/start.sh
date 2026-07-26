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

STEP=0
TOTAL=8

log_info()    { echo -e "${CYAN}[INFO]${NC}    $1"; }
log_success() { echo -e "${GREEN}[OK]${NC}      $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}    $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC}   $1"; }

step() {
    STEP=$((STEP + 1))
    echo ""
    echo -e "${BOLD}${CYAN}============================================${NC}"
    echo -e "${BOLD}${CYAN}  Step $STEP/$TOTAL: $1${NC}"
    echo -e "${BOLD}${CYAN}============================================${NC}"
}

banner() {
    echo ""
    echo -e "${BOLD}${GREEN}"
    echo "  ____                        _____  "
    echo " |  _ \  _____   ______  _ __| ____| "
    echo " | | | |/ _ \ \ / / _ \| '__/  _|   "
    echo " | |_| |  __/\ V /  __/| | | |___   "
    echo " |____/ \___| \_/ \___||_| |_____|  "
    echo ""
    echo -e "  ${BOLD}云南自然灾害应急协同决策平台 - Docker Dev Start${NC}"
    echo ""
}

banner

# ============================================================
# Step 1: Environment pre-check
# ============================================================
step "Environment pre-check"

HAS_ERROR=false

if ! command -v docker &>/dev/null; then
    log_error "Docker is not installed"
    HAS_ERROR=true
else
    DOCKER_VERSION=$(docker --version 2>&1 | awk '{print $3}')
    log_success "Docker installed: $DOCKER_VERSION"
fi

if ! docker info &>/dev/null 2>&1; then
    log_error "Docker daemon is not running. Please start Docker Desktop first."
    HAS_ERROR=true
else
    log_success "Docker daemon is running"
fi

if docker compose version &>/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
    COMPOSE_VERSION=$(docker compose version 2>&1)
    log_success "Docker Compose: $COMPOSE_VERSION"
elif command -v docker-compose &>/dev/null; then
    COMPOSE_CMD="docker-compose"
    COMPOSE_VERSION=$(docker-compose --version 2>&1)
    log_success "Docker Compose (standalone): $COMPOSE_VERSION"
else
    log_error "Docker Compose is not available"
    HAS_ERROR=true
fi

if [ -f ".env" ]; then
    log_success ".env file found"
else
    log_warn ".env not found, using default values from .env.example"
    if [ -f ".env.example" ]; then
        cp .env.example .env
        log_info "Copied .env.example to .env"
    fi
fi

if $HAS_ERROR; then
    echo ""
    log_error "Environment pre-check failed. Please fix the above issues and try again."
    exit 1
fi

# ============================================================
# Step 2: Pull base images if needed
# ============================================================
step "Preparing base images"

BASE_IMAGES=("maven:3.9-eclipse-temurin-19" "python:3.10-slim" "node:20-alpine")
MISSING_IMAGES=()

for img in "${BASE_IMAGES[@]}"; do
    if docker image inspect "$img" &>/dev/null 2>&1; then
        log_success "Image exists: $img"
    else
        log_warn "Image missing: $img (pulling...)"
        MISSING_IMAGES+=("$img")
    fi
done

if [ ${#MISSING_IMAGES[@]} -gt 0 ]; then
    log_info "Pulling missing base images..."
    for img in "${MISSING_IMAGES[@]}"; do
        log_info "  Pulling $img ..."
        if docker pull "$img" 2>&1; then
            log_success "  Pulled: $img"
        else
            log_error "  Failed to pull $img"
            log_error "  Please check your network connection or configure Docker mirror"
            exit 1
        fi
    done
    log_success "All base images pulled successfully"
else
    log_success "All base images are ready"
fi

# ============================================================
# Step 3: Port conflict check
# ============================================================
step "Port conflict check"

REQUIRED_PORTS="3306 5434 6379 9000 9001 8080 8002 3000"
PORT_NAMES="MySQL PostgreSQL Redis MinIO-API MinIO-Console Backend AI-Service Frontend"
CONFLICT_FOUND=false

PORT_ARRAY=($REQUIRED_PORTS)
NAME_ARRAY=($PORT_NAMES)

for i in "${!PORT_ARRAY[@]}"; do
    port="${PORT_ARRAY[$i]}"
    name="${NAME_ARRAY[$i]}"
    pids=$(lsof -i :"$port" -sTCP:LISTEN -t 2>/dev/null || true)
    if [ -n "$pids" ]; then
        log_warn "Port $port ($name) is occupied by PID(s): $pids"
        CONFLICT_FOUND=true
    fi
done

if [ "$CONFLICT_FOUND" = "true" ]; then
    log_info "Attempting to stop conflicting Docker containers..."
    STOPPED_ANY=false

    for i in "${!PORT_ARRAY[@]}"; do
        port="${PORT_ARRAY[$i]}"
        CONTAINER_INFO=$(docker ps --filter "publish=$port" --format "{{.Names}}" 2>/dev/null || true)
        if [ -n "$CONTAINER_INFO" ]; then
            for cname in $CONTAINER_INFO; do
                log_info "  Stopping container $cname on port $port..."
                docker stop "$cname" 2>/dev/null && STOPPED_ANY=true
            done
        fi
    done

    if [ "$STOPPED_ANY" = "true" ]; then
        sleep 3
    fi

    REMAINING_PORTS=""
    for i in "${!PORT_ARRAY[@]}"; do
        port="${PORT_ARRAY[$i]}"
        name="${NAME_ARRAY[$i]}"
        pids=$(lsof -i :"$port" -sTCP:LISTEN -t 2>/dev/null || true)
        if [ -n "$pids" ]; then
            REMAINING_PORTS="$REMAINING_PORTS  Port $port ($name) still occupied by PID(s): $pids"
        fi
    done

    if [ -n "$REMAINING_PORTS" ]; then
        log_error "Some ports are still occupied:"
        echo "$REMAINING_PORTS"
        log_error "Please manually stop these services, then re-run ./start.sh"
        exit 1
    fi
    log_success "All port conflicts resolved"
else
    log_success "All required ports are available"
fi

# ============================================================
# Step 4: Directory initialization
# ============================================================
step "Directory initialization"

mkdir -p .docker-volumes/mysql
mkdir -p .docker-volumes/pg
mkdir -p .docker-volumes/redis
mkdir -p .docker-volumes/minio
mkdir -p .docker-volumes/uploads

BACKEND_UPLOADS="$SCRIPT_DIR/../backend/uploads"
if [ ! -d "$BACKEND_UPLOADS" ]; then
    mkdir -p "$BACKEND_UPLOADS"
    log_info "Created backend uploads directory"
fi

log_success "Data directories initialized"

# ============================================================
# Step 5: Start infrastructure
# ============================================================
step "Starting infrastructure services"

log_info "Building and starting infrastructure (MySQL, PostgreSQL, Redis, MinIO)..."
$COMPOSE_CMD up -d mysql postgres redis minio

log_success "Infrastructure containers started, waiting for health checks..."

# ============================================================
# Step 6: Health check
# ============================================================
step "Waiting for infrastructure health checks"

MAX_RETRIES=30
RETRY_INTERVAL=5

check_port() {
    local host=$1
    local port=$2
    local service_name=$3
    local retries=0

    while [ $retries -lt $MAX_RETRIES ]; do
        if nc -z -w 2 "$host" "$port" 2>/dev/null; then
            log_success "$service_name is ready (port $port)"
            return 0
        fi
        retries=$((retries + 1))
        log_info "Waiting for $service_name... attempt $retries/$MAX_RETRIES"
        sleep "$RETRY_INTERVAL"
    done

    log_error "$service_name failed to become ready after $MAX_RETRIES attempts"
    return 1
}

MYSQL_OK=true
PG_OK=true
REDIS_OK=true
MINIO_OK=true

log_info "Checking MySQL (localhost:3306)..."
check_port "127.0.0.1" 3306 "MySQL" || MYSQL_OK=false

log_info "Checking PostgreSQL (localhost:5434)..."
check_port "127.0.0.1" 5434 "PostgreSQL" || PG_OK=false

log_info "Checking Redis (localhost:6379)..."
check_port "127.0.0.1" 6379 "Redis" || REDIS_OK=false

log_info "Checking MinIO (localhost:9000)..."
check_port "127.0.0.1" 9000 "MinIO" || MINIO_OK=false

if [ "$MYSQL_OK" = "true" ] && [ "$PG_OK" = "true" ] && [ "$REDIS_OK" = "true" ] && [ "$MINIO_OK" = "true" ]; then
    log_success "All infrastructure services are ready"
else
    log_warn "Some infrastructure services are not fully ready, but will continue anyway"
fi

# ============================================================
# Step 7: Start business services
# ============================================================
step "Starting business services"

log_info "Building and starting backend (Spring Boot)..."
$COMPOSE_CMD up -d --build backend

log_info "Building and starting AI service (FastAPI)..."
$COMPOSE_CMD up -d --build ai-service

log_info "Building and starting frontend (Vue 3 + Vite)..."
$COMPOSE_CMD up -d --build frontend

log_success "All business services started"

# ============================================================
# Step 8: Startup summary
# ============================================================
step "Startup summary"

sleep 5

echo ""
echo -e "${BOLD}${GREEN}============================================${NC}"
echo -e "${BOLD}${GREEN}  All Services Started!                   ${NC}"
echo -e "${BOLD}${GREEN}============================================${NC}"
echo ""

echo -e "${BOLD}  Service Access Points:${NC}"
echo ""
echo -e "  ${GREEN}Frontend App${NC}        ${CYAN}http://localhost:3000${NC}"
echo -e "  ${GREEN}Backend API${NC}         ${CYAN}http://localhost:8080${NC}"
echo -e "  ${GREEN}MinIO Console${NC}       ${CYAN}http://localhost:9001${NC}"
echo -e "  ${GREEN}FastAPI Docs${NC}        ${CYAN}http://localhost:8002/docs${NC}"
echo -e "  ${GREEN}Spring Swagger${NC}      ${CYAN}http://localhost:8080/swagger-ui.html${NC}"
echo ""

echo -e "${BOLD}  Default Accounts:${NC}"
echo ""
echo -e "  ${YELLOW}admin${NC}     / ${YELLOW}ZAQ12wsx581!${NC}    (系统管理员)"
echo -e "  ${YELLOW}operator${NC} / ${YELLOW}ZAQ12wsx581!${NC}    (应急操作员)"
echo -e "  ${YELLOW}viewer${NC}    / ${YELLOW}ZAQ12wsx581!${NC}    (查看员)"
echo ""

echo -e "${BOLD}  Infrastructure:${NC}"
echo ""
echo -e "  ${GREEN}MySQL${NC}     localhost:3306    (user: root)"
echo -e "  ${GREEN}PostgreSQL${NC} localhost:5434   (user: postgres)"
echo -e "  ${GREEN}Redis${NC}     localhost:6379"
echo -e "  ${GREEN}MinIO${NC}     localhost:9000    (API)"
echo ""

echo -e "${BOLD}  Useful Commands:${NC}"
echo ""
echo -e "  ${CYAN}./stop.sh${NC}          Stop all services"
echo -e "  ${CYAN}./stop.sh --clean${NC}  Stop and remove all data"
echo -e "  ${CYAN}docker compose logs -f  View all logs"
echo -e "  ${CYAN}docker compose ps       Check container status"
echo ""

CONTAINERS=$(docker ps --filter "name=emergency" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || true)
if [ -n "$CONTAINERS" ]; then
    echo -e "${BOLD}  Running Containers:${NC}"
    echo "$CONTAINERS"
    echo ""
fi

echo -e "${GREEN}Done! Happy coding!${NC}"
echo ""

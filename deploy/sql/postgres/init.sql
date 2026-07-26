CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS locations (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100),
    address VARCHAR(500),
    coordinates GEOMETRY(Point, 4326),
    accuracy DECIMAL(10,6),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resources (
    id BIGSERIAL PRIMARY KEY,
    resource_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    quantity INT DEFAULT 0,
    unit VARCHAR(20),
    status VARCHAR(20) DEFAULT 'available',
    location GEOMETRY(Point, 4326),
    storage_address VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS emergency_plans (
    id BIGSERIAL PRIMARY KEY,
    plan_id VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    plan_type VARCHAR(50),
    keywords TEXT[],
    embedding vector(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id SERIAL PRIMARY KEY,
    chunk_id VARCHAR(255) UNIQUE NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    chapter VARCHAR(50) NOT NULL,
    section VARCHAR(50),
    page INTEGER NOT NULL,
    content TEXT NOT NULL,
    length INTEGER NOT NULL,
    "order" INTEGER NOT NULL,
    source VARCHAR(255) NOT NULL,
    publish_org VARCHAR(255) NOT NULL,
    publish_date VARCHAR(20),
    version VARCHAR(50) NOT NULL,
    embedding double precision[],
    model_name VARCHAR(100) NOT NULL DEFAULT 'BAAI/bge-small-zh-v1.5',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_locations_coordinates ON locations USING GIST (coordinates);
CREATE INDEX IF NOT EXISTS idx_resources_location ON resources USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_emergency_plans_embedding ON emergency_plans USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

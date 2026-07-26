SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS emergency_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE emergency_db;

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    real_name VARCHAR(50),
    role_id BIGINT,
    status INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS incidents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id VARCHAR(50) NOT NULL UNIQUE,
    incident_name VARCHAR(200) NOT NULL,
    title VARCHAR(200) NOT NULL,
    disaster_type VARCHAR(50) NOT NULL,
    incident_level VARCHAR(10),
    occur_time TIMESTAMP NULL,
    location VARCHAR(200),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'processing',
    disposal_plan_status VARCHAR(20),
    resource_dispatch_status VARCHAR(20),
    image_urls TEXT,
    reporter_id BIGINT,
    death_count INT,
    property_loss DOUBLE,
    report_time TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_incidents_reporter_id (reporter_id),
    INDEX idx_incidents_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id VARCHAR(64) NOT NULL UNIQUE,
    incident_id VARCHAR(50) NOT NULL,
    plan_title VARCHAR(200) NOT NULL,
    plan_content TEXT,
    generate_time TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    submitted_by BIGINT,
    submitted_at TIMESTAMP NULL,
    reject_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plans_incident_id (incident_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS emergency_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id VARCHAR(64) NOT NULL UNIQUE,
    resource_name VARCHAR(200) NOT NULL,
    resource_type VARCHAR(50),
    unit VARCHAR(20),
    total_stock INT DEFAULT 0,
    available_stock INT DEFAULT 0,
    locked_stock INT DEFAULT 0,
    location VARCHAR(200),
    status VARCHAR(20) DEFAULT 'available',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resource_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL UNIQUE,
    incident_id VARCHAR(50) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    resource_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    quantity INT NOT NULL,
    unit VARCHAR(20),
    priority VARCHAR(20),
    destination VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    remark TEXT,
    requester_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resource_requests_incident_id (incident_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resource_dispatch_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id VARCHAR(64) NOT NULL UNIQUE,
    resource_id VARCHAR(64) NOT NULL,
    resource_name VARCHAR(200),
    incident_id VARCHAR(64),
    plan_id VARCHAR(64),
    dispatch_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    from_location VARCHAR(200),
    to_location VARCHAR(200),
    operator_id BIGINT,
    operator_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'pending',
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_dispatch_record_incident_id (incident_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL UNIQUE,
    incident_id VARCHAR(64),
    agent_name VARCHAR(100),
    input_params TEXT,
    output_result TEXT,
    status VARCHAR(20),
    error_message TEXT,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_runs_incident_id (incident_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS citations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    citation_id VARCHAR(64) NOT NULL UNIQUE,
    incident_id VARCHAR(64),
    source_text TEXT,
    source_url VARCHAR(500),
    relevance_score DECIMAL(5,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_citations_incident_id (incident_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100),
    target_type VARCHAR(50),
    target_id VARCHAR(64),
    detail TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id VARCHAR(50) NOT NULL UNIQUE,
    file_name VARCHAR(300) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50),
    object_key VARCHAR(500) NOT NULL,
    bucket VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    uploader_id BIGINT,
    uploader_name VARCHAR(100),
    vectorize_status VARCHAR(20) DEFAULT 'pending',
    vectorize_fail_reason VARCHAR(500),
    vectorize_started_at TIMESTAMP NULL,
    vectorize_completed_at TIMESTAMP NULL,
    vectorize_retry_count INT DEFAULT 0,
    chunk_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

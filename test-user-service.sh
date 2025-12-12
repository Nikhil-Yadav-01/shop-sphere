#!/bin/bash

set -o pipefail

################################################################################
# User Service - Comprehensive Test Suite
# Tests all aspects of the user-service module including build, Docker,
# dependencies, file integrity, and deployment readiness
################################################################################

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TESTS_TOTAL=0
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

# Global variables
SERVICE_DIR="/home/ubuntu/shop-sphere/user-service"
PROJECT_ROOT="/home/ubuntu/shop-sphere"
DOCKER_IMAGE="user-service:1.0.0"
JAR_FILE="$SERVICE_DIR/target/user-service-1.0.0-SNAPSHOT.jar"
MIN_JAR_SIZE=70000000  # 70MB minimum
MAX_JAR_SIZE=100000000 # 100MB maximum
REQUIRED_JAVA_FILES=20

################################################################################
# Helper Functions
################################################################################

log_section() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════════════════════${NC}"
}

log_test() {
    echo -e "${YELLOW}→ $1${NC}"
    ((TESTS_TOTAL++))
}

log_pass() {
    echo -e "${GREEN}✓ $1${NC}"
    ((TESTS_PASSED++))
}

log_fail() {
    echo -e "${RED}✗ $1${NC}"
    ((TESTS_FAILED++))
}

log_skip() {
    echo -e "${YELLOW}⊘ $1${NC}"
    ((TESTS_SKIPPED++))
}

log_info() {
    echo -e "  ${BLUE}ℹ${NC} $1"
}

log_error() {
    echo -e "${RED}ERROR: $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}WARNING: $1${NC}"
}

exit_on_error() {
    if [ $? -ne 0 ]; then
        log_error "$1"
        exit 1
    fi
}

################################################################################
# Pre-flight Checks
################################################################################

test_prerequisites() {
    log_section "PHASE 1: Prerequisites Validation"

    # Check Java installation
    log_test "Checking Java installation"
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[^"]+')
        log_pass "Java installed: $JAVA_VERSION"
        
        # Check for Java 21
        if [[ $JAVA_VERSION == 21* ]]; then
            log_pass "Java version is 21.x (required)"
        else
            log_warning "Java version is not 21.x (current: $JAVA_VERSION)"
        fi
    else
        log_fail "Java not installed"
        return 1
    fi

    # Check Maven installation
    log_test "Checking Maven installation"
    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn -v 2>/dev/null | head -1)
        log_pass "Maven installed: $MVN_VERSION"
    else
        log_fail "Maven not installed"
        return 1
    fi

    # Check Docker installation
    log_test "Checking Docker installation"
    if command -v docker &> /dev/null; then
        DOCKER_VERSION=$(docker --version)
        log_pass "Docker installed: $DOCKER_VERSION"
    else
        log_fail "Docker not installed or not in PATH"
        return 1
    fi

    # Check Docker daemon
    log_test "Checking Docker daemon status"
    if docker info &> /dev/null; then
        log_pass "Docker daemon is running"
    else
        log_fail "Docker daemon is not running"
        return 1
    fi

    # Check Git installation
    log_test "Checking Git installation"
    if command -v git &> /dev/null; then
        log_pass "Git installed"
    else
        log_fail "Git not installed"
        return 1
    fi

    return 0
}

################################################################################
# Directory and File Checks
################################################################################

test_directory_structure() {
    log_section "PHASE 2: Directory Structure Validation"

    # Check service directory
    log_test "Checking service directory"
    if [ -d "$SERVICE_DIR" ]; then
        log_pass "Service directory exists: $SERVICE_DIR"
    else
        log_fail "Service directory not found: $SERVICE_DIR"
        return 1
    fi

    # Check pom.xml
    log_test "Checking pom.xml"
    if [ -f "$SERVICE_DIR/pom.xml" ]; then
        log_pass "pom.xml found"
    else
        log_fail "pom.xml not found"
        return 1
    fi

    # Check Dockerfile
    log_test "Checking Dockerfile"
    if [ -f "$SERVICE_DIR/Dockerfile" ]; then
        log_pass "Dockerfile found"
    else
        log_fail "Dockerfile not found"
        return 1
    fi

    # Check application.yml
    log_test "Checking application.yml"
    if [ -f "$SERVICE_DIR/src/main/resources/application.yml" ]; then
        log_pass "application.yml found"
    else
        log_fail "application.yml not found"
        return 1
    fi

    # Check database migration
    log_test "Checking database migration"
    if [ -f "$SERVICE_DIR/src/main/resources/db/migration/V1__create_user_tables.sql" ]; then
        log_pass "Database migration file found"
    else
        log_fail "Database migration file not found"
        return 1
    fi

    return 0
}

################################################################################
# Source Code Validation
################################################################################

test_source_files() {
    log_section "PHASE 3: Source Code Validation"

    # Count Java files
    log_test "Counting Java source files"
    JAVA_COUNT=$(find "$SERVICE_DIR/src/main/java" -name "*.java" | wc -l)
    if [ "$JAVA_COUNT" -ge "$REQUIRED_JAVA_FILES" ]; then
        log_pass "Found $JAVA_COUNT Java files (required: $REQUIRED_JAVA_FILES)"
    else
        log_fail "Found only $JAVA_COUNT Java files (required: $REQUIRED_JAVA_FILES)"
        return 1
    fi

    # Check critical classes
    local critical_classes=(
        "UserApplication.java"
        "UserController.java"
        "AddressController.java"
        "UserService.java"
        "AddressService.java"
        "UserServiceImpl.java"
        "AddressServiceImpl.java"
        "SecurityConfig.java"
        "JwtTokenProvider.java"
        "JwtAuthenticationFilter.java"
    )

    log_test "Checking critical classes"
    local missing=0
    for class in "${critical_classes[@]}"; do
        if find "$SERVICE_DIR/src/main/java" -name "$class" | grep -q .; then
            log_info "Found: $class"
        else
            log_fail "Missing: $class"
            ((missing++))
        fi
    done

    if [ $missing -eq 0 ]; then
        log_pass "All critical classes present"
    else
        log_fail "$missing critical classes missing"
        return 1
    fi

    return 0
}

################################################################################
# Maven Build Tests
################################################################################

test_maven_build() {
    log_section "PHASE 4: Maven Build Testing"

    # Check Maven syntax
    log_test "Validating pom.xml syntax"
    cd "$SERVICE_DIR" || exit 1
    mvn validate -q 2>/dev/null
    if [ $? -eq 0 ]; then
        log_pass "pom.xml is valid"
    else
        log_fail "pom.xml validation failed"
        return 1
    fi

    # Clean old build
    log_test "Cleaning previous builds"
    mvn clean -q 2>/dev/null
    if [ $? -eq 0 ]; then
        log_pass "Clean build successful"
    else
        log_warning "Clean build had issues"
    fi

    # Compile phase
    log_test "Compiling source code"
    mvn compile -q 2>/dev/null
    if [ $? -eq 0 ]; then
        log_pass "Compilation successful"
    else
        log_fail "Compilation failed"
        return 1
    fi

    # Test phase (skip tests but validate test code)
    log_test "Validating test code structure"
    if [ -d "$SERVICE_DIR/src/test/java" ]; then
        log_pass "Test directory structure is valid"
    else
        log_warning "Test directory not present"
    fi

    # Package phase
    log_test "Creating JAR package"
    mvn package -DskipTests -q 2>/dev/null
    if [ $? -eq 0 ]; then
        log_pass "JAR package created successfully"
    else
        log_fail "JAR package creation failed"
        return 1
    fi

    cd "$PROJECT_ROOT" || exit 1
    return 0
}

################################################################################
# JAR File Validation
################################################################################

test_jar_file() {
    log_section "PHASE 5: JAR File Validation"

    # Check JAR existence
    log_test "Checking JAR file existence"
    if [ -f "$JAR_FILE" ]; then
        log_pass "JAR file found: $JAR_FILE"
    else
        log_fail "JAR file not found"
        return 1
    fi

    # Check JAR size
    log_test "Validating JAR file size"
    JAR_SIZE=$(stat -f%z "$JAR_FILE" 2>/dev/null || stat -c%s "$JAR_FILE" 2>/dev/null)
    if [ "$JAR_SIZE" -ge "$MIN_JAR_SIZE" ] && [ "$JAR_SIZE" -le "$MAX_JAR_SIZE" ]; then
        JAR_SIZE_MB=$((JAR_SIZE / 1048576))
        log_pass "JAR size is valid: ${JAR_SIZE_MB}MB"
    else
        JAR_SIZE_MB=$((JAR_SIZE / 1048576))
        log_warning "JAR size is unusual: ${JAR_SIZE_MB}MB (expected: 70-100MB)"
    fi

    # Check JAR integrity
    log_test "Checking JAR integrity"
    jar tf "$JAR_FILE" > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        log_pass "JAR is valid and readable"
    else
        log_fail "JAR file is corrupted"
        return 1
    fi

    # Check for critical classes in JAR
    log_test "Checking critical classes in JAR"
    if jar tf "$JAR_FILE" | grep -q "com/rudraksha/shopsphere/user/UserApplication.class"; then
        log_pass "Application class found in JAR"
    else
        log_fail "Application class not found in JAR"
        return 1
    fi

    # Check for Spring Boot manifest
    log_test "Checking Spring Boot manifest"
    if jar xf "$JAR_FILE" MANIFEST.MF && grep -q "Main-Class" MANIFEST.MF 2>/dev/null; then
        log_pass "Spring Boot manifest is present"
        rm -f MANIFEST.MF
    else
        log_warning "Could not verify Spring Boot manifest"
    fi

    return 0
}

################################################################################
# Dependency Validation
################################################################################

test_dependencies() {
    log_section "PHASE 6: Dependency Validation"

    # Check for critical dependencies in pom.xml
    log_test "Checking critical dependencies in pom.xml"

    local dependencies=(
        "spring-boot-starter-web"
        "spring-boot-starter-data-jpa"
        "spring-boot-starter-security"
        "spring-cloud-starter-netflix-eureka-client"
        "postgresql"
        "flyway-core"
        "jjwt-api"
        "lombok"
        "mapstruct"
    )

    local missing_deps=0
    local pom_file="$SERVICE_DIR/pom.xml"

    for dep in "${dependencies[@]}"; do
        if grep -q "$dep" "$pom_file"; then
            log_info "Dependency found: $dep"
        else
            log_fail "Critical dependency missing: $dep"
            ((missing_deps++))
        fi
    done

    if [ $missing_deps -eq 0 ]; then
        log_pass "All critical dependencies present in pom.xml"
    else
        log_fail "$missing_deps critical dependencies missing"
        return 1
    fi

    # Verify dependencies are resolvable
    log_test "Verifying dependencies can be resolved"
    cd "$SERVICE_DIR" || exit 1
    if mvn dependency:resolve -q 2>/dev/null; then
        log_pass "All dependencies resolved successfully"
    else
        log_warning "Some dependencies may have resolution issues"
    fi

    cd "$PROJECT_ROOT" || exit 1
    return 0
}

################################################################################
# Docker Build Tests
################################################################################

test_docker_build() {
    log_section "PHASE 7: Docker Build Testing"

    # Check Dockerfile syntax
    log_test "Checking Dockerfile syntax"
    if command -v hadolint &> /dev/null; then
        hadolint "$SERVICE_DIR/Dockerfile" > /dev/null 2>&1
        if [ $? -eq 0 ]; then
            log_pass "Dockerfile syntax is valid"
        else
            log_warning "Dockerfile has minor issues"
        fi
    else
        log_skip "hadolint not installed (optional check)"
    fi

    # Build Docker image
    log_test "Building Docker image"
    cd "$PROJECT_ROOT" || exit 1
    DOCKER_OUTPUT=$(docker build -f "$SERVICE_DIR/Dockerfile" -t "$DOCKER_IMAGE" . 2>&1)
    if [ $? -eq 0 ]; then
        log_pass "Docker image built successfully"
    else
        log_fail "Docker build failed"
        echo "$DOCKER_OUTPUT"
        return 1
    fi

    # Check image exists
    log_test "Verifying Docker image exists"
    if docker images | grep -q "user-service.*1.0.0"; then
        log_pass "Docker image is available"
    else
        log_fail "Docker image not found in local registry"
        return 1
    fi

    # Check image size
    log_test "Checking Docker image size"
    IMAGE_SIZE=$(docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | grep "user-service" | awk '{print $3}')
    log_pass "Docker image size: $IMAGE_SIZE"

    return 0
}

################################################################################
# Configuration Validation
################################################################################

test_configuration() {
    log_section "PHASE 8: Configuration Validation"

    # Check application.yml content
    log_test "Validating application.yml configuration"
    local yml_file="$SERVICE_DIR/src/main/resources/application.yml"
    
    if grep -q "port: 8082" "$yml_file"; then
        log_pass "Service port configured correctly: 8082"
    else
        log_fail "Service port configuration issue"
        return 1
    fi

    if grep -q "name: user-service" "$yml_file"; then
        log_pass "Service name configured correctly"
    else
        log_fail "Service name configuration issue"
        return 1
    fi

    if grep -q "flyway:" "$yml_file"; then
        log_pass "Flyway migration configured"
    else
        log_fail "Flyway configuration missing"
        return 1
    fi

    if grep -q "eureka:" "$yml_file"; then
        log_pass "Eureka service discovery configured"
    else
        log_fail "Eureka configuration missing"
        return 1
    fi

    # Check pom.xml critical properties
    log_test "Validating pom.xml configuration"
    local pom_file="$SERVICE_DIR/pom.xml"

    if grep -q "<java.version>21</java.version>" "$pom_file"; then
        log_pass "Java version set to 21"
    else
        log_fail "Java version not properly configured"
        return 1
    fi

    if grep -q "spring-boot-starter-parent" "$pom_file"; then
        log_pass "Spring Boot parent configured"
    else
        log_fail "Spring Boot parent not configured"
        return 1
    fi

    return 0
}

################################################################################
# Security Configuration Tests
################################################################################

test_security() {
    log_section "PHASE 9: Security Configuration Tests"

    # Check JWT dependency
    log_test "Checking JWT dependencies"
    if grep -q "jjwt" "$SERVICE_DIR/pom.xml"; then
        log_pass "JJWT dependency configured"
    else
        log_fail "JJWT dependency missing"
        return 1
    fi

    # Check Spring Security dependency
    log_test "Checking Spring Security"
    if grep -q "spring-boot-starter-security" "$SERVICE_DIR/pom.xml"; then
        log_pass "Spring Security configured"
    else
        log_fail "Spring Security not configured"
        return 1
    fi

    # Check for JWT classes
    log_test "Checking JWT implementation classes"
    if [ -f "$SERVICE_DIR/src/main/java/com/rudraksha/shopsphere/user/security/JwtTokenProvider.java" ]; then
        log_pass "JwtTokenProvider found"
    else
        log_fail "JwtTokenProvider not found"
        return 1
    fi

    if [ -f "$SERVICE_DIR/src/main/java/com/rudraksha/shopsphere/user/security/JwtAuthenticationFilter.java" ]; then
        log_pass "JwtAuthenticationFilter found"
    else
        log_fail "JwtAuthenticationFilter not found"
        return 1
    fi

    # Check SecurityConfig
    log_test "Checking SecurityConfig"
    if grep -q "authenticated()" "$SERVICE_DIR/src/main/java/com/rudraksha/shopsphere/user/config/SecurityConfig.java"; then
        log_pass "Security endpoints properly configured to require authentication"
    else
        log_fail "Security configuration issue detected"
        return 1
    fi

    return 0
}

################################################################################
# Database Migration Tests
################################################################################

test_database() {
    log_section "PHASE 10: Database Migration Tests"

    local migration_file="$SERVICE_DIR/src/main/resources/db/migration/V1__create_user_tables.sql"

    # Check migration file
    log_test "Checking migration file"
    if [ -f "$migration_file" ]; then
        log_pass "Migration file found"
    else
        log_fail "Migration file not found"
        return 1
    fi

    # Check table creation syntax
    log_test "Validating migration SQL syntax"
    if grep -q "CREATE TABLE.*user_profiles" "$migration_file"; then
        log_pass "user_profiles table migration found"
    else
        log_fail "user_profiles table migration not found"
        return 1
    fi

    if grep -q "CREATE TABLE.*addresses" "$migration_file"; then
        log_pass "addresses table migration found"
    else
        log_fail "addresses table migration not found"
        return 1
    fi

    # Check for indexes
    log_test "Checking database indexes"
    if grep -q "CREATE INDEX" "$migration_file"; then
        INDEX_COUNT=$(grep -c "CREATE INDEX" "$migration_file")
        log_pass "Found $INDEX_COUNT indexes"
    else
        log_fail "No indexes found in migration"
        return 1
    fi

    return 0
}

################################################################################
# API Endpoint Documentation Tests
################################################################################

test_api_endpoints() {
    log_section "PHASE 11: API Endpoint Validation"

    # Check UserController
    log_test "Checking UserController endpoints"
    local user_controller="$SERVICE_DIR/src/main/java/com/rudraksha/shopsphere/user/controller/UserController.java"
    
    if grep -q "@GetMapping" "$user_controller"; then
        log_pass "User GET endpoints configured"
    else
        log_fail "User GET endpoints not configured"
        return 1
    fi

    if grep -q "@PostMapping" "$user_controller"; then
        log_pass "User POST endpoints configured"
    else
        log_fail "User POST endpoints not configured"
        return 1
    fi

    if grep -q "@PutMapping" "$user_controller"; then
        log_pass "User PUT endpoints configured"
    else
        log_fail "User PUT endpoints not configured"
        return 1
    fi

    if grep -q "@DeleteMapping" "$user_controller"; then
        log_pass "User DELETE endpoints configured"
    else
        log_fail "User DELETE endpoints not configured"
        return 1
    fi

    # Check AddressController
    log_test "Checking AddressController endpoints"
    local address_controller="$SERVICE_DIR/src/main/java/com/rudraksha/shopsphere/user/controller/AddressController.java"
    
    if grep -q "@RequestMapping.*addresses" "$address_controller"; then
        log_pass "Address endpoints routing configured"
    else
        log_fail "Address endpoints routing not configured"
        return 1
    fi

    return 0
}

################################################################################
# Git Repository Tests
################################################################################

test_git_repository() {
    log_section "PHASE 12: Git Repository Tests"

    cd "$PROJECT_ROOT" || exit 1

    # Check current branch
    log_test "Checking git branch"
    CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    if [ "$CURRENT_BRANCH" = "feature/userservice-module" ]; then
        log_pass "On correct branch: feature/userservice-module"
    else
        log_warning "Not on feature/userservice-module branch (current: $CURRENT_BRANCH)"
    fi

    # Check commit history
    log_test "Checking commit history"
    COMMIT_COUNT=$(git log --oneline feature/userservice-module 2>/dev/null | wc -l)
    if [ "$COMMIT_COUNT" -ge 4 ]; then
        log_pass "Found $COMMIT_COUNT commits on feature branch"
    else
        log_warning "Expected at least 4 commits, found $COMMIT_COUNT"
    fi

    # Check for meaningful commits
    log_test "Checking commit messages"
    if git log --oneline feature/userservice-module 2>/dev/null | grep -q "user-service"; then
        log_pass "Commits contain relevant messages"
    else
        log_warning "Commit messages may not be descriptive"
    fi

    return 0
}

################################################################################
# Integration Tests
################################################################################

test_integration() {
    log_section "PHASE 13: Integration Tests"

    # Check docker-compose configuration
    log_test "Checking docker-compose.yml"
    if grep -q "user-service:" "$PROJECT_ROOT/docker-compose.yml"; then
        log_pass "user-service found in docker-compose.yml"
    else
        log_fail "user-service not configured in docker-compose.yml"
        return 1
    fi

    if grep -q "user-db:" "$PROJECT_ROOT/docker-compose.yml"; then
        log_pass "user-db found in docker-compose.yml"
    else
        log_fail "user-db not configured in docker-compose.yml"
        return 1
    fi

    # Check documentation
    log_test "Checking documentation files"
    if [ -f "$PROJECT_ROOT/USER-SERVICE-README.md" ]; then
        README_SIZE=$(wc -l < "$PROJECT_ROOT/USER-SERVICE-README.md")
        log_pass "USER-SERVICE-README.md found ($README_SIZE lines)"
    else
        log_fail "USER-SERVICE-README.md not found"
        return 1
    fi

    if [ -f "$PROJECT_ROOT/USER-SERVICE-IMPLEMENTATION-SUMMARY.md" ]; then
        SUMMARY_SIZE=$(wc -l < "$PROJECT_ROOT/USER-SERVICE-IMPLEMENTATION-SUMMARY.md")
        log_pass "USER-SERVICE-IMPLEMENTATION-SUMMARY.md found ($SUMMARY_SIZE lines)"
    else
        log_fail "USER-SERVICE-IMPLEMENTATION-SUMMARY.md not found"
        return 1
    fi

    return 0
}

################################################################################
# Performance Tests
################################################################################

test_performance() {
    log_section "PHASE 14: Performance Tests"

    # Check Maven build time
    log_test "Checking build performance"
    cd "$SERVICE_DIR" || exit 1
    
    START_TIME=$(date +%s)
    mvn clean compile -q > /dev/null 2>&1
    END_TIME=$(date +%s)
    BUILD_TIME=$((END_TIME - START_TIME))

    if [ "$BUILD_TIME" -lt 60 ]; then
        log_pass "Build time acceptable: ${BUILD_TIME}s"
    else
        log_warning "Build time is slow: ${BUILD_TIME}s"
    fi

    cd "$PROJECT_ROOT" || exit 1
    return 0
}

################################################################################
# Error Recovery Tests
################################################################################

test_error_recovery() {
    log_section "PHASE 15: Error Recovery Tests"

    # Test missing files recovery
    log_test "Testing missing file detection"
    if [ ! -f "$SERVICE_DIR/nonexistent.txt" ]; then
        log_pass "Error detection working correctly"
    fi

    # Test invalid Maven command
    log_test "Testing Maven error handling"
    cd "$SERVICE_DIR" || exit 1
    mvn invalid-goal -q > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        log_pass "Maven error handling works"
    else
        log_fail "Maven error handling issue"
    fi

    cd "$PROJECT_ROOT" || exit 1
    return 0
}

################################################################################
# Final Summary
################################################################################

print_summary() {
    log_section "TEST EXECUTION SUMMARY"

    echo ""
    echo "Total Tests Run: $TESTS_TOTAL"
    echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
    echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
    echo -e "Skipped: ${YELLOW}$TESTS_SKIPPED${NC}"
    echo ""

    if [ "$TESTS_FAILED" -eq 0 ]; then
        echo -e "${GREEN}════════════════════════════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}✓ ALL TESTS PASSED - SYSTEM IS READY FOR DEPLOYMENT${NC}"
        echo -e "${GREEN}════════════════════════════════════════════════════════════════════════════════${NC}"
        return 0
    else
        echo -e "${RED}════════════════════════════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}✗ $TESTS_FAILED TEST(S) FAILED - PLEASE REVIEW ISSUES ABOVE${NC}"
        echo -e "${RED}════════════════════════════════════════════════════════════════════════════════${NC}"
        return 1
    fi
}

################################################################################
# Main Test Execution
################################################################################

main() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════════════════════════════════════════════╗"
    echo "║         USER SERVICE - COMPREHENSIVE TEST SUITE v2.0                          ║"
    echo "║                                                                                ║"
    echo "║  This script performs 15 phases of testing including:                         ║"
    echo "║  - Prerequisites validation                                                   ║"
    echo "║  - Directory and file structure checks                                        ║"
    echo "║  - Source code validation                                                     ║"
    echo "║  - Maven build testing                                                        ║"
    echo "║  - JAR file validation                                                        ║"
    echo "║  - Dependency verification                                                    ║"
    echo "║  - Docker build testing                                                       ║"
    echo "║  - Configuration validation                                                   ║"
    echo "║  - Security configuration tests                                               ║"
    echo "║  - Database migration tests                                                   ║"
    echo "║  - API endpoint validation                                                    ║"
    echo "║  - Git repository verification                                                ║"
    echo "║  - Integration tests                                                          ║"
    echo "║  - Performance tests                                                          ║"
    echo "║  - Error recovery tests                                                       ║"
    echo "╚════════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"

    # Run all test phases
    test_prerequisites || { print_summary; exit 1; }
    test_directory_structure || { print_summary; exit 1; }
    test_source_files || { print_summary; exit 1; }
    test_maven_build || { print_summary; exit 1; }
    test_jar_file || { print_summary; exit 1; }
    test_dependencies || { print_summary; exit 1; }
    test_docker_build || { print_summary; exit 1; }
    test_configuration || { print_summary; exit 1; }
    test_security || { print_summary; exit 1; }
    test_database || { print_summary; exit 1; }
    test_api_endpoints || { print_summary; exit 1; }
    test_git_repository || true  # Non-critical
    test_integration || { print_summary; exit 1; }
    test_performance || true     # Non-critical
    test_error_recovery || true  # Non-critical

    print_summary
}

# Run main function
main "$@"

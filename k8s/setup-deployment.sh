#!/bin/bash

# EKS Deployment Setup Script for Product Catalog Service
# This script automates the deployment of the Product Catalog Service to AWS EKS
# Usage: ./k8s/setup-deployment.sh [dev|staging|production]

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
ENVIRONMENT="${1:-production}"
NAMESPACE=""
CLUSTER_NAME="${EKS_CLUSTER_NAME:-product-catalog-cluster}"
AWS_REGION="${AWS_REGION:-us-east-1}"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
    echo ""
}

# Check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"

    local tools=("kubectl" "kustomize" "aws" "grep")

    for tool in "${tools[@]}"; do
        if command -v "$tool" &> /dev/null; then
            log_success "$tool is installed"
        else
            log_error "$tool is not installed"
            exit 1
        fi
    done
}

# Validate environment
validate_environment() {
    print_header "Validating Environment"

    case "$ENVIRONMENT" in
        dev)
            NAMESPACE="product-catalog-dev"
            log_success "Using development environment"
            ;;
        staging)
            NAMESPACE="product-catalog-staging"
            log_success "Using staging environment"
            ;;
        production)
            NAMESPACE="product-catalog"
            log_success "Using production environment"
            ;;
        *)
            log_error "Invalid environment: $ENVIRONMENT"
            echo "Usage: $0 [dev|staging|production]"
            exit 1
            ;;
    esac
}

# Configure AWS and EKS access
configure_aws_access() {
    print_header "Configuring AWS Access"

    # Check AWS credentials
    if aws sts get-caller-identity &> /dev/null; then
        log_success "AWS credentials configured"
    else
        log_error "AWS credentials not configured"
        echo "Run 'aws configure' to setup AWS credentials"
        exit 1
    fi

    # Update kubeconfig
    log_info "Updating kubeconfig for EKS cluster: $CLUSTER_NAME"
    if aws eks update-kubeconfig \
        --name "$CLUSTER_NAME" \
        --region "$AWS_REGION" 2>/dev/null; then
        log_success "kubeconfig updated successfully"
    else
        log_error "Failed to update kubeconfig"
        log_info "Make sure the EKS cluster exists and you have appropriate permissions"
        exit 1
    fi
}

# Create namespace
create_namespace() {
    print_header "Creating Namespace"

    if kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_warning "Namespace $NAMESPACE already exists"
    else
        log_info "Creating namespace: $NAMESPACE"
        kubectl create namespace "$NAMESPACE"
        log_success "Namespace created"
    fi
}

# Create secrets
create_secrets() {
    print_header "Setting Up Secrets"

    read -p "Enter JWT Secret Key (or press Enter to skip): " jwt_secret
    if [ ! -z "$jwt_secret" ]; then
        kubectl delete secret product-catalog-secrets \
            -n "$NAMESPACE" 2>/dev/null || true

        log_info "Creating JWT secret..."
        kubectl create secret generic product-catalog-secrets \
            --from-literal=jwt-secret="$jwt_secret" \
            -n "$NAMESPACE"
        log_success "JWT secret created"
    else
        log_warning "Skipping JWT secret creation"
    fi

    read -p "Enter Database Password (or press Enter to skip): " db_password
    if [ ! -z "$db_password" ]; then
        log_info "Updating database password in secret..."
        kubectl patch secret product-catalog-secrets \
            --type merge \
            -p "{\"stringData\":{\"db-password\":\"$db_password\"}}" \
            -n "$NAMESPACE" 2>/dev/null || \
        kubectl create secret generic product-catalog-secrets \
            --from-literal=db-password="$db_password" \
            -n "$NAMESPACE"
        log_success "Database password set"
    else
        log_warning "Skipping database password configuration"
    fi
}

# Deploy application
deploy_application() {
    print_header "Deploying Application"

    local overlay_path="$SCRIPT_DIR/overlays/$ENVIRONMENT"

    if [ ! -d "$overlay_path" ]; then
        log_error "Overlay path not found: $overlay_path"
        exit 1
    fi

    log_info "Building Kustomization from: $overlay_path"

    if kubectl apply -k "$overlay_path"; then
        log_success "Application deployed successfully"
    else
        log_error "Deployment failed"
        exit 1
    fi
}

# Wait for deployment
wait_for_deployment() {
    print_header "Waiting for Deployment"

    log_info "Waiting for $NAMESPACE deployment to be ready..."

    if kubectl rollout status deployment/product-catalog \
        -n "$NAMESPACE" \
        --timeout=5m; then
        log_success "Deployment is ready"
    else
        log_error "Deployment failed to become ready"
        log_info "Checking pod status..."
        kubectl get pods -n "$NAMESPACE"
        exit 1
    fi
}

# Verify deployment
verify_deployment() {
    print_header "Verifying Deployment"

    log_info "Deployment Status:"
    kubectl get deployment -n "$NAMESPACE"

    log_info ""
    log_info "Pod Status:"
    kubectl get pods -n "$NAMESPACE" -o wide

    log_info ""
    log_info "Service Status:"
    kubectl get svc -n "$NAMESPACE"

    log_info ""
    log_info "Checking health endpoints..."

    # Port forward and test
    kubectl port-forward svc/product-catalog 8087:8087 -n "$NAMESPACE" &
    local pf_pid=$!
    sleep 3

    if curl -s http://localhost:8087/actuator/health | grep -q "UP"; then
        log_success "Application health check passed"
    else
        log_warning "Could not verify health endpoint"
    fi

    kill $pf_pid 2>/dev/null || true
}

# Display post-deployment information
post_deployment_info() {
    print_header "Post-Deployment Information"

    echo ""
    log_info "Deployment completed successfully!"
    echo ""
    log_info "Next Steps:"
    echo "1. Get Load Balancer endpoint:"
    echo "   kubectl get svc product-catalog-lb -n $NAMESPACE"
    echo ""
    echo "2. Port forward for local testing:"
    echo "   kubectl port-forward svc/product-catalog 8087:8087 -n $NAMESPACE"
    echo ""
    echo "3. View logs:"
    echo "   kubectl logs -f deployment/product-catalog -n $NAMESPACE"
    echo ""
    echo "4. Access Swagger UI:"
    echo "   http://localhost:8087/swagger-ui.html"
    echo ""
    echo "5. Monitor metrics:"
    echo "   kubectl port-forward svc/product-catalog 9090:8087 -n $NAMESPACE"
    echo "   # Visit http://localhost:9090/actuator/prometheus"
    echo ""
    echo "For more information, see:"
    echo "  - README: $SCRIPT_DIR/README.md"
    echo "  - Deployment Guide: $SCRIPT_DIR/DEPLOYMENT.md"
    echo "  - Troubleshooting: $SCRIPT_DIR/TROUBLESHOOTING.md"
    echo ""
}

# Main execution
main() {
    print_header "Product Catalog Service - EKS Deployment"

    log_info "Environment: $ENVIRONMENT"
    log_info "Region: $AWS_REGION"
    log_info "Cluster: $CLUSTER_NAME"
    echo ""

    check_prerequisites
    validate_environment
    configure_aws_access
    create_namespace
    create_secrets
    deploy_application
    wait_for_deployment
    verify_deployment
    post_deployment_info

    log_success "EKS deployment completed!"
}

# Run main function
main


# New robust Makefile for building and managing services
.PHONY: help build clean compile start stop restart status logs logs-eureka logs-hotel logs-booking logs-gateway start-eureka start-hotel start-booking start-gateway package run-jars dev backup

# Настройки
PROJECT_DIR := $(CURDIR)
LOGS_DIR := $(PROJECT_DIR)/logs
MODULES := spring-final-eureka spring-final-hotel spring-final-booking spring-final-gateway
EUREKA_PORT := 8761
GATEWAY_PORT := 8080
HOTEL_PORT := 8081
BOOKING_PORT := 8082

# Colors
GREEN  := \033[0;32m
YELLOW := \033[1;33m
RED    := \033[0;31m
NC     := \033[0m

help: ## Show this help
	@echo "$(GREEN)Makefile commands for Spring Final Project$(NC)"
	@echo ""
	@echo "  build            - full build (mvn clean install -DskipTests)"
	@echo "  compile          - compile all modules (mvn -DskipTests clean compile)"
	@echo "  start            - start all services (eureka -> hotel -> booking -> gateway)"
	@echo "  stop             - stop all running services (by pid files)"
	@echo "  restart          - restart all services"
	@echo "  status           - check ports to determine service status"
	@echo "  logs             - tail all logs"
	@echo "  logs-<service>   - tail specific service log (eureka, hotel, booking, gateway)"
	@echo "  package          - mvn package for all modules"
	@echo "  run-jars         - run built jars from target/"
	@echo "  dev SERVICE=<name> - run single service via mvn spring-boot:run"
	@echo "  backup           - create backup Makefile (Makefile.bak)"
	@echo ""

backup: ## create backup of this Makefile
	@cp $(PROJECT_DIR)/Makefile $(PROJECT_DIR)/Makefile.bak.new && echo "Backup created: Makefile.bak.new"

# Internal helper: ensure logs directory exists
.PHONY: _ensure_logs
_ensure_logs:
	@mkdir -p $(LOGS_DIR)

# Build / Compile
build: ## Full build
	@echo "$(YELLOW)Building project (skip tests)...$(NC)"
	mvn clean install -DskipTests
	@echo "$(GREEN)Build finished$(NC)"

compile: ## Compile only
	@echo "$(YELLOW)Compiling modules...$(NC)"
	mvn -DskipTests clean compile
	@echo "$(GREEN)Compile finished$(NC)"

package: build

# Start services (order matters)
start: _ensure_logs start-eureka start-hotel start-booking start-gateway
	@echo ""
	@echo "$(GREEN)All start tasks invoked. Check 'make status' and logs if needed.$(NC)"

# Start individual services
start-eureka: _ensure_logs
	@echo "$(YELLOW)Starting Eureka Server (port $(EUREKA_PORT))...$(NC)"
	@cd spring-final-eureka && \
		(nohup mvn spring-boot:run > $(LOGS_DIR)/eureka.log 2>&1 & echo $$! > $(LOGS_DIR)/eureka.pid) && \
		sleep 3 && \
		echo "Eureka started with PID: $$(cat $(LOGS_DIR)/eureka.pid 2>/dev/null || echo 'unknown')"

start-hotel: _ensure_logs
	@echo "$(YELLOW)Starting Hotel Service (port $(HOTEL_PORT))...$(NC)"
	@cd spring-final-hotel && \
		(nohup mvn spring-boot:run > $(LOGS_DIR)/hotel.log 2>&1 & echo $$! > $(LOGS_DIR)/hotel.pid) && \
		sleep 3 && \
		echo "Hotel started with PID: $$(cat $(LOGS_DIR)/hotel.pid 2>/dev/null || echo 'unknown')"

start-booking: _ensure_logs
	@echo "$(YELLOW)Starting Booking Service (port $(BOOKING_PORT))...$(NC)"
	@cd spring-final-booking && \
		(nohup mvn spring-boot:run > $(LOGS_DIR)/booking.log 2>&1 & echo $$! > $(LOGS_DIR)/booking.pid) && \
		sleep 3 && \
		echo "Booking started with PID: $$(cat $(LOGS_DIR)/booking.pid 2>/dev/null || echo 'unknown')"

start-gateway: _ensure_logs
	@echo "$(YELLOW)Starting Gateway Service (port $(GATEWAY_PORT))...$(NC)"
	@cd spring-final-gateway && \
		(nohup mvn spring-boot:run > $(LOGS_DIR)/gateway.log 2>&1 & echo $$! > $(LOGS_DIR)/gateway.pid) && \
		sleep 3 && \
		echo "Gateway started with PID: $$(cat $(LOGS_DIR)/gateway.pid 2>/dev/null || echo 'unknown')"

# Stop services by pid files
stop: ## Stop all services
	@echo "$(YELLOW)Stopping services...$(NC)"
	@if [ -f $(LOGS_DIR)/gateway.pid ]; then \
		kill $$(cat $(LOGS_DIR)/gateway.pid) 2>/dev/null || true; \
		rm -f $(LOGS_DIR)/gateway.pid; \
		echo "Gateway stopped"; \
	fi
	@if [ -f $(LOGS_DIR)/booking.pid ]; then \
		kill $$(cat $(LOGS_DIR)/booking.pid) 2>/dev/null || true; \
		rm -f $(LOGS_DIR)/booking.pid; \
		echo "Booking stopped"; \
	fi
	@if [ -f $(LOGS_DIR)/hotel.pid ]; then \
		kill $$(cat $(LOGS_DIR)/hotel.pid) 2>/dev/null || true; \
		rm -f $(LOGS_DIR)/hotel.pid; \
		echo "Hotel stopped"; \
	fi
	@if [ -f $(LOGS_DIR)/eureka.pid ]; then \
		kill $$(cat $(LOGS_DIR)/eureka.pid) 2>/dev/null || true; \
		rm -f $(LOGS_DIR)/eureka.pid; \
		echo "Eureka stopped"; \
	fi
	@pkill -f 'spring-final' 2>/dev/null || true
	@echo "$(GREEN)All services stopped$(NC)"

restart: stop start

# Status: checks ports and reports RUNNING/STOPPED
status: ## Check port listeners for services
	@echo "$(YELLOW)Service Status:$(NC)"
	@printf "  Eureka Server ($(EUREKA_PORT)):   "
	@if ss -tuln 2>/dev/null | grep -q ":$(EUREKA_PORT) " || lsof -Pi :$(EUREKA_PORT) -sTCP:LISTEN -t >/dev/null 2>&1; then \
		PID=$$(cat $(LOGS_DIR)/eureka.pid 2>/dev/null || echo "?"); \
		echo "$(GREEN)RUNNING$(NC) (PID: $$PID)"; \
	else \
		echo "$(RED)STOPPED$(NC)"; \
	fi
	@printf "  Hotel Service ($(HOTEL_PORT)):    "
	@if ss -tuln 2>/dev/null | grep -q ":$(HOTEL_PORT) " || lsof -Pi :$(HOTEL_PORT) -sTCP:LISTEN -t >/dev/null 2>&1; then \
		PID=$$(cat $(LOGS_DIR)/hotel.pid 2>/dev/null || echo "?"); \
		echo "$(GREEN)RUNNING$(NC) (PID: $$PID)"; \
	else \
		echo "$(RED)STOPPED$(NC)"; \
	fi
	@printf "  Booking Service ($(BOOKING_PORT)): "
	@if ss -tuln 2>/dev/null | grep -q ":$(BOOKING_PORT) " || lsof -Pi :$(BOOKING_PORT) -sTCP:LISTEN -t >/dev/null 2>&1; then \
		PID=$$(cat $(LOGS_DIR)/booking.pid 2>/dev/null || echo "?"); \
		echo "$(GREEN)RUNNING$(NC) (PID: $$PID)"; \
	else \
		echo "$(RED)STOPPED$(NC)"; \
	fi
	@printf "  API Gateway ($(GATEWAY_PORT)):     "
	@if ss -tuln 2>/dev/null | grep -q ":$(GATEWAY_PORT) " || lsof -Pi :$(GATEWAY_PORT) -sTCP:LISTEN -t >/dev/null 2>&1; then \
		PID=$$(cat $(LOGS_DIR)/gateway.pid 2>/dev/null || echo "?"); \
		echo "$(GREEN)RUNNING$(NC) (PID: $$PID)"; \
	else \
		echo "$(RED)STOPPED$(NC)"; \
	fi

# Logs
logs: ## Tail logs for all services
	@echo "$(YELLOW)Tailing logs (press Ctrl-C to exit)...$(NC)"
	@tail -n +1 -f $(LOGS_DIR)/*.log

logs-eureka:
	@tail -n 200 -f $(LOGS_DIR)/eureka.log

logs-hotel:
	@tail -n 200 -f $(LOGS_DIR)/hotel.log

logs-booking:
	@tail -n 200 -f $(LOGS_DIR)/booking.log

logs-gateway:
	@tail -n 200 -f $(LOGS_DIR)/gateway.log

# Run built jars (after package)
run-jars:
	@mkdir -p $(LOGS_DIR)
	@echo "Starting jars..."
	@java -jar spring-final-eureka/target/*.jar > $(LOGS_DIR)/eureka.log 2>&1 & echo $$! > $(LOGS_DIR)/eureka.pid
	@sleep 3
	@java -jar spring-final-hotel/target/*.jar > $(LOGS_DIR)/hotel.log 2>&1 & echo $$! > $(LOGS_DIR)/hotel.pid
	@sleep 3
	@java -jar spring-final-booking/target/*.jar > $(LOGS_DIR)/booking.log 2>&1 & echo $$! > $(LOGS_DIR)/booking.pid
	@sleep 3
	@java -jar spring-final-gateway/target/*.jar > $(LOGS_DIR)/gateway.log 2>&1 & echo $$! > $(LOGS_DIR)/gateway.pid
	@echo "$(GREEN)JARs started$(NC)"

# Dev: run a single service via maven
dev:
	@if [ -z "$(SERVICE)" ]; then echo "Specify SERVICE=eureka|hotel|booking|gateway"; exit 1; fi
	@cd spring-final-$(SERVICE) && mvn spring-boot:run

.DEFAULT_GOAL := help

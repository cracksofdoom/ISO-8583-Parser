APP_NAME := iso-8583-parser
VERSION := 1.0.2-SNAPSHOT
JAR := target/$(APP_NAME)-$(VERSION).jar
MVN := ./mvnw
COMPOSE := docker compose

.DEFAULT_GOAL := help

.PHONY: help run build package test verify clean rebuild jar deps deps-updates \
	docker-build docker-run docker-up docker-down docker-logs docker-ps docker-config

help: ## Show available commands
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z_-]+:.*## / {printf "  %-16s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

run: ## Start the site and API locally
	$(MVN) spring-boot:run

build: ## Compile application code
	$(MVN) compile

package: ## Build the runnable JAR
	$(MVN) package

test: ## Run all tests
	$(MVN) test

verify: ## Run the Maven verification lifecycle
	$(MVN) verify

clean: ## Remove build output
	$(MVN) clean

rebuild: ## Clean and build the runnable JAR
	$(MVN) clean package

jar: package ## Build and run the JAR
	java -jar $(JAR)

deps: ## Show the dependency tree
	$(MVN) dependency:tree

deps-updates: ## Show available dependency updates
	$(MVN) versions:display-dependency-updates

docker-config: ## Validate Compose configuration
	$(COMPOSE) config -q

docker-build: docker-config ## Build the application image
	$(COMPOSE) build

docker-up: docker-config ## Start the site and API
	$(COMPOSE) up -d --build

docker-run: docker-up ## Alias for docker-up

docker-down: ## Stop the site and API
	$(COMPOSE) down

docker-logs: ## Follow application logs
	$(COMPOSE) logs -f api

docker-ps: ## Show Compose service status
	$(COMPOSE) ps

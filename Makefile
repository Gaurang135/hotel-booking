APP         := hotel-booking
TAG         ?= latest
PORT        ?= 8081
BASE_URL    ?= http://localhost:$(PORT)
DOCKER_USER ?=
IMAGE       := $(APP):$(TAG)

.DEFAULT_GOAL := help
.PHONY: help run run-seeded seed test test-coverage build clean docker-build docker-run docker-push

help: ## List available commands
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  make %-18s %s\n", $$1, $$2}'

run: ## Start the app locally on http://localhost:8081
	PORT=$(PORT) ./mvnw spring-boot:run

run-seeded: ## Start the app locally and load demo data once it is up (scripts/seed.sh)
	@! curl -sf http://localhost:$(PORT)/actuator/health >/dev/null 2>&1 || \
		{ echo "An app is already running on port $(PORT). Stop it first, or seed it with: make seed"; exit 1; }
	@./scripts/seed.sh http://localhost:$(PORT) &
	@$(MAKE) --no-print-directory run

seed: ## Load demo data into an already running app (BASE_URL=http://localhost:8081)
	./scripts/seed.sh $(BASE_URL)

test: ## Run all tests
	./mvnw test

test-coverage: ## Run all tests with coverage (report: target/site/jacoco/index.html)
	./mvnw jacoco:prepare-agent test jacoco:report
	@awk -f scripts/coverage-summary.awk target/site/jacoco/jacoco.csv
	@echo "Full report: target/site/jacoco/index.html"

build: ## Build the jar (target/app.jar)
	./mvnw -DskipTests package

clean: ## Remove build output
	./mvnw -q clean

docker-build: ## Build the Docker image
	docker build -t $(IMAGE) .

docker-run: docker-build ## Run the app in Docker on http://localhost:8081
	docker run --rm --name $(APP) -p $(PORT):8081 $(IMAGE)

docker-push: ## Build for linux/amd64 and push to Docker Hub (DOCKER_USER=<username>)
	@test -n "$(DOCKER_USER)" || { echo "Usage: make docker-push DOCKER_USER=<your-dockerhub-username> [TAG=v1]"; exit 1; }
	docker buildx build --platform linux/amd64 -t $(DOCKER_USER)/$(IMAGE) --push .

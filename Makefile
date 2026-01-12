.PHONY: help web-install web-build web-test web-lint web-typecheck android-build android-test clean

help:
	@echo "GRIN build helpers"
	@echo "  make web-install   Install web dependencies"
	@echo "  make web-build     Build web library"
	@echo "  make web-test      Run web tests"
	@echo "  make web-lint      Lint web code"
	@echo "  make web-typecheck Typecheck web code"
	@echo "  make android-build Build Android library"
	@echo "  make android-test  Run Android unit tests"
	@echo "  make clean         Clean build outputs"

web-install:
	cd web && npm install

web-build:
	cd web && npm run build

web-test:
	cd web && npm test

web-lint:
	cd web && npm run lint

web-typecheck:
	cd web && npm run typecheck

android-build:
	cd android && ./gradlew :lib:assembleRelease

android-test:
	cd android && ./gradlew :lib:testReleaseUnitTest

clean:
	cd web && npm run clean
	cd android && ./gradlew clean

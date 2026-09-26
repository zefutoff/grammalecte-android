GRADLE ?= $(if $(wildcard ./gradlew),./gradlew,gradle)

.PHONY: bootstrap vendor check test lint assemble instrumented clean

bootstrap:
	./tools/bootstrap-gradle-wrapper.sh

vendor:
	./tools/vendor-grammalecte.sh

check:
	./tools/check-no-network-permission.sh
	node tools/test-js-bridge.mjs
	$(GRADLE) --no-daemon ktlintCheck :core:test :engine-grammalecte:test :spellchecker:test :app:lintDebug

test:
	$(GRADLE) --no-daemon :core:test :engine-grammalecte:test :spellchecker:test

lint:
	$(GRADLE) --no-daemon ktlintCheck :app:lintDebug

assemble:
	$(GRADLE) --no-daemon :app:assembleDebug

instrumented:
	$(GRADLE) --no-daemon :app:connectedDebugAndroidTest

clean:
	$(GRADLE) --no-daemon clean

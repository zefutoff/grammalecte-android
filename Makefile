GRADLE := ./gradlew

.PHONY: bootstrap vendor check test lint assemble instrumented clean

bootstrap:
	./tools/bootstrap-gradle-wrapper.sh

vendor:
	./tools/vendor-grammalecte.sh

check:
	./tools/check-no-network-permission.sh
	shellcheck tools/*.sh
	node tools/test-js-bridge.mjs
	$(GRADLE) --no-daemon ktlintCheck :core:test :engine-grammalecte:test :spellchecker:test :app:lintDebug

test:
	$(GRADLE) --no-daemon :core:test :engine-grammalecte:test :spellchecker:test

lint:
	$(GRADLE) --no-daemon ktlintCheck :app:lintDebug

assemble:
	$(GRADLE) --no-daemon :app:assembleDebug
	./tools/check-apk-permissions.sh app/build/outputs/apk/debug/app-debug.apk

instrumented:
	$(GRADLE) --no-daemon :app:connectedDebugAndroidTest

clean:
	$(GRADLE) --no-daemon clean

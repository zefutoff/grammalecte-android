GRADLE := ./gradlew

.PHONY: bootstrap vendor check performance-check stress-check release-reproducibility test lint assemble instrumented refresh-verification-metadata clean

bootstrap:
	./tools/bootstrap-gradle-wrapper.sh

vendor:
	./tools/vendor-grammalecte.sh

check:
	./tools/check-no-network-permission.sh
	shellcheck tools/*.sh
	./tools/run-actionlint.sh
	node tools/test-js-bridge.mjs
	$(GRADLE) --no-daemon ktlintCheck :core:test :engine-grammalecte:test :spellchecker:test :app:lintDebug

performance-check:
	$(GRADLE) --no-daemon \
		:engine-grammalecte:testDebugUnitTest \
		--tests '*EnginePerformanceRegressionTest*' \
		-PgrammalectePerf=1 \
		--rerun-tasks

stress-check:
	$(GRADLE) --no-daemon \
		:engine-grammalecte:testDebugUnitTest \
		--tests '*EngineRuntimeStressTest*' \
		-PgrammalecteStress=1 \
		--rerun-tasks

release-reproducibility:
	./tools/check-release-reproducibility.sh

test:
	$(GRADLE) --no-daemon :core:test :engine-grammalecte:test :spellchecker:test

lint:
	$(GRADLE) --no-daemon ktlintCheck :app:lintDebug

assemble:
	$(GRADLE) --no-daemon :app:assembleDebug
	./tools/check-apk-permissions.sh app/build/outputs/apk/debug/app-debug.apk

instrumented:
	$(GRADLE) --no-daemon :app:connectedDebugAndroidTest

refresh-verification-metadata:
	$(GRADLE) --no-daemon --refresh-dependencies --write-verification-metadata sha256 \
		ktlintCheck \
		:core:test \
		:engine-grammalecte:test \
		:spellchecker:test \
		:app:lintDebug \
		:app:assembleDebug \
		:spellchecker:assembleDebugAndroidTest \
		:app:assembleDebugAndroidTest

clean:
	$(GRADLE) --no-daemon clean

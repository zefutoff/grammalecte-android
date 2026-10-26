GRADLE := ./gradlew

.PHONY: bootstrap vendor check performance-check stress-check release-reproducibility fdroid-reproducibility signed-release unsigned-release sbom test lint assemble instrumented refresh-verification-metadata clean

bootstrap:
	./tools/bootstrap-gradle-wrapper.sh

vendor:
	./tools/vendor-grammalecte.sh

check:
	./tools/check-android-package-id.sh
	./tools/check-no-network-permission.sh
	shellcheck tools/*.sh
	./tools/run-actionlint.sh
	node tools/check-fdroid-metadata.mjs
	node tools/test-js-bridge.mjs
	python3 -m unittest discover -s tools -p 'test_instrumentation_results.py' -v
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

fdroid-reproducibility:
	./tools/check-fdroid-reproducibility.sh

unsigned-release:
	$(GRADLE) --no-daemon :app:assembleRelease
	test -s app/build/outputs/apk/release/app-release-unsigned.apk
	./tools/check-apk-permissions.sh app/build/outputs/apk/release/app-release-unsigned.apk

signed-release:
	$(GRADLE) :app:assembleRelease \
		--no-daemon \
		--no-build-cache \
		--no-configuration-cache
	./tools/check-apk-permissions.sh app/build/outputs/apk/release/app-release-unsigned.apk
	./tools/sign-release-apk.sh
	./tools/check-apk-permissions.sh app/build/outputs/apk/release/app-release-signed.apk

sbom:
	$(GRADLE) --no-daemon cyclonedxBom
	node tools/enrich-sbom.mjs
	node tools/check-sbom.mjs

test:
	$(GRADLE) --no-daemon :core:test :engine-grammalecte:test :spellchecker:test

lint:
	$(GRADLE) --no-daemon ktlintCheck :app:lintDebug

assemble:
	$(GRADLE) --no-daemon :app:assembleDebug
	./tools/check-apk-permissions.sh app/build/outputs/apk/debug/app-debug.apk

instrumented:
	python3 tools/instrumentation_results.py preflight
	python3 tools/instrumentation_results.py prepare spellchecker
	$(GRADLE) --no-daemon --no-parallel :spellchecker:connectedDebugAndroidTest
	python3 tools/instrumentation_results.py verify spellchecker
	python3 tools/instrumentation_results.py prepare app
	$(GRADLE) --no-daemon --no-parallel :app:connectedDebugAndroidTest
	python3 tools/instrumentation_results.py verify app

refresh-verification-metadata:
	$(GRADLE) --no-daemon --refresh-dependencies --write-verification-metadata sha256 \
		ktlintCheck \
		:core:test \
		:engine-grammalecte:test \
		:spellchecker:test \
		:app:lintDebug \
		:app:assembleDebug \
		:spellchecker:assembleDebugAndroidTest \
		:app:assembleDebugAndroidTest \
		cyclonedxBom

clean:
	$(GRADLE) --no-daemon clean

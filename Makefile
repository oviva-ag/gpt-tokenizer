INTERACTIVE:=$(shell [ -t 0 ] && echo 1)

ifndef INTERACTIVE
MAVEN_CLI_OPTS+=--batch-mode
endif

all: clean
	@mvn $(MAVEN_CLI_OPTS) install

# for now do a clean before in order to avoid generating javadoc/soure archives
deploy: clean
	#WORKAROUND: GitHub Package Registry currently fails to manage source & javadoc snapshot dependencies, disable for now
	#See: https://github.community/t/error-on-github-packages-for-snapshot-maven-artifacts-with-a-classifier/116732/5
	@mvn $(MAVEN_CLI_OPTS) -Dmaven.wagon.http.pool=false -Dmaven.javadoc.skip=true -Dmaven.source.skip=true deploy

clean:
	@mvn $(MAVEN_CLI_OPTS) clean

update-version:
ifndef VERSION
	$(error VERSION is not set)
endif
	@mvn -B versions:set "-DnewVersion=$(VERSION)"

.PHONY: all clean deploy update-version

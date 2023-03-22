# This makefile is not used to build/compile source files to binaries on CI
# The purpose here is to provide organized shortcuts
# and entry points for running tools and invoking builds during development.
# (Makefiles especially convenient for expressing and executing dependent goals
# where simple bash scripts would need to constantly check if something is already done.)
#
# Please, leave this file as brief and high level as possible. The makefile provides
# birds eye view of how this repository works, major things what can be done with it.
# What belongs here:
# * download, install, run and cleanup for tools, project file generation
# * invoke high-level build goals and tools

# phony targets are not bound to existence of directories or files by the same name
# and always re-executed (once per make call)
.PHONY: default build_java

.DEFAULT_GOAL := default
# explicitly requiring /bin/bash
# without this it's hard to setup any env vars in container
SHELL := /bin/bash

# here lower_snake_case variables are private to Makefile,
# UPPER_SNAKE_CASE ones we reserve for the ones coming from environment
# or special reserved Makefile variables.
# '=' lazy var, ':=' eagerly computed var, '?=' assign if not set

prereq_bin = git curl java
prereq_check = $(foreach bin,$(prereq_bin),$(if $(shell which $(bin)),,\
		$(error "No `$(bin)` found in PATH. see README.md")))

build_cp := ".build/classes"
build_src := "build"

# The default is just to fetch, build, test all targets
default: build_java
	@printf "\e[00;33m[build]\e[00m\n"
	@java -ea --enable-preview \
	-Xmx512m -Xms512m \
	-classpath $(build_cp) \
	Build a b c

# This only creates dir, we recompile every time in phony goal "build_java"
$(build_cp):
	@mkdir -p $@

# Compile build script and it's dependencies
build_java: $(build_cp)
	$(prereq_check)
	@javac \
	--release 17 \
	--enable-preview \
	-sourcepath $(build_src) \
	-d $(build_cp) \
	build/Build.java build/build/*.java

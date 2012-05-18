# Pallet-Common Release Notes

The latest 0.2.x release is 0.2.3.

## 0.2.3

- Add a function for throwing a map within a context

- Add slingshot version compatibility

## 0.2.2

### Features
- Add utility for testing code that should throw slingshot exceptions

  Adds is-thrown-slingshot?, is-thrown-with-msg-slingshot?, ex-data,
  slingshot-exception and slingshot-object

- Enable generation of :deprecated meta data in pallet.common.deprecate

## 0.2.1

### Features
- Propogate exception message when context exception wrapping

- Upgrade to tools.logging 0.2.0
  Introduce tools-logging-compat implementation macro to provide
  implementation details across 0.1.2 and 0.2.0

- Add pallet.common.context for hierarchical contexts
  Provides a facility for maintaining hierarchical contexts, with
  extensible behaviour on entering and exiting a context.

  Add try-context, with-context, with-logged-context and throw+
  Remove atom from common.context, and made on-enter and on-exit callbacks
  composed with juxt.

- Add logging-to-string to pallet.common.logging.logutils
  Logs to a string via with-out-str, and logging-to-stdount

- Add strint from clojure.contrib

- Add map-utils/deep-merge-with from clojure.contrib


### Fixes

- Change forward-no-warn to generate a forwarding function
  The forwarding was failing when used with protocol functions, due to lack
  of metadata on the forwarded var

- Remove extraneous println's from context-test

- Improve robustness of logger specific namespace loading
  The loading of namespaces from a macro was leading to unbound vars.
  Requiring the namespaces at the top level seems to fix this.

## 0.2.0

- Add name-with-attributes from monolithic clojure contrib

- Make content optional for with-temp-file

- Remove use of clojure.contrib

- Fixes for switch to tools.logging and logback

- Add pallet.common.logging.logutils
  Utility functions for manipulating loggers and levels, with either log4j
  or slf4j/logback

- Add logging context support based on slf4j

- Update common.string to follow clojure.string design
  Changed type hints to accept CharSequence arguments and return String.
  Changed argument order to take the string object as the first argument.
  Renamed add-quotes to quoted. Added two index version of substring.

- Added underscore

- Moved utility functions from stevedore

## 0.1.0

This is the initial standalone release.  The library has been extracted based on
functions from the main [pallet repository](https://github.com/pallet/pallet).

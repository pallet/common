# thread-expr Release Notes

The latest release is 0.2.0.

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

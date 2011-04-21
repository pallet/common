(ns pallet.common.shell
  "Shell functions"
  (:require
   [pallet.common.filesystem :as filesystem]
   [clojure.contrib.shell :as shell]))

(defn system
  "Launch a system process, return a map containing the exit code, standard
  output and standard error of the process."
  [cmd]
  (apply shell/sh :return-map true (.split cmd " ")))

(defn bash [cmds]
  (filesystem/with-temp-file [file cmds]
    (shell/sh :return-map true "/usr/bin/env" "bash"  (.getPath file))))

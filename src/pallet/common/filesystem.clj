(ns pallet.common.filesystem
  "Filesystem functions"
  (:require
   [clojure.java.io :as io]))

(defmacro with-temp-file
  "Create a block where `varname` is a temporary `File` containing `content`."
  [[varname & [content]] & body]
  `(let [~varname (java.io.File/createTempFile "stevedore", ".tmp")]
     (when-let [content# ~content]
       (io/copy content# ~varname))
     (let [rv# (do ~@body)]
       (.delete ~varname)
       rv#)))

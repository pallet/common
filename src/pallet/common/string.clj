(ns pallet.common.string
  "String utilities

   Follows design notes from clojure.string"
  (:require
    [clojure.string :as string]))

(defn ^String substring
  "The one index version drops first n characters from s.  Returns an empty
   string if n is greater than the length of s.

   The two index version returns a substring between the two indices.

       (substring \"test\" 1) => \"est\"
       (substring \"test\" 1 3) => \"es\""
  ([^CharSequence s n]
     (if (< (count s) n)
       ""
       (.substring s n)))
  ([^CharSequence s n m]
     (if (< (count s) n)
       ""
       (.substring s n (min m (count s))))))

(defn ^String quoted
  "Add quotes to the argument s as a string"
  [s]
  (str "\"" s "\""))

(defn ^String underscore
  "Replace all occurances of - with _"
  [^CharSequence s]
  (string/replace s \- \_))

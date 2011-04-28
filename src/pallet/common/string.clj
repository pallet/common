(ns pallet.common.string)

(defn ^String substring
    "Drops first n characters from s.  Returns an empty string if n is
      greater than the length of s."
    [n ^String s]
    (if (< (count s) n)
          ""
          (.substring s n)))

(defn ^String add-quotes
    "Add quotes to the argument s as a string"
    [s]
    (str "\"" s "\""))

(ns agnesse.slim
  "A Slim 0.5 server. Port 1 is stdin and stdout. Any other port accepts one
  socket on 127.0.0.1. A lanes instruction runs each lane on its own thread.
  Symbols follow the Java forms, and call arguments convert through a signature
  table because a fixture method has no reflected parameter types."
  (:import [java.io InputStream OutputStream]
           [java.net InetAddress ServerSocket Socket]
           [java.util.regex Pattern]))

(def catalog
  (str "beginTable() -> string\n"
       "block() -> string\n"
       "book() -> string\n"
       "echo(string) -> string\n"
       "endIsAfterStart(string, string) -> string\n"
       "endTable() -> string\n"
       "execute() -> string\n"
       "day(date) -> string\n"
       "flag(boolean) -> string\n"
       "hello() -> string\n"
       "initial(char) -> string\n"
       "join(list) -> string\n"
       "kind(string) -> string\n"
       "label(map) -> string\n"
       "open() -> string\n"
       "phase() -> string\n"
       "plus(int, int) -> string\n"
       "release() -> string\n"
       "released() -> string\n"
       "reset() -> string\n"
       "scale(double) -> string\n"
       "setWord(string) -> string\n"
       "shade(enum) -> string\n"
       "table(list) -> string\n"
       "wide(long) -> string"))

(def helper-catalog
  "cloneSymbol(object) -> object\npopFixture() -> string\npushFixture() -> string")

(def book-lock (Object.))

(defn text [row index]
  (let [value (nth row index nil)]
    (if (string? value) value "")))

(defn rejected? [number]
  (< number 0))

(defn parse-port [raw]
  (try
    (let [number (Integer/parseInt raw)]
      (if (rejected? number) :bad number))
    (catch Exception _ :bad)))

(defn command-port [args]
  (let [raw (last args)]
    (cond
      (nil? raw) :bad
      (= "1" raw) :pipe
      :else (parse-port raw))))

(defn status [failed]
  (if failed 1 0))

(defn class-name [raw]
  (if (= "" raw) "slim" raw))

(defn opened [klass mark]
  (if (or (= "racy" mark) (= "safe" mark))
    {:phase "" :seats 1 :race mark :word "" :class (class-name klass)}
    {:phase "new" :seats nil :race "" :word "" :class (class-name klass)}))

(declare as-text expand-cell label-row)

(defn actor-from [raw symbols]
  (when (string? raw)
    (when (.startsWith raw "$")
      (when (> (count raw) 1)
        (let [key (subs raw 1)]
          (when (contains? symbols key)
            (let [value (get symbols key)]
              (when-not (string? value) value))))))))

(defn made [row instances symbols]
  (let [name (text row 2)]
    (if (= "" name)
      "OK"
      (if-let [actor (actor-from (nth row 3 nil) symbols)]
        (do (swap! instances assoc name actor) "OK")
        (let [class-name (as-text (expand-cell (text row 3) symbols))]
          (swap! instances assoc name (atom (opened class-name (text row 4))))
          "OK")))))

(defn missing-instance [name]
  (str "__EXCEPTION__:message:<<NO_INSTANCE " name ">>"))

(defn seat? [value]
  (and (instance? clojure.lang.IAtom value)
       (map? @value)
       (contains? @value :class)))

(defn no-method [method args klass]
  (str "__EXCEPTION__:message:<<NO_METHOD_IN_CLASS No Method "
       method "[" args "] in class " (class-name klass)
       ".\n Available methods:\n"
       (if (= "SlimHelperLibrary" klass) helper-catalog catalog) ">>"))

(defn unknown [method name args seat]
  (if (seat? seat)
    (no-method method args (:class @seat))
    (missing-instance name)))

(defn move [seat needed next]
  (if (not (seat? seat))
    "refused"
    (if (not= needed (:phase @seat))
      "refused"
      (do (swap! seat assoc :phase next) "ok"))))

(defn phase-of [seat]
  (if (seat? seat) (:phase @seat) ""))

(defn released-of [seat]
  (if (and (seat? seat) (= "released" (:phase @seat))) "true" "false"))

(defn store-word [seat word]
  (when (seat? seat) (swap! seat assoc :word word))
  "OK")

(defn word-of [seat]
  (if (or (not (seat? seat)) (= "" (:word @seat))) "OK" (:word @seat)))

(defn whole [value]
  (when (and (string? value) (re-matches #"-?\d+" value))
    (Integer/parseInt value)))

(defn ordered [args]
  (let [left (whole (first args))
        right (whole (second args))]
    (if (and left right (> right left)) "yes" "no")))

(defn kind-of [args]
  (if (.contains (str (first args)) "\"kind\":\"id\"") "id" "no"))

(defn echo-of [seat args]
  (if (empty? args) (word-of seat) (if (string? (first args)) (first args) "OK")))

(defn race-book [seat]
  (let [seen (:seats @seat)]
    (if (or (nil? seen) (<= seen 0))
      "full"
      (do (Thread/sleep 50)
          (swap! seat assoc :seats (dec seen))
          "booked"))))

(defn safe-book [seat]
  (locking book-lock
    (let [seen (:seats @seat)]
      (if (or (nil? seen) (<= seen 0))
        "full"
        (do (swap! seat assoc :seats (dec seen)) "booked")))))

(defn book [seat name]
  (cond
    (not (seat? seat)) (missing-instance name)
    (= "" (:race @seat)) (missing-instance name)
    (= "racy" (:race @seat)) (race-book seat)
    :else (safe-book seat)))

(defn plus [args]
  (let [left (first args)
        right (second args)]
    (if (not (int? left))
      "no"
      (if (not (int? right))
        "no"
        (str (+ left right))))))

(defn wide [args]
  (if (instance? Long (first args)) (str (first args)) "no"))

(defn scale [args]
  (if (instance? Double (first args)) (str (first args)) "no"))

(defn flag [args]
  (if (true? (first args)) "yes" "no"))

(defn initial [args]
  (let [text (first args)]
    (if (and (string? text) (= 1 (count text))) text "no")))

(defn dated? [text]
  (some? (try (java.time.LocalDate/parse text) (catch Exception _ nil))))

(defn day [args]
  (let [text (first args)]
    (if (and (string? text) (dated? text)) text "no")))

(defn shade [args]
  (let [text (first args)]
    (if (contains? #{"red" "green" "blue"} text) text "no")))

(defn join [args]
  (let [items (first args)]
    (if (sequential? items) (clojure.string/join "," (map as-text items)) "no")))

(defn label [args]
  (let [rows (first args)]
    (if (sequential? rows) (label-row rows) "no")))

(defn label-row [rows]
  (if (empty? rows)
    "no"
    (let [row (first rows)]
      (if (and (sequential? row) (= "name" (as-text (first row))))
        (as-text (second row))
        (let [tail (rest rows)]
          (if (seq? tail)
            (if (empty? tail) "no" (label-row tail))
            "broken"))))))

(defn numbers [method args]
  (case method
    "plus" (plus args)
    "wide" (wide args)
    "scale" (scale args)
    "flag" (flag args)
    nil))

(defn greetings [method args]
  (case method
    "hello" "world"
    "endIsAfterStart" (ordered args)
    "kind" (kind-of args)
    "initial" (initial args)
    nil))

(defn shaped [method args]
  (case method
    "day" (day args)
    "shade" (shade args)
    "join" (join args)
    "label" (label args)
    nil))

(defn plain [method args]
  (or (numbers method args) (greetings method args) (shaped method args)))

(defn lifecycle? [method]
  (contains? #{"table" "beginTable" "reset" "execute" "endTable"} method))

(defn transition [method seat]
  (case method
    "open" (move seat "new" "open")
    "block" (move seat "open" "blocked")
    "release" (move seat "open" "released")
    nil))

(defn asked [method name arg args seat]
  (cond
    (= "book" method) (book seat name)
    (= "phase" method) (phase-of seat)
    (= "released" method) (released-of seat)
    (= "setWord" method) (store-word seat arg)
    (lifecycle? method) "OK"
    :else (unknown method name (count args) seat)))

(defn on-seat [method name arg args seat]
  (let [moved (transition method seat)]
    (if (some? moved) moved (asked method name arg args seat))))

(def symbol-pattern
  (Pattern/compile "\\$(([A-Za-z\\p{L}][\\w\\p{L}]*)|`([^`]+)`)"))

(def assignment-pattern
  (Pattern/compile "\\A\\s*\\$([A-Za-z\\p{L}][\\w\\p{L}]*)\\s*=\\s*\\z"))

(def signatures
  {"plus" ["int" "int"]
   "wide" ["long"]
   "scale" ["double"]
   "flag" ["boolean"]
   "initial" ["char"]
   "day" ["date"]
   "shade" ["enum"]
   "join" ["list"]
   "label" ["map"]})

(defn as-text [value]
  (cond
    (string? value) value
    (nil? value) "null"
    (sequential? value) (str "[" (clojure.string/join ", " (map as-text value)) "]")
    :else (str value)))

(defn assignment? [text]
  (boolean (and (string? text) (.matches (.matcher assignment-pattern text)))))

(defn shorter-name [name symbols]
  (if (<= (count name) 1)
    nil
    (let [next-name (subs name 0 (dec (count name)))]
      (if (contains? symbols next-name)
        [next-name (get symbols next-name)]
        (shorter-name next-name symbols)))))

(defn resolve-name [name symbols]
  (if (contains? symbols name)
    [name (get symbols name)]
    (shorter-name name symbols)))

(defn substitute [text symbols]
  (let [matcher (.matcher symbol-pattern text)]
    (if-not (.find matcher)
      text
      (let [found (resolve-name (.group matcher 1) symbols)]
        (if (nil? found)
          (str (subs text 0 (.end matcher)) (substitute (subs text (.end matcher)) symbols))
          (let [[used value] found
                end (+ (.start matcher) 1 (count used))]
            (str (subs text 0 (.start matcher)) (as-text value) (substitute (subs text end) symbols))))))))

(defn whole-stored [text symbols]
  (if-not (string? text)
    ::miss
    (if-not (.startsWith text "$")
      ::miss
      (if-not (> (count text) 1)
        ::miss
        (if (contains? symbols (subs text 1))
          (get symbols (subs text 1))
          ::miss)))))

(defn expand-cell [text symbols]
  (let [stored (whole-stored text symbols)]
    (if (not= stored ::miss)
      stored
      (if (assignment? text)
        ""
        (substitute text symbols)))))

(defn expand-string [text symbols]
  (as-text (expand-cell text symbols)))

(defn expand [value symbols]
  (cond
    (string? value) (expand-cell value symbols)
    (sequential? value) (mapv #(expand % symbols) value)
    :else value))

(defn args-from [row index]
  (vec (drop index row)))

(defn parsed-int [text]
  (try (Integer/parseInt text) (catch Exception _ text)))

(defn parsed-long [text]
  (try (Long/parseLong text) (catch Exception _ text)))

(defn parsed-double [text]
  (try (Double/parseDouble text) (catch Exception _ text)))

(defn parsed-list [text]
  (if (and (string? text) (.startsWith text "[") (.endsWith text "]"))
    (let [body (.trim (subs text 1 (dec (count text))))]
      (if (= "" body) [] (mapv #(.trim %) (.split body ","))))
    text))

(defn convert [value kind]
  (if (string? value)
    (case kind
      "int" (parsed-int value)
      "long" (parsed-long value)
      "double" (parsed-double value)
      "boolean" (= "true" value)
      "list" (parsed-list value)
      value)
    value))

(defn kind-at [kinds index]
  (if (< index (count kinds)) (nth kinds index) "string"))

(defn convert-args [method args]
  (if-let [kinds (get signatures method)]
    (mapv (fn [index arg] (convert arg (kind-at kinds index))) (range) args)
    args))

(defn opt-pattern [value]
  (try (Pattern/compile (str value)) (catch Exception _ nil)))

(defn skips? [symbols instance method]
  (boolean
    (when-let [value (get symbols "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS")]
      (when-let [pattern (opt-pattern value)]
        (.matches (.matcher pattern (str instance "." method)))))))

(defn helper? [value]
  (and (map? value) (:helper value)))

(defn ensure-helper [instances]
  (when-not (contains? @instances "SlimHelperLibrary")
    (swap! instances assoc "SlimHelperLibrary" {:helper true :stack (atom [])})))

(defn pop-fixture [helper instances]
  (let [stack (:stack helper)]
    (if (empty? @stack)
      "OK"
      (let [actor (peek @stack)]
        (swap! stack pop)
        (when (some? actor) (swap! instances assoc "scriptTableActor" actor))
        "OK"))))

(defn helper-call [helper method args instances]
  (case method
    "pushFixture" (do (swap! (:stack helper) conj (get @instances "scriptTableActor")) "OK")
    "popFixture" (pop-fixture helper instances)
    "cloneSymbol" (first args)
    (no-method method (count args) "SlimHelperLibrary")))

(defn first-text [args]
  (let [value (first args)]
    (if (string? value) value "")))

(defn call [row instance-at method-at instances symbols]
  (let [method (text row method-at)
        name (text row instance-at)
        raw (args-from row (inc method-at))
        table @symbols
        args (convert-args method (if (skips? table name method) raw (mapv #(expand % table) raw)))
        target (get @instances name)]
    (cond
      (helper? target) (helper-call target method args instances)
      (= "echo" method) (echo-of target args)
      (and (= "self" method) (seat? target)) target
      :else (if-let [value (plain method args)]
              value
              (on-seat method name (first-text args) args target)))))

(defn remember [symbols name value]
  (when-not (= "" name) (swap! symbols assoc name value))
  value)

(defn stamp! [clock]
  (str (swap! clock inc)))

(defn lane-calls [group]
  (let [calls (second group)]
    (when (sequential? calls) calls)))

(declare reply)

(defn one-call [call-row tag clock rows instances symbols]
  (when (sequential? call-row)
    (let [started (stamp! clock)
          value (reply call-row instances symbols)
          finished (stamp! clock)]
      (swap! rows conj [tag (text call-row 0) started finished value]))))

(defn run-group [group clock rows instances symbols]
  (when-let [calls (lane-calls group)]
    (doseq [call-row calls]
      (one-call call-row (text group 0) clock rows instances symbols))))

(defn run-lanes [row instances symbols]
  (if (<= (count row) 2)
    "lanes"
    (let [clock (atom 0)
          rows (atom [])
          tasks (mapv (fn [group]
                        (future (run-group group clock rows instances symbols)))
                      (drop 2 row))]
      (doseq [task tasks] @task)
      @rows)))

(defn reply [row instances symbols]
  (case (text row 1)
    "make" (made row instances @symbols)
    "call" (call row 2 3 instances symbols)
    "callAndAssign" (remember symbols (text row 2) (call row 3 4 instances symbols))
    "assign" (do (remember symbols (text row 2) (nth row 3 nil)) "OK")
    "lanes" (run-lanes row instances symbols)
    "OK"))

(defn answer [instructions instances symbols]
  (ensure-helper instances)
  (into []
        (keep (fn [row]
                (when (sequential? row)
                  [(text row 0) (reply row instances symbols)])))
        instructions))

(defn field [text]
  (format "%06d:%s:" (count text) text))

(defn serialize [value]
  (if (sequential? value)
    (str "[" (format "%06d:" (count value)) (apply str (map (comp field serialize) value)) "]")
    (str value)))

(defn read-length [text index]
  (let [end (clojure.string/index-of text ":" index)]
    (if (nil? end)
      [0 (count text)]
      (try
        [(Integer/parseInt (subs text index end)) (inc end)]
        (catch Exception _ [0 (inc end)])))))

(defn read-field [text index]
  (let [[length next-index] (read-length text index)
        end (+ next-index length)]
    (if (> end (count text))
      nil
      [(subs text next-index end) (inc end)])))

(declare read-list)

(defn field-value [raw]
  (if (clojure.string/starts-with? raw "[")
    (first (read-list raw))
    raw))

(defn read-list [text]
  (if-not (clojure.string/starts-with? text "[")
    [nil 0]
    (let [[count index] (read-length text 1)]
      (loop [left count at index items []]
        (if (<= left 0)
          [items at]
          (if-let [[raw next-at] (read-field text at)]
            (recur (dec left) next-at (conj items (field-value raw)))
            [items at]))))))

(defn parse [text]
  (or (first (read-list text)) []))

(defn read-one [^InputStream in]
  (let [n (.read in)]
    (when-not (neg? n) (char n))))

(defn read-prefix [^InputStream in]
  (loop [prefix "" seen 0]
    (if (> seen 10)
      nil
      (let [ch (read-one in)]
        (cond
          (nil? ch) nil
          (= \: ch) prefix
          :else (recur (str prefix ch) (inc seen)))))))

(defn read-exact [^InputStream in length]
  (loop [left length out ""]
    (if (<= left 0)
      out
      (let [ch (read-one in)]
        (when ch (recur (dec left) (str out ch)))))))

(defn read-message [^InputStream in]
  (when-let [prefix (read-prefix in)]
    (try
      (let [length (Integer/parseInt prefix)]
        (when-not (rejected? length) (read-exact in length)))
      (catch Exception _ nil))))

(defn write-message [^OutputStream out value]
  (let [body (.getBytes (serialize value) "UTF-8")
        header (.getBytes (format "%06d:" (count body)) "UTF-8")]
    (.write out header)
    (.write out body)
    (.flush out)))

(defn speak [^InputStream in ^OutputStream out instances symbols]
  (.write out (.getBytes "Slim -- V0.5\n" "UTF-8"))
  (.flush out)
  (loop []
    (when-let [message (read-message in)]
      (when-not (= "bye" (clojure.string/lower-case message))
        (write-message out (answer (parse message) instances symbols))
        (recur)))))

(defn serve [port]
  (try
    (let [socket (ServerSocket. port 50 (InetAddress/getByName "127.0.0.1"))]
      (try
        (let [^Socket conn (.accept socket)]
          (.close socket)
          (try
            (speak (.getInputStream conn) (.getOutputStream conn) (atom {}) (atom {}))
            nil
            (finally (.close conn))))
        (finally (.close socket))))
    (catch Exception e
      (.getMessage e))))

(defn launch [args in out]
  (let [port (command-port args)]
    (cond
      (= :bad port) (status true)
      (= :pipe port) (do (speak in out (atom {}) (atom {})) (status false))
      :else (do (serve port) (status false)))))

(defn exit! [code]
  (shutdown-agents)
  (System/exit code))

(defn -main [& args]
  (exit! (launch args System/in System/out)))

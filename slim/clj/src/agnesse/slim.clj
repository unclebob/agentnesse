(ns agnesse.slim
  "A Slim 0.5 server. Port 1 is stdin and stdout. Any other port accepts one
  socket on 127.0.0.1. A lanes instruction runs each lane on its own thread."
  (:import [java.io InputStream OutputStream]
           [java.net InetAddress ServerSocket Socket]))

(def catalog
  (str "beginTable() -> string\n"
       "block() -> string\n"
       "book() -> string\n"
       "echo(string) -> string\n"
       "endIsAfterStart(string, string) -> string\n"
       "endTable() -> string\n"
       "execute() -> string\n"
       "hello() -> string\n"
       "kind(string) -> string\n"
       "open() -> string\n"
       "phase() -> string\n"
       "release() -> string\n"
       "released() -> string\n"
       "reset() -> string\n"
       "setWord(string) -> string\n"
       "table(list) -> string"))

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

(defn made [row instances]
  (let [name (text row 2)]
    (when-not (= "" name)
      (swap! instances assoc name (atom (opened (text row 3) (text row 4)))))
    "OK"))

(defn missing-instance [name]
  (str "__EXCEPTION__:message:<<NO_INSTANCE " name ">>"))

(defn no-method [method args klass]
  (str "__EXCEPTION__:message:<<NO_METHOD_IN_CLASS No Method "
       method "[" args "] in class " (class-name klass)
       ".\n Available methods:\n" catalog ">>"))

(defn unknown [method name args seat]
  (if (nil? seat)
    (missing-instance name)
    (no-method method args (:class @seat))))

(defn move [seat needed next]
  (if (or (nil? seat) (not= needed (:phase @seat)))
    "refused"
    (do (swap! seat assoc :phase next) "ok")))

(defn phase-of [seat]
  (if (nil? seat) "" (:phase @seat)))

(defn released-of [seat]
  (if (and seat (= "released" (:phase @seat))) "true" "false"))

(defn store-word [seat word]
  (when seat (swap! seat assoc :word word))
  "OK")

(defn word-of [seat]
  (if (or (nil? seat) (= "" (:word @seat))) "OK" (:word @seat)))

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
    (or (nil? seat) (= "" (:race @seat))) (missing-instance name)
    (= "racy" (:race @seat)) (race-book seat)
    :else (safe-book seat)))

(defn plain [method args]
  (case method
    "hello" "world"
    "endIsAfterStart" (ordered args)
    "kind" (kind-of args)
    nil))

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

(defn name-start? [ch]
  (or (and ch (<= (int \A) (int ch) (int \Z)))
      (and ch (<= (int \a) (int ch) (int \z)))))

(defn name-part? [ch]
  (or (name-start? ch)
      (and ch (<= (int \0) (int ch) (int \9)))
      (= \_ ch)))

(defn symbol-end [text index]
  (loop [at index]
    (if (name-part? (nth text at nil))
      (recur (inc at))
      at)))

(defn symbol-at [text index]
  (let [start (nth text index nil)
        next-ch (nth text (inc index) nil)]
    (when (and (= \$ start) (name-start? next-ch))
      (let [end (symbol-end text (+ index 2))]
        [(subs text (inc index) end) end]))))

(defn expand-string [text symbols]
  (loop [index 0 out ""]
    (if (>= index (count text))
      out
      (if-let [[name end] (symbol-at text index)]
        (recur end (str out (get symbols name (subs text index end))))
        (recur (inc index) (str out (subs text index (inc index))))))))

(defn expand [value symbols]
  (cond
    (string? value) (expand-string value symbols)
    (sequential? value) (mapv #(expand % symbols) value)
    :else value))

(defn args-from [row index]
  (vec (drop index row)))

(defn call [row instance-at method-at instances symbols]
  (let [method (text row method-at)
        args (mapv #(expand % @symbols) (args-from row (inc method-at)))
        name (text row instance-at)
        seat (get @instances name)]
    (if (= "echo" method)
      (echo-of seat args)
      (if-let [value (plain method args)]
        value
        (on-seat method name (if (string? (first args)) (first args) "") args seat)))))

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
    "make" (made row instances)
    "call" (call row 2 3 instances symbols)
    "callAndAssign" (remember symbols (text row 2) (call row 3 4 instances symbols))
    "lanes" (run-lanes row instances symbols)
    "OK"))

(defn answer [instructions instances symbols]
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

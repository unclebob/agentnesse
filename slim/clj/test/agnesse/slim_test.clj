(ns agnesse.slim-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [agnesse.slim :as slim])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.net Socket]))

(use-fixtures :once (fn [tests]
                      (try (tests) (finally (shutdown-agents)))))

(defn cell [rows index]
  (second (nth rows index)))

(defn call [id instance method & args]
  (into [id "call" instance method] args))

(deftest hello-echo-and-a-symbol
  (let [instances (atom {})
        symbols (atom {})]
    (is (= "world" (cell (slim/answer [(call "1" "actor" "hello")] instances symbols) 0)))
    (is (= "red" (cell (slim/answer [["2" "callAndAssign" "name" "actor" "echo" "red"]] instances symbols) 0)))
    (is (= "red" (cell (slim/answer [(call "3" "actor" "echo" "$name")] instances symbols) 0)))
    (is (= "$missing" (slim/expand-string "$missing" {})))
    (is (= :bad (slim/command-port [])))
    (is (= :bad (slim/command-port ["slim" "nope"])))
    (is (= :bad (slim/command-port ["slim" "-1"])))
    (is (= :pipe (slim/command-port ["slim" "1"])))
    (is (zero? (slim/command-port ["slim" "0"])))
    (is (= 1 (slim/status true)))
    (is (zero? (slim/status false)))))

(deftest a-gate-a-word-and-an-unknown-method
  (let [instances (atom {})
        symbols (atom {})
        batch [["1" "make" "gate" "slim"]
               (call "2" "gate" "open")
               (call "3" "gate" "block")
               (call "4" "gate" "release")
               (call "5" "gate" "phase")
               (call "6" "gate" "released")
               (call "9" "gate" "setWord" "red")
               (call "10" "gate" "echo")
               (call "11" "gate" "hello")
               (call "12" "gate" "beginTable")
               (call "13" "gate" "execute")]
        got (into {} (map (fn [[id value]] [id value]) (slim/answer batch instances symbols)))]
    (is (= "OK" (get got "1")))
    (is (= "ok" (get got "2")))
    (is (= "ok" (get got "3")))
    (is (= "refused" (get got "4")))
    (is (= "blocked" (get got "5")))
    (is (= "false" (get got "6")))
    (is (= "OK" (get got "9")))
    (is (= "red" (get got "10")))
    (is (= "world" (get got "11")))
    (is (= "OK" (get got "12")))
    (is (= "OK" (get got "13")))
    (let [unknown (cell (slim/answer [(call "16" "gate" "noSuch")] instances symbols) 0)]
      (is (.contains unknown "No Method noSuch[0] in class slim"))
      (is (.contains unknown "Available methods:"))
      (is (.contains unknown "hello() -> string")))
    (is (.contains (cell (slim/answer [(call "17" "gate" "noSuch" "x")] instances symbols) 0)
                   "No Method noSuch[1] in class slim"))
    (is (.contains (cell (slim/answer [(call "18" "absent" "noSuch")] instances symbols) 0)
                   "NO_INSTANCE absent"))
    (is (= "OK" (cell (slim/answer [["19" "note"]] instances symbols) 0)))
    (is (= "refused" (cell (slim/answer [(call "20" "missing" "open")] instances symbols) 0)))
    (is (= "" (cell (slim/answer [(call "21" "missing" "phase")] instances symbols) 0)))
    (is (= "false" (cell (slim/answer [(call "22" "missing" "released")] instances symbols) 0)))))

(defn lane-results [mark]
  (let [instances (atom {})
        symbols (atom {})
        _ (slim/answer [["1" "make" "seat" "slim" mark]] instances symbols)
        rows (cell (slim/answer [["9" "lanes"
                                  ["t1" [(call "a" "seat" "book")]]
                                  ["t2" [(call "b" "seat" "book")]]]]
                                instances symbols)
                    0)]
    (mapv #(nth % 4) rows)))

(deftest comparisons-symbols-and-launch
  (let [instances (atom {})
        symbols (atom {})]
    (is (= "yes" (cell (slim/answer [(call "1" "a" "endIsAfterStart" "0" "1")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "2" "a" "endIsAfterStart" "2" "2")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "3" "a" "endIsAfterStart" "4" "1")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "4" "a" "endIsAfterStart" "a" "1")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "5" "a" "endIsAfterStart")] instances symbols) 0)))
    (is (= "id" (cell (slim/answer [(call "6" "a" "kind" "{\"doc\":{\"kind\":\"id\"}}")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "7" "a" "kind" "{\"kind\":\"passport\"}")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "8" "a" "kind")] instances symbols) 0)))
    (is (= "OK" (cell (slim/answer [(call "9" "a" "echo")] instances symbols) 0)))
    (is (= "OK" (cell (slim/answer [(call "10" "a" "echo" ["nested"])] instances symbols) 0)))
    (is (= "OK" (cell (slim/answer [["11" "make" "" "slim"]] instances symbols) 0)))
    (is (= "OK" (cell (slim/answer [["12" "make" "blank" ""]] instances symbols) 0)))
    (is (.contains (cell (slim/answer [(call "13" "blank" "noSuch")] instances symbols) 0) "in class slim")))
  (is (= "pre kept tail" (slim/expand-string "pre $kept tail" {"kept" "kept"})))
  (is (= "$missing" (slim/expand-string "$missing" {})))
  (is (= "$" (slim/expand-string "$" {})))
  (is (= "A1" (slim/expand-string "$a1" {"a1" "A1"})))
  (is (= "A0" (slim/expand-string "$a0" {"a0" "A0"})))
  (is (= "A9" (slim/expand-string "$a9" {"a9" "A9"})))
  (is (= "up" (slim/expand-string "$A" {"A" "up"})))
  (is (= "up" (slim/expand-string "$Z" {"Z" "up"})))
  (is (= "low" (slim/expand-string "$a" {"a" "low"})))
  (is (= "low" (slim/expand-string "$z" {"z" "low"})))
  (is (= "yes" (slim/expand-string "$a_b" {"a_b" "yes"})))
  (is (= [nil 0] (slim/read-list "nope")))
  (is (= [0 4] (slim/read-length "nope" 0)))
  (is (= [0 5] (slim/read-length "abcd:" 0)))
  (is (= ["a" 9] (slim/read-field "000001:a" 0)))
  (is (= "ab" (slim/read-exact (ByteArrayInputStream. (.getBytes "ab" "UTF-8")) 2)))
  (is (= "0000000001" (slim/read-prefix (ByteArrayInputStream. (.getBytes "0000000001:" "UTF-8")))))
  (is (nil? (slim/read-prefix (ByteArrayInputStream. (.getBytes (str (apply str (repeat 11 "x")) ":") "UTF-8")))))
  (is (= ["yes"] (slim/expand ["$a"] {"a" "yes"})))
  (is (= 4 (slim/expand 4 {})))
  (is (= 1 (slim/launch ["slim" "nope"] nil nil)))
  (let [out (ByteArrayOutputStream.)]
    (is (zero? (slim/launch ["slim" "1"] (ByteArrayInputStream. (.getBytes "" "UTF-8")) out)))
    (is (= "Slim -- V0.5\n" (.toString out "UTF-8"))))
  (let [busy (java.net.ServerSocket. 0)
        code (atom nil)]
    (try
      (is (zero? (slim/launch ["slim" (str (.getLocalPort busy))] nil nil)))
      (with-redefs [slim/exit! (fn [status] (reset! code status))]
        (slim/-main "slim" "nope")
        (is (= 1 @code)))
      (finally (.close busy)))))

(defn assign [id name value]
  [id "assign" name value])

(deftest java-symbol-and-argument-forms
  (is (= "YESc" (slim/expand-string "$abc" {"ab" "YES"})))
  (is (= "fruit!" (slim/expand-string "$Äpfel!" {"Äpfel" "fruit"})))
  (is (= "hi!" (slim/expand-string "$`hello world`!" {"`hello world`" "hi"})))
  (is (= "" (slim/expand-string "  $name=  " {})))
  (is (= "$a-=" (slim/expand-string "$a-=" {})))
  (is (= "$" (slim/expand-string "$" {"" "EMPTY"})))
  (is (= "$q" (slim/expand-string "$q" {"" "EMPTY"})))
  (is (= "YES" (slim/expand-string "$a" {"a" "YES"})))
  (is (= "pre [a, b] tail" (slim/expand-string "pre $items tail" {"items" ["a" "b"]})))
  (let [instances (atom {})
        symbols (atom {})]
    (slim/answer [[ "1" "make" "gate" "slim"]] instances symbols)
    (let [seat (get @instances "gate")]
      (slim/answer [["2" "callAndAssign" "actor" "gate" "self"]] instances symbols)
      (is (identical? seat (get @symbols "actor")))
      (slim/answer [["3" "make" "again" "$actor"]] instances symbols)
      (slim/answer [(call "4" "gate" "open")] instances symbols)
      (is (= "open" (cell (slim/answer [(call "5" "again" "phase")] instances symbols) 0))))
    (slim/answer [["6" "make" "scriptTableActor" "slim"]] instances symbols)
    (slim/answer [(call "7" "SlimHelperLibrary" "pushFixture")] instances symbols)
    (slim/answer [["8" "make" "scriptTableActor" "slim" "safe"]] instances symbols)
    (slim/answer [(call "9" "SlimHelperLibrary" "pushFixture")] instances symbols)
    (slim/answer [(call "10" "SlimHelperLibrary" "popFixture")] instances symbols)
    (is (= "" (cell (slim/answer [(call "11" "scriptTableActor" "phase")] instances symbols) 0)))
    (slim/answer [(call "12" "SlimHelperLibrary" "popFixture")] instances symbols)
    (is (= "new" (cell (slim/answer [(call "13" "scriptTableActor" "phase")] instances symbols) 0)))
    (slim/answer [(assign "14" "ab" "YES")] instances symbols)
    (slim/answer [["15" "callAndAssign" "copy" "SlimHelperLibrary" "cloneSymbol" "$actor"]] instances symbols)
    (is (identical? (get @symbols "actor") (get @symbols "copy")))
    (is (= "OK" (cell (slim/answer [(call "16" "again" "echo" "$actor")] instances symbols) 0)))
    (is (= "YES" (cell (slim/answer [["17" "call" "again" "label" [["name" "$ab"]]]] instances symbols) 0)))
    (is (= "5" (cell (slim/answer [(call "18" "again" "plus" "2" "3")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "19" "again" "plus" "x" "3")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "20" "again" "plus" "2" "x")] instances symbols) 0)))
    (is (= "3" (cell (slim/answer [(call "21" "again" "plus" "1" "2" "9")] instances symbols) 0)))
    (is (= "yes" (cell (slim/answer [(call "22" "again" "flag" "true")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "23" "again" "flag" "false")] instances symbols) 0)))
    (is (= "red,blue" (cell (slim/answer [(call "24" "again" "join" "[red, blue]")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "25" "again" "join" "red")] instances symbols) 0)))
    (is (= "2020-02-29" (cell (slim/answer [(call "26" "again" "day" "2020-02-29")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "27" "again" "day" "2019-02-29")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "28" "again" "day" "nope")] instances symbols) 0)))
    (is (= "green" (cell (slim/answer [(call "29" "again" "shade" "green")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "30" "again" "shade" "pink")] instances symbols) 0)))
    (is (= "4" (cell (slim/answer [(call "31" "again" "wide" "4")] instances symbols) 0)))
    (is (= "1.5" (cell (slim/answer [(call "32" "again" "scale" "1.5")] instances symbols) 0)))
    (is (= "Q" (cell (slim/answer [(call "33" "again" "initial" "Q")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "34" "again" "initial" "QQ")] instances symbols) 0)))
    (slim/answer [(assign "35" "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS" "again\\.echo")] instances symbols)
    (is (= "$ab" (cell (slim/answer [(call "36" "again" "echo" "$ab")] instances symbols) 0)))
    (slim/answer [(assign "37" "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS" "[")] instances symbols)
    (is (= "YES" (cell (slim/answer [(call "38" "again" "echo" "$ab")] instances symbols) 0)))
    (slim/answer [(assign "39" "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS" "again\\.e[a-z]+")] instances symbols)
    (is (= "$ab" (cell (slim/answer [(call "40" "again" "echo" "$ab")] instances symbols) 0)))
    (slim/answer [(assign "41" "SLIM_AGENT_FIXTURE_HANDLES_SYMBOLS" "echo")] instances symbols)
    (is (= "YES" (cell (slim/answer [(call "42" "again" "echo" "$ab")] instances symbols) 0)))
    (is (.contains (cell (slim/answer [(call "43" "SlimHelperLibrary" "missing")] instances symbols) 0)
                   "in class SlimHelperLibrary"))
    (is (= "Ada" (cell (slim/answer [["44" "call" "again" "label" [1 ["name" "Ada"]]]] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [(call "45" "again" "label" "[]")] instances symbols) 0)))
    (is (= "no" (cell (slim/answer [["45b" "call" "again" "label" [1]]] instances symbols) 0)))
    (swap! symbols assoc "" (get @instances "gate"))
    (slim/answer [["46" "make" "dollar" "$"]] instances symbols)
    (is (= "new" (cell (slim/answer [(call "47" "dollar" "phase")] instances symbols) 0)))
    (swap! symbols assoc "bag" {:word "x"})
    (slim/answer [["48" "make" "thing" "$bag"]] instances symbols)
    (is (= "hi" (cell (slim/answer [(call "49" "thing" "echo" "hi")] instances symbols) 0)))))

(deftest lanes-book-a-racy-seat-twice-and-a-safe-seat-once
  (is (= "lanes" (cell (slim/answer [["1" "lanes"]] (atom {}) (atom {})) 0)))
  (is (= [] (cell (slim/answer [["2" "lanes" "not-a-group"]] (atom {}) (atom {})) 0)))
  (is (= ["booked" "booked"] (sort (lane-results "racy"))))
  (is (= ["booked" "full"] (sort (lane-results "safe"))))
  (let [instances (atom {})
        symbols (atom {})]
    (slim/answer [["1" "make" "seat" "slim" "racy"]] instances symbols)
    (is (= "booked" (cell (slim/answer [(call "2" "seat" "book")] instances symbols) 0)))
    (is (= "full" (cell (slim/answer [(call "3" "seat" "book")] instances symbols) 0)))
    (swap! (get @instances "seat") assoc :seats nil)
    (is (= "full" (cell (slim/answer [(call "4" "seat" "book")] instances symbols) 0)))
    (is (.contains (cell (slim/answer [(call "5" "missing" "book")] instances symbols) 0) "NO_INSTANCE")))
  (let [instances (atom {})
        symbols (atom {})
        _ (slim/answer [["1" "make" "seat" "slim"]] instances symbols)
        rows (cell (slim/answer [["9" "lanes"
                                  ["t1" [(call "a" "seat" "hello") (call "b" "seat" "hello")]]]]
                                instances symbols)
                    0)]
    (is (= ["t1" "a" "1" "2" "world"] (subvec (first rows) 0 5)))
    (is (= "3" (nth (second rows) 2)))))

(defn frame [value]
  (let [body (slim/serialize [value])]
    (str (format "%06d:" (count body)) body)))

(deftest pipe-frames
  (let [incoming (str (frame ["1" "call" "actor" "hello"]) (frame "bye"))
        out (ByteArrayOutputStream.)]
    (slim/speak (ByteArrayInputStream. (.getBytes incoming "UTF-8")) out (atom {}) (atom {}))
    (is (.startsWith (.toString out "UTF-8") "Slim -- V0.5\n"))
    (is (.contains (.toString out "UTF-8") "world")))
  (let [out (ByteArrayOutputStream.)]
    (slim/speak (ByteArrayInputStream. (.getBytes "" "UTF-8")) out (atom {}) (atom {}))
    (is (= "Slim -- V0.5\n" (.toString out "UTF-8"))))
  (is (= [["a"]] (slim/parse (slim/serialize [["a"]]))))
  (is (= [] (slim/parse "nope"))))

(defn free-port []
  (let [socket (java.net.ServerSocket. 0)]
    (try (.getLocalPort socket) (finally (.close socket)))))

(defn read-until [^Socket socket marker]
  (let [in (.getInputStream socket)]
    (loop [out ""]
      (if (.contains out marker)
        out
        (let [n (.read in)]
          (recur (str out (char n))))))))

(deftest a-socket-accepts-one-connection
  (let [port (free-port)
        done (future (slim/serve port))
        socket (loop [tries 50]
                 (or (try (Socket. "127.0.0.1" port) (catch Exception _ nil))
                     (do (Thread/sleep 20) (when (pos? tries) (recur (dec tries))))))]
    (is (= "Slim -- V0.5\n" (read-until socket "\n")))
    (.write (.getOutputStream socket) (.getBytes (frame ["1" "call" "actor" "hello"]) "UTF-8"))
    (.flush (.getOutputStream socket))
    (is (.contains (read-until socket "world") "world"))
    (.write (.getOutputStream socket) (.getBytes "000003:bye" "UTF-8"))
    (.flush (.getOutputStream socket))
    (.close socket)
    (is (nil? @done)))
  (let [busy (java.net.ServerSocket. 0)]
    (try
      (is (string? (slim/serve (.getLocalPort busy))))
      (finally (.close busy)))))

(defn -main [& _]
  (let [{:keys [fail error]} (clojure.test/run-tests 'agnesse.slim-test)]
    (shutdown-agents)
    (when (pos? (+ fail error))
      (throw (ex-info "tests failed" {:fail fail :error error})))))

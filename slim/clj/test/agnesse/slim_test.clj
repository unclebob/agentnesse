(ns agnesse.slim-test
  (:require [clojure.test :refer [deftest is testing]]
            [agnesse.slim :as slim])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.net Socket]))

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

(deftest lanes-book-a-racy-seat-twice-and-a-safe-seat-once
  (is (= "lanes" (cell (slim/answer [["1" "lanes"]] (atom {}) (atom {})) 0)))
  (is (= [] (cell (slim/answer [["2" "lanes" "not-a-group"]] (atom {}) (atom {})) 0)))
  (is (= ["booked" "booked"] (sort (lane-results "racy"))))
  (is (= ["booked" "full"] (sort (lane-results "safe")))))

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
    (when (pos? (+ fail error))
      (throw (ex-info "tests failed" {:fail fail :error error})))))

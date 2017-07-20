(ns rx-playground.core
  (:require [beicon.core :as rx]
            [clojure.string :as str])
  (:import (io.reactivex Flowable
                         Observable)
           (java.util.concurrent TimeUnit)))

;;; Creating Observables

(def obs (rx/just 10))

(rx/on-value obs (fn [value]
                   (prn (str "Got value: " value))))

(-> (rx/from-coll [1 2 3 4 5 6 7 8 9 10])
    (rx/on-value prn))

(-> (rx/range 1 10)
    (rx/on-value prn))

(def subscription (-> (rx/interval 100)
                      (rx/on-value prn)))

(Thread/sleep 1000)
(rx/cancel! subscription)

(defn just-obs [v]
  (rx/create
   (fn [observer]
     (observer (rx/end v)))))

(rx/on-value (just-obs 20) prn)

;;; Manipulating Observables

(rx/on-value (->> (Observable/interval 1 TimeUnit/MICROSECONDS)
                  (rx/filter even?)
                  (rx/take 5)
                  (rx/reduce + 0))
             prn)

(defn musicians []
  (rx/from-coll ["James Hetfield" "Dave Mustaine" "Kerry King"]))

(defn bands []
  (rx/from-coll ["Metallica" "Megadeth" "Slayer"]))

(defn uppercased-obs []
  (rx/map str/upper-case (bands)))

(-> (rx/zip (musicians)
            (uppercased-obs))
    (rx/on-value (fn [[musician band]]
                   (prn (str musician " - from: " band)))))

;;; Flatmap and friends

(defn factorial [n]
  (reduce * (range 1 (inc n))))

(defn all-positive-integers []
  (Observable/interval 1 TimeUnit/MICROSECONDS))

(defn fact-obs [n]
  (rx/create
   (fn [observer]
     (observer (rx/end (factorial n))))))

(rx/on-value (->> (all-positive-integers)
                  (rx/filter even?)
                  (rx/flat-map fact-obs)
                  (rx/take 5))
             prn)

(defn repeat-obs [n]
  (rx/from-coll (repeat 2 n)))

(rx/on-value (->> (all-positive-integers)
                  (rx/flat-map repeat-obs)
                  (rx/take 6))
             prn)

;;; Error handling

(defn exceptional-obs []
  (rx/create
   (fn [observer]
     (observer (throw (Exception. "Oops. Something went wrong."))))))

(rx/on-value (->> (exceptional-obs)
                  (rx/map inc))
             (fn [v] (prn "result is " v)))

(rx/subscribe (->> (exceptional-obs)
                   (rx/map inc))
              (fn [v] (prn "result is " v))
              (fn [e] (prn "error is " e)))

(rx/on-value (->> (exceptional-obs)
                  (rx/catch #(instance? Exception %)
                      (fn [_] (rx/just 10)))
                  (rx/map inc))
             (fn [v] (prn "result is " v)))

(rx/on-value (->> (exceptional-obs)
                  (rx/catch #(instance? Exception %)
                      (fn [_] (rx/from-coll (range 5))))
                  (rx/map inc))
             (fn [v] (prn "result is " v)))

(defn retry-obs []
  (let [errored (atom false)]
    (rx/create
     (fn [observer]
       (if @errored
         (observer (rx/end 20))
         (do (reset! errored true)
             (throw (Exception. "Oops. Something went wrong."))))))))

(rx/on-value (retry-obs)
             (fn [v] (prn "result is" v)))

(rx/on-value (->> (retry-obs)
                  rx/retry)
             (fn [v] (prn "result is" v)))

;;; Backpressure

(defn fast-producing-obs []
  (->> (rx/interval 1)
       (rx/map inc)))

(defn slow-producing-obs []
  (->> (rx/interval 500)
       (rx/map inc)))

(rx/subscribe (->> (Flowable/zip (rx/to-flowable :error
                                                 (fast-producing-obs))
                                 (rx/to-flowable :error
                                                 (slow-producing-obs))
                                 (rx/as-bifunction vector))
                   (rx/map (fn [[x y]]
                             (+ x y)))
                   (rx/take 10))
              prn
              (fn [e] (prn "error is " e)))

(rx/subscribe (->> (Flowable/zip (->> (fast-producing-obs)
                                      (rx/to-flowable :error)
                                      (rx/sample 200))
                                 (rx/to-flowable :error
                                                 (slow-producing-obs))
                                 (rx/as-bifunction vector))
                   (rx/map (fn [[x y]]
                             (+ x y)))
                   (rx/take 10))
              prn
              (fn [e] (prn "error is " e)))

(rx/subscribe (->> (Flowable/zip (rx/to-flowable :buffer
                                                 (fast-producing-obs))
                                 (rx/to-flowable :error
                                                 (slow-producing-obs))
                                 (rx/as-bifunction vector))
                   (rx/map (fn [[x y]]
                             (+ x y)))
                   (rx/take 10))
              prn
              (fn [e] (prn "error is " e)))

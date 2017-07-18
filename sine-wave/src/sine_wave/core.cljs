(ns sine-wave.core
  (:require [cljsjs.rx])
  (:refer-clojure :exclude [time]))

(def canvas (.getElementById js/document "myCanvas"))
(def ctx (.getContext canvas "2d"))

(.clearRect ctx 0 0 (.-width canvas) (.-height canvas))

(def interval js/Rx.Observable.interval)
(def time (interval 10))

(defn deg->rad [n]
  (* (/ Math/PI 180) n))

(defn sine-coord [x]
  (let [sin (Math/sin (deg->rad x))
        y (- 100 (* sin 90))]
    {:x x
     :y y
     :sin sin}))

(def sine-wave
  (.map time sine-coord))

(defn fill-rect [x y colour]
  (set! (.-fillStyle ctx) colour)
  (.fillRect ctx x y 2 2))

(-> sine-wave
    (.take 600)
    (.subscribe (fn [{:keys [x y]}]
                  (fill-rect x y "orange"))))

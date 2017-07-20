(ns sine-wave.core
  (:require [cljsjs.rx])
  (:refer-clojure :exclude [concat time]))

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

#_(-> sine-wave
      (.take 600)
      (.subscribe (fn [{:keys [x y]}]
                    (fill-rect x y "orange"))))

(def colour
  (.map sine-wave
        (fn [{:keys [sin]}]
          (if (neg? sin)
            "red"
            "blue"))))

#_(-> sine-wave
      (.zip colour vector)
      (.take 600)
      (.subscribe (fn [[{:keys [x y]} colour]]
                    (fill-rect x y colour))))

(def red (.map time (fn [_] "red")))
(def blue (.map time (fn [_] "blue")))

(def concat js/Rx.Observable.concat)
(def defer js/Rx.Observable.defer)
(def from-event js/Rx.Observable.fromEvent)

(def mouse-click (from-event canvas "click"))

(def cycle-colour
  (concat (.takeUntil red mouse-click)
          (defer #(concat (.takeUntil blue mouse-click)
                          cycle-colour))))

(-> sine-wave
    (.zip cycle-colour vector)
    (.take 600)
    (.subscribe (fn [[{:keys [x y]} colour]]
                  (fill-rect x y colour))))

;; TODO: Exercise 1.1

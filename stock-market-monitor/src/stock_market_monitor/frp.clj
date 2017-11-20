(ns stock-market-monitor.frp
  (:require [beicon.core :as rx]
            [seesaw.core :refer :all]))

(native!)

(def main-frame
  (frame :title "Stock price monitor"
         :width 200 :height 100
         :on-close :exit))

(def price-label (label "Price: -"))

(def running-average-label (label "Running average: -"))

(config! main-frame :content
         (border-panel
          :north price-label
          :center running-average-label
          :border 5))

(defn share-price [company-code]
  (Thread/sleep 200)
  (rand-int 1000))

(defn avg [numbers]
  (float (/ (apply + numbers)
            (count numbers))))

(defn make-price-obs [_]
  (rx/just (share-price "XYZ")))

(defn -main [& args]
  (show! main-frame)
  (let [price-obs (-> (rx/flat-map make-price-obs
                                   (rx/interval 500))
                      .publish)
        sliding-buffer-obs (rx/buffer 5 1 price-obs)]
    (rx/on-value price-obs
                 (fn [price]
                   (text! price-label (str "Price: " price))))
    (rx/on-value sliding-buffer-obs
                 (fn [buffer]
                   (text! running-average-label (str "Running average: " (avg buffer)))))
    (.connect price-obs)))

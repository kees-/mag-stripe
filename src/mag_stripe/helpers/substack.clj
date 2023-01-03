(ns mag-stripe.helpers.substack
  (:require [etaoin.api :as e]
            [mag-stripe.util :as u]))

(defn scroll-full-page!
  [d]
  (let [hs (atom [1 0])]
    (u/printlnf "Scrolling down\033[s")
    (while (not (zero? (apply - @hs)))
      (e/scroll-bottom d)
      (e/wait 0.75)
      (e/wait-invisible d {:class :post-preview-silhouette})
      (u/printlnf "\033[u.\033[s")
      (reset! hs [(e/js-execute d "return document.body.scrollHeight") (first @hs)]))))

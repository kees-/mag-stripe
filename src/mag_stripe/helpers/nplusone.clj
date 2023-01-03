(ns mag-stripe.helpers.nplusone
  (:require [etaoin.api :as e]))

(defn paywalled?
  [d]
  (when-let [p? (e/exists? d {:class :roadblock})]
    (println "  | Paywalled, skipping")
    p?))

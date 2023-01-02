(ns mag-stripe.swipe
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [etaoin.api :as e]
            [mag-stripe.archive :as archive]
            [mag-stripe.post :as post]))

(def ^:private opts-defaults
     {:timeout 10
      :retries 3})

(def ^:private wd-defaults
  {:driver-log-level "SEVERE"
   :log-level :error
   :headless true})

(def ^:private wd-keys
  [:path-browser :log-level :driver-log-level :headless])

#_{:clj-kondo/ignore [:unresolved-symbol]}
(defn- scrape*
  [opts]
  (let [driver-opts (merge wd-defaults (select-keys opts wd-keys))]
    (e/with-chrome driver-opts driver
      (let [archive (archive/parse opts driver)]
        (println (map keys archive))
        (into (:existing opts)
              (map #(post/parse opts driver %) archive))))))

;; =============================================================================
(defmulti ^:private scrape
  "Normalize passed options and initiate the webdriver."
  (fn [opts]
    (keyword (:platform opts))))

(defmethod scrape :nplusone
  [opts]
  (let [url (format "https://www.nplusonemag.com/%s/%s/"
                    (:query opts)
                    (:slug opts))]
    (scrape*
     (assoc opts :url url))))

(defmethod scrape :spike-art-magazine
  [opts]
  (let [url (format "https://www.spikeartmagazine.com/?q=%s/%s"
                    (:query opts)
                    (:slug opts))]
    (scrape*
     (assoc opts :url url))))

(defmethod scrape :substack
  [opts]
  (let [opts (merge {:sort-method "new"} opts)
        url (format "%s/archive?sort=%s"
                    (:domain opts)
                    (:sort-method opts))]
    (scrape*
     (assoc opts :url url))))

(defmethod scrape
  :default
  [opts]
  (scrape* opts))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn write!
  [opts]
  (let [opts (-> opts-defaults
                 (merge opts)
                 (assoc :existing
                        (if (and (:append? opts)
                                 (fs/exists? (:outfile opts)))
                          (edn/read-string (slurp (:outfile opts)))
                          [])))]
    (spit (:outfile opts) (scrape opts))
    (println "Complete" (char 0x3020))))

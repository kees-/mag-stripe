(ns mag-stripe.swipe
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as s]
            [etaoin.api :as e]
            [mag-stripe.archive :as archive]
            [mag-stripe.post :as post]
            [mag-stripe.util :as u]))

(def ^:private defaults
  {:timeout 10
   :retries 3
   :append? true
   :outfile "target/posts.edn"})

(defn- form-wd-opts
  [opts]
  (let [wd-keys [:path-browser :log-level :driver-log-level :headless]
        defaults {:driver-log-level "SEVERE" :log-level :error :headless true}]
    (merge defaults (select-keys opts wd-keys))))

(defn- ->keyword
  [o]
  (cond
    (string? o) (keyword (s/replace o #"^:+" ""))
    (keyword? o) o
    :else o))

(defn- form-opts
  [opts]
  (let [{:keys [outfile] :as opts} (merge defaults opts)]
    (-> (merge defaults opts)
        (update :platform ->keyword)
        (assoc :existing
               (if (and (:append? opts)
                        (fs/exists? outfile))
                 (edn/read-string (slurp outfile))
                 [])))))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(defn- scrape*
  [opts]
  (u/printlnf "Booting webdriver...\033[s")
  (e/with-chrome (form-wd-opts opts) driver
    (u/printlnf "\033[u Booted")
    (let [archive (archive/parse opts driver)]
      (into (:existing opts)
            (map #(post/parse opts driver %) archive)))))

;; =============================================================================
(defmulti ^:private scrape
  "Normalize passed options and initiate the webdriver."
  :platform)

(defmethod scrape :n+1
  [opts]
  (let [url (format "https://www.nplusonemag.com/%s/%s/"
                    (:query opts) (:slug opts))]
    (scrape*
     (assoc opts :url url))))

(defmethod scrape :spike-art-magazine
  [opts]
  (let [url (format "https://www.spikeartmagazine.com/?q=%s/%s"
                    (:query opts) (:slug opts))]
    (scrape*
     (assoc opts :url url))))

(defmethod scrape :substack
  [opts]
  (let [opts (merge {:sort-method "new"} opts)
        url (format "%s/archive?sort=%s"
                    (:domain opts) (:sort-method opts))]
    (scrape*
     (assoc opts :url url))))

(defmethod scrape :system
  [opts]
  (let [url (format "https://system-magazine.com/%s/%s"
                    (:query opts) (:slug opts))]
    (scrape*
     (assoc opts :url url))))

(defmethod scrape
  :default
  [_]
  (println "Platform not recognized; scraping a generic URL won't work."))

(defn platforms
  []
  (->> (dissoc (methods scrape) :default)
       keys
       (map name)
       sort
       (s/join \newline)
       println))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn write!
  [opts]
  (let [opts (form-opts opts)]
    (fs/create-dirs (fs/parent (:outfile opts)))
    (when (:append? opts) (println (count (:existing opts)) "existing posts found."))
    (->> (scrape opts)
         (remove :paywalled?)
         (sort-by :datetime)
         reverse
         (mapv #(dissoc % :paywalled?))
         (spit (:outfile opts)))
    (println "Complete" (char 0x3020))))

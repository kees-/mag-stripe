(ns mag-stripe.post
  (:require [etaoin.api :as e]
            [mag-stripe.util :as util]
            [mag-stripe.helpers.nplusone :as n+1]))

(defn- outer-htmlv
  [d data]
  (mapv #(e/get-element-attr-el d % "outerHTML") data))

(defn- datetime-meta
  [d]
  (e/get-element-attr
   d {:css "meta[property='article:published_time']"}
   "content"))

;; =============================================================================
(defmulti ^:private parse*
  (fn [opts _ _]
    (keyword (:platform opts))))

(defmethod parse* :n+1
  [_ d post]
  (let [parent (if (e/exists? d {:class :post-wrapper})
                 ".post-wrapper > *" ".post-body > *")]
    (assoc
     post
     :paywalled? (n+1/paywalled? d)
     :content (outer-htmlv d (e/query-all d {:css parent}))
     :datetime (datetime-meta d)
     :byline (e/get-element-text d {:css ".post-dek"})
     :hero (e/get-element-attr d {:css ".post-hero"} "outerHTML"))))

(defmethod parse* :spike-art-magazine
  [_ d post]
  (assoc
   post
   :content (outer-htmlv d (e/query-all d {:css ".field-name-body > div > div > *"}))
   :datetime (datetime-meta d)))

(defmethod parse* :substack
  [opts d post]
  (e/wait-exists d {:css ".body"} {:timeout (:timeout opts)})
  (assoc
   post
   :content (outer-htmlv d (e/query-all d {:css ".body > *:not(.subscribe-widget)"}))
   :byline (if (e/exists? d {:css "h3.subtitle"})
             (e/get-element-text d {:css "h3.subtitle"})
             (e/get-element-attr d {:css "meta[name='description']"} "content"))
   :title (e/get-element-attr
           d {:css "meta[property='og:title']"}
           "content")))

(defn parse
  [opts d post]
  (println "Parsing" (:url post))
  (e/go d (:url post))
  (util/try-times (:retries opts) (parse* opts d post)))

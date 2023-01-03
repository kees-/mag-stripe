(ns mag-stripe.archive
  (:require [etaoin.api :as e]
            [mag-stripe.helpers.substack :as substack]))

(defn- drop-dupes
  [existing posts]
  (remove #(some #{(:url %)} existing) posts))

(defn- limit
  [{lim :limit} posts]
  (if lim (take lim posts) posts))

(defn- process
  "Map `f` to each post, querying the relevant elements."
  [opts posts f]
  (->> posts (limit opts) (mapv f) (drop-dupes (map :url (:existing opts)))))

(defn- completed
  [posts]
  (println "Found" (count posts) "new posts")
  posts)

;; =============================================================================
(defmulti ^:private parse*
  (fn [opts _]
    (keyword (:platform opts))))

(defmethod parse* :n+1
  [opts d]
  (let [selector (if (= "magazine" (:query opts))
                   ".module-issue-toc .post"
                   "#content > .post")
        posts (e/query-all d {:css selector})
        data (fn [el]
               {:source (e/get-element-text-el
                         d (e/child d el {:css ".post-author"}))
                :title (e/get-element-text-el
                        d (e/child d el {:css ".post-title"}))
                :url (e/get-element-attr-el
                      d (e/child d el {:css ".post-title a"})
                      "href")})]
    (process opts posts data)))

(defmethod parse* :spike-art-magazine
  [opts d]
  (let [source (e/get-element-text d {:css "h1.page-header"})
        posts (e/query-all d {:css "#page-content .node-article"})
        data (fn [el]
               {:source source
                :title (e/get-element-text-el
                        d (e/child d el {:css ".field-name-title"}))
                :url (e/get-element-attr-el
                      d (e/child d el {:css ".field-name-title a"})
                      "href")
                :byline (e/get-element-text-el
                         d (e/child d el {:css ".field-name-field-blurb"}))
                :category (e/get-element-text-el
                           d (e/child d el {:id :article-category}))
                :hero (when (pos? (count (e/children
                                          d
                                          el
                                          {:css ".field-name-field-image"})))
                        (e/get-element-attr-el
                         d (e/child d el {:css ".field-name-field-image img"})
                         "outerHTML"))})]
    (->> (process opts posts data)
         (remove #(some #{(:category %)} ["" "OFFLINE ARTICLE"])))))

(defmethod parse* :substack
  [opts d]
  (substack/scroll-full-page! d)
  (let [source (e/get-element-text d {:tag :h1})
        posts (e/query-all d {:css ".post-preview"})
        data (fn [el]
               {:source source
                :url (->> (e/get-element-attr-el
                           d (e/child d el {:css ".post-preview-content a"})
                           "href")
                          (re-find #"^[^?]+"))
                :datetime (e/get-element-attr-el
                           d (e/child d el {:tag :time})
                           "datetime")
                :hero (e/get-element-inner-html-el
                       d (e/child d el {:tag :picture}))})]
    (process opts posts data)))

(defmethod parse* :system
  [opts d]
  (let [posts (e/query-all d {:css ".section > a"})
        data (fn [el]
               {:title (->> (e/get-element-text-el
                             d (e/child d el {:css ".articles-item__title"}))
                            (re-find #"^[^?]+"))
                :url (e/get-element-attr-el d el "href")
                :issue (e/get-element-text-el
                        d (e/child d el {:css ".articles-item__info span:last-child"}))})]
    (process opts posts data)))

(defn parse
  [opts d]
  (e/go d (:url opts))
  (println "Parsing archive page")
  (completed (parse* opts d)))

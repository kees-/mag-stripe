(ns mag-stripe.archive
  (:require [etaoin.api :as e]))

(defn- drop-dupes
  [existing posts]
  (remove #(some #{(:url %)} existing) posts))

(defn- limit
  [{lim :limit} posts]
  (if lim (take lim posts) posts))

(defn- completed
  [posts]
  (println "Found" (count posts) "new posts")
  posts)

;; =============================================================================
(defmulti ^:private parse*
  (fn [opts _]
    (keyword (:platform opts))))

(defmethod parse* :nplusone
  [opts d]
  (let [selector (if (= "magazine" (:query opts))
                   ".module-issue-toc .post"
                   "#content > .post")
        all-posts (e/query-all d {:css selector})
        post-meta (fn [el]
                    (-> {}
                        (assoc :title
                               (e/get-element-text-el
                                d
                                (e/child d el {:css ".post-title"})))
                        (assoc :url
                               (e/get-element-attr-el
                                d
                                (e/child d el {:css ".post-title a"})
                                "href"))
                        (assoc :source
                               (e/get-element-text-el
                                d
                                (e/child d el {:css ".post-author"})))))
        posts (->> all-posts
                   (limit opts)
                   (mapv post-meta)
                   (drop-dupes (map :url (:existing opts))))]
    (completed posts)))

(defmethod parse* :spike-art-magazine
  [opts d]
  (let [source (e/get-element-text d {:css "h1.page-header"})
        all-posts (e/query-all d {:css "#page-content .node-article"})
        post-meta (fn [el]
                    (-> {:source source}
                        (assoc :title
                               (e/get-element-text-el
                                d
                                (e/child d el {:css ".field-name-title"})))
                        (assoc :url
                               (e/get-element-attr-el
                                d
                                (e/child d el {:css ".field-name-title a"})
                                "href"))
                        (assoc :hero
                               (when (pos?
                                      (count (e/children
                                              d
                                              el
                                              {:css ".field-name-field-image"})))
                                 (e/get-element-attr-el
                                  d
                                  (e/child d el {:css ".field-name-field-image img"})
                                  "outerHTML")))
                        (assoc :byline
                               (e/get-element-text-el
                                d
                                (e/child d el {:css ".field-name-field-blurb"})))
                        (assoc :category
                               (e/get-element-text-el
                                d
                                (e/child d el {:id :article-category})))))
        posts (->> all-posts
                   (limit opts)
                   (mapv post-meta)
                   (remove #(some #{(:category %)} ["" "OFFLINE ARTICLE"]))
                   (drop-dupes (map :url (:existing opts))))]
    (completed posts)))

(defmethod parse* :substack
  [opts d]
  (let [hs (atom [1 0])]
    (println (format "Scrolling down\033[s"))
    (while (not (zero? (apply - @hs)))
      (e/scroll-bottom d)
      (e/wait 0.75)
      (e/wait-invisible d {:class :post-preview-silhouette})
      (println (format "\033[u.\033[s"))
      (reset! hs [(e/js-execute d "return document.body.scrollHeight") (first @hs)])))
  (println "Searching for unparsed posts")
  (let [source (e/get-element-text d {:tag :h1})
        all-posts (e/query-all d {:css ".post-preview"})
        post-meta (fn [el]
                    (-> {:source source}
                        (assoc :url
                               (->> (e/get-element-attr-el
                                     d
                                     (e/child d el {:css ".post-preview-content a"})
                                     "href")
                                    (re-find #"^[^?]+")))
                        (assoc :datetime
                               (e/get-element-attr-el
                                d
                                (e/child d el {:tag :time})
                                "datetime"))
                        (assoc :hero
                               (e/get-element-inner-html-el
                                d
                                (e/child d el {:tag :picture})))))
        posts (->> all-posts
                   (limit opts)
                   (mapv post-meta)
                   (drop-dupes (map :url (:existing opts))))]
    (completed posts)))

(defn parse
  [opts d]
  (e/go d (:url opts))
  (println "Searching for posts...")
  (parse* opts d))

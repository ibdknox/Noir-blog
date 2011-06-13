(ns noir-blog.models.post
  (:require [simpledb.core :as db]
            [clj-time.core :as ctime]
            [clj-time.format :as tform]
            [clj-time.coerce :as coerce]
            [clojure.string :as string]
            [noir-blog.models.user :as users]
            [noir.validation :as vali]
            [noir.session :as session])
  (:import com.petebevin.markdown.MarkdownProcessor))

(def posts-per-page 10)
(def date-format (tform/formatter "MM/dd/yy" (ctime/default-time-zone)))
(def time-format (tform/formatter "h:mma" (ctime/default-time-zone)))
(def mdp (MarkdownProcessor.))

;; Gets

(defn total []
  (count (db/get :post-ids)))

(defn id->post [id]
  (db/get-in :posts [id]))

(defn ids->posts [ids]
  (map id->post ids))

(defn moniker->post [moniker]
  (id->post (db/get-in :post-monikers [moniker])))

(defn get-page [page]
  (let [page-num (- (Integer. page) 1) ;; make it 1-based indexing
        prev (* page-num posts-per-page)]
    (ids->posts (take posts-per-page (drop prev (db/get :post-ids))))))

(defn get-latest []
  (get-page 1))

;; Mutations and checks

(defn next-id []
  (str (db/update! :next-post-id inc)))

(defn gen-moniker [title]
  (-> title
    (string/lower-case)
    (string/replace #"[^a-zA-Z0-9\s]" "")
    (string/replace #" " "-")))

(defn new-moniker? [moniker]
  (not (contains? (db/get :post-monikers) moniker)))

(defn perma-link [moniker]
  (str "/blog/view/" moniker))

(defn edit-url [{:keys [id]}]
  (str "/blog/admin/post/edit/" id))

(defn md->html [text]
  (. mdp (markdown text)))

(defn wrap-moniker [{:keys [title] :as post}]
  (let [moniker (gen-moniker title)]
    (-> post
      (assoc :moniker moniker)
      (assoc :perma-link (perma-link moniker)))))
    
(defn wrap-markdown [{:keys [body] :as post}]
  (assoc post :md-body (md->html body)))

(defn wrap-time [post]
  (let [ts (ctime/now)]
    (-> post
      (assoc :ts (coerce/to-long ts))
      (assoc :date (tform/unparse date-format ts))
      (assoc :tme (tform/unparse time-format ts)))))

(defn prepare-new [{:keys [title body] :as post}]
  (let [id (next-id)
        ts (ctime/now)]
    (-> post
      (assoc :id id)
      (assoc :username (users/me))
      (wrap-time)
      (wrap-moniker)
      (wrap-markdown))))

(defn valid? [{:keys [title body]}]
  (vali/rule (vali/has-value? title)
             [:title "There must be a title"])
  (vali/rule (new-moniker? (gen-moniker title))
             [:title "That title is already taken."])
  (vali/rule (vali/has-value? body)
             [:body "There's no post content."])
  (not (vali/errors? :title :body)))

;; Operations

(defn add! [post]
  (when (valid? post)
    (let [{:keys [id moniker] :as final} (prepare-new post)]
      (db/update! :posts assoc id final)
      (db/update! :post-ids conj id)
      (db/update! :post-monikers assoc moniker id))))

(defn edit! [{:keys [id title] :as post}]
  (let [{orig-moniker :moniker :as original} (id->post id)
        {:keys [moniker] :as final} (-> post
                                      (wrap-moniker)
                                      (wrap-markdown))]
    (db/update! :posts assoc id (merge original final))
    (db/update! :post-monikers dissoc orig-moniker) ;; remove the old moniker entry in case it changed
    (db/update! :post-monikers assoc moniker id)))

(defn remove! [id]
  (let [{:keys [moniker]} (id->post id)
        neue-ids (remove #{id} (db/get :post-ids))]
    (db/put! :post-ids neue-ids) 
    (db/update! :posts dissoc id)
    (db/update! :post-monikers dissoc moniker)))

(defn init! []
    (db/put! :next-post-id -1)
    (db/put! :posts {})
    (db/put! :post-ids (list))
    (db/put! :post-monikers {}))

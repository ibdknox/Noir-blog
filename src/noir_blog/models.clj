(ns noir-blog.models
  (:require [simpledb.core :as db]
            [noir-blog.models.user :as users]
            [noir-blog.models.post :as posts]))

(defn initialize []
  (db/init)
  (when-not (db/get :users)
    ;;db values need to be initialized.. this should only happen once.
    (users/init!)
    (posts/init!)))

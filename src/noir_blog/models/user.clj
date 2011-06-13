(ns noir-blog.models.user
  (:require [simpledb.core :as db]
            [noir.util.crypt :as crypt]
            [noir.validation :as vali]
            [noir.session :as session]))

;; Gets

(defn all []
  (vals (db/get :users)))

(defn get-username [username]
  (db/get-in :users [username]))
    
(defn admin? []
  (session/get :admin))

(defn me []
  (session/get :username))

;; Mutations and Checks

(defn prepare [{password :password :as user}]
  (assoc user :password (crypt/encrypt password)))

(defn valid? [{:keys [username password]}]
  (vali/rule (not (db/get-in :users [username]))
             [:username "That username is already taken"])
  (vali/rule (vali/min-length? username 3)
             [:username "Username must be at least 3 characters."])
  (vali/rule (vali/min-length? password 5)
             [:password "Password must be at least 5 characters."])
  (not (vali/errors? :username :password)))

;; Operations

(defn- store! [{username :username :as user}]
  (db/update! :users assoc username user))

(defn login! [{:keys [username password] :as user}]
  (let [{stored-pass :password} (get-username username)]
    (if (and stored-pass 
             (crypt/compare password stored-pass))
      (do
        (session/put! :admin true)
        (session/put! :username username))
      (vali/set-error :username "Invalid username or password"))))

(defn add! [{:keys [username password]}]
  (let [user (-> {:username username :password password}
               (prepare))]
    (when (valid? user)
      (store! user))))

(defn edit! [user]
  (add! user))

(defn remove! [username]
  (db/update! :users dissoc username))

(defn init! []
    (db/put! :users {})
    (store! (prepare {:username "admin" :password "admin"})))

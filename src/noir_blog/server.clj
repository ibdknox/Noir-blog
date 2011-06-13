(ns noir-blog.server
  (:require [noir.server :as server]
            [noir-blog.models :as models]))

(server/load-views "src/noir_blog/views/")

(defn -main [& m]
  (let [mode (or (first m) :dev)]
    (models/initialize)
    (server/start 8080 {:mode (keyword mode)
                        :ns 'noir-blog})))


(ns noir-blog.server
  (:require [noir.server :as server]
            [noir-blog.models :as models]))

(server/load-views "src/noir_blog/views/")

(defn -main [& m]
  (let [mode (or (first m) :dev)
        port (Integer. (get (System/getenv) "PORT" "8080"))]
    (models/initialize)
    (server/start port {:mode (keyword mode)
                        :ns 'noir-blog})))


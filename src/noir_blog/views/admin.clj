(ns noir-blog.views.admin
  (:use noir.core
        hiccup.core
        hiccup.page-helpers
        hiccup.form-helpers)
  (:require [noir.session :as session]
            [noir.validation :as vali]
            [noir.response :as resp]
            [clojure.string :as string]
            [noir-blog.models.post :as posts]
            [noir-blog.models.user :as users]
            [noir-blog.views.common :as common]))

;; Links

(def post-actions [{:url "/blog/admin/post/add" :text "Add a post"}])
(def user-actions [{:url "/blog/admin/user/add" :text "Add a user"}])

;; Partials

(defpartial post-fields [{:keys [title body]}]
            (vali/on-error :title #(html [:p (string/join "<br/>" %)]))
            (text-field {:placeholder "Title"} :title title)
            (vali/on-error :body #(html [:p (string/join "<br/>" %)]))
            (text-area {:placeholder "Body"} :body body))

(defpartial user-fields [{:keys [username] :as usr}]
            (vali/on-error :username #(html [:p.error (string/join "<br/>" %)]))
            (text-field {:placeholder "Username"} :username username)
            (password-field {:placeholder "Password"} :password))

(defpartial post-item [{:keys [title] :as post}]
            [:li
             (link-to (posts/edit-url post) title)])

(defpartial action-item [{:keys [url text]}]
            [:li
             (link-to url text)])

(defpartial user-item [{:keys [username]}]
            [:li
             (link-to (str "/blog/admin/user/edit/" username) username)])

;; Admin pages

;;force you to be an admin to get to the admin section
(pre-route "/blog/admin/*" {}
           (when-not (users/admin?)
             (resp/redirect "/blog/login")))

(defpage "/blog/login" {:as user}
         (common/main-layout
           (form-to [:post "/blog/login"]
                    [:ul.actions
                     [:li (link-to {:class "submit"} "/" "Login")]]
                    (user-fields user)
                    (submit-button {:class "submit"} "submit"))))

(defpage [:post "/blog/login"] {:as user}
         (if (users/login! user)
           (resp/redirect "/blog/admin")
            (render "/blog/login" user)))

(defpage "/blog/logout" {}
         (session/clear!)
         (resp/redirect "/blog/"))

;; Post pages

(defpage "/blog/admin" {}
         (common/admin-layout
           [:ul.actions
            (map action-item post-actions)]
           [:ul.items
            (map post-item (posts/get-latest))]))

(defpage "/blog/admin/post/add" {:as post}
         (common/admin-layout
           (form-to [:post "/blog/admin/post/add"]
                      [:ul.actions
                        [:li (link-to {:class "submit"} "/" "Add")]]
                    (post-fields post)
                    (submit-button {:class "submit"} "add post"))))

(defpage [:post "/blog/admin/post/add"] {:as post}
           (if (posts/add! post)
             (resp/redirect "/blog/admin")
             (render "/blog/admin/post/add" post)))

(defpage "/blog/admin/post/edit/:id" {:keys [id]}
         (if-let [post (posts/id->post id)]
           (common/admin-layout
             (form-to [:post (str "/blog/admin/post/edit/" id)]
                      [:ul.actions
                        [:li (link-to {:class "submit"} "/" "Submit")]
                        [:li (link-to {:class "delete"} (str "/blog/admin/post/remove/" id) "Remove")]]
                      (post-fields post)
                      (submit-button {:class "submit"} "submit")))))

(defpage [:post "/blog/admin/post/edit/:id"] {:keys [id] :as post}
         (if (posts/edit! post)
           (resp/redirect "/blog/admin")
           (render (str "/blog/admin/post/edit/" id) post)))

(defpage "/blog/admin/post/remove/:id" {:keys [id]}
         (posts/remove! id)
         (resp/redirect "/blog/admin"))

;; User pages

(defpage "/blog/admin/users" {}
         (common/admin-layout
           [:ul.actions
            (map action-item user-actions)]
           [:ul.items
            (map user-item (users/all))]))

(defpage "/blog/admin/user/add" {}
         (common/admin-layout
           (form-to [:post "/blog/admin/user/add"]
                      [:ul.actions
                        [:li (link-to {:class "submit"} "/" "Add")]]
                    (user-fields {})
                    (submit-button {:class "submit"} "add user"))))

(defpage [:post "/blog/admin/user/add"] {:keys [username password] :as neue-user}
         (if (users/add! neue-user)
           (resp/redirect "/blog/admin/users")
           (render "/blog/admin/user/add" neue-user)))

(defpage "/blog/admin/user/edit/:old-name" {:keys [old-name]}
         (let [user (users/get-username old-name)]
           (common/admin-layout
             (form-to [:post (str "/blog/admin/user/edit/" old-name)]
                      [:ul.actions
                        [:li (link-to {:class "submit"} "/" "Submit")]
                        [:li (link-to {:class "delete"} (str "/blog/admin/user/remove/" old-name) "Remove")]]
                      (user-fields user)))))

(defpage [:post "/blog/admin/user/edit/:old-name"] {:keys [old-name] :as user}
         (if (users/edit! old-name user)
           (resp/redirect "/blog/admin/users")
           (render "/blog/admin/user/edit/:old-name" user)))

(defpage "/blog/admin/user/remove/:id" {:keys [id]}
         (users/remove! id)
         (resp/redirect "/blog/admin/users"))

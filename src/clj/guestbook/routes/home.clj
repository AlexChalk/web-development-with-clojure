(ns guestbook.routes.home
  (:require [guestbook.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [guestbook.db.core :as db]
            [ring.util.response :refer [redirect]]
            [struct.core :as st]
            [clojure.java.io :as io]
            [ring.util.response :refer [response status]]))

(def message-schema
  [[:name
    st/required
    st/string]

   [:message
    st/required
    st/string
    {:message "message must contain at least 10 characters"
     :validate #(> (count %) 9)}]])

(defn validate-message [params]
  (first (st/validate params message-schema)))

   
(defn save-message! [{:keys [params]}]
  (if-let [errors (validate-message params)]
    (response/bad-request {:errors errors})
    (try
      (db/save-message!
        (assoc params :timestamp (java.util.Date.)))
      (response/ok {:status :ok})
      (catch Exception e
        (response/internal-server-error
          {:errors {:server-error ["Failed to save message!"]}})))))

(defn home-page []
  (layout/render "home.html")) 

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/messages" [] (response/ok (db/get-messages)))
  (POST "/message" request (save-message! request))
  (GET "/about" [] (about-page)))

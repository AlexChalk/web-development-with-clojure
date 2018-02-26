(ns guestbook.routes.ws
  (:require [compojure.core :refer [GET POST defroutes]]
            [struct.core :as st]
            [guestbook.db.core :as db]
            [mount.core :refer [defstate]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.immutant 
             :refer [sente-web-server-adapter]]))

(let [connection (sente/make-channel-socket!
                   sente-web-server-adapter
                   {:user-id-fn
                    (fn [ring-req] (get-in ring-req [:params :client-id]))})]
  (def ring-ajax-post (:ajax-post-fn connection))
  (def ring-ajax-get-or-ws-handshake (:ajax-get-or-ws-handshake-fn connection))
  (def ch-chsk (:ch-recv connection))
  (def chsk-send! (:send-fn connection))
  (def connected-uids (:connected-uids connection)))

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

(defn save-message! [message]
  (if-let [errors (validate-message message)]
    {:errors errors}
    (try
      (db/save-message! message)
      message)))

(defn handle-message! [{:keys [id client-id ?data]}]
  (when (= id :guestbook/add-message)
    (let [response (-> 
                     ?data
                     (assoc :timestamp (java.util.Date.))
                     save-message!)]
     (if (:errors response)
       (chsk-send! client-id [:guestbook/error response])
       (doseq [uid (:any @connected-uids)]
         (chsk-send! uid [:guestbook/add-message response]))))))

(defn stop-router! [stop-fn]
  (when stop-fn (stop-fn)))

(defn start-router! []
  (sente/start-chsk-router! ch-chsk handle-message!))

(defstate router
  :start (start-router!)
  :stop (stop-router! router))

(defroutes websocket-routes
  (GET "/ws" req (ring-ajax-get-or-ws-handshake req))
  (POST "/ws" req (ring-ajax-post req)))

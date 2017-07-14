(ns contentserver.core (:gen-class)
  ^{:author "jabala"
    :doc "Bank Account API"
    :added "1.0"
    }
  (:require [clojure.tools.logging :as log]
            [contentserver.httpserver :as http]
            [contentserver.utils.config :as config]))

;; called when the program exits
(defn end-program
  "Shut application down gracefully"
  []
  ; stop server
  (http/stop-server ))

(defn -main
  "main entry of application"
  [& args]
  (log/info "===================================================")
  (log/info "BANK ACCOUNT API - VERSION 1.1.0")
  (log/info "===================================================")

  (config/load-configuration)                                 ; load configuration
  (http/initialize-http )                                   ; initialize http server

  ;; register runtime hook
  (.addShutdownHook (Runtime/getRuntime) (Thread. end-program))

  ;; start server
  (try
    (http/start-server)
    (catch Exception e
      (do
        (log/error "start server E: " (.getMessage e))
        ;; end program gracefully
        (end-program)))))

(-main)



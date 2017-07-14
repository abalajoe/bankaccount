(ns contentserver.httpserver
  ^{:author "joeabala"
    :doc "HTTP Server"
    :added "1.0"
    }
  (:require [clojure.tools.logging :as log]
            [contentserver.utils.config :as config]
            [contentserver.model.datasource :as db]
            [clojure.data.json :as json]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]])
  (:use org.httpkit.server
        [compojure.route :only [not-found]]
        [compojure.handler :only [site]] ; form, query params decode; cookie; session, etc
        [compojure.core :only [defroutes GET POST DELETE ANY context]]))

(defn initialize-http
  "Function defines server once"
  []
  (defonce server (atom nil))
  (log/info "initializing http server ..."))

(def track-deposit-amount (atom 0)) ;; track state of deposit amount, to curb maximum daily deposit
(def track-withdraw-amount (atom 0)) ;; track state of withdrawal amount

(defn home
  "Welcome page"
  [req]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/write-str {:status-code 0 :status-msg "Welcome to Bank Account API"})})

(defn process-balance-request
  [req]
  "Process balance request"
  (let [{balance :balance} (first (db/get-account-information)) ;; get balance from datastore
        balance (if (nil? balance) 0 balance)] ;; if there is no information returned, set balance as 0
    {:status 200
     :headers {"Content-Type" "application/json; charset=utf-8"}
     :body (json/write-str {:status-code 0 :status-msg "Your balance request is successful" :balance balance})}))

(defn process-deposit-request
  [{:keys [params] :as args}]
  "Process deposit request"
  (let [{deposit :deposit} params ;; get deposit amount from request
        today-date (.format (java.text.SimpleDateFormat. "dd-MM-yyyy") (new java.util.Date)) ;; get today's date
        db-result (first (db/get-account-information)) ;; get account information from datastore
        deposit (try ;; validate amount i.e make sure its a valid integer and its more than $9
                    (if (> (Long/parseLong deposit) 9) (Long/parseLong deposit) false) ;; return deposit value if successful
                   (catch ClassCastException e
                     (log/error (.getMessage e))
                     false ) ;; return false on exception
                    (catch NumberFormatException e
                      (log/error (.getMessage e))
                      false ) ;; return false on exception
                    (catch Exception e
                      (log/error (.getMessage e))
                      false ))] ;; return false on exception

    (if (not= false deposit) ;; check if deposit value is valid, if its valid proceed
      (if (< deposit config/max-deposit-per-transact) ;; check if deposit does not exceed daily max, if it does not proceed
        (if (seq db-result) ;; check if there is any data returned from datasource, if yes proceed
          (if  (< (+ deposit @track-deposit-amount) config/daily-max-deposit) ;; check if daily max deposit has been achieved, if not proceed
            (if (= (clojure.string/trim today-date) (clojure.string/trim (:transaction_timestamp db-result))) ;; check if this is first user transaction for the day, if not proceed
              (if (> config/max-deposit-freq (:deposit_count db-result)) ;; check if daily deposit limit has been reached, if not proceed
                (do
                  ;; all steps achieved, track deposit amount and update account
                  (log/info (:deposit_count db-result) " transaction for the day ")
                  (let [increment-deposit-amount-tracker (+ @track-deposit-amount deposit) ;; track deposit amount
                        deposit-amount-tracker (reset! track-deposit-amount increment-deposit-amount-tracker) ;; track deposit amount
                        _ (log/info "deposit-amount-tracker|depo-count " deposit-amount-tracker (:deposit_count db-result)) ;; log deposit amount and deposit count
                        {id :id} db-result ;; get record id
                        {balance :balance} db-result ;; get balance
                        {deposit-count :deposit_count} db-result ;; get deposit count
                        increment-daily-count (+ 1 deposit-count) ;; increment deposit by 1, to track deposit limit
                        _ (log/infof "{%s %s %s}" id balance deposit) ;; log id, balance and deposit
                        db-result (db/update-deposit-information id (+ balance deposit) increment-daily-count) ;; update account
                        {new-balance :balance} (first db-result);; get new balance
                        _ (log/info "new bal " new-balance)]
                    {:status 422
                     :headers {"Content-Type" "application/json; charset=utf-8"}
                     :body (json/write-str {:status-code 0 :status-msg "Your deposit was successful" :balance new-balance})}))
                (do
                  ;; daily deposit limit reached, return 422
                  {:status 422
                   :headers {"Content-Type" "application/json; charset=utf-8"}
                   :body (json/write-str {:status-code -2 :status-msg "Your daily deposit limit has reached" })}))
              (do
                ;; this is the first transaction for the day, record to database
                (log/info "new transaction for the day " deposit)
                (let [increment-deposit-amount-tracker (+ @track-deposit-amount deposit) ;; track deposit amount
                      _ (reset! track-deposit-amount increment-deposit-amount-tracker) ;; track deposit amount
                      db-result (db/record-account (+ deposit (:balance db-result)) 1 0) ;; load to database
                      {new-balance :balance} (first db-result);; get new balance
                      _ (log/info "new bal " new-balance)]
                  {:status 200
                   :headers {"Content-Type" "application/json; charset=utf-8"}
                   :body (json/write-str {:status-code 0 :status-msg "Your deposit was successful" :balance new-balance})})))
            (do
              ;; daily max deposit has been achieved, return 422
              {:status 422
               :headers {"Content-Type" "application/json; charset=utf-8"}
               :body (json/write-str {:status-code -2 :status-msg "You have reached your daily max deposit of $150,000"})})
            )
          (do
            ;; we dont have any data on database i.e account is inactive, record new data
            (let [increment-deposit-amount-tracker (+ @track-deposit-amount deposit) ;; calculate daily deposit amount
                  _ (reset! track-deposit-amount increment-deposit-amount-tracker) ;; track deposit amount
                  db-result (db/record-account deposit 1 0) ;; record data to datasource
                  {new-balance :balance} (first db-result)] ;; get new balance
              {:status 422
               :headers {"Content-Type" "application/json; charset=utf-8"}
               :body (json/write-str {:status-code -2 :status-msg "Your deposit was successful" :balance new-balance})})))
        (do
          ;; deposit is more than daily max deposit, return 422
          {:status 422
           :headers {"Content-Type" "application/json; charset=utf-8"}
           :body (json/write-str {:status-code -2 :status-msg "You cannot deposit more than $40,000"})}))
      (do
        ;; invalid deposit, return 422
        {:status 422
         :headers {"Content-Type" "application/json; charset=utf-8"}
         :body (json/write-str {:status-code -2 :status-msg "Invalid Amount"})}))))


(defn process-withdrawal-request
  [{:keys [params] :as args}]
  (log/info "my request " args)
  (let [{withdraw :withdraw} params  ;; get withdraw amount from request
        today-date (.format (java.text.SimpleDateFormat. "dd-MM-yyyy") (new java.util.Date)) ;; get today's date
        db-result (first (db/get-account-information)) ;; get account information
        withdraw (try ;; validate amount i.e make sure its a valid integer and its more than $9
                  (if (> (Long/parseLong withdraw) 9) (Long/parseLong withdraw) false);; return deposit value if successful
                  (catch ClassCastException e
                    (log/error "::"(.getMessage e))
                    false ) ;; return false on exception
                  (catch NumberFormatException e
                    (log/error (.getMessage e))
                    false ) ;; return false on exception
                  (catch Exception e
                    (log/error (.getMessage e))
                    false ))] ;; return false on exception
    (if (not= false withdraw) ;; check if withdraw value is valid, if its valid proceed
      (if (< withdraw config/max-withdraw-per-transact);; check if withdrawal does not exceed daily max, if it does not proceed
        (if (seq db-result) ;; check if there is any account information in datastore
          (if  (< (+ withdraw @track-withdraw-amount) config/daily-max-withdraw) ;;check if daily max withdrawal has been achieved, if not proceed
            (if (= (clojure.string/trim today-date) (clojure.string/trim (:transaction_timestamp db-result))) ;; check if this is first user transaction for the day, if not proceed
              (if (> config/max-withdraw-freq (:withdrawal_count db-result))
                (let [increment-withdraw-amount-tracker (+ @track-withdraw-amount withdraw);; track withdrawal amount
                      withdraw-amount-tracker (reset! track-withdraw-amount increment-withdraw-amount-tracker) ;; track withdrawal amount
                      _ (log/info "withdraw-amount-tracker3|depo-count " withdraw-amount-tracker (:withdrawal_count db-result))
                      {id :id} db-result ;; get record id
                      {balance :balance} db-result ;; get balance
                      {withdraw-count :withdrawal_count} db-result ;; get withdrawal count
                      increment-daily-count (+ 1 withdraw-count) ;; increment withdrawal count
                      db-result (db/update-withdrawal-information id (- balance withdraw) increment-daily-count) ;; update account
                      {new-balance :balance} (first db-result)] ;; get new balance
                  {:status 200
                   :headers {"Content-Type" "application/json; charset=utf-8"}
                   :body (json/write-str {:status-code 0 :status-msg (str "You have withdrawn $" withdraw) :balance new-balance})})
                {:status 200
                 :headers {"Content-Type" "application/json; charset=utf-8"}
                 :body (json/write-str {:status-code 0 :status-msg "Your daily withdrawal limit has reached"})}
                )
              (do
                ;; first user withdrawal for the day, record account info
                (let [increment-withdraw-amount-tracker (+ @track-withdraw-amount withdraw) ;; track withdrawal amount
                      withdraw-amount-tracker (reset! track-withdraw-amount increment-withdraw-amount-tracker);; track withdrawal amount
                      _ (log/info "withdraw-amount-tracker2 " withdraw-amount-tracker)
                      db-result (db/record-account (- (:balance db-result) withdraw) 0 1) ;; record account information
                      {new-balance :balance} (first db-result)] ;; get new balance
                  {:status 200
                   :headers {"Content-Type" "application/json; charset=utf-8"}
                   :body (json/write-str {:status-code 0 :status-msg (str "You have withdrawn $" withdraw) :balance new-balance})})))
            (do
              ;; you have exceeded your daily withdrawal
              {:status 422
               :headers {"Content-Type" "application/json; charset=utf-8"}
               :body (json/write-str {:status-code -2 :status-msg "You have reached your daily max withdrawal of $50,000"})}))
          (do
            ;; there is no account information, please make deposit
            {:status 422
             :headers {"Content-Type" "application/json; charset=utf-8"}
             :body (json/write-str {:status-code -2 :status-msg "Please activate your account"})}))
        (do
          ;; withdrawal is more than daily max withdrawal, return 422
          {:status 422
           :headers {"Content-Type" "application/json; charset=utf-8"}
           :body (json/write-str {:status-code -2 :status-msg "You cannot withdraw more than $20,000"})}))
      (do
        ;; invalid withdrawal, return 422
        {:status 422
         :headers {"Content-Type" "application/json; charset=utf-8"}
         :body (json/write-str {:status-code -2 :status-msg "Invalid Amount"})}))))


;; routes
(defroutes all-routes
           (GET "/" [] home)
           (GET "/balance" [] process-balance-request)
           (POST "/deposit" [] process-deposit-request)
           (POST "/withdraw" [] process-withdrawal-request)
           (not-found {:status 404
                       :headers {"Content-Type" "application/json; charset=utf-8"}
                       :body (json/write-str {:status-code -1 :status-msg "Invalid Request"})})) ;; return 404


(def app
  (wrap-defaults all-routes (assoc-in site-defaults [:security :anti-forgery] false)))

(defn start-server
  "Starts server"
  []
  (try
    (do
      (reset! server (run-server (site #'all-routes) {:port config/server-port}))
      (log/info "server started at port " config/server-port))
    (catch Exception e
      (do
        (throw (Exception. (str "Error: port " config/server-port " is already in use. " e)))))))

(defn stop-server
  "Gracefuly shut down server"
  []
  (when-not (nil? @server)
    (@server :timeout 100) ;; wait 100ms for existing requests to be finished
    (reset! server nil)
    (log/info "server shut down")))



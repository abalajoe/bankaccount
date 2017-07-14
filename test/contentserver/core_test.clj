(ns contentserver.core-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [contentserver.core :refer :all]
            [clj-http.client :as http]
            [ring.mock.request :as mock]
            [contentserver.httpserver :as httpserver]
            [clojure.data.json :as json]
            [contentserver.model.datasource :as db]))

(facts "Bank Account testing"
       (fact "Default route"
             (let [response (httpserver/app (mock/request
                                              :get "/"
                                              ))] ;
               (:status response) => 200
               (:headers response) => {"Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Frame-Options" "SAMEORIGIN", "X-XSS-Protection" "1; mode=block"}
               (:body response) => (json/write-str {:status-code 0 :status-msg "Welcome to Bank Account API"})))
       (fact "Check Balance"
             (let [response (httpserver/app (mock/request
                                              :get "/balance"
                                              ))] ;
               (:status response) => 200
               (:headers response) => {"Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Frame-Options" "SAMEORIGIN", "X-XSS-Protection" "1; mode=block"}
               (:body response) => (json/write-str {:status-code 0 :status-msg "Your balance request is successful"
                                                    :balance (:balance (first (db/get-account-information)))})))
       (fact "Invalid Route"
             (let [response (httpserver/app (mock/request
                                              :get "/invalid"
                                              ))] ;
               (:status response) => 404
               (:headers response) => {"Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Frame-Options" "SAMEORIGIN", "X-XSS-Protection" "1; mode=block"}
               (:body response) => (json/write-str {:status-code -1 :status-msg "Invalid Request"}))))

(facts "Deposit Account testing"
       (fact "Invalid input"
             (let [response (httpserver/app (mock/request
                                              :post "/deposit"
                                              {:deposit "hello"}))] ;
               (:status response) => 422
               (:headers response) => {"Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Frame-Options" "SAMEORIGIN", "X-XSS-Protection" "1; mode=block"}
               (:body response) => (json/write-str {:status-code -2 :status-msg "Invalid Amount"})))
       (fact "Invalid Amount"
             (let [response (httpserver/app (mock/request
                                              :post "/deposit"
                                              {:deposit "9"}))] ;
               (:status response) => 422
               (:headers response) => {"Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Frame-Options" "SAMEORIGIN", "X-XSS-Protection" "1; mode=block"}
               (:body response) => (json/write-str {:status-code -2 :status-msg "Invalid Amount"})))
       (fact "Max Daily Deposit"
             (let [response (httpserver/app (mock/request
                                              :post "/deposit"
                                              {:deposit "50000"}))] ;
               (:status response) => 422
               (:headers response) => {"Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Frame-Options" "SAMEORIGIN", "X-XSS-Protection" "1; mode=block"}
               (:body response) => (json/write-str {:status-code -2 :status-msg "You cannot deposit more than $40,000"}))))

(facts "Withdraw Account testing"
       (fact "Invalid input"
             (let [response (httpserver/app (mock/request
                                              :post "/withdraw"
                                              {:deposit "hello"}))] ;
               (:status response) => 422
               (:headers response) => {"Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Frame-Options" "SAMEORIGIN", "X-XSS-Protection" "1; mode=block"}
               (:body response) => (json/write-str {:status-code -2 :status-msg "Invalid Amount"})))
       (fact "Invalid Amount"
             (let [response (httpserver/app (mock/request
                                              :post "/withdraw"
                                              {:deposit "9"}))] ;
               (:status response) => 422
               (:headers response) => {"Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Frame-Options" "SAMEORIGIN", "X-XSS-Protection" "1; mode=block"}
               (:body response) => (json/write-str {:status-code -2 :status-msg "Invalid Amount"})))
       (fact "Max Daily Deposit"
             (let [response (httpserver/app (mock/request
                                              :post "/withdraw"
                                              {:withdraw "50000"}))] ;
               (:status response) => 422
               (:headers response) => {"Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Frame-Options" "SAMEORIGIN", "X-XSS-Protection" "1; mode=block"}
               (:body response) => (json/write-str {:status-code -2 :status-msg "You cannot withdraw more than $20,000"}))))
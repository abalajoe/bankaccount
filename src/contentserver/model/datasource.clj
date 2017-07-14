(ns contentserver.model.datasource
  ^{:author "joeabala"
    :doc    "Database configurations"
    :added  "1.0"
    }
  (:require
            [clojure.tools.logging :as log])
  (:use korma.core)
  (:use korma.db)
  (:use pg-hstore.core)
  (:import (java.sql SQLException)))

;==================================================================================================
;;                                  DATABASE SPECIFICATIONS
;==================================================================================================

(defdb db (postgres {:db         "bankaccount"
                     :user       "postgres"
                     :password   "root"
                     :host       "159.203.104.154"
                    ; :host       "localhost"
                     :port       "5432"
                     :delimiters ""}))

;==================================================================================================
;;                                    GENERAL INFORMATION
;==================================================================================================
(defn record-account
  "record new day transaction"
  [balance deposit withdrawal]
  (try
    (exec-raw db [(format "insert into tbl_account (balance, deposit_count, withdrawal_count)
                          values (%d,%d,%d) returning balance" balance deposit withdrawal)] :results)
    (catch SQLException e
      (log/error (.getMessage e)))
    (catch Exception e
      (log/error (.getMessage e)))))

(defn get-account-information
  "Get accoung information"
  []
  (try
    (exec-raw db ["select id, balance, deposit_count, withdrawal_count,
                  to_char(transaction_timestamp, 'dd-MM-yyyy')as transaction_timestamp from tbl_account order by transaction_timestamp desc limit 1"] :results)
    (catch SQLException e
      (log/error (.getMessage e)))
    (catch Exception e
      (log/error (.getMessage e)))))

(get-account-information)

(defn update-deposit-information
  "update deposit information"
  [id balance deposit-count]
  (try
    (exec-raw db [(format "update tbl_account set balance=%d, deposit_count=%d where id=%d RETURNING balance;" balance deposit-count id)] :results)
    (catch SQLException e
      (log/error (.getMessage e)))
    (catch Exception e
      (log/error (.getMessage e)))))

(defn update-withdrawal-information
  "update withdrawal information"
  [id balance withdraw-count]
  (try
    (exec-raw db [(format "update tbl_account set balance=%d, withdrawal_count=%d where id=%d returning balance" balance withdraw-count id)] :results)
    (catch SQLException e
      (log/error (.getMessage e)))
    (catch Exception e
      (log/error (.getMessage e)))))

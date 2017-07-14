(ns contentserver.utils.config
  ^{:author "joeabala"
    :doc "Load configurations"
    :added "1.0"
    }
  (:require [propertea.core :as propertea]
            [clojure.tools.logging :as log]))


;; configuration-file path
(def config (System/getProperty "bankaccount.config"))
;(def config "/Users/abala/Desktop/bankaccount/src/config.properties")

(defn get-configuration
  "Read from configuration file"
  [key]
  ((propertea/read-properties config)key))

(defn load-configuration
  "Load configuration file"
  []
  (log/infof "Loading configuration...")
  (def server-port (Integer/parseInt (get-configuration :server-port)))
  (def daily-max-deposit (Long/parseLong (get-configuration :daily-max-deposit)))            ;
  (def max-deposit-per-transact (Long/parseLong (get-configuration :max-deposit-per-transact)))
  (def max-deposit-freq (Integer/parseInt (get-configuration :max-deposit-freq)))
  (def daily-max-withdraw (Long/parseLong (get-configuration :daily-max-withdraw)))            ;
  (def max-withdraw-per-transact (Long/parseLong (get-configuration :max-withdraw-per-transact)))
  (def max-withdraw-freq (Integer/parseInt (get-configuration :max-withdraw-freq)))
  (log/infof "finished loading conf:  [%s %s %s %s %s %s %s]"
             server-port daily-max-deposit max-deposit-per-transact max-deposit-freq
             daily-max-withdraw max-withdraw-per-transact max-withdraw-freq))

;(load-configuration)

  
  
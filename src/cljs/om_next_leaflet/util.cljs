(ns om-next-leaflet.util
  (:require [clojure.string :as str]
            [cognitect.transit :as t]
            [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [cljs.core.async :refer [put! chan <!]])
  (:import [goog.net XhrIo]
           [goog.events.listen]))

(defn transit-post [url]
  (fn [edn callback-fn]
    (.send XhrIo url
      (fn [e]
        (this-as this
          (try
            (let [res (.getResponseText this)
                  parsed (t/read (t/reader :json) res)]
              (try
                (callback-fn parsed)
                (catch js/Error ex
                  (error "Exception occurred in transit-post callback." ex)
                  (error "parsed edn=" parsed)
                  (error "stacktrace=" ex.stack))))
            (catch js/Error ex
              (error "Exception occurred in parse transit response." ex)
              (error "responseText=" (.getResponseText this))
              (error "stacktrace=" ex.stack)))))
      "POST" (t/write (t/writer :json) edn)
      #js {"Content-Type" "application/transit+json"})))

(defn download-file
  [url & {:keys [on-success on-error]}]
  (let [xhrio (XhrIo.)]
    (.setResponseType xhrio XhrIo.ResponseType.ARRAY_BUFFER)
    (goog.events.listen xhrio EventType.SUCCESS on-success)
    (goog.events.listen xhrio EventType.ERROR   on-error)
    (.send xhrio url "GET" nil #js {"Content-type" "application/octet-stream"})))

(defn send-request
  [method url content-type callback]
  (.send XhrIo url
         (fn [e]
           (this-as this
             (callback e this))
           method
           content-type)))

(def http-methods #{"GET" "PUT" "POST" "DELETE"})

(defn handler
  [k e chan]
  (put! chan {:result k :event e}))

(def xhrio (XhrIo.))

(defn send-request!
  [method url content-type data chan]
  (let [method (str/upper-case (name method))
        _ (assert (http-methods method))]
    (.setResponseType xhrio XhrIo.ResponseType.ARRAY_BUFFER)
    (goog.events.listen xhrio EventType.SUCCESS (fn [e] (handler :success e chan)))
    (goog.events.listen xhrio EventType.ERROR (fn [e] (handler :error e chan )))
    (goog.events.listen xhrio EventType.COMPLETE (fn [e] (handler :complete e chan)))
    (goog.events.listen xhrio EventType.ABORT (fn [e] (handler :abort e chan)))
    (goog.events.listen xhrio EventType.TIMEOUT (fn [e] (handler :timeout e chan)))
    (.send XhrIo url method data #js {"Content-type" "application/octet-stream"})))

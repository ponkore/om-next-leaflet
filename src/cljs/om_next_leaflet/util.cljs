(ns om-next-leaflet.util
  (:require [clojure.string :as str]
            [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [cljs.core.async :refer [put! chan <!]]
            [goog.events :as events])
  (:import [goog.net XhrIo]
           [goog.net EventType]))

;; (defn transit-post [url]
;;   (fn [edn callback-fn]
;;     (.send XhrIo url
;;       (fn [e]
;;         (this-as this
;;           (try
;;             (let [res (.getResponseText this)
;;                   parsed (t/read (t/reader :json) res)]
;;               (try
;;                 (callback-fn parsed)
;;                 (catch js/Error ex
;;                   (error "Exception occurred in transit-post callback." ex)
;;                   (error "parsed edn=" parsed)
;;                   (error "stacktrace=" ex.stack))))
;;             (catch js/Error ex
;;               (error "Exception occurred in parse transit response." ex)
;;               (error "responseText=" (.getResponseText this))
;;               (error "stacktrace=" ex.stack)))))
;;       "POST" (t/write (t/writer :json) edn)
;;       #js {"Content-Type" "application/transit+json"})))

(defn download-file
  [url & {:keys [on-success on-error]}]
  (let [xhrio (XhrIo.)]
    (.setResponseType xhrio XhrIo.ResponseType.ARRAY_BUFFER)
    (events/listen xhrio EventType.SUCCESS on-success)
    (events/listen xhrio EventType.ERROR   on-error)
    (.send xhrio url "GET" nil #js {"Content-type" "application/octet-stream"})))

;; (defn send-req0
;;   [method url content-type callback]
;;   (.send XhrIo url
;;          (fn [e]
;;            (this-as this
;;              (callback e this))
;;            method
;;            content-type)))

(def http-methods #{"GET" "PUT" "POST" "DELETE"})

(defn handler
  [xhrio k e chan]
  (if (= k :success)
    (let [data (js->clj (.getResponseJson xhrio) :keywordize-keys true)]
      (put! chan {:result k :event e :data data}))
    (put! chan {:result k :event e})))

(defn send-request!
  [method url data chan]
  (let [method (str/upper-case (name method))
        _ (assert (http-methods method))
        xhrio (XhrIo.)]
    ;; (.setResponseType xhrio XhrIo.ResponseType.ARRAY_BUFFER)
    (events/listen xhrio EventType.SUCCESS (fn [e] (handler xhrio :success e chan)))
    (events/listen xhrio EventType.ERROR (fn [e] (handler xhrio :error e chan )))
    (events/listen xhrio EventType.COMPLETE (fn [e] (handler xhrio :complete e chan)))
    (events/listen xhrio EventType.ABORT (fn [e] (handler xhrio :abort e chan)))
    (events/listen xhrio EventType.TIMEOUT (fn [e] (handler xhrio :timeout e chan)))
    (.send xhrio url method data #js {"Content-type" "application/json"})))

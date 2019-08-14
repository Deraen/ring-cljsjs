(ns ring.middleware.cljsjs
  (:require [clojure.java.io :as io]
            [ring.middleware.head :as head]
            [ring.util.codec :as codec]
            [ring.util.request :as req]
            [ring.util.response :as resp])
  (:import [java.util.jar JarFile]))

(def ^:private cljsjs-pattern
  #"cljsjs/([^/]+)/([^/]+)/(.*)")

(defn- asset-path [prefix resource]
  (if-let [[_ name build-type path] (re-matches cljsjs-pattern resource)]
    (str prefix "/" name "/" path)))

(defn asset-map
  "Build map of request uri to classpath uri.

  Finds all resources in cljsjs classpath prefix and parses those paths to
  build the map. Build type part of the path is ignored when building the url.

  For example:
  cljsjs/react-mdl/development/material.css => cljsjs/react-mdl/material.css"
  ([] (asset-map "/cljsjs"))
  ([prefix]
   (->> (.getResources (.getContextClassLoader (Thread/currentThread)) "cljsjs")
        enumeration-seq
        (mapcat
          (fn [url]
            (if (= "jar" (.getProtocol url))
              (let [[_ jar] (re-find #"^file:(.*\.jar)\!/.*$" (.getPath url))]
                (->> (enumeration-seq (.entries (JarFile. (io/file jar))))
                     (remove #(.isDirectory %))
                     (map #(.getName %))
                     (filter #(.startsWith % "cljsjs")))))))
        set
        (keep (juxt (partial asset-path prefix) identity))
        (into {}))))

(defn- request-path [request]
  (codec/url-decode (req/path-info request)))

(defn cljsjs-request
  "If request uri matches given prefix (default is /cljsjs) and a resource
  in classpath is found under cljsjs/ prefix, returns it in a response map.
  Otherwise returns nil.

  For example, request uri `cljsjs/react-mdl/material.css` will match resource
  `cljsjs/react-mdl/development/material.css`. As the build type part of the
  resource path is ignored."
  ([request]
   (cljsjs-request request {}))
  ([request {:keys [prefix assets]
             :or {prefix "/cljsjs"}}]
   (let [assets (or assets (asset-map prefix))]
     (if (#{:head :get} (:request-method request))
       (if-let [path (assets (request-path request))]
         (-> (resp/resource-response path)
             (head/head-response request)))))))

(defn wrap-cljsjs
  "Middleware that first checks to see whether the request map matches
  a cljsjs resource. If request uri matches given prefix (default is /cljsjs) and a resource
  in classpath is found under cljsjs/ prefix, returns it in a response map.
  Otherwise the request map is passed onto the handler.

  For example, request uri `cljsjs/react-mdl/material.css` will match resource
  `cljsjs/react-mdl/development/material.css`. As the build type part of the
  resource path is ignored."
  ([handler]
   (wrap-cljsjs handler nil))
  ([handler {:keys [prefix]
             :or {prefix "/cljsjs"}}]
   (let [assets (asset-map prefix)]
     (fn
       ([request]
        (or (cljsjs-request request {:assets assets})
            (handler request)))
       ([request respond raise]
        (if-let [response (cljsjs-request request {:assets assets})]
          (respond response)
          (handler request respond raise)))))))

(ns ring.middleware.cljsjs-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [ring.middleware.cljsjs :refer :all]
            [ring.mock.request :as mock]))

(defn- slurp-response [handler uri]
  (some-> (mock/request :get uri) handler :body slurp))

(defn- slurp-cljsjs [path]
  (slurp (io/resource (str "cljsjs/" path))))

(deftest asset-map-test
  (is (= {nil "cljsjs/react-mdl/react-mdl-extra.ext.js"
          "/cljsjs/react-mdl/material.min.css.map" "cljsjs/react-mdl/production/material.min.css.map"
          "/cljsjs/react-mdl/ReactMDL.min.inc.js" "cljsjs/react-mdl/production/ReactMDL.min.inc.js"
          "/cljsjs/react-mdl/react-mdl.ext.js" "cljsjs/react-mdl/common/react-mdl.ext.js"
          "/cljsjs/react-mdl/material.min.inc.js.map" "cljsjs/react-mdl/production/material.min.inc.js.map"
          "/cljsjs/react-mdl/ReactMDL.min.inc.js.map" "cljsjs/react-mdl/production/ReactMDL.min.inc.js.map"
          "/cljsjs/react-mdl/ReactMDL.inc.js" "cljsjs/react-mdl/development/ReactMDL.inc.js"
          "/cljsjs/react-mdl/material.min.css" "cljsjs/react-mdl/production/material.min.css"
          "/cljsjs/react-mdl/material.inc.js" "cljsjs/react-mdl/development/material.inc.js"
          "/cljsjs/react-mdl/material.css" "cljsjs/react-mdl/development/material.css"
          "/cljsjs/react-mdl/material.min.inc.js" "cljsjs/react-mdl/production/material.min.inc.js"
          "/cljsjs/react-mdl/ReactMDL.inc.js.map" "cljsjs/react-mdl/development/ReactMDL.inc.js.map"}
         (asset-map))))

(deftest cljsjs-request-test
  (is (= nil (cljsjs-request {:request-method :post :uri "/foo"})))
  (is (= nil (cljsjs-request {:request-method :get :uri "/foo"})))
  (is (= nil (cljsjs-request {:request-method :post :uri "/cljsjs/react-mdl/material.css"})))
  (is (= {:status 200
          :headers {"Content-Length" "357514", "Last-Modified" "Wed, 14 Aug 2019 10:30:54 GMT"}}
         (dissoc (cljsjs-request {:request-method :get :uri "/cljsjs/react-mdl/material.css"}) :body)))
  (is (instance? java.io.InputStream (:body (cljsjs-request {:request-method :get :uri "/cljsjs/react-mdl/material.css"}) :body)))
  (is (= {:status 200
          :headers {"Content-Length" "357514", "Last-Modified" "Wed, 14 Aug 2019 10:30:54 GMT"}
          :body nil}
         (cljsjs-request {:request-method :head :uri "/cljsjs/react-mdl/material.css"}))))

(deftest test-wrap-cljsjs
  (let [handler (wrap-cljsjs (constantly nil))]
    (is (nil? (handler (mock/request :get "/foo"))))
    (is (nil? (handler (mock/request :get "/cljsjs"))))
    (is (nil? (handler (mock/request :get "/cljsjs/bootstrap"))))
    (is (= (slurp-response handler "/cljsjs/react-mdl/material.min.css")
           (slurp-cljsjs "react-mdl/production/material.min.css")))))

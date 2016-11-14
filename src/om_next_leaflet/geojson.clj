(ns om-next-leaflet.geojson
  (:require [clojure.data.json :as json]))

(def ^{:private true}
  json-root-dir
  "GeoJSON の配置しているディレクトリ"
  "src/om_next_leaflet/")

(def ^{:private true}
  earth-r
  "地球の半径(km)"
  6378.137)

(defn- calc-bounding-box
  "[[lon1 lat1][lon2 lat2] ...] の形式の「最大領域」となる矩形領域を算出する。"
  [coll]
  (reduce
   (fn [[min-lng min-lat max-lng max-lat] [lng lat]]
     [(min min-lng lng) (min min-lat lat)
      (max max-lng lng) (max max-lat lat)])
   [999 999 0 0] coll))

(defn- json->station
  "GeoJson から読み取った元データの駅の情報を内部形式に変換する。"
  [station-info]
  (if (and (= (:type station-info) "Feature")
           (= (get-in station-info [:geometry :type]) "Point"))
    (let [getprop-fn #(get-in station-info [:properties %])]
      (assoc {}
        :id (:id station-info)
        :station-name (getprop-fn :N05_011) ;; (get-in station-info [:properties :N05_011])
        :line-name (getprop-fn :N05_002)    ;; (get-in station-info [:properties :N05_002])
        :geometry (get-in station-info [:geometry :coordinates])))
    nil))

(defn- json->line
  "GeoJson から読み取った元データの路線の情報を内部形式に変換する。"
  [line-info]
  (if (and (= (:type line-info) "Feature")
           (= (get-in line-info [:geometry :type]) "LineString"))
    (let [geometry (get-in line-info [:geometry :coordinates])]
      (assoc {}
        :id (:id line-info)
        :line-name (get-in line-info [:properties :N05_002])
        :geometry geometry
        :bounding-box (calc-bounding-box geometry)))
    nil))

(defn- read-all-data
  ""
  ([fname transform-fn]
     (read-all-data json-root-dir fname transform-fn))
  ([base-dir fname transform-fn]
     (let [fullpath (str base-dir fname)
           json-data (-> fullpath slurp (json/read-str :key-fn keyword))]
       (->> json-data
            :features
            (map transform-fn)))))

(def ^{:private true}
  lines
  "全路線を読み取った Collection (delayed)。"
  (delay (read-all-data "JRW-railroad.geojson" json->line)))

(def ^{:private true}
  stations
  "全駅を読み取った Collection (delayed)。"
  (delay (read-all-data "JRW-stations.geojson" json->station)))

(defn get-lines
  "条件に適合する路線情報を返す。"
  ([] (get-lines identity))
  ([filter-fn]
     (filter filter-fn @lines)))

(defn get-stations
  "条件に適合する駅を返す。"
  ([] (get-stations identity))
  ([filter-fn]
     (filter filter-fn @stations)))

(defn distance
  "２つの点の間の距離(km)を求める。
see http://www.kiteretsu-so.com/archives/1183 "
  ([lng1 lat1 lng2 lat2]
     (distance [lng1 lat1] [lng2 lat2]))
  ([[lng1 lat1] [lng2 lat2]]
     (let [[lng1 lng2] (if (< lng1 lng2) [lng1 lng2] [lng2 lng1])
           [lat1 lat2] (if (< lat1 lat2) [lat1 lat2] [lat2 lat1])
           lat-rad (-> (- lat2 lat1) Math/toRadians)
           lng-rad (-> (- lng2 lng1) Math/toRadians)
           y-diff (* earth-r lat-rad)
           x-diff (* (Math/cos (Math/toRadians lat1)) earth-r lng-rad)]
       (Math/sqrt (+ (* x-diff x-diff) (* y-diff y-diff))))))

(defn find-mid-point
  "２つの点を結ぶ直線の上の、始点から特定の距離にある位置を求める"
  ([lng1 lat1 lng2 lat2 mid]
     (find-mid-point [lng1 lat1] [lng2 lat2] mid))
  ([[lng1 lat1] [lng2 lat2] mid]
     (let [d (distance lng1 lat1 lng2 lat2)]
       (if (> mid d)
         nil
         (let [r (/ mid d)
               lng-n (+ lng1 (* r (- lng2 lng1)))
               lat-n (+ lat1 (* r (- lat2 lat1)))]
           [lng-n lat-n])))))

(defn- nearby?
  "２点が5m以内にあれば true、そうでなければ false を返す"
  [p1 p2]
  (< (distance p1 p2) 0.005))

(defn find-station-index
  "線における駅の index を返す。線における駅が見つからなければ nil を返す。"
  [station line]
  (when-let [st-pos (:geometry station)]
    (when-let [pos (->> line
                        :geometry
                        (map-indexed (fn [i v] [i v]))
                        (filter #(nearby? st-pos (second %))))]
      (if (= (count pos) 1)
        (ffirst pos)
        (ffirst pos))))) ;; TODO: 5m以内に複数あれば、最も距離の短いものを正解とすべき

;; (def s-h (->> @stations (filter #(= (:line-name %) "北陸線"))))
;;
;; (->> s-h (map #(find-station-index % (first @lines))) sort)
;; => (0 20 47 84 135 169 211 260 301 337 365 439 554 584 628 663 685 725 794 850 939 977 1008 1080 1097 1119 1161 1220 1287 1357 1400 1445 1484 1527 1560 1578 1597 1618 1645 1683 1706 1729 1801 1831 1861 1888 1958 2030 2088 2115 2186 2225 2245 2290 2329 2408 2464 2516 2537 2573 2639 2677 2728 2764 2806 2857 2919 3083 3135 3218 3246 3295 3332 3388 3428 3461 3526 3601)
;;
;; (->> s-h (map #(find-station-index % (first @lines))) sort (partition 2 1))
;; => ((0 20) (20 47) (47 84) (84 135) (135 169) (169 211) (211 260) (260 301) (301 337) (337 365) (365 439) (439 554) (554 584) (584 628) (628 663) (663 685) (685 725) (725 794) (794 850) (850 939) (939 977) (977 1008) (1008 1080) (1080 1097) (1097 1119) (1119 1161) (1161 1220) (1220 1287) (1287 1357) (1357 1400) (1400 1445) (1445 1484) (1484 1527) (1527 1560) (1560 1578) (1578 1597) (1597 1618) (1618 1645) (1645 1683) (1683 1706) (1706 1729) (1729 1801) (1801 1831) (1831 1861) (1861 1888) (1888 1958) (1958 2030) (2030 2088) (2088 2115) (2115 2186) (2186 2225) (2225 2245) (2245 2290) (2290 2329) (2329 2408) (2408 2464) (2464 2516) (2516 2537) (2537 2573) (2573 2639) (2639 2677) (2677 2728) (2728 2764) (2764 2806) (2806 2857) (2857 2919) (2919 3083) (3083 3135) (3135 3218) (3218 3246) (3246 3295) (3295 3332) (3332 3388) (3388 3428) (3428 3461) (3461 3526) (3526 3601))
;;
;; (subvec (:geometry (first @lines)) 0 20)
;; => [[136.289505 35.31387] [136.28993 35.315] [136.290575 35.316243] [136.290876 35.316909] [136.291508 35.319472] [136.29179 35.3209] [136.2918 35.32116] [136.29175 35.32217] [136.2915 35.32309] [136.29116 35.32434] [136.29085 35.32533] [136.29069 35.32586] [136.29015 35.32771] [136.28994 35.32842] [136.28984 35.32877] [136.28975 35.32907] [136.2895 35.32992] [136.28934 35.33044] [136.28863 35.3329] [136.28777 35.33581]]
;;
;; (count (subvec (:geometry (first @lines)) 0 20))
;; => 20

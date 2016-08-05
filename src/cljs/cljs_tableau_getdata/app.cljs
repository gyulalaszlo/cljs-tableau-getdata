(ns cljs-tableau-getdata.app
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent-modals.modals :as reagent-modals]))

(defonce viz (reagent/atom {:maxRows 100 :includeAllColumns false}))
(declare modal)

;; Tableau API
(def viz-url
  "https://public.tableau.com/views/SuperGetData/CustomerSales")

(def viz-options
  (js-obj
    "hideTabs" true
    "hideToolbar" true
    "height" "500px"
    "width" "500px"
    "onFirstInteractive" #(swap! viz assoc :ready true)))

(swap! viz assoc :ready false :vizobj
       (js/tableau.Viz. (.getElementById js/document "tableau-div") viz-url viz-options))

(defn get-data-and-show-modal!
  [f]
  (-> (:vizobj @viz)
      (.getWorkbook)
      (.getActiveSheet)
      (f (clj->js @viz))
      (.then (fn [data] 
               (swap! viz assoc :columns (.getColumns data) :data (.getData data))
               (reagent-modals/modal! [modal] {:size :lg}) ))))

;; UI  / Reagent Components
(defn modal-render []
  [:div {:style {:overflow-y :auto}}
   [reagent-modals/close-button]
   [:table#data-table.table.table-striped.table-bordered.nowrap
    {:cell-spacing "0" :width "100%"}

    [:thead
     (reduce (fn [res col] (conj res (vector :th (.getFieldName col)))) [:tr] (:columns @viz))]

    ; tbody
    (reduce (fn [res row] 
              (conj res 
                    (reduce 
                      (fn [tr col] (conj tr (vector :td (get col "formattedValue")))) 
                      [:tr] 
                      row))) 
            [:tbody] 
            (js->clj (:data @viz)))]])

(defn modal-did-mount [this]
  (.DataTable (js/$ (.getElementById js/document "data-table"))
              ))


(defn modal []
  (reagent/create-class {:reagent-render modal-render
                         :component-did-mount modal-did-mount}))

(defn underlying-button []
  [:div.btn.btn-primary 
   {:disabled (not (:ready @viz))
    :on-click (fn [] (get-data-and-show-modal! #(.getUnderlyingDataAsync %1 %2)))} 
   "Show underlying data"])

(defn summary-button []
  [:div.btn.btn-primary
   {:disabled (not (:ready @viz))
    :on-click (fn [] (get-data-and-show-modal! #(.getSummaryDataAsync %1 %2)))} 
   "Show summary data"])

(defn get-data-component []
  [:div.form-horizontal {:style {:margin "10px"}}
   [:p "Select customers to see their summary or underlying data in tabular format"]
   [:div.form-group
    [:label "Number of rows to show"] 
    [:div.col-xs-2
     [:input#num-rows.form-control 
      {:type "text" 
       :value (:maxRows @viz)  
       :on-change #(swap! viz assoc :maxRows (.-target.value %))} ]]]
   [:div.form-group
    [:label {:for "all-cols"} "Show all columns"]
    [:div.col-xs-2
     [:input#all-cols.toggle.form-control 
      {:type "checkbox"
       :checked (:includeAllColumns @viz)
       :on-change #(swap! viz update-in [:includeAllColumns] not) }]]]
   [underlying-button] [summary-button] 
   [reagent-modals/modal-window]])

(defn init []
  (reagent/render-component [get-data-component]
                            (.getElementById js/document "container")))

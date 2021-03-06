(ns character-sheet.endpoint.character-sheet
  (:require [integrant.core :as ig]
            [character-sheet.endpoint.common :as lc]
            [sweet-tooth.endpoint.utils :as eu]
            [sweet-tooth.endpoint.page :as epg]
            [sweet-tooth.endpoint.datomic :as ed]
            [compojure.core :refer :all]
            [character-sheet.db.query.character-sheet :as qcs]))

(defn decisions
  [component]
  (lc/initialize-decisions
    component
    {:list {:handle-ok (fn [ctx]
                         [(->> (qcs/character-sheets (lc/db ctx))
                               (epg/paginate (epg/page-params ctx)))])}
     :show {:handle-ok #(-> (qcs/character-sheet (lc/db %) (eu/ctx-id %))
                            lc/format-ent)}

     :update {:put! (fn [ctx]
                      (-> @(ed/update ctx)
                          (eu/->ctx :result)))
              :handle-ok #(-> (qcs/character-sheet (ed/db-after %) (eu/ctx-id %))
                              lc/format-ent)}

     :delete {:delete! (comp deref ed/delete)}
     
     :create {:post! #(-> @(ed/create (update-in % [:request :params] dissoc :page))
                          (eu/->ctx :result))
              :handle-created (fn [ctx]
                                [(->> (qcs/character-sheets (ed/db-after ctx))
                                      (epg/paginate (assoc (:page (eu/params ctx)) :page 1)))])}}))

(def endpoint (lc/endpoint "/api/v1/character-sheet" decisions))

(defmethod ig/init-key :character-sheet.endpoint/character-sheet [_ options]
  (endpoint options))

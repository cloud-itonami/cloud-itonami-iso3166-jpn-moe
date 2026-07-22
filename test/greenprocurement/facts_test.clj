(ns greenprocurement.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [greenprocurement.facts :as facts]))

(deftest green-purchasing-has-spec-basis
  (let [sb (facts/spec-basis :green-purchasing)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (= 22 (:designated-procurement-fields-count sb)))
    (is (= 287 (:designated-procurement-items-count sb)))
    (is (= "2023-02-24" (:basic-policy-revision-date sb)))))

(deftest eco-conscious-contracts-has-spec-basis
  (let [sb (facts/spec-basis :eco-conscious-contracts)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (true? (:environmental-documentation-is-prerequisite? sb)))
    (is (= "2023-02-24" (:basic-policy-revision-date sb)))))

(deftest unknown-track-has-no-spec-basis
  (is (nil? (facts/spec-basis :unknown-track)))
  (is (nil? (facts/spec-basis :zzz))))

(deftest required-evidence-satisfied
  (let [sb (facts/spec-basis :green-purchasing)
        all (:required-evidence sb)]
    (is (true? (facts/required-evidence-satisfied? :green-purchasing all)))
    (is (not (facts/required-evidence-satisfied? :green-purchasing (take 1 all))))
    (is (nil? (facts/required-evidence-satisfied? :unknown-track all)))))

(deftest coverage-is-honest
  (let [c (facts/coverage [:green-purchasing :eco-conscious-contracts :unknown-track])]
    (is (= 3 (:requested c)))
    (is (= 2 (:covered c)))
    (is (= ["unknown-track"] (:missing-tracks c)))))

(deftest conformity-confirmation-prerequisite-track-is-green-purchasing-only
  (is (true? (facts/conformity-confirmation-prerequisite-track? :green-purchasing)))
  (is (false? (facts/conformity-confirmation-prerequisite-track? :eco-conscious-contracts))))

(deftest environmental-documentation-prerequisite-track-is-eco-conscious-contracts-only
  (is (true? (facts/environmental-documentation-prerequisite-track? :eco-conscious-contracts)))
  (is (false? (facts/environmental-documentation-prerequisite-track? :green-purchasing))))

(deftest catalog-does-not-enumerate-item-list
  (testing "the catalog only carries aggregate counts, never an itemized list of the 287 designated items"
    (let [sb (facts/spec-basis :green-purchasing)]
      (is (not (contains? sb :designated-items)))
      (is (not (contains? sb :example-items))))))

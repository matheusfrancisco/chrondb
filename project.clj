(defproject chrondb "0.1.0-alpha"
  :description "Chronological Database storing based on database-shaped git (core) architecture"
  :url "https://github.com/avelino/chrondb"
  :license {:name "MIT"
            :url "https://github.com/avelino/chrondb/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.json "0.2.7"]
                 [clj-jgit "1.0.0-beta3"]
                 [org.eclipse.jgit/org.eclipse.jgit "5.5.1.201910021850-r"]
                 [org.eclipse.jgit/org.eclipse.jgit.ssh.apache "5.5.1.201910021850-r"]
                 [clucie "0.4.2"]
                 [environ "1.1.0"]]
  :plugins [[lein-codox "0.10.7"]
            [environ/environ.lein "0.3.1"]
            [lein-marginalia "0.9.1"]]
  :java-source-paths ["src/java"]
  :main ^:skip-aot chrondb.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/clojure "1.10.1"]]}}
  :repositories {"jgit-repository" "https://repo.eclipse.org/content/groups/releases/"})

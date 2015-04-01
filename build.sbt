name := "akka_http_test"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.5"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-kernel" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.6"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0-M5"

libraryDependencies += "com.typesafe.akka" %% "akka-http-experimental" % "1.0-M5"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M5"

libraryDependencies += "io.spray" %% "spray-json" % "1.3.0"

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.7"

libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.7"

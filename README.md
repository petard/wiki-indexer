wiki-indexer
============

Small wiki indexing/searching tool.

#### Runnable jar:
  `mvn install`

#### Indexing
  `java -cp wiki-indexer-[version]-jar-with-dependencies.jar app.IndexerTool --input < wiki xml dump > --output < index output folder>`

#### Searching
  `java -cp wiki-indexer-[version]-jar-with-dependencies.jar app.SearchTool -index  < index folder >`

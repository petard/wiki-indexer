wiki-indexer
============

Small wiki indexing/searching tool.

#### Runnable jar:
  `mvn install`

#### Indexing
  `jar wiki-indexer-[version]-jar-with-dependencies.jar app.IndexerTool --input < wiki xml dump > --output < index output folder>`

#### Searching
  `jar wiki-indexer-[version]-jar-with-dependencies.jar app.SearchTool -index  < index folder >`

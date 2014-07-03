#!/bin/bash

scriptDir="$(dirname $0)"
pushd ${scriptDir}


java -jar ../wiki-indexer-*.jar app.IndexerTool $@

popd

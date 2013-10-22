#!/bin/bash

cd bin
java -cp . snp.linker.LinkerV2 8888 ../keystores/linker-keystore.jks cits3231 ../keystores/linker-truststore.jks cits3231
cd ..

#!/bin/bash

cd bin
java -cp . snp.linker.Linker 8888 ../keystores/linker-keystore.jks cits3231 ../keystores/linker-truststore.jks cits3231
cd ..

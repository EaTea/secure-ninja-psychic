#!/bin/bash

cd bin
echo " 1 Fizz fb/fizz/Fizz.class" | java -cp . snp.swh.SWHV2 8002 ../keystores/fb-keystore.jks cits3231
cd ..

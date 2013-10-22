#!/bin/bash

cd bin
echo "1 Buzz goo/buzz/Buzz.class" | java -cp . snp.swh.SWHV2 8001 ../keystores/goog-keystore.jks cits3231 
cd ..

#/bin/sh

java -Xmx64m -Xbootclasspath/p:$XMLBEANS_PERFROOT/3rdparty/xerces/xerces-2_6_2/xml-apis.jar:$XMLBEANS_PERFROOT/3rdparty/xerces/xerces-2_6_2/xercesImpl.jar -classpath $XMLBEANS_PERFROOT/build:$XMLBEANS_PERFROOT/schema_build/castor-purchase-order.jar:$XMLBEANS_PERFROOT/schema_build/castor-primitives.jar:$XMLBEANS_PERFROOT/schema_build/castor-non-primitives.jar:$XMLBEANS_PERFROOT/3rdparty/castor/castor-0.9.5.4.jar -DPERF_ROOT=$XMLBEANS_PERFROOT org.apache.xmlbeans.test.performance.castor.$* 

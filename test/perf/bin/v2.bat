java -Xmx64m -classpath %XMLBEANS_PERFROOT%\build;%XMLBEANS_PERFROOT%\schema_build\v2-purchase-order.jar;%XMLBEANS_PERFROOT%\schema_build\v2-primitives.jar;%XMLBEANS_PERFROOT%\schema_build\v2-non-primitives.jar;%XMLBEANS_HOME%\build\lib\xbean.jar;%XMLBEANS_HOME%\build\lib\jsr173_api.jar;%XMLBEANS_HOME%\external\lib\piccolo_apache_dist_20040629_v2.jar -DPERF_ROOT=%XMLBEANS_PERFROOT% org.apache.xmlbeans.test.performance.v2.%* 

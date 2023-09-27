
ONC ML HAPI FHIR Sandbox

To build 

docker build -t oncml/org.onc.ml.transformation:latest .
 
 
docker run -p 8080:8080 -e hapi.fhir.default_encoding=xml oncml/onc-sml-sandbox 

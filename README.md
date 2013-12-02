IRMA web service
================

The IRMA web service contains web applications that can verify and issue credentials.

Voucher Store
-------------

To get this running, make sure that you copy WebContent/META-INF/context.xml.sample to WebContent/META-INF/context.xml and enter the required database details. A description of the required database schema can be found here in data/vouchers/vouchers.sql.

Dependencies
------------

Needs jar files in /WebContent/WEB-INF/lib. These can currently be obtained by running ant in the respective project and linking the libraries here. In due time we will release these seperately as well.

	credentials_api.jar
	idemix_library.jar
	idemix_terminal.jar
	scuba_lib.jar

Also, you are required to link the irma_configuration directory from credentials/irma_configuration into data/.

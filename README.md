IRMA web service
================

The IRMA web service contains web applications that can verify and issue credentials.

Voucher Store
-------------

To get this running, make sure that you copy src/main/webapp/META-INF/context.xml.sample to src/main/webapp/META-INF/context.xml and enter the required database details. A description of the required database schema can be found in data/vouchers/vouchers.sql.

IRMATube
--------

IRMATube relies on some trailers and accompanying configuration. These are not included in the repository because these files are big. See the instruction in the `data/README.md` file for how to setup these files.

### irma_configuration

Download or link the `irma_configuration` project to `data/`.

See the credentials/irma_configuration project for the specifics. Remember that you can check out a different branch to get a different set of credentials and corresponding keys. In particular, the demo branch contains keys for all the issuers as well, thus making it very easy to test and develop applications.

Building
--------

To build the war for this java web-application simply run

    gradle assemble

you can find the resulting `.war` in `build/libs`.

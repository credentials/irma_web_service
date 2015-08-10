IRMA web service
================

The IRMA web service contains web applications that can verify and issue credentials.

Voucher Store
-------------

When you deploy this application to a server, make sure you setup database access for it. The easiest way to do this is to copy src/main/webapp/META-INF/context.xml.sample to src/main/webapp/META-INF/context.xml and enter the required database details. A description of the required database schema can be found in data/vouchers/vouchers.sql.

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

Full local server
-----------------

You can also run a local version of the server. Cross origin policies currently block access to the global relay, so you will also need to run a local instance of the relay. This is also usefull if you do not have an internet connection so that the cardproxy application can reach your server. To do so follow the folling steps.

 1. Run `irma_web_relay` as is specified in its README (it will use a different port automatically)
 2. Find the IP address of your machine in the same network as the cardproxy application. Usually this is a local IP address, like 10.0.0.1
 3. Run `irma_web_service` and specify the local IP address:

        gradle -PlocalIP=<localIP> appRunWar

 4. Open the application in the browser by using the local IP, for example `http://<localIP>:8080/irma_web_serbice/fullDemo/irmaTube`

For demo's, remember that the first verification after starting the server is slower, as some configuration is dynamically parsed. Be aware that

 1. If you used a different setting before, you need to run

        gradle clean

    to make sure the files are correctly generated.
 2. Although gretty supports the much faster `runApp` command, we currently cannot use it due to this [bug](https://github.com/akhikhl/gretty/issues/206)

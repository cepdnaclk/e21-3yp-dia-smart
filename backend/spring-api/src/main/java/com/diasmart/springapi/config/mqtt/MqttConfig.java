package com.diasmart.springapi.config.mqtt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLSocketFactory;

import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;

import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Configuration
public class MqttConfig {

    @Bean
    public MqttConnectOptions mqttConnectOptions() {

        try {

            MqttConnectOptions options =
                    new MqttConnectOptions();

            options.setAutomaticReconnect(true);

            options.setCleanSession(false);

            options.setSocketFactory(
                    getSocketFactory(
                            "certs/AmazonRootCA1.pem",
                            "certs/certificate.pem.crt",
                            "certs/private.pem.key"
                    )
            );

            return options;

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    private SSLSocketFactory getSocketFactory(
            final String caCrtFile,
            final String crtFile,
            final String keyFile
    ) throws Exception {

        Security.addProvider(
                new BouncyCastleProvider()
        );

        CertificateFactory cf =
                CertificateFactory.getInstance("X.509");

        X509Certificate caCert;

        try (FileInputStream fis =
                     new FileInputStream(caCrtFile)) {

            caCert = (X509Certificate)
                    cf.generateCertificate(fis);
        }

        X509Certificate cert;

        try (FileInputStream fis =
                     new FileInputStream(crtFile)) {

            cert = (X509Certificate)
                    cf.generateCertificate(fis);
        }

        PEMParser pemParser =
                new PEMParser(
                        new InputStreamReader(
                                new FileInputStream(keyFile)
                        )
                );

        PEMKeyPair pemKeyPair =
                (PEMKeyPair) pemParser.readObject();

        KeyPair keyPair =
                new JcaPEMKeyConverter()
                        .getKeyPair(pemKeyPair);

        pemParser.close();

        KeyStore keyStore =
                KeyStore.getInstance(
                        KeyStore.getDefaultType()
                );

        keyStore.load(null);

        keyStore.setCertificateEntry(
                "ca-certificate",
                caCert
        );

        keyStore.setCertificateEntry(
                "certificate",
                cert
        );

        keyStore.setKeyEntry(
                "private-key",
                keyPair.getPrivate(),
                "".toCharArray(),
                new Certificate[]{cert}
        );

        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm()
                );

        tmf.init(keyStore);

        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm()
                );

        kmf.init(keyStore, "".toCharArray());

        SSLContext context =
                SSLContext.getInstance("TLSv1.2");

        context.init(
                kmf.getKeyManagers(),
                tmf.getTrustManagers(),
                null
        );

        return context.getSocketFactory();
    }
}
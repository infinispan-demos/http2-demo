package org.infinispan.demos;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

public class HTTP2Demo {

   public static final String INFINISPAN_SERVER_ADDRESS = "https://172.17.0.2:8443";

   public static final String USERNAME = "test";
   public static final char[] PASSWORD = "test".toCharArray();

   public static void main(String[] args) throws Exception {
      HttpClient httpClient = HttpClient
            .newBuilder()
            .authenticator(userAndPasswordAuthenticator(USERNAME, PASSWORD))
            .sslContext(trustAllSSLContext())
            .version(HttpClient.Version.HTTP_2)
            .build();

      URI uri = new URI(INFINISPAN_SERVER_ADDRESS + "/rest/default/test");
      HttpRequest getFromCache = HttpRequest.newBuilder(uri)
            .GET()
            .build();

      HttpRequest putToCache = HttpRequest.newBuilder(uri)
            // See https://github.com/netty/netty/issues/7744
            .version(HttpClient.Version.HTTP_1_1)
            .POST(HttpRequest.BodyProcessor.fromString("test"))
            .build();

      httpClient
            .sendAsync(putToCache, HttpResponse.BodyHandler.discard(null))
            .thenCompose(ignoredResponse -> httpClient.sendAsync(getFromCache, HttpResponse.BodyHandler.asString()))
            .handle((response, throwable) -> {
               if (throwable != null) {
                  throwable.printStackTrace();
               } else {
                  System.out.println("RESPONSE: " + response);
                  System.out.println("STATUS: " + response.statusCode());
                  System.out.println("BODY: " + response.body());
                  System.out.println("HEADERS: " + response.headers().map());
               }
               return null;
            }).get(30, TimeUnit.SECONDS);
   }

   private static Authenticator userAndPasswordAuthenticator(String username, char[] password) throws UnknownHostException {
      return new Authenticator() {
         @Override
         public PasswordAuthentication requestPasswordAuthenticationInstance(String host, InetAddress addr, int port, String protocol, String prompt, String scheme, URL url, RequestorType reqType) {
            return getPasswordAuthentication();
         }

         @Override
         protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
         }

         @Override
         protected URL getRequestingURL() {
            return super.getRequestingURL();
         }

         @Override
         protected RequestorType getRequestorType() {
            return super.getRequestorType();
         }
      };
   }

   public static SSLContext trustAllSSLContext() throws GeneralSecurityException {
      class DefaultTrustManager implements X509TrustManager {

         @Override
         public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
         }

         @Override
         public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
         }

         @Override
         public X509Certificate[] getAcceptedIssuers() {
            return null;
         }
      }

      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
      return ctx;
   }
}

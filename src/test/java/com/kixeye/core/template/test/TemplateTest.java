package com.kixeye.core.template.test;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.kixeye.chassis.bootstrap.spring.ArchaiusSpringPropertySource;
import com.kixeye.chassis.scala.transport.ScalaTransportConfiguration;
import com.kixeye.chassis.support.ChassisConfiguration;
import com.kixeye.core.scala.template.TemplateConfiguration;
import com.kixeye.core.scala.template.service.dto.PingMessage;
import com.kixeye.core.scala.template.service.dto.PongMessage;
import com.kixeye.chassis.transport.TransportConfiguration;
import com.kixeye.chassis.transport.dto.Envelope;
import com.kixeye.chassis.transport.http.SerDeHttpMessageConverter;
import com.kixeye.chassis.transport.serde.MessageSerDe;
import com.kixeye.chassis.transport.serde.converter.JsonJacksonMessageSerDe;
import com.kixeye.chassis.transport.websocket.WebSocketMessageRegistry;
import com.netflix.config.ConfigurationManager;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.io.HexDump;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A template test for the services.
 *
 * @author ebahtijaragic
 */
public class TemplateTest {

    private static final Logger logger = LoggerFactory.getLogger(TemplateTest.class);

    private AnnotationConfigWebApplicationContext context;

    private int httpPort = -1;
    private int webSocketPort = -1;

    public static final ClientHttpRequestInterceptor LOGGING_INTERCEPTOR = new ClientHttpRequestInterceptor() {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            if (body.length > 0) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                HexDump.dump(body, 0, baos, 0);

                logger.info("Sending to [{}]: \n{}", request.getURI(), baos.toString(Charsets.UTF_8.name()).trim());
            } else {
                logger.info("Sending empty body to [{}]!", request.getURI());
            }

            return execution.execute(request, body);
        }
    };

    @Before
    public void setup() {
        httpPort = SocketUtils.findAvailableTcpPort();
        webSocketPort = SocketUtils.findAvailableTcpPort();

        // set properties for test
        AbstractConfiguration config = ConfigurationManager.getConfigInstance();

        config.setProperty("chassis.eureka.disable", "true");
        config.setProperty("eureka.datacenter", "local");

        config.setProperty("app.environment", "local");
        config.setProperty("app.name", TemplateTest.class.getSimpleName());
        config.setProperty("app.version", "" + System.currentTimeMillis());

        config.setProperty("http.enabled", "true");
        config.setProperty("http.hostname", "localhost");
        config.setProperty("http.port", "" + httpPort);
        config.setProperty("http.metrics.threadpool.enabled", "false");
        config.setProperty("http.metrics.handler.enabled", "false");

        config.setProperty("websocket.enabled", "true");
        config.setProperty("websocket.hostname", "localhost");
        config.setProperty("websocket.port", "" + webSocketPort);
        config.setProperty("websocket.metrics.threadpool.enabled", "false");
        config.setProperty("websocket.metrics.handler.enabled", "false");

        config.setProperty("metrics.graphite.enabled", "false");
        config.setProperty("metrics.aws.enabled", "false");
        config.setProperty("metrics.aws.filter", "");
        config.setProperty("metrics.aws.publish-interval", "" + 1);
        config.setProperty("metrics.aws.publish-interval-unit", "");
        config.setProperty("metrics.aws.region", "default");
        config.setProperty("aws.accessId", "");
        config.setProperty("aws.secretKey", "");
        config.setProperty("aws.instance.id", "");

        config.setProperty("scala.template.test.string", "unit test string!");

        // shove properties into Spring Environment
        context = new AnnotationConfigWebApplicationContext();
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst( new ArchaiusSpringPropertySource("archaius") );
        context.setEnvironment(environment);

        // start spring
        context.register(PropertySourcesPlaceholderConfigurer.class);
        context.register(TemplateConfiguration.class);
        context.register(ScalaTransportConfiguration.class);
        context.register(ChassisConfiguration.class);
        context.refresh();
    }

    @After
    public void tearDown() {
        context.close();
    }

	@Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testHttpPing() throws Exception {
        final String randomString = RandomStringUtils.randomAlphanumeric(16);
        MessageSerDe ser = context.getBean(JsonJacksonMessageSerDe.class);
        //MessageSerDe ser = context.getBean(MsgPackMessageSerDe.class);
        //MessageSerDe ser = context.getBean(AvroMessageSerDe.class);

        RestTemplate client = new RestTemplate();
        client.setInterceptors( new ArrayList( Lists.newArrayList(LOGGING_INTERCEPTOR)) );
        client.setMessageConverters( new ArrayList( Lists.newArrayList( new SerDeHttpMessageConverter(ser))) );

        PingMessage pingMessage = new PingMessage(randomString);
        //logger.info(StringUtils.join(pingMessage.getClass().getFields(), ","));

        PongMessage response = client.postForObject(new URI("http://localhost:" + httpPort + "/ping"), pingMessage, PongMessage.class);

        Assert.assertNotNull(response);
        Assert.assertEquals(randomString, response.message());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testHttpException() throws Exception {
        MessageSerDe ser = context.getBean(JsonJacksonMessageSerDe.class);

        RestTemplate client = new RestTemplate();
        client.setInterceptors( new ArrayList( Lists.newArrayList(LOGGING_INTERCEPTOR)) );
        client.setMessageConverters( new ArrayList( Lists.newArrayList( new SerDeHttpMessageConverter(ser))) );

        PongMessage response = null;
        try {
            response = client.getForObject(new URI("http://localhost:" + httpPort + "/exception"), PongMessage.class);
        } catch (HttpServerErrorException ex) {
            Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode() );
        }

        Assert.assertNull(response);
    }

    @Test
    public void testWebSocketPing() throws Exception {
        final String randomString = RandomStringUtils.randomAlphanumeric(16);

        WebSocketClient wsClient = new WebSocketClient();

        try {
            wsClient.start();

            final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);

            final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();

            WebSocketListener webSocket = new WebSocketListener() {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                public void onWebSocketBinary(byte[] payload, int offset, int length) {
                    try {
                        Schema<Envelope> envelopeSchema = RuntimeSchema.getSchema(Envelope.class);

                        final Envelope envelope = envelopeSchema.newMessage();

                        ProtobufIOUtil.mergeFrom(payload, offset, length, envelope, envelopeSchema);

                        Object message = null;

                        if (StringUtils.isNotBlank(envelope.typeId)) {
                            Class<?> messageClass = messageRegistry.getClassByTypeId(envelope.typeId);

                            if (messageClass == null) {
                                throw new RuntimeException("Unknown type id: " + envelope.typeId);
                            }

                            Schema messageSchema = RuntimeSchema.getSchema(messageClass);

                            message = messageSchema.newMessage();

                            ProtobufIOUtil.mergeFrom(envelope.payload.array(), message, messageSchema);
                        }

                        queue.offer(message);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                public void onWebSocketClose(int statusCode, String reason) { }

                public void onWebSocketConnect(Session session) { }

                public void onWebSocketError(Throwable cause) { }

                public void onWebSocketText(String message) { }
            };


            Schema<Envelope> envelopeSchema = RuntimeSchema.getSchema(Envelope.class);
            Schema<PingMessage> pingSchema = RuntimeSchema.getSchema(PingMessage.class);

            Session session = wsClient.connect(webSocket, new URI("ws://localhost:" +  webSocketPort + "/protobuf")).get(5000, TimeUnit.MILLISECONDS);

            LinkedBuffer linkedBuffer = LinkedBuffer.allocate(256);
            PingMessage ping = new PingMessage(randomString);
            Envelope envelope = new Envelope("ping", "ping", null, ByteBuffer.wrap(ProtobufIOUtil.toByteArray(ping, pingSchema, linkedBuffer)));
            linkedBuffer.clear();
            byte[] rawEnvelope = ProtobufIOUtil.toByteArray(envelope, envelopeSchema, linkedBuffer);
            linkedBuffer.clear();

            session.getRemote().sendBytes(ByteBuffer.wrap(rawEnvelope));

            PongMessage response = (PongMessage)queue.poll(10, TimeUnit.SECONDS);

            Assert.assertNotNull(response);
            Assert.assertEquals(randomString, response.message());
        } finally {
            wsClient.stop();
        }
    }

    @Test
    public void testWebSocketException() throws Exception {

        WebSocketClient wsClient = new WebSocketClient();

        try {
            wsClient.start();

            final WebSocketMessageRegistry messageRegistry = context.getBean(WebSocketMessageRegistry.class);

            final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();

            WebSocketListener webSocket = new WebSocketListener() {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                public void onWebSocketBinary(byte[] payload, int offset, int length) {
                    try {
                        Schema<Envelope> envelopeSchema = RuntimeSchema.getSchema(Envelope.class);

                        final Envelope envelope = envelopeSchema.newMessage();

                        ProtobufIOUtil.mergeFrom(payload, offset, length, envelope, envelopeSchema);

                        Object message = null;

                        if (StringUtils.isNotBlank(envelope.typeId)) {
                            Class<?> messageClass = messageRegistry.getClassByTypeId(envelope.typeId);

                            if (messageClass == null) {
                                throw new RuntimeException("Unknown type id: " + envelope.typeId);
                            }

                            Schema messageSchema = RuntimeSchema.getSchema(messageClass);

                            message = messageSchema.newMessage();

                            ProtobufIOUtil.mergeFrom(envelope.payload.array(), message, messageSchema);
                        }

                        queue.offer(message);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                public void onWebSocketClose(int statusCode, String reason) { }

                public void onWebSocketConnect(Session session) { }

                public void onWebSocketError(Throwable cause) { }

                public void onWebSocketText(String message) { }
            };


            Schema<Envelope> envelopeSchema = RuntimeSchema.getSchema(Envelope.class);

            Session session = wsClient.connect(webSocket, new URI("ws://localhost:" +  webSocketPort + "/protobuf")).get(5000, TimeUnit.MILLISECONDS);

            LinkedBuffer linkedBuffer = LinkedBuffer.allocate(256);
            Envelope envelope = new Envelope("exception", null, null, null);
            linkedBuffer.clear();
            byte[] rawEnvelope = ProtobufIOUtil.toByteArray(envelope, envelopeSchema, linkedBuffer);
            linkedBuffer.clear();

            session.getRemote().sendBytes(ByteBuffer.wrap(rawEnvelope));

            com.kixeye.chassis.transport.dto.ServiceError response = (com.kixeye.chassis.transport.dto.ServiceError)queue.poll(10, TimeUnit.SECONDS);
            Assert.assertNotNull(response);

        } finally {
            wsClient.stop();
        }
    }
}

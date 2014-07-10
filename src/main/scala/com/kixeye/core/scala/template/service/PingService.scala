package com.kixeye.core.scala.template.service

import org.springframework.web.bind.annotation.{RequestBody, RestController, RequestMethod, RequestMapping}
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct
import scala.concurrent.Future
import scala.concurrent.duration._
import com.wordnik.swagger.annotations.ApiOperation
import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import com.kixeye.core.scala.template.service.dto.{ForceExceptionMessage, PongMessage, PingMessage}
import akka.util.Timeout
import com.codahale.metrics.MetricRegistry
import com.kixeye.chassis.transport.websocket.{ActionPayload, ActionMapping, WebSocketMessageRegistry, WebSocketController}

@RestController
@WebSocketController
class PingService @Autowired()(messageRegistry: WebSocketMessageRegistry, metricRegistry: MetricRegistry) {

  val system = ActorSystem("ping-service-system")
  val pingActor = system.actorOf(Props[PingActor], "ping-actor")

  // Create metric counter to count the number of ping requests
  val metricPingCounter = metricRegistry.counter("ping.counter")

  implicit val timeout = Timeout(2 second)

  @PostConstruct
  def initialize(): Unit = {
    // register messages type ids
    messageRegistry.registerType("ping", classOf[PingMessage])
    messageRegistry.registerType("pong", classOf[PongMessage])
  }

  @ApiOperation("Returns a Pong message with the Ping's message")
  @ActionMapping(Array("ping"))
  @RequestMapping(method = Array(RequestMethod.POST), value = Array("/ping"))
  def ping(@RequestBody @ActionPayload ping: PingMessage): Future[PongMessage] = {
    // Increment the ping counter metric
    metricPingCounter.inc()

    // send PingMessage to Akka actor and wait for response in a futurelo
    (pingActor ? ping).mapTo[PongMessage]
  }

  @ApiOperation("Causes an exception in the worker actor")
  @ActionMapping(Array("exception"))
  @RequestMapping(method = Array(RequestMethod.GET), value = Array("/exception"))
  def forcedException(): Future[PongMessage] = {
    (pingActor ? ForceExceptionMessage).mapTo[PongMessage]
  }
}

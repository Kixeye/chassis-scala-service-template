package com.kixeye.core.scala.template.service

import akka.actor._
import language.implicitConversions


class PingActor extends Actor  {
  import com.netflix.config.scala.DynamicProperties._
  import com.kixeye.core.scala.template.service.dto.PingMessage
  import com.kixeye.core.scala.template.service.dto.{ForceExceptionMessage, PongMessage}

  private val exampleProperty = dynamicStringProperty("scala.template.test.string", "default value")

  override def preRestart( reason: Throwable, message: Option[Any]) : Unit = {
    sender ! akka.actor.Status.Failure(reason)
    super.preRestart(reason, message)
  }

  def receive = {
      case m:PingMessage =>
        sender ! PongMessage(m.message,exampleProperty.get)
      case ForceExceptionMessage =>
        throw new Exception("You asked for it!")
  }
}

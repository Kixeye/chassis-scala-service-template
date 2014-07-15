package com.kixeye.core.scala.template.service.dto

import com.fasterxml.jackson.annotation.JsonProperty

case class PingMessage(@JsonProperty(value="message", required = true) message:String)
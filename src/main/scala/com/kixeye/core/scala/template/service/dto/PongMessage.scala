package com.kixeye.core.scala.template.service.dto

import com.fasterxml.jackson.annotation.JsonProperty

case class PongMessage(@JsonProperty(value="message", required = true) message:String,@JsonProperty(value="extraMessage", required = false) extraMessage:String)
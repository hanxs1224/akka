/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.persistence.typed.internal

import java.util.concurrent.TimeUnit

import akka.actor.typed.ActorSystem
import akka.annotation.InternalApi
import akka.persistence.Persistence
import com.typesafe.config.Config

import scala.concurrent.duration._

/**
 * INTERNAL API
 */
@InternalApi private[akka] object EventsourcedSettings {

  def apply(system: ActorSystem[_], journalPluginId: String, snapshotPluginId: String): EventsourcedSettings =
    apply(system.settings.config, journalPluginId, snapshotPluginId)

  def apply(config: Config, journalPluginId: String, snapshotPluginId: String): EventsourcedSettings = {
    val typedConfig = config.getConfig("akka.persistence.typed")

    // StashOverflowStrategy
    val stashOverflowStrategyConfigurator = typedConfig.getString("internal-stash-overflow-strategy")

    val stashCapacity = typedConfig.getInt("stash-capacity")
    require(stashCapacity > 0, "stash-capacity MUST be > 0, unbounded buffering is not supported.")

    val logOnStashing = typedConfig.getBoolean("log-stashing")

    val journalConfig = journalConfigFor(config, journalPluginId)
    val recoveryEventTimeout: FiniteDuration =
      journalConfig.getDuration("recovery-event-timeout", TimeUnit.MILLISECONDS).millis

    EventsourcedSettings(
      stashCapacity = stashCapacity,
      stashOverflowStrategyConfigurator,
      logOnStashing = logOnStashing,
      recoveryEventTimeout,
      journalPluginId,
      snapshotPluginId
    )
  }

  /**
   * INTERNAL API
   */
  private[akka] final def journalConfigFor(config: Config, journalPluginId: String): Config = {
    val defaultJournalPluginId = config.getString("akka.persistence.journal.plugin")
    val configPath = if (journalPluginId == "") defaultJournalPluginId else journalPluginId
    config.getConfig(configPath)
      .withFallback(config.getConfig(Persistence.JournalFallbackConfigPath))
  }

}

@InternalApi
private[akka] final case class EventsourcedSettings(
  stashCapacity:                     Int,
  stashOverflowStrategyConfigurator: String,
  logOnStashing:                     Boolean,
  recoveryEventTimeout:              FiniteDuration,
  journalPluginId:                   String,
  snapshotPluginId:                  String) {

  require(journalPluginId != null, "journal plugin id must not be null; use empty string for 'default' journal")
  require(snapshotPluginId != null, "snapshot plugin id must not be null; use empty string for 'default' snapshot store")

}


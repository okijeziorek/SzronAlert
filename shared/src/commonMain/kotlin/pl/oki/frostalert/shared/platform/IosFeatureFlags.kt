package pl.oki.frostalert.shared.platform

enum class IosFeatureFlag {
    MAIN_RISK_SCREEN,
    HISTORY,
    SETTINGS,
    NOTIFICATIONS,
    LOCATION,
    SUBSCRIPTIONS,
    WIDGETS,
    GEOFENCING,
    SMART_HOME,
    DEBUG_TOOLS,
    EXTENDED_PRO
}

object IosFeatureMatrix {
    val mvpEnabled: Set<IosFeatureFlag> = setOf(
        IosFeatureFlag.MAIN_RISK_SCREEN,
        IosFeatureFlag.HISTORY,
        IosFeatureFlag.SETTINGS,
        IosFeatureFlag.NOTIFICATIONS,
        IosFeatureFlag.LOCATION,
        IosFeatureFlag.SUBSCRIPTIONS
    )

    val postMvpDeferred: Set<IosFeatureFlag> = setOf(
        IosFeatureFlag.WIDGETS,
        IosFeatureFlag.GEOFENCING,
        IosFeatureFlag.SMART_HOME,
        IosFeatureFlag.DEBUG_TOOLS,
        IosFeatureFlag.EXTENDED_PRO
    )
}

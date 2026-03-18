package pl.oki.frostalert.geofence

import pl.oki.frostalert.data.repository.LocationRepository

/**
 * Kontrakt dla rejestratora geofence. Implementacja powinna być reaktywna
 * i reagować na zmiany ustawień oraz lokalizacji (Flows).
 */
interface GeofenceRegistrarContract {
    /**
     * Wymuś rejestrację geofence dla aktualnej lokalizacji (jednorazowe).
     */
    fun registerForCurrentLocation(locationRepo: LocationRepository)

    /**
     * Wymuś wyrejestrowanie (usuń geofence jeśli istnieje).
     */
    fun unregister()

    /**
     * Uruchamia automatyczne nasłuchiwanie zmian w ustawieniach i lokalizacji.
     * Po uruchomieniu rejestrator będzie (de)rejestrował geofence automatycznie.
     */
    fun start()

    /**
     * Zatrzymuje automatyczne nasłuchiwanie. Po stop() można opcjonalnie
     * wyrejestrować istniejące geofence (implementacja decyduje).
     */
    fun stop()

    /**
     * Informacja czy rejestrator jest aktualnie uruchomiony i nasłuchuje zmian.
     */
    fun isRunning(): Boolean
}


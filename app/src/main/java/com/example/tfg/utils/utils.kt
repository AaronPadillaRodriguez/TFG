package com.example.tfg.utils


suspend fun <T> fetchWithLanguageFallback(primaryLanguage: String = "es-ES",
                                          fallbackLanguage: String = "en-US",
                                          fetchFunction: suspend (String) -> T,
                                          validateResponse: (T) -> Boolean = { true } ): T {
    val primaryResponse = fetchFunction(primaryLanguage)
    if (validateResponse(primaryResponse)) {
        return primaryResponse
    }
    return fetchFunction(fallbackLanguage)
}


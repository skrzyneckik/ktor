/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests.mock

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class MockEngineTests {
    @Test
    fun testClientMock() = runBlocking {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    if (request.url.encodedPath == "/") {
                        respond(
                            byteArrayOf(1, 2, 3),
                            headers = headersOf("X-MyHeader", "My Value")
                        )
                    } else {
                        respondError(HttpStatusCode.NotFound, "Not Found ${request.url.encodedPath}")
                    }
                }
            }
            expectSuccess = false
        }

        assertEquals(byteArrayOf(1, 2, 3).toList(), client.get("/").body<ByteArray>().toList())
        assertEquals("My Value", client.request("/").headers["X-MyHeader"])
        assertEquals("Not Found /other/path", client.get("/other/path").body())
    }

    @Test
    fun testBasic() = testBlocking {
        val client = HttpClient(
            MockEngine { request ->
                if (request.url.toString().endsWith("/fail")) {
                    respondBadRequest()
                } else {
                    respondOk("${request.url}")
                }
            }
        ) {
            expectSuccess = false
        }

        client.prepareRequest {
            url("http://127.0.0.1/normal-request")
        }.execute { response ->
            assertEquals("http://127.0.0.1/normal-request", response.bodyAsText())
            assertEquals(HttpStatusCode.OK, response.status)
        }

        client.prepareRequest {
            url("http://127.0.0.1/fail")
        }.execute { response ->
            assertEquals("Bad Request", response.bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Serializable
    data class User(val name: String)

    @Test
    fun testWithContentNegotationPlugin() = runBlocking {
        val client = HttpClient(
            MockEngine { request ->
                val bodyBytes = (request.body as OutgoingContent.ByteArrayContent).bytes()
                respondOk(String(bodyBytes))
            }
        ) {
            install(ContentNegotiation) { json() }
        }

        val response = client.get {
            setBody(User("admin"))
            contentType(ContentType.Application.Json)
        }.body<String>()

        assertEquals("{\"name\":\"admin\"}", response)
    }

    @Serializable
    data class ResponseJsonData(val status: Int, val message: String)

    @Test
    fun testValidationWithContentNegotiationPlugin() = runBlocking {
        val client = HttpClient(
            MockEngine { request ->
                val bodyBytes = (request.body as OutgoingContent.ByteArrayContent).bytes()
                respondOk(String(bodyBytes))
            }
        ) {
            install(ContentNegotiation) { json() }
            HttpResponseValidator {
                validateResponse {
                    val body: String = it.body()
                    val response = Json.decodeFromString<ResponseJsonData>(body)
                    if (response.status != 200) {
                        throw ClientRequestException(it, response.message)
                    }
                }
            }
        }

        val response: String = client.get {
            setBody(ResponseJsonData(200, "OK"))
            contentType(ContentType.Application.Json)
        }.body()

        assertEquals("{\"status\":200,\"message\":\"OK\"}", response)
        assertEquals(ResponseJsonData(200,"OK"), Json.decodeFromString<ResponseJsonData>(response))
    }

    private fun testBlocking(callback: suspend () -> Unit): Unit = run { runBlocking { callback() } }
}

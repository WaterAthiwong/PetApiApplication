package com.champaca.inventorydata.odoo

import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.databasetable.OdooError
import com.champaca.inventorydata.odoo.model.out.CreatePurchaseOrderRequest
import com.champaca.inventorydata.odoo.model.out.OdooResponse
import com.champaca.inventorydata.odoo.model.out.SalesOrderSendDeliveryRequest
import com.champaca.inventorydata.odoo.model.out.CreateStockPickingRequest
import com.champaca.inventorydata.wms.responsemodel.ErrorResponse
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime

@Service
class OdooService {

    companion object {
        const val API_KEY = "API-KEY"
    }

    @Value("\${odoo.url}")
    lateinit var url: String

    @Value("\${odoo.authentication.key}")
    lateinit var authenticationKey: String

    val webClient: WebClient by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val strategies = ExchangeStrategies
            .builder()
            .codecs { clientDefaultCodecsConfigurer ->
                run {
                    clientDefaultCodecsConfigurer.defaultCodecs()
                        .kotlinSerializationJsonDecoder(KotlinSerializationJsonDecoder(json))
                    clientDefaultCodecsConfigurer.defaultCodecs()
                        .kotlinSerializationJsonEncoder(KotlinSerializationJsonEncoder(json))

                }
            }.build()


        WebClient
            .builder()
            .exchangeStrategies(strategies)
            .baseUrl(url)
            .build()
    }

    fun createPurchaseOrder(body: CreatePurchaseOrderRequest): ResultOf<OdooResponse> {
        val response = webClient.post()
            .uri("/purchase_order/purchase_order/create")
            .contentType(MediaType.APPLICATION_JSON)
            .header(API_KEY, authenticationKey)
            .bodyValue(body)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!
        return renderResponse(response)
    }

    fun sendDelivery(body: SalesOrderSendDeliveryRequest): ResultOf<OdooResponse> {
        val response = webClient.post()
            .uri("/sale/send delivery/send-delivery")
            .contentType(MediaType.APPLICATION_JSON)
            .header(API_KEY, authenticationKey)
            .bodyValue(body)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!
        return renderResponse(response)
    }

    fun pickStock(body: CreateStockPickingRequest): ResultOf<OdooResponse> {
        val response = webClient.post()
            .uri("/stock/picking/create")
            .contentType(MediaType.APPLICATION_JSON)
            .header(API_KEY, authenticationKey)
            .bodyValue(body)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!
        return renderResponse(response)
    }

    private fun renderResponse(response: ResponseEntity<String>): ResultOf<OdooResponse> {
        return if (response.statusCode.is2xxSuccessful) {
            val result = Json.decodeFromString(OdooResponse.serializer(), response.body!!)
            ResultOf.Success(result)
        } else {
            val errorResponse = Json.decodeFromString(ErrorResponse.serializer(), response.body!!)
            ResultOf.Failure(message = errorResponse.consolidatedMessage())
        }
    }

    fun recordError(action: String, payload: String, error: String) {
        OdooError.insert {
            it[OdooError.action] = action
            it[OdooError.payload] = payload.take(1024)
            it[OdooError.error] = error.take(255)
            it[OdooError.createdAt] = LocalDateTime.now()
        }
    }
}
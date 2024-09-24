package com.champaca.inventorydata.wms

import com.champaca.inventorydata.common.ResultOf
import com.champaca.inventorydata.goodmovement.model.GoodMovementData
import com.champaca.inventorydata.goodmovement.model.GoodMovementType
import com.champaca.inventorydata.model.SkuDetail
import com.champaca.inventorydata.wms.responsemodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class WmsService {
    companion object {
        const val WMS_SESSION = "Wms-Session"
        const val WMS_SESSION_LAST_ACTIVE = "Wms-Last-Active"
        const val API_REQUEST = "API-Request"
        const val RESTFUL = "restful"
        const val TOKEN = "token"
        const val PHP_SESSION = "PHPSESSID"
        val DATA_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val WMS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    @Value("\${wms.url}")
    lateinit var url: String

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
//            .clientConnector(ReactorClientHttpConnector(
//                    HttpClient.create().followRedirect(true)
//                    ))
            .exchangeStrategies(strategies)
            .baseUrl(url)
            .build()
    }

    fun login(token: String): Pair<String, UserAuthJsonResponse>? {
        val result = webClient.post()
            .uri("/account/user/auth.json")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(API_REQUEST, RESTFUL)
            .body(BodyInserters.fromFormData(TOKEN, token))
            .exchangeToMono { response ->
                val sessionId = response.cookies()[PHP_SESSION]?.first()?.value ?: ""
                response.bodyToMono(UserAuthJsonResponse::class.java)
                    .map { body -> Pair(sessionId, body) }
            }
            .block()
        return result
    }

    fun getGoodsMovement(sessionId: String,
                         type: GoodMovementType,
                         processTypeId: Int,
                         manufacturingLineId: Int,
                         page: Int = 1,
                         orderBy: String? = null,
                         fromProductionDate: String? = null,
                         toProductionDate: String? = null): GoodMovementIndexResponse {

        val formData = BodyInserters.fromFormData("type", type.wmsName)
                    .with("process_type_id", processTypeId.toString())
                    .with("manufacturing_line_id", manufacturingLineId.toString())
                    .with("page", page.toString())

        if (!orderBy.isNullOrEmpty()) {
            formData.with("order_by", orderBy)
        }
        if (!fromProductionDate.isNullOrEmpty()) {
            formData.with("from_production_date", reformatDate(fromProductionDate))
        }
        if (!toProductionDate.isNullOrEmpty()) {
            formData.with("to_production_date", reformatDate(toProductionDate))
        }

        return webClient.post()
            .uri("wfm/good.movement/index")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)
            .body(formData)
            .retrieve()
            .bodyToMono(GoodMovementIndexResponse::class.java)
            .block()!!
    }

    fun getManufacturingLine(sessionId: String, processId: Int? = null): ManufactuingLineIndexResponse {
        val response = webClient.post()
            .uri("wfm/manufacturing.line/index")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)

        if (processId != null) {
            response.body(BodyInserters.fromFormData("process_type_id", processId.toString()))
        }

        return response.retrieve()
            .bodyToMono(ManufactuingLineIndexResponse::class.java)
            .block()!!
    }

    fun receiveGmItem(sessionId: String, movements: List<ItemMovementEntry>): ResultOf<String> {
        synchronized(this) {
            // This is to make sure that only one thread can create a new LotNo at a time. Hence, avoiding the
            // LotNo.code conflict.
            // API List: https://inventory.champaca.com/wfm/api/list ==> these are special API for WMS.
            // Jew created these APIs to support the WMS integration in the case where the normal API doesn't work.
            val jsonElements = movements.map { it.toWmsJsonString() }.joinToString(",")

            val response = webClient.post()
                .uri("wfm/api/receive.gm.items.json")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromFormData("payload", "[$jsonElements]"))
                .retrieve()
                .toEntity(String::class.java)
                .block()!!

            return if (response.body!!.contains("\"success\"")) {
                ResultOf.Success("")
            } else {
                ResultOf.Failure(message = response.body!!)
            }
        }
    }

    fun pickGmItem(sessionId: String,
                   goodMovementType: GoodMovementType,
                   goodMovementId: Int,
                   skuId: Int,
                   sku: String? = null,
                   storeLocationId: Int,
                   storeLocation: String? = null,
                   manufacturingLineId: Int?,
                   lotNo: String? = null,
                   lotNoId: Int? = null,
                   qty: BigDecimal,
                   refCode: String? = null): ResultOf<String> {
        val movement = ItemMovementEntry(
            goodMovementType, goodMovementId, skuId, sku, storeLocationId, storeLocation, manufacturingLineId, lotNo, lotNoId, qty, refCode
        )
        return pickGmItem(sessionId, listOf(movement))
    }

    fun pickGmItem(sessionId: String, movements: List<ItemMovementEntry>): ResultOf<String> {
        val calls = movements.map {
            val formData = BodyInserters.fromFormData("good_movement_type", it.goodMovementType.wmsName)
                .with("good_movement_id", it.goodMovementId.toString())
                .with("sku_id", it.skuId.toString())
                .with("store_location_id", it.storeLocationId.toString())
                .with("qty", it.qty.toString())

            if (it.goodMovementType == GoodMovementType.GOODS_RECEIPT) {
                formData.with("sku", it.sku!!)
                    .with("store_location", it.storeLocation!!)
                    .with("manufacturing_line_id", it.manufacturingLineId.toString())

                if (!it.refCode.isNullOrEmpty()) {
                    formData.with("ref_code", it.refCode)
                }
                if (!it.remark.isNullOrEmpty()) {
                    formData.with("remark", it.remark)
                }
            }

            if (it.goodMovementType == GoodMovementType.PICKING_ORDER) {
                formData.with("lot_no_id", it.lotNoId!!.toString())
                    .with("lot_no", it.lotNo!!)

                if (it.manufacturingLineId != null) {
                    formData.with("manufacturing_line_id", it.manufacturingLineId.toString())
                }
                if (!it.remark.isNullOrEmpty()) {
                    formData.with("remark", it.remark)
                }
            }

            webClient.post()
                .uri("wfm/gm.item/add")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header(API_REQUEST, RESTFUL)
                .cookie(PHP_SESSION, sessionId)
                .body(formData)
                .exchangeToMono(::handleMonoResponse)
                .onErrorResume { e -> Mono.just("Error: ${e.message}") } // Handling individual Mono errors
        }

        val results = Flux.fromIterable(calls)
            .flatMap { it } // Merge the Monos into a single Flux stream
            .collectList()  // Collect the results into a list
            .onErrorReturn(listOf("Global Error occurred"))
            .block()!!
            .toList()
        val errorResults = results.filter { it.isNotEmpty() }
        return if (errorResults.isEmpty()) {
            ResultOf.Success("")
        } else {
            ResultOf.Failure(message = errorResults.joinToString(" ,"))
        }
    }

    fun removeGmItem(sessionId: String, gmItemIds: List<Int>): ResultOf<String> {
        val calls = gmItemIds.map { id ->
            webClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("wfm/gm.item/delete")
                        .queryParam("id", id.toString())
                    uriBuilder.build()
                }
                .header(API_REQUEST, RESTFUL)
                .cookie(PHP_SESSION, sessionId)
                .exchangeToMono { it.bodyToMono(String::class.java) }
                .onErrorResume { e -> Mono.just("Error: ${e.message}") } // Handling individual Mono errors
        }

        val results = Flux.fromIterable(calls)
            .flatMap { it } // Merge the Monos into a single Flux stream
            .collectList()  // Collect the results into a list
            .onErrorReturn(listOf("Global Error occurred"))
            .block()!!
            .toList()
        val errorResults = results.filter { !it.contains("\"success\"") }
        return if (errorResults.isEmpty()) {
            ResultOf.Success("")
        } else {
            ResultOf.Failure(message = errorResults.joinToString(" ,"))
        }
    }

    fun planGmItem(sessionId: String, goodMovementId: Int, matCode: String, skuId: Int, qty: Int): ResultOf<String> {
        val formData = BodyInserters.fromFormData("sku", matCode)
            .with("sku_id", skuId.toString())
            .with("qty", qty.toString())
            .with("good_movement_id", goodMovementId.toString())

        val response = webClient.post()
            .uri("wfm/gm.plan.item/add")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)
            .body(formData)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!

        return renderResponse(response)
    }

    fun handleMonoResponse(response: ClientResponse): Mono<String> {
        return if (response.statusCode().is3xxRedirection) {
            // If WMS execute the request successfully, it will redirect the page to the wfm/good.movement/index page
            // and send 3XX status code back. To cut time of doing redirection stuff we just catch the status code
            // and do no redirection.
            Mono.just("")
        } else {
            response.bodyToMono(String::class.java)
        }
    }

    fun createLotGroupNonBlocking(sessionId: String,
                       lotIds: List<String>,
                       goodMovementId: Int,
                       refCode: String? = null) {
        GlobalScope.launch(Dispatchers.IO) {
            webClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("wfm/gm.item/group.lot")
                        .queryParam("ids", lotIds.joinToString(","))
                        .queryParam("good_movement_id", goodMovementId.toString())
                        .queryParam("type", "merge")

                    if (!refCode.isNullOrEmpty()) {
                        uriBuilder.queryParam("ref_code", refCode)
                    }

                    uriBuilder.build()
                }
                .header(API_REQUEST, RESTFUL)
                .cookie(PHP_SESSION, sessionId)
                .retrieve()
                .bodyToMono(Void::class.java)
                .subscribe()
        }
    }

    fun relocation(sessionId: String, barcode: String): ResultOf<String> {
        val response = webClient.post()
            .uri("wfm/gm.item/relocation")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)
            .body(BodyInserters.fromFormData("barcode", barcode))
            .retrieve()
            .toEntity(String::class.java)
            .block()!!

        return renderResponse(response)
    }

    fun createSku(sessionId: String, sku: SkuDetail, skuGroupId: Int): ResultOf<String> {
        val formData = BodyInserters.fromFormData("code", sku.code)
            .with("mat_code", sku.matCode)
            .with("name", sku.skuName)
            .with("sku_group_id", skuGroupId.toString())
            .with("thickness", "%.5f".format(sku.thickness))
            .with("thickness_uom", sku.thicknessUom)
            .with("width", "%.5f".format(sku.width))
            .with("width_uom", sku.widthUom)
            .with("long", "%.5f".format(sku.length))
            .with("long_uom", sku.lengthUom)
            .with("circumference", "%.5f".format(sku.circumference))
            .with("circumference_uom", sku.circumferenceUom!!)
            .with("volumn_ft3", "%.5f".format(sku.volumnFt3))
            .with("volumn_m3", "%.5f".format(sku.volumnM3))
            .with("species", sku.species)
            .with("grade", sku.grade ?: "")
            .with("fsc", if(sku.fsc) "Y" else "N" )

        val response = webClient.post()
            .uri("wfm/sku/add")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)
            .body(formData)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!

        return renderResponse(response)
    }

    fun createGoodMovement(sessionId: String, data: GoodMovementData): ResultOf<String> {
        val formData = BodyInserters.fromFormData("type", data.type.wmsName)
            .with("production_date", WMS_FORMAT.format(LocalDate.parse(data.productionDate, DATA_FORMAT)))
            .with("department_id", data.departmentId.toString())
            .with("is_transfer", "0")

        if (data.processTypeId != null) {
            formData.with("process_type_id", data.processTypeId.toString())
                .with("for_process", "y")
        } else {
            formData.with("for_process", "n")
        }
        if (data.manufacturingLineId != null) {
            formData.with("manufacturing_line_id", data.manufacturingLineId.toString())
        }
        if (!data.orderNo.isNullOrEmpty()) {
            formData.with("order_no", data.orderNo)
        }
        if (!data.jobNo.isNullOrEmpty()) {
            formData.with("job_no", data.jobNo)
        }
        if (!data.poNo.isNullOrEmpty()) {
            formData.with("po_no", data.poNo)
        }
        if (!data.invoiceNo.isNullOrEmpty()) {
            formData.with("invoice_no", data.invoiceNo)
        }
        if (data.supplierId != null) {
            formData.with("supplier_id", data.supplierId.toString())
        }
        if (!data.lotNo.isNullOrEmpty()) {
            formData.with("lot_no", data.lotNo)
        }
        if (!data.remark.isNullOrEmpty()) {
            formData.with("remark", data.remark)
        }
        if (data.extraAttributes != null) {
            formData.with("extra_attributes", Json.encodeToString(data.extraAttributes))
        }
        if (!data.productType.isNullOrEmpty()) {
            formData.with("doc_product_type", data.productType)
        }

        val response = webClient.post()
            .uri("wfm/good.movement/add")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)
            .body(formData)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!

        return renderResponse(response)
    }

    fun editGoodMovement(sessionId: String, data: GoodMovementData): ResultOf<String> {
        val formData = BodyInserters.fromFormData("id", data.id.toString())
            .with("type", data.type.wmsName)
            .with("production_date", WMS_FORMAT.format(LocalDate.parse(data.productionDate, DATA_FORMAT)))
            .with("department_id", data.departmentId.toString())
            .with("is_transfer", "0")

        if (data.processTypeId != null) {
            formData.with("process_type_id", data.processTypeId.toString())
                .with("for_process", "y")
        } else {
            formData.with("for_process", "n")
        }
        if (data.manufacturingLineId != null) {
            formData.with("manufacturing_line_id", data.manufacturingLineId.toString())
        }
        if (!data.orderNo.isNullOrEmpty()) {
            formData.with("order_no", data.orderNo)
        }
        if (!data.jobNo.isNullOrEmpty()) {
            formData.with("job_no", data.jobNo)
        }
        if (!data.poNo.isNullOrEmpty()) {
            formData.with("po_no", data.poNo)
        }
        if (!data.invoiceNo.isNullOrEmpty()) {
            formData.with("invoice_no", data.invoiceNo)
        }
        if (data.supplierId != null) {
            formData.with("supplier_id", data.supplierId.toString())
        }
        if (!data.lotNo.isNullOrEmpty()) {
            formData.with("lot_no", data.lotNo)
        }
        if (!data.remark.isNullOrEmpty()) {
            formData.with("remark", data.remark)
        }
        if (data.extraAttributes != null) {
            formData.with("extra_attributes", Json.encodeToString(data.extraAttributes))
        }
        if (!data.productType.isNullOrEmpty()) {
            formData.with("doc_product_type", data.productType)
        }

        val response = webClient.post()
            .uri("wfm/good.movement/edit")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)
            .body(formData)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!

        return renderResponse(response)
    }

    fun removeGoodMovement(sessionId: String, goodMovementId: Int): ResultOf<String> {
        val response = webClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("wfm/good.movement/delete")
                    .queryParam("id", goodMovementId.toString())
                uriBuilder.build()
            }
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!

        return if (response.statusCode.is2xxSuccessful) {
            ResultOf.Success("")
        } else {
            ResultOf.Failure(message = response.body!!)
        }
    }

    fun approveGoodMovement(sessionId: String, goodMovementId: Int): ResultOf<String> {
        val response = webClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("wfm/good.movement/approve")
                    .queryParam("id", goodMovementId.toString())
                    .queryParam("type", "approve")
                uriBuilder.build()
            }
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!

        return renderResponse(response)
    }

    fun addReferenceGoodMovement(sessionId: String, goodReceiptGoodMovementId: Int, pickingOrderGoodMovementIds: List<Int>): ResultOf<String> {
        val response = webClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("wfm/good.movement/add.ref.doc")
                    .queryParam("ref_good_movement_id", goodReceiptGoodMovementId.toString())
                    .queryParam("ids", pickingOrderGoodMovementIds.map { it.toString() }.joinToString(","))
                uriBuilder.build()
            }
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!

        return renderResponse(response)
    }

    fun removeReferenceGoodMovement(sessionId: String, pickingOrderGoodMovementId: Int): ResultOf<String> {
        val response = webClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("wfm/good.movement/delete.ref.doc")
                    .queryParam("id", pickingOrderGoodMovementId.toString())
                uriBuilder.build()
            }
            .header(API_REQUEST, RESTFUL)
            .cookie(PHP_SESSION, sessionId)
            .retrieve()
            .toEntity(String::class.java)
            .block()!!

        return renderResponse(response)
    }

    private fun renderResponse(response: ResponseEntity<String>): ResultOf<String> {
        return if (response.statusCode.is3xxRedirection) {
            // If WMS execute the request successfully, it will redirect the page to the wfm/good.movement/index page
            // and send 3XX status code back. To cut time of doing redirection stuff we just catch the status code
            // and do no redirection.
            ResultOf.Success("")
        } else {
            val errorResponse = Json.decodeFromString(ErrorResponse.serializer(), response.body!!)
            ResultOf.Failure(message = errorResponse.consolidatedMessage())
        }
    }

    private fun reformatDate(dateStr: String): String {
        val date = LocalDate.parse(dateStr, DATA_FORMAT)
        return date.format(WMS_FORMAT)
    }

    data class ItemMovementEntry(
        val goodMovementType: GoodMovementType,
        val goodMovementId: Int, // Goods movement id
        val skuId: Int, // SKU id
        val sku: String? = null,
        val storeLocationId: Int,  // Store location id
        val storeLocation: String? = null,
        val manufacturingLineId: Int?,
        var lotNo: String? = null,
        var lotNoId: Int? = null,
        val qty: BigDecimal, // Quantity
        val refCode: String? = null, // Reference code
        val remark: String? = null, // Remark
        val additionalFieldId: Int? = null,
        val additionalFields: Map<String, String>? = null
    ) {

        // Sample payload
        //    {
        //        "good_movement_id": 203,
        //        "sku_id": 33884,
        //        "store_location_id": 1,
        //        "qty": 1,
        //        "ref_code": "test-01",
        //        "remark": "re-test-01"
        //    },
        fun toWmsJsonString(): String {
            val json = StringBuilder("{")
            json.apply {
                append("\"good_movement_id\": $goodMovementId,")
                append("\"sku_id\": $skuId,")
                append("\"store_location_id\": $storeLocationId,")
                append("\"qty\": $qty,")
                if (refCode != null) {
                    append("\"ref_code\": \"$refCode\",")
                }
                if (remark != null) {
                    append("\"remark\": \"$remark\",")
                }
                if (additionalFieldId != null) {
                    append("\"additional_field_id\": $additionalFieldId,")
                }
                additionalFields?.forEach { (key, value) ->
                    append("\"$key\": \"$value\",")
                }
                deleteCharAt(json.length - 1)
                append("}")
            }
            return json.toString().trimIndent()
        }
    }


}
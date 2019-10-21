package com.example.api.places

import com.example.api.places.common.db.PlaceRepo
import com.example.api.places.common.rest.mutation.Mutations
import com.example.api.places.common.rest.mutation.toRecord
import com.example.api.places.common.rest.response.ListResponseDto
import com.example.api.places.common.rest.response.PlaceDto
import com.example.api.places.common.rest.response.toPlaceDto
import mu.KLogging
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

private const val API_BASE_URI = "/api/places"

@RestController
class PlacesApiController(
        private val repo: PlaceRepo
) {
    companion object : KLogging()

    @GetMapping(API_BASE_URI)
    @Transactional(readOnly = true)
    fun findAll(): ListResponseDto<PlaceDto> = repo
            .findAll(isActive = true)
            .map { it.toPlaceDto() }
            .let { ListResponseDto(items = it) }

    @PutMapping(API_BASE_URI)
    @Transactional(readOnly = false)
    fun create(@RequestBody req: Mutations.CreatePlace): PlaceDto = req
            .toRecord(placeId = UUID.randomUUID(), now = Instant.now())
            .let(repo::insert)
            .also { logger.info { "INSERT DB ENTITY: $it" } }
            .toPlaceDto()

    @DeleteMapping("$API_BASE_URI/{placeId}")
    @Transactional(readOnly = false)
    fun softDelete(@PathVariable("placeId") placeId: UUID): PlaceDto = repo
            .softDeleteById(placeId = placeId, deletedAt = Instant.now())
            .also { logger.info { "SOFT DELETE DB ENTITY: $it" } }
            .toPlaceDto()

    @PostMapping("$API_BASE_URI/{placeId}/restore")
    @Transactional(readOnly = false)
    fun softRestore(@PathVariable("placeId") placeId: UUID): PlaceDto = repo
            .softRestoreById(placeId = placeId)
            .also { logger.info { "SOFT RESTORE DB ENTITY: $it" } }
            .toPlaceDto()
}





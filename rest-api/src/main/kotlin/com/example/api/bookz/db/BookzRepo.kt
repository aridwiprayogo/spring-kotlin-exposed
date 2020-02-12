package com.example.api.bookz.db

import com.example.util.exposed.crud.UUIDCrudRepo
import com.example.util.exposed.crud.UUIDCrudTable
import com.example.util.exposed.crud.updateRowById
import mu.KLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*


private typealias CrudTable = BookzTable
private typealias CrudRecord = BookzRecord

@Repository
@Transactional // Should be at @Service level in real applications
class BookzRepo : UUIDCrudRepo<UUIDCrudTable, CrudRecord>() {
    companion object : KLogging()

    override val table = CrudTable
    override val mapr: (ResultRow) -> CrudRecord = ResultRow::toBookzRecord

    fun newRecord(id: UUID, data: BookzData, now: Instant): CrudRecord =
            CrudRecord(
                    id = id,
                    createdAt = now,
                    modifiedAt = now,
                    isActive = true,
                    data = data
            )

    fun insert(record: BookzRecord): BookzRecord = table
            .insert {
                it[id] = record.crudRecordId()
                it[createdAt] = record.createdAt
                it[modifiedAt] = record.modifiedAt
                it[isActive] = record.isActive
                it[data] = record.data
            }
            .let { this[record.crudRecordId()] }
            .also { logger.info { "INSERT: table: ${table.tableName} id: ${record.crudRecordId()} record: $it" } }

    fun update(record: BookzRecord): BookzRecord = table
            .update({ table.id eq record.id }) {
                it[id] = record.crudRecordId()
                it[createdAt] = record.createdAt
                it[modifiedAt] = record.modifiedAt
                it[isActive] = record.isActive
                it[data] = record.data
            }
            .let { this[record.crudRecordId()] }
            .also { logger.info { "UPDATE: table: ${table.tableName} id: ${record.crudRecordId()} record: $it" } }

    fun updateOne(id: UUID, body: CrudTable.(UpdateStatement) -> Unit) =
            table.updateRowById(id, body = body)
                    .let { this[id] }

    fun insertOrUpdate(insertRecord: CrudRecord, updateStatement: CrudTable.(UpdateStatement) -> Unit): CrudRecord {
        return when (val oldRecordId = findOne(insertRecord.crudRecordId())?.crudRecordId()) {
            null -> insert(record = insertRecord)
            else -> updateOne(oldRecordId, updateStatement)
        }
    }

    fun findAll() =
            table.selectAll().map { mapr(it) }

    fun findAllActive() =
            table.select { table.isActive eq true }.map { mapr(it) }

}

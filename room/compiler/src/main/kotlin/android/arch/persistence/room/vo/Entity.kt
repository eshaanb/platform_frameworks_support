/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.arch.persistence.room.vo

import android.arch.persistence.room.migration.bundle.BundleUtil
import android.arch.persistence.room.migration.bundle.EntityBundle
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

// TODO make data class when move to kotlin 1.1
class Entity(element: TypeElement, val tableName: String, type: DeclaredType,
             fields: List<Field>, embeddedFields: List<EmbeddedField>,
             val primaryKey: PrimaryKey, val indices: List<Index>,
             val foreignKeys: List<ForeignKey>,
             constructor: Constructor?)
    : Pojo(element, type, fields, embeddedFields, emptyList(), constructor) {

    val createTableQuery by lazy {
        createTableQuery(tableName)
    }

    fun createTableQuery(tableName : String) : String {
        val definitions = (fields.map {
            val autoIncrement = primaryKey.autoGenerateId && primaryKey.fields.contains(it)
            it.databaseDefinition(autoIncrement)
        } + createPrimaryKeyDefinition() + createForeignKeyDefinitions()).filterNotNull()
        return "CREATE TABLE IF NOT EXISTS `$tableName` (${definitions.joinToString(", ")})"
    }

    private fun createForeignKeyDefinitions() : List<String> {
        return foreignKeys.map { it.databaseDefinition() }
    }

    private fun createPrimaryKeyDefinition(): String? {
        return if (primaryKey.fields.isEmpty() || primaryKey.autoGenerateId) {
            null
        } else {
            val keys = primaryKey.fields
                    .map { "`${it.columnName}`" }
                    .joinToString(", ")
            "PRIMARY KEY($keys)"
        }
    }

    fun toBundle(): EntityBundle = EntityBundle(
            tableName,
            createTableQuery(BundleUtil.TABLE_NAME_PLACEHOLDER),
            fields.map {it.toBundle()},
            primaryKey.toBundle(),
            indices.map { it.toBundle() },
            foreignKeys.map { it.toBundle() })

    fun isUnique(columns: List<String>) : Boolean {
        return if (primaryKey.columnNames.size == columns.size
                && primaryKey.columnNames.containsAll(columns)) {
            true
        } else {
            indices.any { index ->
                index.unique
                        && index.fields.size == columns.size
                        && index.columnNames.containsAll(columns)
            }
        }
    }
}

package com.kinvey.bookshelf.entity

import com.google.api.client.json.GenericJson
import com.google.api.client.util.Key

data class Author(
    @Key
    var name: String? = null,
    @Key
    var age: Int? = null
) : GenericJson()
package com.kinvey.bookshelf.entity

import com.google.api.client.json.GenericJson
import com.google.api.client.util.Key

/**
 * Created by Prots on 3/15/16.
 */
data class Book(
    @Key
    var name: String? = null,
    @Key
    var imageId: String? = null,
    @Key
    var author: Author? = null
) : GenericJson()
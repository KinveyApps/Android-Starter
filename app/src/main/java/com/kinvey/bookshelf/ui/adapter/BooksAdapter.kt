package com.kinvey.bookshelf.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.kinvey.bookshelf.R.layout
import com.kinvey.bookshelf.entity.Book

/**
 * Created by Prots on 3/16/16.
 */
class BooksAdapter(private val books: List<Book>, private val context: Context) : BaseAdapter() {
    override fun getCount(): Int {
        return books.size
    }

    override fun getItem(position: Int): Book {
        return books[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(layout.book_item, parent, false)
        }
        val tv = view as TextView
        val book = books[position]
        tv.text = book.name
        return tv
    }
}
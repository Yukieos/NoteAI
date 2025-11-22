//这个就是database
package com.example.noteai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Note::class, Tag::class, NoteTagCross::class],
    version = 2, //添加了tag颜色字段
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    //这个是为了保证整个app只有一个数据库实例
    companion object {
        //这个volatile则是为了保证多线程环境下instance的可见性
        @Volatile
        private var instance: NoteDatabase? = null

        //double check一下避免重复创建数据库
        fun getInstance(context: Context): NoteDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_ai.db"
                ).fallbackToDestructiveMigration() //失败就直接删除重建好了
                    .build()
                    .also { instance = it }
            }
        }
    }
}
